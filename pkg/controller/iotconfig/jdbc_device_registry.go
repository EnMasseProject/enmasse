/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/ext"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"github.com/ghodss/yaml"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

// this aligns with the Java side properties
type JdbcDeviceProperties struct {
	Mode string `json:"mode,omitempty"`

	Adapter    *iotv1alpha1.JdbcConnectionInformation `json:"adapter,omitempty"`
	Management *iotv1alpha1.JdbcConnectionInformation `json:"management,omitempty"`
}

func (r *ReconcileIoTConfig) processJdbcDeviceRegistry(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	change := cchange.NewRecorder()

	// configmap

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameDeviceRegistry+"-config", config, false, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {
			return r.reconcileJdbcDeviceRegistryConfigMap(config, configMap, change)
		})
	})

	// deployment

	if config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Management != nil &&
		config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Adapter == nil {

		// combined deployment

		rc.ProcessSimple(func() error {
			return r.processDeployment(ctx, nameDeviceRegistry, config, false, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
				return r.reconcileCommonJdbcDeviceRegistryDeployment(
					config,
					deployment,
					change,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Management.ServiceConfig,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Management.CommonServiceConfig,
					nameDeviceRegistry,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Mode,
					"registry-adapter,registry-management",
					true, true,
				)
			})
		})

		// delete extra management deployment

		rc.Delete(ctx, r.client, &appsv1.Deployment{ObjectMeta: metav1.ObjectMeta{Namespace: config.Namespace, Name: nameDeviceRegistryManagement}})

	} else if config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Management != nil &&
		config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Adapter != nil {

		// split deployment

		rc.ProcessSimple(func() error {
			return r.processDeployment(ctx, nameDeviceRegistry, config, false, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
				return r.reconcileCommonJdbcDeviceRegistryDeployment(
					config,
					deployment,
					change,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Adapter.ServiceConfig,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Adapter.CommonServiceConfig,
					nameDeviceRegistry,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Mode,
					"registry-adapter",
					true, false,
				)
			})
		})
		rc.ProcessSimple(func() error {
			return r.processDeployment(ctx, nameDeviceRegistryManagement, config, false, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
				return r.reconcileCommonJdbcDeviceRegistryDeployment(
					config,
					deployment,
					change,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Management.ServiceConfig,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Management.CommonServiceConfig,
					nameDeviceRegistryManagement,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Mode,
					"registry-management",
					false, true,
				)
			})
		})

	} else if config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Management == nil &&
		config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Adapter != nil {

		// read-only deployment

		rc.ProcessSimple(func() error {
			return r.processDeployment(ctx, nameDeviceRegistry, config, false, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
				return r.reconcileCommonJdbcDeviceRegistryDeployment(
					config,
					deployment,
					change,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Adapter.ServiceConfig,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Adapter.CommonServiceConfig,
					nameDeviceRegistry,
					config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External.Mode,
					"registry-adapter",
					true, false,
				)
			})
		})

		// delete extra management

		rc.Delete(ctx, r.client, &appsv1.Deployment{ObjectMeta: metav1.ObjectMeta{Namespace: config.Namespace, Name: nameDeviceRegistryManagement}})

	} else {

		return reconcile.Result{}, util.NewConfigurationError("illegal device registry configuration")

	}

	// done

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileCommonJdbcDeviceRegistryDeployment(
	config *iotv1alpha1.IoTConfig,
	deployment *appsv1.Deployment,
	change *cchange.ConfigChangeRecorder,
	serviceConfig iotv1alpha1.ServiceConfig,
	commonConfig iotv1alpha1.CommonServiceConfig,
	serviceNameForTls string,
	mode string,
	profiles string,
	adapter bool,
	management bool,
) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)
	deployment.Annotations[RegistryTypeAnnotation] = "jdbc"
	deployment.Annotations[util.ConnectsTo] = "iot-auth-service"
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-device-registry"
	deployment.Spec.Template.Annotations[RegistryTypeAnnotation] = "jdbc"

	service := config.Spec.ServicesConfig.DeviceRegistry
	applyDefaultDeploymentConfig(deployment, serviceConfig, change)

	deployment.Annotations[RegistryJdbcModeAnnotation] = mode
	deployment.Spec.Template.Annotations[RegistryJdbcModeAnnotation] = mode

	// container

	var tracingContainer *corev1.Container
	err := install.ApplyDeploymentContainerWithError(deployment, "device-registry", func(container *corev1.Container) error {

		tracingContainer = container

		if err := install.SetContainerImage(container, "iot-device-registry-jdbc", config); err != nil {
			return err
		}

		container.Args = []string{"/iot-device-registry-jdbc.jar"}
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
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: profiles},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: install.FromFieldNamespace()},

			{Name: "HONO_AUTH_HOST", Value: FullHostNameForEnvVar("iot-auth-service")},
			{Name: "HONO_AUTH_VALIDATION_SHARED_SECRET", Value: *config.Status.AuthenticationServicePSK},

			{Name: "ENMASSE_IOT_REST_AUTH_TOKEN_CACHE_EXPIRATION", Value: service.JDBC.Management.AuthTokenCacheExpiration},
		}

		appendCommonHonoJavaEnv(container, "ENMASSE_IOT_AMQP_", config, &commonConfig)
		appendCommonHonoJavaEnv(container, "ENMASSE_IOT_REST_", config, &commonConfig)

		AppendStandardHonoJavaOptions(container)

		// append trust stores

		if err := AppendTrustStores(config, container, []string{"HONO_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		if adapter {
			install.ApplyVolumeMountSimple(container, "tls", "/etc/tls-internal", true)
		} else {
			install.DropVolumeMount(container, "tls")
		}
		if management {
			install.ApplyVolumeMountSimple(container, "tls-endpoint", "/etc/tls-external", true)
		} else {
			install.DropVolumeMount(container, "tls-endpoint")
		}
		install.DropVolumeMount(container, "registry")

		// extensions
		ext.MapExtensionVolume(container)

		// apply container options

		if service.JDBC != nil {
			applyContainerConfig(container, commonConfig.Container)
		}

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// extension containers

	if service.JDBC.Server.External != nil {
		if err := ext.AddExtensionContainers(service.JDBC.Server.External.Extensions, &deployment.Spec.Template.Spec, "ext-"); err != nil {
			return err
		}
	} else {
		// we don't support any init containers
		deployment.Spec.Template.Spec.InitContainers = nil
	}

	// tracing

	SetupTracing(config, deployment, tracingContainer)

	// volumes

	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "config", nameDeviceRegistry+"-config")
	install.DropVolume(&deployment.Spec.Template.Spec, "registry")
	ext.AddExtensionVolume(&deployment.Spec.Template.Spec)

	// inter service secrets

	var interServiceName string
	if adapter {
		// only set when we have an internal service (adapter facing service)
		interServiceName = serviceNameForTls
	} else {
		interServiceName = ""
	}
	if err := ApplyInterServiceForDeployment(r.client, config, deployment, tlsServiceKeyVolumeName, interServiceName); err != nil {
		return err
	}

	// endpoint

	if management {
		// if the management part is enabled, set the management endpoint
		if err := applyEndpointDeployment(r.client, service.Management.Endpoint, deployment, serviceNameForTls, "tls-endpoint"); err != nil {
			return err
		}
	}

	// return

	return nil
}

func ExternalJdbcRegistryConnections(config *iotv1alpha1.IoTConfig) (*JdbcDeviceProperties, error) {

	// apply JDBC server options

	service := config.Spec.ServicesConfig.DeviceRegistry.JDBC.Server.External

	if service.Management != nil && service.Adapter == nil {
		// combined mode
		return &JdbcDeviceProperties{
			Adapter:    &service.Management.Connection,
			Management: &service.Management.Connection,
			Mode:       service.Mode,
		}, nil
	} else if service.Management != nil && service.Adapter != nil {
		// split mode
		return &JdbcDeviceProperties{
			Adapter:    &service.Adapter.Connection,
			Management: &service.Management.Connection,
			Mode:       service.Mode,
		}, nil
	} else if service.Management == nil && service.Adapter != nil {
		// read only mode
		return &JdbcDeviceProperties{
			Adapter:    &service.Adapter.Connection,
			Management: nil, // remains nil
			Mode:       service.Mode,
		}, nil
	} else {
		return nil, util.NewConfigurationError("illegal device registry configuration")
	}

}

func (r *ReconcileIoTConfig) reconcileJdbcDeviceRegistryConfigMap(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	if configMap.Data["logback-spring.xml"] == "" {
		configMap.Data["logback-spring.xml"] = DefaultLogbackConfig
	}

	devices, err := ExternalJdbcRegistryConnections(config)
	if err != nil {
		return err
	}

	// app config sections

	app := map[string]interface{}{
		"hono": map[string]interface{}{
			"auth": map[string]interface{}{
				"port":             5671,
				"keyPath":          "/etc/tls/tls.key",
				"certPath":         "/etc/tls/tls.crt",
				"keyFormat":        "PEM",
				"trustStorePath":   "/var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt",
				"trustStoreFormat": "PEM",
			},
		},
		"enmasse": map[string]interface{}{
			"iot": map[string]interface{}{
				"app": map[string]interface{}{
					"maxInstances": 1,
				},
				"vertx": map[string]interface{}{
					"preferNative": true,
				},
				"healthCheck": map[string]interface{}{
					"insecurePortBindAddress": "0.0.0.0",
					"startupTimeout":          90,
				},
				"registry": map[string]interface{}{
					"device": map[string]interface{}{
						"credentials": map[string]interface{}{
							"ttl": "1m",
						},
					},
					"jdbc": devices,
				},
				"credentials": map[string]interface{}{
					"credentials": map[string]interface{}{
						"svc": map[string]interface{}{
							"maxBcryptIterations": 10,
						},
					},
				},
				"amqp": map[string]interface{}{
					"bindAddress": "0.0.0.0",
					"keyPath":     "/etc/tls-internal/tls.key",
					"certPath":    "/etc/tls-internal/tls.crt",
					"keyFormat":   "PEM",
				},
				"rest": map[string]interface{}{
					"bindAddress": "0.0.0.0",
					"keyPath":     "/etc/tls-external/tls.key",
					"certPath":    "/etc/tls-external/tls.crt",
					"keyFormat":   "PEM",
				},
			},
		},
	}

	// encode app config

	appStr, err := yaml.Marshal(app)
	if err != nil {
		return err
	}
	configMap.Data["application.yml"] = string(appStr)

	// config change

	change.AddStringsFromMap(configMap.Data)

	// return ok

	return nil
}
