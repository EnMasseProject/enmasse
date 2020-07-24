/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"strconv"

	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	"k8s.io/api/core/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
)

func (r *ReconcileIoTInfrastructure) processInfinispanDeviceConnection(ctx context.Context, infra *iotv1.IoTInfrastructure, authServicePsk *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	service := infra.Spec.ServicesConfig.DeviceConnection.Infinispan

	rc := &recon.ReconcileContext{}
	change := cchange.NewRecorder()

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameDeviceConnection+"-config", infra, false, func(config *iotv1.IoTInfrastructure, configMap *corev1.ConfigMap) error {
			return r.reconcileInfinispanDeviceConnectionConfigMap(config, service, configMap, change)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameDeviceConnection, infra, false, func(config *iotv1.IoTInfrastructure, deployment *appsv1.Deployment) error {
			return r.reconcileInfinispanDeviceConnectionDeployment(config, deployment, change, authServicePsk)
		})
	})

	return rc.Result()
}

func (r *ReconcileIoTInfrastructure) reconcileInfinispanDeviceConnectionDeployment(infra *iotv1.IoTInfrastructure, deployment *appsv1.Deployment, change *cchange.ConfigChangeRecorder, authServicePsk *cchange.ConfigChangeRecorder) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)
	deployment.Annotations[DeviceConnectionTypeAnnotation] = "infinispan"
	deployment.Annotations[util.ConnectsTo] = "iot-auth-service"
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-device-connection"
	deployment.Spec.Template.Annotations[DeviceConnectionTypeAnnotation] = "infinispan"

	service := infra.Spec.ServicesConfig.DeviceConnection
	applyDefaultDeploymentConfig(deployment, service.Infinispan.ServiceConfig, change)
	cchange.ApplyTo(authServicePsk, "iot.enmasse.io/auth-psk-hash", &deployment.Spec.Template.Annotations)

	var tracingContainer *corev1.Container
	err := install.ApplyDeploymentContainerWithError(deployment, "device-connection", func(container *corev1.Container) error {

		tracingContainer = container

		// we indeed re-use the device registry image here
		if err := install.SetContainerImage(container, "iot-device-connection-infinispan", infra); err != nil {
			return err
		}

		container.Args = []string{"/iot-device-connection-infinispan.jar"}
		container.Command = nil

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(512*1024*1024 /* 512Mi */, resource.BinarySI),
			},
		}

		container.Ports = []corev1.ContainerPort{
			{Name: "amqps", ContainerPort: 5671, Protocol: corev1.ProtocolTCP},
		}

		container.Ports = appendHonoStandardPorts(container.Ports)
		SetHonoProbes(container)

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/infra/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: "device-connection"},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/infra/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: install.FromFieldNamespace()},

			{Name: "HONO_AUTH_HOST", Value: FullHostNameForEnvVar("iot-auth-service")},
			{Name: "HONO_AUTH_VALIDATION_SHARED_SECRET", ValueFrom: install.FromSecret(nameAuthServicePskSecret, keyInterServicePsk)},
		}

		applyServiceConnectionOptions(container, "HONO_AUTH", infra.Spec.ServicesConfig.Authentication.TlsVersions(infra))
		appendCommonHonoJavaEnv(container, "HONO_DEVICECONNECTION_AMQP_", infra, &service.Infinispan.CommonServiceConfig)

		SetupTracing(infra, deployment, container)
		AppendStandardHonoJavaOptions(container)

		// append trust stores

		if err := AppendTrustStores(infra, container, []string{"HONO_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "infra", "/etc/infra", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)

		// apply container options

		if service.Infinispan != nil {
			applyContainerConfig(container, service.Infinispan.Container.ContainerConfig)
		}

		// apply infinispan server options

		if service.Infinispan.Server.External != nil {
			if err := appendInfinispanExternalConnectionServer(container, infra.Spec.ServicesConfig.DeviceConnection.Infinispan.Server.External); err != nil {
				return err
			}
		} else {
			return util.NewConfigurationError("infinispan backend server configuration missing")
		}

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// reset init containers

	deployment.Spec.Template.Spec.InitContainers = nil

	// tracing

	SetupTracing(infra, deployment, tracingContainer)

	// volumes

	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "infra", nameDeviceConnection+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(r.client, infra, deployment, tlsServiceKeyVolumeName, nameDeviceConnection); err != nil {
		return err
	}

	// return

	return nil
}

func appendInfinispanExternalConnectionServer(container *v1.Container, external *iotv1.ExternalInfinispanDeviceConnectionServer) error {

	// basic connection

	install.ApplyEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_SERVERLIST", external.Host+":"+strconv.Itoa(int(external.Port)))
	install.ApplyEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_AUTHUSERNAME", external.Username)
	install.ApplyEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_AUTHPASSWORD", external.Password)

	// SASL

	install.ApplyOrRemoveEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_AUTHSERVERNAME", external.SaslServerName)
	install.ApplyOrRemoveEnvSimple(container, "HONO_DEVICECONNECTION_REMOTE_AUTHREALM", external.SaslRealm)

	// cache names

	deviceStates := ""
	if external.CacheNames != nil {
		deviceStates = external.CacheNames.DeviceConnections
	}

	install.ApplyOrRemoveEnvSimple(container, "HONO_DEVICECONNECTION_COMMON_CACHENAME", deviceStates)

	// done

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileInfinispanDeviceConnectionConfigMap(infra *iotv1.IoTInfrastructure, service *iotv1.InfinispanDeviceConnection, configMap *corev1.ConfigMap, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	configMap.Data["logback-spring.xml"] = service.RenderConfiguration(infra, logbackDefault, configMap.Data["logback-custom.xml"])

	configMap.Data["application.yml"] = `
hono:

  auth:
    port: 5671
    keyPath: /etc/tls/tls.key
    certPath: /etc/tls/tls.crt
    keyFormat: PEM
    trustStorePath: /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt
    trustStoreFormat: PEM

  app:
    maxInstances: 1

  vertx:
    preferNative: true

  healthCheck:
    insecurePortBindAddress: 0.0.0.0
    startupTimeout: 90

  deviceConnection:
    amqp:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls/tls.key
      certPath: /etc/tls/tls.crt
      keyFormat: PEM
`

	change.AddStringsFromMap(configMap.Data, "application.yml", "logback-spring.xml")

	return nil
}
