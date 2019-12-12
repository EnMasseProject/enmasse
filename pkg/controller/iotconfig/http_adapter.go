/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"strconv"

	"github.com/enmasseproject/enmasse/pkg/util/cchange"

	"k8s.io/apimachinery/pkg/util/intstr"

	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	routev1 "github.com/openshift/api/route/v1"

	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

const nameHttpAdapter = "iot-http-adapter"
const routeHttpAdapter = "iot-http-adapter"

func (r *ReconcileIoTConfig) processHttpAdapter(ctx context.Context, config *iotv1alpha1.IoTConfig, qdrProxyConfigCtx *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	configCtx := qdrProxyConfigCtx.Clone()
	rc := &recon.ReconcileContext{}

	adapter := findAdapter("http")
	enabled := adapter.IsEnabled(config)
	adapterConfig := config.Spec.AdaptersConfig.HttpAdapterConfig

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameHttpAdapter, config, !enabled, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
			return r.reconcileHttpAdapterDeployment(config, deployment, configCtx)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameHttpAdapter, config, !enabled, r.reconcileHttpAdapterService)
	})
	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameHttpAdapter+"-config", config, !enabled, r.reconcileHttpAdapterConfigMap)
	})
	if !util.IsOpenshift() {
		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameHttpAdapter+"-external", config, !enabled, r.reconcileHttpAdapterServiceExternal)
		})
	}
	if util.IsOpenshift() {
		routesEnabled := enabled && config.WantDefaultRoutes(adapterConfig.EndpointConfig)

		rc.ProcessSimple(func() error {
			endpoint := config.Status.Adapters["http"]
			err := r.processRoute(ctx, routeHttpAdapter, config, !routesEnabled, &endpoint.Endpoint, r.reconcileHttpAdapterRoute)
			config.Status.Adapters["http"] = endpoint
			return err
		})
	}
	rc.ProcessSimple(func() error {
		return r.reconcileEndpointKeyCertificateSecret(ctx, config, adapterConfig.EndpointConfig, nameHttpAdapter, !enabled)
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileHttpAdapterDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, configCtx *cchange.ConfigChangeRecorder) error {

	adapter := config.Spec.AdaptersConfig.HttpAdapterConfig

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	applyDefaultDeploymentConfig(deployment, adapter.ServiceConfig, configCtx)
	applyDefaultAdapterDeploymentSpec(deployment)

	install.DropContainer(deployment, "http-adapter")
	err := install.ApplyContainerWithError(deployment, "adapter", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-http-adapter", config); err != nil {
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
			{Name: "jolokia", ContainerPort: 8778, Protocol: corev1.ProtocolTCP},
			{Name: "https", ContainerPort: 8443, Protocol: corev1.ProtocolTCP},
		}

		SetHonoProbes(container)

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: ""},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: &corev1.EnvVarSource{FieldRef: &corev1.ObjectFieldSelector{FieldPath: "metadata.namespace"}}},

			{Name: "HONO_AUTH_HOST", Value: FullHostNameForEnvVar("iot-auth-service")},

			{Name: "HONO_HTTP_NATIVE_TLS_REQUIRED", Value: strconv.FormatBool(adapter.IsNativeTlsRequired(config))},
		}

		AppendStandardHonoJavaOptions(container)

		if err := AppendHonoAdapterEnvs(config, container, findAdapter("http")); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)

		// apply container options

		applyContainerConfig(container, adapter.Containers.Adapter)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// qdr config & proxy

	if err := r.addQpidProxySetup(config, deployment, adapter.Containers); err != nil {
		return err
	}

	// volumes

	install.ApplyConfigMapVolume(deployment, "config", nameHttpAdapter+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(config, deployment, ""); err != nil {
		return err
	}

	// endpoint key/cert

	if err := applyAdapterEndpointDeployment(adapter.EndpointConfig, deployment, nameHttpAdapter); err != nil {
		return err
	}

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileHttpAdapterService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	if len(service.Spec.Ports) != 1 {
		service.Spec.Ports = make([]corev1.ServicePort, 1)
	}

	// HTTP port

	service.Spec.Ports[0].Name = "https"
	service.Spec.Ports[0].Port = 8443
	service.Spec.Ports[0].TargetPort = intstr.FromInt(8443)
	service.Spec.Ports[0].Protocol = corev1.ProtocolTCP

	// annotations

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, ""); err != nil {
		return err
	}

	if err := applyAdapterEndpointService(config.Spec.AdaptersConfig.HttpAdapterConfig.EndpointConfig, service, nameHttpAdapter); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileHttpAdapterConfigMap(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	if configMap.Data["logback-spring.xml"] == "" {
		configMap.Data["logback-spring.xml"] = DefaultLogbackConfig
	}

	configMap.Data["application.yml"] = `
hono:
  app:
    maxInstances: 1
  healthCheck:
    insecurePortBindAddress: 0.0.0.0
    insecurePortEnabled: true
    insecurePort: 8088
  http:
    bindAddress: 0.0.0.0
    keyPath: /etc/tls/tls.key
    certPath: /etc/tls/tls.crt
    keyFormat: PEM
  registration:
    port: 5671
    trustStoreFormat: PEM
  credentials:
    port: 5671
    trustStoreFormat: PEM
  deviceConnection:
    port: 5671
    trustStoreFormat: PEM
  tenant:
    port: 5671
    trustStoreFormat: PEM
`

	return nil
}

func (r *ReconcileIoTConfig) reconcileHttpAdapterRoute(config *iotv1alpha1.IoTConfig, route *routev1.Route, endpointStatus *iotv1alpha1.EndpointStatus) error {

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

	if config.Spec.AdaptersConfig.HttpAdapterConfig.EndpointConfig != nil &&
		config.Spec.AdaptersConfig.HttpAdapterConfig.EndpointConfig.HasCustomCertificate() {

		route.Spec.TLS.Termination = routev1.TLSTerminationPassthrough
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone

	} else {

		route.Spec.TLS.Termination = routev1.TLSTerminationReencrypt
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone

	}

	// Service

	route.Spec.To.Kind = "Service"
	route.Spec.To.Name = nameHttpAdapter

	// Update endpoint

	updateEndpointStatus("https", false, route, endpointStatus)

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileHttpAdapterServiceExternal(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	if len(service.Spec.Ports) != 1 {
		service.Spec.Ports = make([]corev1.ServicePort, 1)
	}

	service.Spec.Ports[0].Name = "https"
	service.Spec.Ports[0].Port = 30443
	service.Spec.Ports[0].TargetPort = intstr.FromInt(8443)
	service.Spec.Ports[0].Protocol = corev1.ProtocolTCP

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, ""); err != nil {
		return err
	}

	if err := applyAdapterEndpointService(config.Spec.AdaptersConfig.HttpAdapterConfig.EndpointConfig, service, nameHttpAdapter); err != nil {
		return err
	}

	service.Spec.Type = "LoadBalancer"
	service.Spec.Selector["name"] = nameHttpAdapter

	return nil
}
