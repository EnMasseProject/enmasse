/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/enmasseproject/enmasse/pkg/util/ext"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"github.com/ghodss/yaml"
	routev1 "github.com/openshift/api/route/v1"
	"k8s.io/apimachinery/pkg/util/intstr"
	"strconv"

	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

// this aligns with the Java side properties
type JdbcProperties struct {
	URL             string `json:"url,omitempty"`
	DriverClass     string `json:"driverClass,omitempty"`
	Username        string `json:"username,omitempty"`
	Password        string `json:"password,omitempty"`
	MaximumPoolSize uint32 `json:"maximumPoolSize,omitempty"`

	TableName string `json:"tableName,omitempty"`
}

type JdbcDeviceProperties struct {
	JdbcProperties

	Mode string `json:"mode,omitempty"`
}

func (r *ReconcileIoTConfig) processJdbcDeviceRegistry(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	change := cchange.NewRecorder()

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameDeviceRegistry+"-config", config, false, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {
			return r.reconcileJdbcDeviceRegistryConfigMap(config, configMap, change)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameDeviceRegistry, config, false, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
			return r.reconcileJdbcDeviceRegistryDeployment(config, deployment, change)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameDeviceRegistry, config, false, r.reconcileJdbcDeviceRegistryService)
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameDeviceRegistry+"-metrics", config, false, r.reconcileMetricsService(nameDeviceRegistry))
	})
	if !util.IsOpenshift() {
		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameDeviceRegistry+"-external", config, false, r.reconcileJdbcDeviceRegistryServiceExternal)
		})
	}

	if util.IsOpenshift() {
		routesEnabled := config.WantDefaultRoutes(nil)

		rc.ProcessSimple(func() error {
			endpoint := config.Status.Services["deviceRegistry"]
			err := r.processRoute(ctx, routeDeviceRegistry, config, !routesEnabled, &endpoint.Endpoint, r.reconcileJdbcDeviceRegistryRoute)
			config.Status.Services["deviceRegistry"] = endpoint
			return err
		})
	}

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileJdbcDeviceRegistryDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)
	deployment.Annotations[RegistryTypeAnnotation] = "jdbc"
	deployment.Annotations[util.ConnectsTo] = "iot-auth-service"
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-device-registry"
	deployment.Spec.Template.Annotations[RegistryTypeAnnotation] = "jdbc"

	service := config.Spec.ServicesConfig.DeviceRegistry
	applyDefaultDeploymentConfig(deployment, service.ServiceConfig, change)

	devices, _, err := JdbcConnections(config)
	if err != nil {
		return err
	}

	deployment.Annotations[RegistryJdbcModeAnnotation] = devices.Mode
	deployment.Spec.Template.Annotations[RegistryJdbcModeAnnotation] = devices.Mode

	var tracingContainer *corev1.Container
	err = install.ApplyDeploymentContainerWithError(deployment, "device-registry", func(container *corev1.Container) error {

		tracingContainer = container

		if err := install.SetContainerImage(container, "iot-device-registry-jdbc", config); err != nil {
			return err
		}

		container.Args = nil

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

		// eval native TLS flag

		var nativeTls bool
		if service.JDBC != nil {
			nativeTls = service.JDBC.IsNativeTlsRequired(config)
		} else {
			nativeTls = false
		}

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: ""},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: &corev1.EnvVarSource{FieldRef: &corev1.ObjectFieldSelector{FieldPath: "metadata.namespace"}}},

			{Name: "HONO_AUTH_HOST", Value: FullHostNameForEnvVar("iot-auth-service")},
			{Name: "HONO_AUTH_VALIDATION_SHARED_SECRET", Value: *config.Status.AuthenticationServicePSK},

			{Name: "HONO_REGISTRY_SVC_SIGNING_SHARED_SECRET", Value: *config.Status.AuthenticationServicePSK},
			{Name: "ENMASSE_IOT_REGISTRY_AMQP_NATIVE_TLS_REQUIRED", Value: strconv.FormatBool(nativeTls)},
			{Name: "ENMASSE_IOT_REGISTRY_REST_AUTH_TOKEN_CACHE_EXPIRATION", Value: service.JDBC.Management.AuthTokenCacheExpiration},
		}

		AppendStandardHonoJavaOptions(container)

		// append trust stores

		if err := AppendTrustStores(config, container, []string{"HONO_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)
		install.DropVolumeMount(container, "registry")

		// extensions
		ext.MapExtensionVolume(container)

		// apply container options

		if service.JDBC != nil {
			applyContainerConfig(container, service.JDBC.Container)
		}

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// extension containers

	if service.JDBC.Server.External != nil {
		if err := ext.AddExtensionContainers(service.JDBC.Server.External.Extensions, &deployment.Spec.Template.Spec); err != nil {
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

	if err := ApplyInterServiceForDeployment(config, deployment, nameDeviceRegistry); err != nil {
		return err
	}

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileJdbcDeviceRegistryService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	if len(service.Spec.Ports) != 2 {
		service.Spec.Ports = make([]corev1.ServicePort, 2)
	}

	// AMQPS port

	service.Spec.Ports[0].Name = "amqps"
	service.Spec.Ports[0].Port = 5671
	service.Spec.Ports[0].TargetPort = intstr.FromInt(5671)
	service.Spec.Ports[0].Protocol = corev1.ProtocolTCP

	// HTTP port

	service.Spec.Ports[1].Name = "https"
	service.Spec.Ports[1].Port = 8443
	service.Spec.Ports[1].TargetPort = intstr.FromInt(8443)
	service.Spec.Ports[1].Protocol = corev1.ProtocolTCP

	// annotations

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, nameDeviceRegistry); err != nil {
		return err
	}

	return nil
}

func firstString(strings ...string) string {
	for _, s := range strings {
		if s != "" {
			return s
		}
	}
	return ""
}

func transfer(devices *JdbcProperties, external *iotv1alpha1.ExternalJdbcServer, service iotv1alpha1.ExternalJdbcService) {

	devices.URL = firstString(service.URL, external.URL)
	devices.DriverClass = firstString(service.DriverClass, external.DriverClass)
	devices.Username = firstString(service.Username, external.Username)
	devices.Password = firstString(service.Password, external.Password)

	devices.MaximumPoolSize = service.MaximumPoolSize
	if devices.MaximumPoolSize <= 0 {
		devices.MaximumPoolSize = external.MaximumPoolSize
	}

}

func JdbcConnections(config *iotv1alpha1.IoTConfig) (*JdbcDeviceProperties, *JdbcProperties, error) {

	// apply JDBC server options
	service := config.Spec.ServicesConfig.DeviceRegistry

	// app config sections
	devices := JdbcDeviceProperties{}
	deviceInformation := JdbcProperties{}

	if service.JDBC.Server.External != nil {

		external := service.JDBC.Server.External
		transfer(&devices.JdbcProperties, external, external.Devices.ExternalJdbcService)
		transfer(&deviceInformation, external, external.DeviceInformation.ExternalJdbcService)

		devices.TableName = external.Devices.TableName
		deviceInformation.TableName = external.DeviceInformation.TableName

		devices.Mode = external.Devices.Mode

		return &devices, &deviceInformation, nil

	} else {
		return nil, nil, fmt.Errorf("JDBC backend server configuration missing")
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

	// app config sections
	devices, deviceInformation, err := JdbcConnections(config)
	if err != nil {
		return err
	}

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
					"jdbc": map[string]interface{}{
						"devices":           devices,
						"deviceInformation": deviceInformation,
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
				"credentials": map[string]interface{}{
					"credentials": map[string]interface{}{
						"svc": map[string]interface{}{
							"maxBcryptIterations": 10,
						},
					},
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

func (r *ReconcileIoTConfig) reconcileJdbcDeviceRegistryRoute(config *iotv1alpha1.IoTConfig, route *routev1.Route, endpointStatus *iotv1alpha1.EndpointStatus) error {

	install.ApplyDefaultLabels(&route.ObjectMeta, "iot", route.Name)

	// Port

	route.Spec.Port = &routev1.RoutePort{
		TargetPort: intstr.FromString("https"),
	}

	// Path

	route.Spec.Path = ""

	// TLS

	if route.Spec.TLS == nil {
		route.Spec.TLS = &routev1.TLSConfig{}
	}

	route.Spec.TLS.Termination = routev1.TLSTerminationReencrypt
	route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone

	// Service

	route.Spec.To.Kind = "Service"
	route.Spec.To.Name = nameDeviceRegistry

	// Update endpoint

	updateEndpointStatus("https", false, route, endpointStatus)

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileJdbcDeviceRegistryServiceExternal(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	if len(service.Spec.Ports) != 1 {
		service.Spec.Ports = make([]corev1.ServicePort, 1)
	}

	service.Spec.Ports[0].Name = "https"
	service.Spec.Ports[0].Port = 31443
	service.Spec.Ports[0].TargetPort = intstr.FromInt(8443)
	service.Spec.Ports[0].Protocol = corev1.ProtocolTCP

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, nameDeviceRegistry); err != nil {
		return err
	}

	service.Spec.Type = "LoadBalancer"
	service.Spec.Selector["name"] = nameDeviceRegistry

	return nil
}
