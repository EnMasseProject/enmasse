/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"strconv"

	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	"k8s.io/api/core/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

func (r *ReconcileIoTConfig) processInfinispanDeviceConnection(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameDeviceConnection+"-config", config, false, r.reconcileInfinispanDeviceConnectionConfigMap)
	})
	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameDeviceConnection, config, false, r.reconcileInfinispanDeviceConnectionDeployment)
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileInfinispanDeviceConnectionDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)
	deployment.Annotations[DeviceConnectionTypeAnnotation] = "infinispan"
	deployment.Annotations[util.ConnectsTo] = "iot-auth-service"
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-device-connection"
	deployment.Spec.Template.Annotations[DeviceConnectionTypeAnnotation] = "infinispan"

	service := config.Spec.ServicesConfig.DeviceConnection
	applyDefaultDeploymentConfig(deployment, service.Infinispan.ServiceConfig, nil)

	var tracingContainer *corev1.Container
	err := install.ApplyDeploymentContainerWithError(deployment, "device-connection", func(container *corev1.Container) error {

		tracingContainer = container

		// we indeed re-use the device registry image here
		if err := install.SetContainerImage(container, "iot-device-connection-infinispan", config); err != nil {
			return err
		}

		container.Args = []string{"/iot-device-registry-infinispan.jar"}

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
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: "device-connection"},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: install.FromFieldNamespace()},

			{Name: "HONO_AUTH_HOST", Value: FullHostNameForEnvVar("iot-auth-service")},
			{Name: "HONO_AUTH_VALIDATION_SHARED_SECRET", Value: *config.Status.AuthenticationServicePSK},
		}

		appendCommonHonoJavaEnv(container, "ENMASSE_IOT_AMQP_", config, &service.Infinispan.CommonServiceConfig)

		SetupTracing(config, deployment, container)
		AppendStandardHonoJavaOptions(container)

		// append trust stores

		if err := AppendTrustStores(config, container, []string{"HONO_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)

		// apply container options

		if service.Infinispan != nil {
			applyContainerConfig(container, service.Infinispan.Container)
		}

		// apply infinispan server options

		if service.Infinispan.Server.External != nil {
			if err := appendInfinispanExternalConnectionServer(container, config.Spec.ServicesConfig.DeviceConnection.Infinispan.Server.External); err != nil {
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

	SetupTracing(config, deployment, tracingContainer)

	// volumes

	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "config", nameDeviceConnection+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(r.client, config, deployment, tlsServiceKeyVolumeName, nameDeviceConnection); err != nil {
		return err
	}

	// return

	return nil
}

func appendInfinispanExternalConnectionServer(container *v1.Container, external *iotv1alpha1.ExternalInfinispanDeviceConnectionServer) error {

	// basic connection

	install.ApplyEnvSimple(container, "ENMASSE_IOT_DEVICECONNECTION_INFINISPAN_HOST", external.Host)
	install.ApplyEnvSimple(container, "ENMASSE_IOT_DEVICECONNECTION_INFINISPAN_PORT", strconv.Itoa(int(external.Port)))
	install.ApplyEnvSimple(container, "ENMASSE_IOT_DEVICECONNECTION_INFINISPAN_USERNAME", external.Username)
	install.ApplyEnvSimple(container, "ENMASSE_IOT_DEVICECONNECTION_INFINISPAN_PASSWORD", external.Password)

	// SASL

	install.ApplyOrRemoveEnvSimple(container, "ENMASSE_IOT_DEVICECONNECTION_INFINISPAN_SASLSERVERNAME", external.SaslServerName)
	install.ApplyOrRemoveEnvSimple(container, "ENMASSE_IOT_DEVICECONNECTION_INFINISPAN_SASLREALM", external.SaslRealm)

	// cache names

	deviceStates := ""
	if external.CacheNames != nil {
		deviceStates = external.CacheNames.DeviceConnections
	}

	install.ApplyOrRemoveEnvSimple(container, "HONO_DEVICECONNECTION_COMMON_CACHE_NAME", deviceStates)

	// done

	return nil
}

func (r *ReconcileIoTConfig) reconcileInfinispanDeviceConnectionConfigMap(_ *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	if configMap.Data["logback-spring.xml"] == "" {
		configMap.Data["logback-spring.xml"] = DefaultLogbackConfig
	}

	configMap.Data["application.yml"] = `
hono:

  auth:
    port: 5671
    keyPath: /etc/tls/tls.key
    certPath: /etc/tls/tls.crt
    keyFormat: PEM
    trustStorePath: /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt
    trustStoreFormat: PEM

enmasse:
  iot:

    app:
      maxInstances: 1

    vertx:
      preferNative: true

    healthCheck:
      insecurePortBindAddress: 0.0.0.0
      startupTimeout: 90

    amqp:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls/tls.key
      certPath: /etc/tls/tls.crt
      keyFormat: PEM

    rest:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls/tls.key
      certPath: /etc/tls/tls.crt
      keyFormat: PEM
`
	return nil
}
