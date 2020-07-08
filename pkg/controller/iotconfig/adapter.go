/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"encoding/json"
	"fmt"
	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/pkg/errors"
	"k8s.io/apimachinery/pkg/util/intstr"
	"os"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"strconv"
	"strings"

	"github.com/enmasseproject/enmasse/pkg/util/cchange"

	"github.com/enmasseproject/enmasse/pkg/util"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/enmasseproject/enmasse/pkg/util/recon"

	routev1 "github.com/openshift/api/route/v1"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

// the name of the router image key
const imageNameRouter = "router"
const SharedInfraConnectionName = "shared-infra"

type adapterPort struct {
	ContainerPort    int32
	ServicePort      int32
	LoadBalancerPort int32

	Protocol corev1.Protocol

	DoesntSupportReencrypt bool
	EndpointUrlScheme      string
	EndpointUrlForcePort   bool
}

func (a adapterPort) GetProtocol() corev1.Protocol {
	if a.Protocol != "" {
		return a.Protocol
	} else {
		return corev1.ProtocolTCP
	}
}

func (a adapterPort) GetServicePort() int32 {
	if a.ServicePort > 0 {
		return a.ServicePort
	} else {
		return a.ContainerPort
	}
}

func (a adapterPort) GetEndpointUrlScheme() string {
	if a.EndpointUrlScheme == "" {
		return "https"
	} else {
		return a.EndpointUrlScheme
	}
}

type adapter struct {
	Name           string
	ReadyCondition iotv1alpha1.ConfigConditionType
	EnvPrefix      string

	AdapterConfigProvider func(*iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig

	Port adapterPort
}

var adapters = []adapter{
	{
		Name:           "amqp",
		ReadyCondition: iotv1alpha1.ConfigConditionTypeAmqpAdapterReady,
		EnvPrefix:      "HONO_AMQP_",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig {
			return &config.Spec.AdaptersConfig.AmqpAdapterConfig.CommonAdapterConfig
		},
		Port: adapterPort{
			ContainerPort:    5671,
			LoadBalancerPort: 35671,

			EndpointUrlScheme:      "amqps",
			EndpointUrlForcePort:   true,
			DoesntSupportReencrypt: true,
		},
	},
	{
		Name:           "mqtt",
		ReadyCondition: iotv1alpha1.ConfigConditionTypeMqttAdapterReady,
		EnvPrefix:      "HONO_MQTT_",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig {
			return &config.Spec.AdaptersConfig.MqttAdapterConfig.CommonAdapterConfig
		},
		Port: adapterPort{
			ContainerPort:    8883,
			LoadBalancerPort: 30883,

			EndpointUrlScheme:      "ssl",
			EndpointUrlForcePort:   true,
			DoesntSupportReencrypt: true,
		},
	},
	{
		Name:           "http",
		ReadyCondition: iotv1alpha1.ConfigConditionTypeHttpAdapterReady,
		EnvPrefix:      "HONO_HTTP_",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig {
			return &config.Spec.AdaptersConfig.HttpAdapterConfig.CommonAdapterConfig
		},
		Port: adapterPort{
			ContainerPort:    8443,
			LoadBalancerPort: 30443,
		},
	},
	{
		Name:           "lorawan",
		ReadyCondition: iotv1alpha1.ConfigConditionTypeLorawanAdapterReady,
		EnvPrefix:      "HONO_LORAWAN_",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig {
			return &config.Spec.AdaptersConfig.LoraWanAdapterConfig.CommonAdapterConfig
		},
		Port: adapterPort{
			ContainerPort:    8443,
			LoadBalancerPort: 30443,
		},
	},
	{
		Name:           "sigfox",
		ReadyCondition: iotv1alpha1.ConfigConditionTypeSigfoxAdapterReady,
		EnvPrefix:      "HONO_SIGFOX_",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig {
			return &config.Spec.AdaptersConfig.SigfoxAdapterConfig.CommonAdapterConfig
		},
		Port: adapterPort{
			ContainerPort:    8443,
			LoadBalancerPort: 30443,
		},
	},
}

// render the logback configuration
func (a adapter) RenderLoggingConfig(config *iotv1alpha1.IoTConfig, override string) string {
	return a.AdapterConfigProvider(config).Containers.Adapter.Logback.RenderConfiguration(config, logbackDefault, override)
}

func (a adapter) IsEnabled(config *iotv1alpha1.IoTConfig) bool {

	// find adapter config

	adapterConfig := a.AdapterConfigProvider(config)

	if adapterConfig != nil && adapterConfig.Enabled != nil {
		return *adapterConfig.Enabled
	}

	// return setting from env-var

	return globalIsAdapterEnabled(a.Name)
}

func (a adapter) FullName() string {
	return "iot-" + a.Name + "-adapter"
}

func findAdapter(name string) adapter {
	for _, a := range adapters {
		if a.Name == name {
			return a
		}
	}

	panic(fmt.Errorf("failed to find adapter '%s'", name))
}

// prepare the adapter status section
func prepareAdapterStatus(config *iotv1alpha1.IoTConfig) {

	config.Status.Adapters = make(map[string]iotv1alpha1.AdapterStatus)

	for _, a := range adapters {

		config.Status.Adapters[a.Name] = iotv1alpha1.AdapterStatus{
			Enabled: a.IsEnabled(config),
		}

	}

	config.Status.Services = make(map[string]iotv1alpha1.ServiceStatus)
}

func (r *ReconcileIoTConfig) processStandardAdapter(
	ctx context.Context,
	config *iotv1alpha1.IoTConfig,
	_ *enmassev1.MessagingInfrastructure,
	change *cchange.ConfigChangeRecorder,
	adapter adapter,
) (reconcile.Result, error) {

	enabled := adapter.IsEnabled(config)

	rc := recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, adapter.FullName(), config, !enabled, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
			return r.reconcileStandardAdapterDeployment(config, deployment, adapter, change)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, adapter.FullName(), config, !enabled, func(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {
			return r.reconcileStandardAdapterService(config, service, adapter)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, adapter.FullName()+"-metrics", config, !enabled, r.reconcileMetricsService(adapter.FullName()))
	})
	rc.ProcessSimple(func() error {
		return r.processAdapterRoute(
			ctx, config, adapter,
			func(config *iotv1alpha1.IoTConfig, router *routev1.Route, endpointStatus *iotv1alpha1.EndpointStatus) error {
				return r.reconcileStandardAdapterRoute(config, router, endpointStatus, adapter)
			},
			func(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {
				return r.reconcileStandardAdapterServiceExternal(config, service, adapter)
			})
	})

	// done

	return rc.Result()
}

// process the service route
func (r *ReconcileIoTConfig) processServiceRoute(ctx context.Context, config *iotv1alpha1.IoTConfig,
	name string,
	endpoint iotv1alpha1.EndpointConfig,
	routeManipulator func(config *iotv1alpha1.IoTConfig, service *routev1.Route, endpointStatus *iotv1alpha1.EndpointStatus) error,
	serviceManipulator func(config *iotv1alpha1.IoTConfig, service *corev1.Service) error,
) error {

	routesEnabled := config.WantDefaultRoutes(endpoint)

	if util.IsOpenshift() {

		endpoint := config.Status.Services[name]
		err := r.processRoute(ctx, name, config, !routesEnabled, &endpoint.Endpoint, routeManipulator)
		config.Status.Services[name] = endpoint
		return err

	} else {

		return r.processService(ctx, "iot-"+name+"-external", config, !routesEnabled, serviceManipulator)

	}

}

// process the adapter route
func (r *ReconcileIoTConfig) processAdapterRoute(ctx context.Context, config *iotv1alpha1.IoTConfig, adapter adapter,
	routeManipulator func(config *iotv1alpha1.IoTConfig, service *routev1.Route, endpointStatus *iotv1alpha1.EndpointStatus) error,
	serviceManipulator func(config *iotv1alpha1.IoTConfig, service *corev1.Service) error,
) error {

	enabled := adapter.IsEnabled(config)
	adapterConfig := adapter.AdapterConfigProvider(config)
	routesEnabled := enabled && config.WantDefaultRoutes(adapterConfig.EndpointConfig)

	name := "iot-" + adapter.Name + "-adapter"

	if util.IsOpenshift() {

		endpoint := config.Status.Adapters[adapter.Name]
		err := r.processRoute(ctx, name, config, !routesEnabled, &endpoint.Endpoint, routeManipulator)
		config.Status.Adapters[adapter.Name] = endpoint
		return err

	} else {

		return r.processService(ctx, name+"-external", config, !routesEnabled, serviceManipulator)

	}

}

func (r *ReconcileIoTConfig) addQpidProxySetup(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, containers iotv1alpha1.CommonAdapterContainers) error {

	// create qdr configurator sidecar

	err := install.ApplyDeploymentContainerWithError(deployment, "qdr-cfg", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-proxy-configurator", config); err != nil {
			return err
		}

		container.Args = nil
		container.Command = nil

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(64*1024*1024 /* 64Mi */, resource.BinarySI),
			},
		}

		install.ApplyVolumeMountSimple(container, "qdr-tmp-certs", "/var/qdr-certs", false)

		// tracing config

		BlockTracingSidecarConfig(config, container)

		// apply container options

		applyContainerConfig(container, containers.ProxyConfigurator)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// create qdr proxy sidecar

	err = install.ApplyDeploymentContainerWithError(deployment, "qdr-proxy", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, imageNameRouter, config); err != nil {
			return err
		}

		container.Args = []string{"/sbin/qdrouterd", "-c", "/etc/qdr/config/qdrouterd.json"}
		container.Command = nil

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(128*1024*1024 /* 128Mi */, resource.BinarySI),
			},
		}

		install.ApplyVolumeMountSimple(container, "qdr-tmp-certs", "/var/qdr-certs", true)
		install.ApplyVolumeMountSimple(container, "qdr-proxy-config", "/etc/qdr/config", true)
		install.ApplyVolumeMountSimple(container, "qdr-command-config", "/etc/qdr/command", true)
		install.ApplyVolumeMountSimple(container, tlsServiceCAVolumeName, "/etc/tls-service-ca", true)
		install.ApplyVolumeMountSimple(container, "shared-infra", "/etc/shared-infra-internal", true)

		// tracing config

		BlockTracingSidecarConfig(config, container)

		// apply container options

		applyContainerConfig(container, containers.Proxy)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	install.ApplyEmptyDirVolume(&deployment.Spec.Template.Spec, "qdr-tmp-certs")
	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "qdr-proxy-config", "qdr-proxy-configurator")
	install.ApplySecretVolume(&deployment.Spec.Template.Spec, "qdr-command-config", nameCommandMeshSecretName)
	install.ApplySecretVolume(&deployment.Spec.Template.Spec, "shared-infra", getSharedInfraSecretName(config))

	if err := ApplyInterServiceForDeployment(r.client, config, deployment, "", ""); err != nil {
		return err
	}

	return nil
}

func AppendHonoAdapterEnvs(config *iotv1alpha1.IoTConfig, container *corev1.Container, adapter adapter) error {

	username := install.FromSecret(adapter.FullName()+"-credentials", keyAdapterUsername)
	password := install.FromSecret(adapter.FullName()+"-credentials", keyAdapterPassword)

	container.Env = append(container.Env, []corev1.EnvVar{
		{Name: "HONO_MESSAGING_HOST", Value: "localhost"},
		{Name: "HONO_MESSAGING_PORT", Value: "5672"},
		{Name: "HONO_COMMAND_HOST", Value: "localhost"},
		{Name: "HONO_COMMAND_PORT", Value: "5672"},

		{Name: "HONO_REGISTRATION_HOST", Value: FullHostNameForEnvVar(nameDeviceRegistry)},
		{Name: "HONO_REGISTRATION_USERNAME", ValueFrom: username},
		{Name: "HONO_REGISTRATION_PASSWORD", ValueFrom: password},
		{Name: "HONO_CREDENTIALS_HOST", Value: FullHostNameForEnvVar(nameDeviceRegistry)},
		{Name: "HONO_CREDENTIALS_USERNAME", ValueFrom: username},
		{Name: "HONO_CREDENTIALS_PASSWORD", ValueFrom: password},
		{Name: "HONO_DEVICE_CONNECTION_HOST", Value: FullHostNameForEnvVar("iot-device-connection")},
		{Name: "HONO_DEVICE_CONNECTION_USERNAME", ValueFrom: username},
		{Name: "HONO_DEVICE_CONNECTION_PASSWORD", ValueFrom: password},
		{Name: "HONO_TENANT_HOST", Value: FullHostNameForEnvVar("iot-tenant-service")},
		{Name: "HONO_TENANT_USERNAME", ValueFrom: username},
		{Name: "HONO_TENANT_PASSWORD", ValueFrom: password},
	}...)

	adapterConfig := adapter.AdapterConfigProvider(config)
	options := mergeAdapterOptions(config.Spec.AdaptersConfig.DefaultOptions, adapterConfig.Options)

	appendCommonHonoJavaEnv(container, adapter.EnvPrefix, config, adapterConfig)

	// set max payload size
	if options.MaxPayloadSize > 0 {
		appendAdapterEnvVar(container, adapter, "MAX_PAYLOAD_SIZE", strconv.FormatInt(int64(options.MaxPayloadSize), 10))
	}

	// set tenant idle timeout
	if options.TenantIdleTimeout != "" {
		appendAdapterEnvVar(container, adapter, "TENANT_IDLE_TIMEOUT", options.TenantIdleTimeout)
	} else {
		// the hono default for this historically is "no timeout", but it would be better to have a timeout
		appendAdapterEnvVar(container, adapter, "TENANT_IDLE_TIMEOUT", "30m")
	}

	if err := AppendTrustStores(config, container, []string{
		"HONO_CREDENTIALS_TRUST_STORE_PATH",
		"HONO_DEVICE_CONNECTION_TRUST_STORE_PATH",
		"HONO_REGISTRATION_TRUST_STORE_PATH",
		"HONO_TENANT_TRUST_STORE_PATH",
	}); err != nil {
		return err
	}

	return nil

}

func appendAdapterEnvVar(container *corev1.Container, a adapter, key string, value string) {
	install.ApplyOrRemoveEnvSimple(container, a.EnvPrefix+key, value)
}

func (r *ReconcileIoTConfig) processQdrProxyConfig(ctx context.Context, config *iotv1alpha1.IoTConfig, infra *enmassev1.MessagingInfrastructure, configCtx *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, "qdr-proxy-configurator", config, false, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {
			return r.reconcileAdapterConfigMap(config, infra, configMap, configCtx)
		})
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileAdapterConfigMap(config *iotv1alpha1.IoTConfig, infra *enmassev1.MessagingInfrastructure, configMap *corev1.ConfigMap, configCtx *cchange.ConfigChangeRecorder) error {

	// tls versions

	tlsVersions := strings.Join(config.Spec.Mesh.TlsVersions(config), " ")

	// build config

	const internalCommandConnectionName = "iot-command-mesh"
	commandMeshHost := nameCommandMesh + "." + config.Namespace + ".svc"
	sharedInfraHost := infra.GetInternalClusterServiceName() + "." + config.Namespace + ".svc"

	router := [][]interface{}{
		{
			"router",
			map[string]interface{}{
				"mode":                "standalone",
				"id":                  "Router.Proxy",
				"timestampsInUTC":     true,
				"defaultDistribution": "unavailable",
			},
		},
		{
			// for local management access
			"listener",
			map[string]interface{}{
				"host":             "127.0.0.1",
				"port":             5672,
				"role":             "normal",
				"authenticatePeer": "no",
				"saslMechanisms":   "ANONYMOUS",
			},
		},
		// configuration for connecting to the shared infrastructure
		{
			"sslProfile",
			map[string]interface{}{
				"name":           SharedInfraConnectionName + "-ssl",
				"privateKeyFile": "/etc/shared-infra-internal/tls.key",
				"certFile":       "/etc/shared-infra-internal/tls.crt",
				"caCertFile":     "/etc/shared-infra-internal/ca.crt",
			},
		},
		{
			"connector",
			map[string]interface{}{
				"name":           SharedInfraConnectionName, // the name inside the configuration
				"host":           sharedInfraHost,           // the hostname to connect to
				"port":           55667,
				"sslProfile":     SharedInfraConnectionName + "-ssl",
				"role":           "route-container",
				"saslMechanisms": "EXTERNAL",
			},
		},
		// configuration for connecting to the command mesh
		{
			"sslProfile",
			map[string]interface{}{
				"name":       internalCommandConnectionName + "-ssl",
				"caCertFile": "/etc/tls-service-ca/service-ca.crt",
				"protocols":  tlsVersions,
			},
		},
		{
			"connector",
			map[string]interface{}{
				"name":           internalCommandConnectionName, // the name inside the configuration
				"host":           commandMeshHost,               // the hostname to connect to
				"port":           5671,
				"sslProfile":     internalCommandConnectionName + "-ssl",
				"role":           "route-container",
				"saslMechanisms": "PLAIN",
				"saslUsername":   iotCommandMeshUserName + "@" + iotCommandMeshDomainName,
				"saslPassword":   "file:/etc/qdr/command/password",
			},
		},
		{
			"linkRoute",
			map[string]interface{}{
				"pattern":    "command_internal/#",
				"connection": internalCommandConnectionName,
				"direction":  "in",
			},
		},
		{
			"linkRoute",
			map[string]interface{}{
				"pattern":    "command_internal/#",
				"connection": internalCommandConnectionName,
				"direction":  "out",
			},
		},
	}

	// serialize config

	j, err := json.MarshalIndent(router, "", "  ")
	if err != nil {
		return errors.Wrap(err, "Failed serializing router configuration")
	}
	jstr := string(j)

	// set content

	configMap.Data = map[string]string{
		"qdrouterd.json": jstr,
	}

	// add to hash

	configCtx.AddString(jstr)

	return nil

}

func applyEndpointDeployment(client client.Client, endpoint iotv1alpha1.EndpointConfig, deployment *appsv1.Deployment, endpointSecretName string, volumeName string) error {

	if endpoint.SecretNameStrategy != nil {

		// use provided secret

		install.ApplySecretVolume(&deployment.Spec.Template.Spec, volumeName, endpoint.SecretNameStrategy.TlsSecretName)
		if err := install.ApplySecretHash(client, &deployment.Spec.Template, iotPrefix+"/endpoint-secret-hash", deployment.Namespace, endpoint.SecretNameStrategy.TlsSecretName, "tls.crt", "tls.key"); err != nil {
			return err
		}

	} else {

		// use service CA as fallback

		if !util.IsOpenshift4() {
			return util.NewConfigurationError("Not running in OpenShift 4, unable to use service CA. You need to provide a protocol adapter endpoint key/certificate")
		}

		install.ApplySecretVolume(&deployment.Spec.Template.Spec, volumeName, endpointSecretName+"-tls")
		if err := install.ApplySecretHash(client, &deployment.Spec.Template, iotPrefix+"/endpoint-secret-hash", deployment.Namespace, endpointSecretName+"-tls"); err != nil {
			return err
		}
	}

	return nil
}

func applyEndpointService(endpoint iotv1alpha1.EndpointConfig, service *corev1.Service, endpointSecretName string) error {

	if service.Annotations != nil {
		// always delete "alpha" annotation
		delete(service.Annotations, openShiftServiceCAAnnotationServingCertAlpha)
	}

	if endpoint.SecretNameStrategy != nil {

		// use provided secret

		if service.Annotations != nil {
			// delete service ca annotation
			delete(service.Annotations, openShiftServiceCAAnnotationServingCertBeta)
		}

	} else {

		if !util.IsOpenshift() {
			return util.NewConfigurationError("not running in OpenShift, unable to use service CA, you need to provide an endpoint key/certificate for: %s", endpointSecretName)
		}

		// use service CA as fallback

		if service.Annotations == nil {
			service.Annotations = make(map[string]string)
		}

		service.Annotations[openShiftServiceCAAnnotationServingCertBeta] = endpointSecretName + "-tls"
	}

	return nil
}

func globalIsAdapterEnabled(name string) bool {
	v := os.Getenv("IOT_ADAPTER_" + strings.ToUpper(name) + "_ENABLED")
	return v == "" || v == "true"
}

func mergeAdapterOptions(first, second *iotv1alpha1.AdapterOptions) iotv1alpha1.AdapterOptions {

	if first == nil {
		first = &iotv1alpha1.AdapterOptions{}
	}

	result := *first

	if second == nil {
		second = &iotv1alpha1.AdapterOptions{}
	}

	if second.TenantIdleTimeout != "" {
		result.TenantIdleTimeout = second.TenantIdleTimeout
	}
	if second.MaxPayloadSize > 0 {
		result.MaxPayloadSize = second.MaxPayloadSize
	}

	return result
}

func applyDefaultAdapterDeploymentSpec(deployment *appsv1.Deployment) {
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-protocol-adapter"
	deployment.Annotations[util.ConnectsTo] = "iot-auth-service,iot-device-connection,iot-device-registry,iot-tenant-service"
}

func (r *ReconcileIoTConfig) reconcileStandardAdapterDeployment(
	config *iotv1alpha1.IoTConfig,
	deployment *appsv1.Deployment,
	adapter adapter,
	change *cchange.ConfigChangeRecorder,
) error {

	adapterConfig := adapter.AdapterConfigProvider(config)

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	applyDefaultDeploymentConfig(deployment, adapterConfig.ServiceConfig, change)
	applyDefaultAdapterDeploymentSpec(deployment)

	var tracingContainer *corev1.Container
	err := install.ApplyDeploymentContainerWithError(deployment, "adapter", func(container *corev1.Container) error {

		tracingContainer = container

		if err := install.SetContainerImage(container, adapter.FullName(), config); err != nil {
			return err
		}

		container.Args = []string{"/" + adapter.FullName() + ".jar"}
		container.Command = nil

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(512*1024*1024 /* 512Mi */, resource.BinarySI),
			},
		}

		container.Ports = []corev1.ContainerPort{
			{
				Name:          "adapter",
				ContainerPort: adapter.Port.ContainerPort,
				Protocol:      adapter.Port.GetProtocol(),
			},
		}

		container.Ports = appendHonoStandardPorts(container.Ports)
		SetHonoProbes(container)

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: ""},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: install.FromFieldNamespace()},

			{Name: "HONO_AUTH_HOST", Value: FullHostNameForEnvVar("iot-auth-service")},
		}

		SetupTracing(config, deployment, container)
		AppendStandardHonoJavaOptions(container)

		if err := AppendHonoAdapterEnvs(config, container, adapter); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)

		// apply container options

		applyContainerConfig(container, adapterConfig.Containers.Adapter.ContainerConfig)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// qdr config & proxy

	if err := r.addQpidProxySetup(config, deployment, adapterConfig.Containers); err != nil {
		return err
	}

	// reset init containers

	deployment.Spec.Template.Spec.InitContainers = nil

	// tracing

	SetupTracing(config, deployment, tracingContainer)

	// volumes

	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "config", adapter.FullName()+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(r.client, config, deployment, tlsServiceKeyVolumeName, ""); err != nil {
		return err
	}

	// endpoint key/cert

	if err := applyEndpointDeployment(r.client, adapterConfig.EndpointConfig, deployment, adapter.FullName(), "tls"); err != nil {
		return err
	}

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileStandardAdapterService(config *iotv1alpha1.IoTConfig, service *corev1.Service, adapter adapter) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	service.Spec.Type = corev1.ServiceTypeClusterIP

	if len(service.Spec.Ports) != 1 {
		service.Spec.Ports = make([]corev1.ServicePort, 1)
	}

	service.Spec.Ports[0].Name = "adapter"
	service.Spec.Ports[0].Protocol = adapter.Port.GetProtocol()
	service.Spec.Ports[0].Port = adapter.Port.GetServicePort()
	service.Spec.Ports[0].TargetPort = intstr.FromString("adapter")

	// annotations

	if err := ApplyInterServiceForService(config, service, ""); err != nil {
		return err
	}

	adapterConfig := adapter.AdapterConfigProvider(config)
	if err := applyEndpointService(adapterConfig.EndpointConfig, service, adapter.FullName()); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileStandardAdapterServiceExternal(config *iotv1alpha1.IoTConfig, service *corev1.Service, adapter adapter) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)
	service.Spec.Selector["name"] = adapter.FullName()

	service.Spec.Type = corev1.ServiceTypeLoadBalancer

	if len(service.Spec.Ports) != 1 {
		service.Spec.Ports = make([]corev1.ServicePort, 1)
	}

	service.Spec.Ports[0].Name = "adapter"
	service.Spec.Ports[0].Protocol = adapter.Port.GetProtocol()
	service.Spec.Ports[0].Port = adapter.Port.LoadBalancerPort
	service.Spec.Ports[0].TargetPort = intstr.FromString("adapter")

	// annotations

	if err := ApplyInterServiceForService(config, service, ""); err != nil {
		return err
	}

	adapterConfig := adapter.AdapterConfigProvider(config)
	if err := applyEndpointService(adapterConfig.EndpointConfig, service, adapter.FullName()); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileStandardAdapterRoute(config *iotv1alpha1.IoTConfig, route *routev1.Route, endpointStatus *iotv1alpha1.EndpointStatus, adapter adapter) error {

	install.ApplyDefaultLabels(&route.ObjectMeta, "iot", route.Name)

	// Port

	route.Spec.Port = &routev1.RoutePort{
		TargetPort: intstr.FromString("adapter"),
	}

	// Path

	route.Spec.Path = ""

	// TLS

	if route.Spec.TLS == nil {
		route.Spec.TLS = &routev1.TLSConfig{}
	}

	adapterConfig := adapter.AdapterConfigProvider(config)

	if adapterConfig.EndpointConfig.HasCustomCertificate() {

		route.Spec.TLS.Termination = routev1.TLSTerminationPassthrough
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone

	} else if !adapter.Port.DoesntSupportReencrypt {

		route.Spec.TLS.Termination = routev1.TLSTerminationReencrypt
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone

	} else {

		return util.NewConfigurationError("reencrypt routes are not supported for adapter type '%s'", adapter.Name)

	}

	// Service

	route.Spec.To.Kind = "Service"
	route.Spec.To.Name = adapter.FullName()

	// Update endpoint

	updateEndpointStatus(adapter.Port.GetEndpointUrlScheme(), adapter.Port.EndpointUrlForcePort, route, endpointStatus)

	// return

	return nil
}

// create the client certificate for the QDR proxies of the protocol adapters
func (r *ReconcileIoTConfig) processSharedInfraSecrets(ctx context.Context, config *iotv1alpha1.IoTConfig, infra *enmassev1.MessagingInfrastructure) error {
	if _, err := r.certController.ReconcileCertWithName(ctx, log, infra, config, getSharedInfraSecretName(config), "adapter-client-cert"); err != nil {
		return errors.Wrap(err, "Failed to create client certificate for adapters")
	}
	return nil
}

func getSharedInfraSecretName(config *iotv1alpha1.IoTConfig) string {
	// FIXME: we still have name clashes, but maybe less
	return cert.GetCertSecretName("iot-" + config.Name)
}
