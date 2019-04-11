/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"fmt"

	"k8s.io/apimachinery/pkg/apis/meta/v1"

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

const nameMqttAdapter = "iot-mqtt-adapter"
const routeMqttAdapter = "iot-mqtt-adapter"

func (r *ReconcileIoTConfig) processMqttAdapter(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameMqttAdapter, config, r.reconcileMqttAdapterDeployment)
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameMqttAdapter, config, r.reconcileMqttAdapterService)
	})
	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameMqttAdapter+"-config", config, r.reconcileMqttAdapterConfigMap)
	})
	if !util.IsOpenshift() {
		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameMqttAdapter + "-external", config, r.reconcileMqttAdapterServiceExternal)
		})
	}
	if config.WantDefaultRoutes(config.Spec.AdaptersConfig.MqttAdapterConfig.EndpointConfig) {
		rc.ProcessSimple(func() error {
			return r.processRoute(ctx, routeMqttAdapter, config, r.reconcileMqttAdapterRoute)
		})
	} else {
		if util.IsOpenshift() {
			rc.Delete(ctx, r.client, &routev1.Route{ObjectMeta: v1.ObjectMeta{Namespace: config.Namespace, Name: routeMqttAdapter}})
		}
	}
	rc.ProcessSimple(func() error {
		return r.reconcileEndpointKeyCertificateSecret(ctx, config, config.Spec.AdaptersConfig.MqttAdapterConfig.EndpointConfig, nameMqttAdapter)
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileMqttAdapterDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	adapter := config.Spec.AdaptersConfig.MqttAdapterConfig

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	applyDefaultDeploymentConfig(deployment, adapter.ServiceConfig)

	err := install.ApplyContainerWithError(deployment, "mqtt-adapter", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-mqtt-adapter", config); err != nil {
			return err
		}

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(512*1024*1024 /* 512Mi */, resource.BinarySI),
			},
		}

		container.Ports = []corev1.ContainerPort{
			{Name: "jolokia", ContainerPort: 8778, Protocol: corev1.ProtocolTCP},
			{Name: "mqtts", ContainerPort: 8883, Protocol: corev1.ProtocolTCP},
		}

		SetHonoProbes(container)

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: ""},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: &corev1.EnvVarSource{FieldRef: &corev1.ObjectFieldSelector{FieldPath: "metadata.namespace"}}},

			{Name: "HONO_AUTH_HOST", Value: "iot-auth-service.$(KUBERNETES_NAMESPACE).svc"},

			{Name: "HONO_MQTT_NATIVE_TLS_REQUIRED", Value: "false"},
		}

		AppendHonoAdapterEnvs(container, "mqtt-adapter@HONO", config.Status.Adapters["mqtt"].InterServicePassword)

		if err := AppendTrustStores(config, container, []string{
			"HONO_CREDENTIALS_TRUST_STORE_PATH",
			"HONO_REGISTRATION_TRUST_STORE_PATH",
			"HONO_TENANT_TRUST_STORE_PATH",
		}); err != nil {
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

	install.ApplyConfigMapVolume(deployment, "config", nameMqttAdapter+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(config, deployment, ""); err != nil {
		return err
	}

	// endpoint key/cert

	if err := applyAdapterEndpointDeployment(adapter.EndpointConfig, deployment, nameMqttAdapter); err != nil {
		return err
	}

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileMqttAdapterService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	if len(service.Spec.Ports) != 1 {
		service.Spec.Ports = make([]corev1.ServicePort, 1)
	}

	// HTTP port

	service.Spec.Ports[0].Name = "mqtts"
	service.Spec.Ports[0].Port = 8883
	service.Spec.Ports[0].TargetPort = intstr.FromInt(8883)
	service.Spec.Ports[0].Protocol = corev1.ProtocolTCP

	// annotations

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, ""); err != nil {
		return err
	}

	if err := applyAdapterEndpointService(config.Spec.AdaptersConfig.MqttAdapterConfig.EndpointConfig, service, nameMqttAdapter); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileMqttAdapterConfigMap(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {

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
    healthCheckPort: 8088
    healthCheckBindAddress: 0.0.0.0
  mqtt:
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
  tenant:
    port: 5671
    trustStoreFormat: PEM
`

	return nil
}

func (r *ReconcileIoTConfig) reconcileMqttAdapterRoute(config *iotv1alpha1.IoTConfig, route *routev1.Route) error {

	install.ApplyDefaultLabels(&route.ObjectMeta, "iot", route.Name)

	// Port

	route.Spec.Port = &routev1.RoutePort{
		TargetPort: intstr.FromString("mqtts"),
	}

	// Path

	route.Spec.Path = ""

	// TLS

	if route.Spec.TLS == nil {
		route.Spec.TLS = &routev1.TLSConfig{}
	}

	if config.Spec.AdaptersConfig.MqttAdapterConfig.EndpointConfig != nil &&
		config.Spec.AdaptersConfig.MqttAdapterConfig.EndpointConfig.HasCustomCertificate() {

		route.Spec.TLS.Termination = routev1.TLSTerminationPassthrough
		route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone

	} else {

		return fmt.Errorf("reencrypt routes are not supported to MQTT")

	}

	// Service

	route.Spec.To.Kind = "Service"
	route.Spec.To.Name = nameMqttAdapter

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileMqttAdapterServiceExternal(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	if len(service.Spec.Ports) != 1 {
		service.Spec.Ports = make([]corev1.ServicePort, 1)
	}

	service.Spec.Ports[0].Name = "mqtts"
	service.Spec.Ports[0].Port = 30883
	service.Spec.Ports[0].TargetPort = intstr.FromInt(8883)
	service.Spec.Ports[0].Protocol = corev1.ProtocolTCP

	// annotations

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, ""); err != nil {
		return err
	}

	if err := applyAdapterEndpointService(config.Spec.AdaptersConfig.MqttAdapterConfig.EndpointConfig, service, nameMqttAdapter); err != nil {
		return err
	}

	service.Spec.Type = "LoadBalancer"
	service.Spec.Selector["name"] = nameMqttAdapter

	return nil
}
