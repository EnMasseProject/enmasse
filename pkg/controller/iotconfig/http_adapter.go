/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"

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

const nameHttpAdapter = "iot-http-adapter"
const routeHttpAdapter = "iot-http-adapter"

func (r *ReconcileIoTConfig) processHttpAdapter(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameHttpAdapter, config, r.reconcileHttpAdapterDeployment)
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameHttpAdapter, config, r.reconcileHttpAdapterService)
	})
	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameHttpAdapter+"-config", config, r.reconcileHttpAdapterConfigMap)
	})
	if config.WantDefaultRoutes() {
		rc.ProcessSimple(func() error {
			return r.processRoute(ctx, routeHttpAdapter, config, r.reconcileHttpAdapterRoute)
		})
	} else {
		if util.IsOpenshift() {
			rc.Delete(ctx, r.client, &routev1.Route{ObjectMeta: v1.ObjectMeta{Namespace: config.Namespace, Name: routeHttpAdapter}})
		}
	}

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileHttpAdapterDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	deployment.Spec.Replicas = nil

	err := install.ApplyContainerWithError(deployment, "http-adapter", func(container *corev1.Container) error {
		if err := install.SetContainerImage(container, "iot-http-adapter", config); err != nil {
			return err
		}

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
			{Name: "HONO_AUTH_HOST", Value: "iot-auth-service.$(KUBERNETES_NAMESPACE).svc"},

			{Name: "HONO_HTTP_NATIVE_TLS_REQUIRED", Value: "false"},
		}

		AppendHonoAdapterEnvs(container, "http-adapter@HONO", "http-secret")

		// volume mounts

		if len(container.VolumeMounts) != 3 {
			container.VolumeMounts = make([]corev1.VolumeMount, 3)
		}

		container.VolumeMounts[0].Name = "config"
		container.VolumeMounts[0].MountPath = "/etc/config"
		container.VolumeMounts[0].ReadOnly = true

		container.VolumeMounts[1].Name = "tls"
		container.VolumeMounts[1].MountPath = "/etc/tls"
		container.VolumeMounts[1].ReadOnly = true

		container.VolumeMounts[2].Name = "secrets"
		container.VolumeMounts[2].MountPath = "/etc/secrets"
		container.VolumeMounts[2].ReadOnly = true

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// qdr config & proxy

	if err := r.addQpidProxySetup(config, deployment); err != nil {
		return err
	}

	// volumes

	install.ApplyConfigMapVolume(deployment, "config", nameHttpAdapter+"-config")
	install.ApplySecretVolume(deployment, "tls", nameHttpAdapter+"-tls")
	install.ApplySecretVolume(deployment, "secrets", nameHttpAdapter+"-secrets")

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

	// FIXME: remove OpenShift specific feature
	service.Annotations["service.alpha.openshift.io/serving-cert-secret-name"] = nameHttpAdapter + "-tls"

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
    healthCheckPort: 8088
    healthCheckBindAddress: 0.0.0.0
  http:
    bindAddress: 0.0.0.0
    keyPath: /etc/tls/tls.key
    certPath: /etc/tls/tls.crt
    keyFormat: PEM
  registration:
    port: 5671
    trustStorePath: /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt
    trustStoreFormat: PEM
  credentials:
    port: 5671
    trustStorePath: /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt
    trustStoreFormat: PEM
  tenant:
    port: 5671
    trustStorePath: /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt
    trustStoreFormat: PEM
`

	return nil
}

func (r *ReconcileIoTConfig) reconcileHttpAdapterRoute(config *iotv1alpha1.IoTConfig, route *routev1.Route) error {

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

	route.Spec.TLS.Termination = routev1.TLSTerminationEdge
	route.Spec.TLS.InsecureEdgeTerminationPolicy = routev1.InsecureEdgeTerminationPolicyNone

	// Service

	route.Spec.To.Kind = "Service"
	route.Spec.To.Name = nameHttpAdapter

	// return

	return nil
}
