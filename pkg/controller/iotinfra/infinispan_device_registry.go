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

func (r *ReconcileIoTInfrastructure) processInfinispanDeviceRegistry(ctx context.Context, infra *iotv1.IoTInfrastructure, authServicePsk *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	service := infra.Spec.ServicesConfig.DeviceRegistry.Infinispan

	rc := &recon.ReconcileContext{}
	change := cchange.NewRecorder()

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameDeviceRegistry+"-config", infra, false, func(infra *iotv1.IoTInfrastructure, configMap *corev1.ConfigMap) error {
			return r.reconcileInfinispanDeviceRegistryConfigMap(infra, service, configMap, change)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameDeviceRegistry, infra, false, func(infra *iotv1.IoTInfrastructure, deployment *appsv1.Deployment) error {
			return r.reconcileInfinispanDeviceRegistryDeployment(infra, deployment, change, authServicePsk)
		})
	})

	return rc.Result()
}

func (r *ReconcileIoTInfrastructure) reconcileInfinispanDeviceRegistryDeployment(infra *iotv1.IoTInfrastructure, deployment *appsv1.Deployment, change *cchange.ConfigChangeRecorder, authServicePsk *cchange.ConfigChangeRecorder) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)
	deployment.Annotations[RegistryTypeAnnotation] = "infinispan"
	deployment.Annotations[util.ConnectsTo] = "iot-auth-service"
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-device-registry"
	deployment.Spec.Template.Annotations[RegistryTypeAnnotation] = "infinispan"

	service := infra.Spec.ServicesConfig.DeviceRegistry
	applyDefaultDeploymentConfig(deployment, service.Infinispan.ServiceConfig, change)
	cchange.ApplyTo(authServicePsk, "iot.enmasse.io/auth-psk-hash", &deployment.Spec.Template.Annotations)

	var tracingContainer *corev1.Container
	err := install.ApplyDeploymentContainerWithError(deployment, "device-registry", func(container *corev1.Container) error {

		tracingContainer = container

		if err := install.SetContainerImage(container, "iot-device-registry-infinispan", infra); err != nil {
			return err
		}

		container.Args = []string{"/iot-device-registry-infinispan.jar"}
		container.Command = nil

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(512*1024*1024 /* 512Mi */, resource.BinarySI),
			},
		}

		container.Ports = []corev1.ContainerPort{
			{Name: "amqps", ContainerPort: 5671, Protocol: corev1.ProtocolTCP},
			{Name: "http", ContainerPort: 8080, Protocol: corev1.ProtocolTCP},
			{Name: "https", ContainerPort: 8443, Protocol: corev1.ProtocolTCP},
		}

		container.Ports = appendHonoStandardPorts(container.Ports)
		SetHonoProbes(container)

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/infra/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: "device-registry"},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/infra/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: install.FromFieldNamespace()},

			{Name: "HONO_AUTH_HOST", Value: FullHostNameForEnvVar("iot-auth-service")},
			{Name: "HONO_AUTH_VALIDATION_SHARED_SECRET", ValueFrom: install.FromSecret(nameAuthServicePskSecret, keyInterServicePsk)},

			{Name: "ENMASSE_IOT_REST_AUTH_TOKEN_CACHE_EXPIRATION", Value: service.Infinispan.Management.AuthTokenCacheExpiration},
		}

		applyServiceConnectionOptions(container, "HONO_AUTH", infra.Spec.ServicesConfig.Authentication.TlsVersions(infra))
		appendCommonHonoJavaEnv(container, "ENMASSE_IOT_AMQP_", infra, &service.Infinispan.CommonServiceConfig)
		appendCommonHonoJavaEnv(container, "ENMASSE_IOT_REST_", infra, &service.Infinispan.CommonServiceConfig)

		SetupTracing(infra, deployment, container)
		AppendStandardHonoJavaOptions(container)

		// append trust stores

		if err := AppendTrustStores(infra, container, []string{"HONO_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "infra", "/etc/infra", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls-internal", true)
		install.ApplyVolumeMountSimple(container, "tls-endpoint", "/etc/tls-external", true)

		// apply container options

		if service.Infinispan != nil {
			applyContainerConfig(container, service.Infinispan.Container.ContainerConfig)
		}

		// apply infinispan server options

		if service.Infinispan.Server.External != nil {
			if err := appendInfinispanExternalServer(container, infra.Spec.ServicesConfig.DeviceRegistry.Infinispan.Server.External); err != nil {
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

	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "infra", nameDeviceRegistry+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(r.client, infra, deployment, tlsServiceKeyVolumeName, nameDeviceRegistry); err != nil {
		return err
	}

	// endpoint

	if err := applyEndpointDeployment(r.client, service.Management.Endpoint, deployment, nameDeviceRegistry, "tls-endpoint"); err != nil {
		return err
	}

	// return

	return nil
}

func appendInfinispanExternalServer(container *v1.Container, external *iotv1.ExternalInfinispanRegistryServer) error {

	// basic connection

	install.ApplyEnvSimple(container, "ENMASSE_IOT_REGISTRY_INFINISPAN_HOST", external.Host)
	install.ApplyEnvSimple(container, "ENMASSE_IOT_REGISTRY_INFINISPAN_PORT", strconv.Itoa(int(external.Port)))
	install.ApplyEnvSimple(container, "ENMASSE_IOT_REGISTRY_INFINISPAN_USERNAME", external.Username)
	install.ApplyEnvSimple(container, "ENMASSE_IOT_REGISTRY_INFINISPAN_PASSWORD", external.Password)

	// SASL

	install.ApplyOrRemoveEnvSimple(container, "ENMASSE_IOT_REGISTRY_INFINISPAN_SASLSERVERNAME", external.SaslServerName)
	install.ApplyOrRemoveEnvSimple(container, "ENMASSE_IOT_REGISTRY_INFINISPAN_SASLREALM", external.SaslRealm)

	// cache names

	adapterCredentials := ""
	devices := ""
	if external.CacheNames != nil {
		adapterCredentials = external.CacheNames.AdapterCredentials
		devices = external.CacheNames.Devices
	}

	install.ApplyOrRemoveEnvSimple(container, "ENMASSE_IOT_REGISTRY_INFINISPAN_CACHENAMES_ADAPTERCREDENTIALS", adapterCredentials)
	install.ApplyOrRemoveEnvSimple(container, "ENMASSE_IOT_REGISTRY_INFINISPAN_CACHENAMES_DEVICES", devices)

	// done

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileInfinispanDeviceRegistryConfigMap(infra *iotv1.IoTInfrastructure, service *iotv1.InfinispanDeviceRegistry, configMap *corev1.ConfigMap, change *cchange.ConfigChangeRecorder) error {

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

enmasse:
  iot:

    app:
      maxInstances: 1

    vertx:
      preferNative: true

    healthCheck:
      insecurePortBindAddress: 0.0.0.0
      startupTimeout: 90

    registry:
      ttl: 1m
      maxBcryptIterations: 10

    amqp:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls-internal/tls.key
      certPath: /etc/tls-internal/tls.crt
      keyFormat: PEM

    rest:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls-external/tls.key
      certPath: /etc/tls-external/tls.crt
      keyFormat: PEM
`

	change.AddStringsFromMap(configMap.Data, "application.yml", "logback-spring.xml")

	return nil
}
