/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package iotconfig

import (
	"context"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/ext"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"github.com/ghodss/yaml"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"

	"k8s.io/apimachinery/pkg/api/resource"

	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

func (r *ReconcileIoTConfig) processJdbcDeviceConnection(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	service := config.Spec.ServicesConfig.DeviceConnection.JDBC

	rc := &recon.ReconcileContext{}

	change := cchange.NewRecorder()

	// configmap

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameDeviceConnection+"-config", config, false, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {
			return r.reconcileJdbcDeviceConnectionConfigMap(config, service, configMap, change)
		})
	})

	// deployment

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameDeviceConnection, config, false, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
			return r.reconcileJdbcDeviceConnectionDeployment(config, deployment, change)
		})
	})

	// done

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileJdbcDeviceConnectionDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)
	deployment.Annotations[DeviceConnectionTypeAnnotation] = "jdbc"
	deployment.Annotations[util.ConnectsTo] = "iot-auth-service"
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-device-connection"
	deployment.Spec.Template.Annotations[DeviceConnectionTypeAnnotation] = "jdbc"

	service := config.Spec.ServicesConfig.DeviceConnection
	applyDefaultDeploymentConfig(deployment, service.JDBC.ServiceConfig, change)

	var tracingContainer *corev1.Container
	err := install.ApplyDeploymentContainerWithError(deployment, "device-connection", func(container *corev1.Container) error {

		tracingContainer = container

		// we indeed re-use the device registry image here
		if err := install.SetContainerImage(container, "iot-device-connection-jdbc", config); err != nil {
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

		appendCommonHonoJavaEnv(container, "ENMASSE_IOT_AMQP_", config, &service.JDBC.CommonServiceConfig)

		SetupTracing(config, deployment, container)
		AppendStandardHonoJavaOptions(container)

		// append trust stores

		if err := AppendTrustStores(config, container, []string{"HONO_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)

		// extensions
		ext.MapExtensionVolume(container)

		// apply container options

		if service.JDBC != nil {
			applyContainerConfig(container, service.JDBC.Container.ContainerConfig)
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

	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "config", nameDeviceConnection+"-config")
	ext.AddExtensionVolume(&deployment.Spec.Template.Spec)

	// inter service secrets

	if err := ApplyInterServiceForDeployment(r.client, config, deployment, tlsServiceKeyVolumeName, nameDeviceConnection); err != nil {
		return err
	}

	// return

	return nil
}

func ExternalJdbcConnectionConnections(config *iotv1alpha1.IoTConfig) (*iotv1alpha1.JdbcConnectionInformation, error) {
	if config.Spec.ServicesConfig.DeviceConnection.JDBC.Server.External != nil {
		return &config.Spec.ServicesConfig.DeviceConnection.JDBC.Server.External.JdbcConnectionInformation, nil
	} else {
		return nil, util.NewConfigurationError("illegal device connection configuration")
	}
}

func (r *ReconcileIoTConfig) reconcileJdbcDeviceConnectionConfigMap(config *iotv1alpha1.IoTConfig, service *iotv1alpha1.JdbcDeviceConnection, configMap *corev1.ConfigMap, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	configMap.Data["logback-spring.xml"] = service.RenderConfiguration(config, logbackDefault, configMap.Data["logback-custom.xml"])

	deviceInformation, err := ExternalJdbcConnectionConnections(config)
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
				"deviceConnection": map[string]interface{}{
					"jdbc": deviceInformation,
				},
				"amqp": map[string]interface{}{
					"bindAddress": "0.0.0.0",
					"keyPath":     "/etc/tls/tls.key",
					"certPath":    "/etc/tls/tls.crt",
					"keyFormat":   "PEM",
				},
				"rest": map[string]interface{}{
					"bindAddress": "0.0.0.0",
					"keyPath":     "/etc/tls/tls.key",
					"certPath":    "/etc/tls/tls.crt",
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

	change.AddStringsFromMap(configMap.Data, "application.yml", "logback-spring.xml")

	// return ok

	return nil
}
