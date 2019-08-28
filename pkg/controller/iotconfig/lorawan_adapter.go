/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"

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

const nameLoraWanAdapter = "iot-lorawan-adapter"
const routeLoraWanAdapter = "iot-lorawan-adapter"

func (r *ReconcileIoTConfig) processLoraWanAdapter(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	adapter := findAdapter("lorawan")
	enabled := adapter.IsEnabled(config)
	adapterConfig := config.Spec.AdaptersConfig.LoraWanAdapterConfig

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameLoraWanAdapter, config, !enabled, r.reconcileLoraWanAdapterDeployment)
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameLoraWanAdapter, config, !enabled, r.reconcileLoraWanAdapterService)
	})
	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameLoraWanAdapter+"-config", config, !enabled, r.reconcileLoraWanAdapterConfigMap)
	})
	if !util.IsOpenshift() {
		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameLoraWanAdapter+"-external", config, !enabled, r.reconcileLoraWanAdapterServiceExternal)
		})
	}
	if util.IsOpenshift() {
		routesEnabled := enabled && config.WantDefaultRoutes(adapterConfig.EndpointConfig)

		rc.ProcessSimple(func() error {
			return r.processRoute(ctx, routeLoraWanAdapter, config, !routesEnabled, r.reconcileLoraWanAdapterRoute)
		})
	}
	rc.ProcessSimple(func() error {
		return r.reconcileEndpointKeyCertificateSecret(ctx, config, adapterConfig.EndpointConfig, nameLoraWanAdapter, !enabled)
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileLoraWanAdapterDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	adapter := config.Spec.AdaptersConfig.LoraWanAdapterConfig

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	applyDefaultDeploymentConfig(deployment, adapter.ServiceConfig)

	install.DropContainer(deployment, "lorawan-adapter")
	err := install.ApplyContainerWithError(deployment, "adapter", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-lorawan-adapter", config); err != nil {
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

			{Name: "HONO_LORA_NATIVE_TLS_REQUIRED", Value: "false"},
		}

		AppendStandardHonoJavaOptions(container)

		if err := AppendHonoAdapterEnvs(config, container, findAdapter("lorawan")); err != nil {
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

	install.ApplyConfigMapVolume(deployment, "config", nameLoraWanAdapter+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(config, deployment, ""); err != nil {
		return err
	}

	// endpoint key/cert

	if err := applyAdapterEndpointDeployment(adapter.EndpointConfig, deployment, nameLoraWanAdapter); err != nil {
		return err
	}

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileLoraWanAdapterService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

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

	if err := applyAdapterEndpointService(config.Spec.AdaptersConfig.LoraWanAdapterConfig.EndpointConfig, service, nameLoraWanAdapter); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileLoraWanAdapterConfigMap(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {

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
  lora:
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

func (r *ReconcileIoTConfig) reconcileLoraWanAdapterRoute(config *iotv1alpha1.IoTConfig, route *routev1.Route) error {

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

	if config.Spec.AdaptersConfig.LoraWanAdapterConfig.EndpointConfig != nil &&
		config.Spec.AdaptersConfig.LoraWanAdapterConfig.EndpointConfig.HasCustomCertificate() {

		route.Spec.TLS.Termination = routev1.TLSTerminationPassthrough
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone

	} else {

		route.Spec.TLS.Termination = routev1.TLSTerminationReencrypt
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone

	}

	// Service

	route.Spec.To.Kind = "Service"
	route.Spec.To.Name = nameLoraWanAdapter

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileLoraWanAdapterServiceExternal(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

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

	if err := applyAdapterEndpointService(config.Spec.AdaptersConfig.LoraWanAdapterConfig.EndpointConfig, service, nameLoraWanAdapter); err != nil {
		return err
	}

	service.Spec.Type = "LoadBalancer"
	service.Spec.Selector["name"] = nameLoraWanAdapter

	return nil
}
