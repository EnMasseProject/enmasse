/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/pkg/errors"
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

type adapter struct {
	Name      string
	EnvPrefix string

	AdapterConfigProvider func(*iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig
}

var adapters = []adapter{
	{
		Name:      "mqtt",
		EnvPrefix: "HONO_MQTT_",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig {
			return &config.Spec.AdaptersConfig.MqttAdapterConfig.CommonAdapterConfig
		},
	},
	{
		Name:      "http",
		EnvPrefix: "HONO_HTTP_",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig {
			return &config.Spec.AdaptersConfig.HttpAdapterConfig.CommonAdapterConfig
		},
	},
	{
		Name:      "lorawan",
		EnvPrefix: "HONO_LORAWAN_",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig {
			return &config.Spec.AdaptersConfig.LoraWanAdapterConfig.CommonAdapterConfig
		},
	},
	{
		Name:      "sigfox",
		EnvPrefix: "HONO_SIGFOX_",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.CommonAdapterConfig {
			return &config.Spec.AdaptersConfig.SigfoxAdapterConfig.CommonAdapterConfig
		},
	},
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

		return r.processService(ctx, name+"-external", config, !routesEnabled, serviceManipulator)

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

	err := install.ApplyDeploymentContainerWithError(deployment, "qdr-cfg", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-proxy-configurator", config); err != nil {
			return err
		}

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

	err = install.ApplyDeploymentContainerWithError(deployment, "qdr-proxy", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, imageNameRouter, config); err != nil {
			return err
		}

		container.Args = []string{"/sbin/qdrouterd", "-c", "/etc/qdr/config/qdrouterd.json"}

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

	if err := ApplyInterServiceForDeployment(r.client, config, deployment, "", ""); err != nil {
		return err
	}

	return nil
}

func AppendHonoAdapterEnvs(config *iotv1alpha1.IoTConfig, container *corev1.Container, adapter adapter) error {

	username := adapter.Name + "-adapter@HONO"
	password := config.Status.Adapters[adapter.Name].InterServicePassword

	container.Env = append(container.Env, []corev1.EnvVar{
		{Name: "HONO_MESSAGING_HOST", Value: "localhost"},
		{Name: "HONO_MESSAGING_PORT", Value: "5672"},
		{Name: "HONO_COMMAND_HOST", Value: "localhost"},
		{Name: "HONO_COMMAND_PORT", Value: "5672"},

		{Name: "HONO_REGISTRATION_HOST", Value: FullHostNameForEnvVar(nameDeviceRegistry)},
		{Name: "HONO_REGISTRATION_USERNAME", Value: username},
		{Name: "HONO_REGISTRATION_PASSWORD", Value: password},
		{Name: "HONO_CREDENTIALS_HOST", Value: FullHostNameForEnvVar(nameDeviceRegistry)},
		{Name: "HONO_CREDENTIALS_USERNAME", Value: username},
		{Name: "HONO_CREDENTIALS_PASSWORD", Value: password},
		{Name: "HONO_DEVICE_CONNECTION_HOST", Value: FullHostNameForEnvVar("iot-device-connection")},
		{Name: "HONO_DEVICE_CONNECTION_USERNAME", Value: username},
		{Name: "HONO_DEVICE_CONNECTION_PASSWORD", Value: password},
		{Name: "HONO_TENANT_HOST", Value: FullHostNameForEnvVar("iot-tenant-service")},
		{Name: "HONO_TENANT_USERNAME", Value: username},
		{Name: "HONO_TENANT_PASSWORD", Value: password},
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

func (r *ReconcileIoTConfig) processQdrProxyConfig(ctx context.Context, config *iotv1alpha1.IoTConfig, configCtx *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, "qdr-proxy-configurator", config, false, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {
			return r.reconcileAdapterConfigMap(config, configMap, configCtx)
		})
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileAdapterConfigMap(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap, configCtx *cchange.ConfigChangeRecorder) error {

	// tls versions

	tlsVersions := strings.Join(config.Spec.Mesh.TlsVersions(config), " ")

	// build config

	const internalCommandConnectionName = "iot-command-mesh"
	commandMeshHost := nameCommandMesh + "." + config.Namespace + ".svc"

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
			"sslProfile",
			map[string]interface{}{
				"name":       internalCommandConnectionName + "-ssl",
				"caCertFile": "/etc/tls-service-ca/service-ca.crt",
				"protocols":  tlsVersions,
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
			return util.NewConfigurationError("not running in OpenShift, unable to use service CA, you need to provide a protocol adapter endpoint key/certificate")
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
