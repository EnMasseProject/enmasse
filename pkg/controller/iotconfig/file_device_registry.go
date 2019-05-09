/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"strconv"

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

const nameDeviceRegistry = "iot-device-registry"
const routeDeviceRegistry = "device-registry"

func (r *ReconcileIoTConfig) processFileDeviceRegistry(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameDeviceRegistry, config, r.reconcileFileDeviceRegistryDeployment)
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameDeviceRegistry, config, r.reconcileFileDeviceRegistryService)
	})
	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameDeviceRegistry+"-config", config, r.reconcileFileDeviceRegistryConfigMap)
	})
	rc.ProcessSimple(func() error {
		return r.processPersistentVolumeClaim(ctx, nameDeviceRegistry+"-pvc", config, r.reconcileFileDeviceRegistryPersistentVolumeClaim)
	})

	if !util.IsOpenshift() {
		rc.ProcessSimple(func() error {
			return r.processService(ctx, nameDeviceRegistry + "-external", config, r.reconcileFileDeviceRegistryServiceExternal)
		})
	}

	if config.WantDefaultRoutes(nil) {
		rc.ProcessSimple(func() error {
			return r.processRoute(ctx, routeDeviceRegistry, config, r.reconcileFileDeviceRegistryRoute)
		})
	} else {
		if util.IsOpenshift() {
			rc.Delete(ctx, r.client, &routev1.Route{ObjectMeta: v1.ObjectMeta{Namespace: config.Namespace, Name: routeDeviceRegistry}})
		}
	}

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileFileDeviceRegistryDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	applyDefaultDeploymentConfig(deployment, config.Spec.ServicesConfig.DeviceRegistry.ServiceConfig)

	err := install.ApplyContainerWithError(deployment, "device-registry", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-device-registry-file", config); err != nil {
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
			{Name: "amqps", ContainerPort: 5671, Protocol: corev1.ProtocolTCP},
			{Name: "http", ContainerPort: 8080, Protocol: corev1.ProtocolTCP},
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
			{Name: "HONO_AUTH_VALIDATION_SHARED_SECRET", Value: *config.Status.AuthenticationServicePSK},

			{Name: "HONO_REGISTRY_SVC_SIGNING_SHARED_SECRET", Value: *config.Status.AuthenticationServicePSK},
			{Name: "HONO_REGISTRY_SVC_SAVE_TO_FILE", Value: "true"},
		}

		// set max devices per tenant limit

		if config.Spec.ServicesConfig.DeviceRegistry.File != nil && config.Spec.ServicesConfig.DeviceRegistry.File.NumberOfDevicesPerTenant != nil {
			v := strconv.FormatUint(uint64(*config.Spec.ServicesConfig.DeviceRegistry.File.NumberOfDevicesPerTenant), 10)
			container.Env = append(container.Env, corev1.EnvVar{
				Name: "HONO_REGISTRY_SVC_MAX_DEVICES_PER_TENANT", Value: v,
			})
		}

		// append trust stores

		if err := AppendTrustStores(config, container, []string{"HONO_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)
		install.ApplyVolumeMountSimple(container, "registry", "/var/lib/hono/device-registry", false)

		// apply container options

		if config.Spec.ServicesConfig.DeviceRegistry.File != nil {
			applyContainerConfig(container, config.Spec.ServicesConfig.DeviceRegistry.File.Container)
		}

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// volumes

	install.ApplyConfigMapVolume(deployment, "config", nameDeviceRegistry+"-config")
	install.ApplyPersistentVolume(deployment, "registry", nameDeviceRegistry+"-pvc")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(config, deployment, nameDeviceRegistry); err != nil {
		return err
	}

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileFileDeviceRegistryService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

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

func (r *ReconcileIoTConfig) reconcileFileDeviceRegistryConfigMap(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {

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
    healthCheckBindAddress: 0.0.0.0
    healthCheckPort: 8088
  auth:
    port: 5671
    keyPath: /etc/tls/tls.key
    certPath: /etc/tls/tls.crt
    keyFormat: PEM
    trustStorePath: /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt
    trustStoreFormat: PEM
  registry:
    amqp:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls/tls.key
      certPath: /etc/tls/tls.crt
      keyFormat: PEM
    rest:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls/tls.key
      certPath: /etc/tls/tls.crt
      keyFormat: PEM
    svc:
      filename: /var/lib/hono/device-registry/device-identities.json
      saveToFile: true
  credentials:
    svc:
      credentialsFilename: /var/lib/hono/device-registry/credentials.json
      saveToFile: true
  tenant:
    svc:
      filename: /var/lib/hono/device-registry/tenants.json
      saveToFile: true
`

	return nil
}

func (r *ReconcileIoTConfig) reconcileFileDeviceRegistryPersistentVolumeClaim(config *iotv1alpha1.IoTConfig, pvc *corev1.PersistentVolumeClaim) error {

	install.ApplyDefaultLabels(&pvc.ObjectMeta, "iot", pvc.Name)

	pvc.Spec.AccessModes = []corev1.PersistentVolumeAccessMode{corev1.ReadWriteOnce}
	pvc.Spec.Resources.Requests = corev1.ResourceList{
		corev1.ResourceStorage: *resource.NewQuantity(256*1024*1024 /* 256Mi */, resource.BinarySI),
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileFileDeviceRegistryRoute(config *iotv1alpha1.IoTConfig, route *routev1.Route) error {

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

	// return

	return nil
}


func (r *ReconcileIoTConfig) reconcileFileDeviceRegistryServiceExternal(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

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