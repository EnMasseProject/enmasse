/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"

	"k8s.io/apimachinery/pkg/util/intstr"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
)

const nameTenantService = "iot-tenant-service"

func (r *ReconcileIoTConfig) processTenantService(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameTenantService, config, r.reconcileTenantServiceDeployment)
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameTenantService, config, r.reconcileTenantServiceService)
	})
	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameTenantService+"-config", config, r.reconcileTenantServiceConfigMap)
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileTenantServiceDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	applyDefaultDeploymentConfig(deployment, config.Spec.ServicesConfig.Tenant.ServiceConfig)

	err := install.ApplyContainerWithError(deployment, "tenant-service", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-tenant-service", config); err != nil {
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
		}

		SetHonoProbes(container)

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "SPRING_PROFILES_ACTIVE", Value: "prod"},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: &corev1.EnvVarSource{FieldRef: &corev1.ObjectFieldSelector{FieldPath: "metadata.namespace"}}},

			{Name: "ENMASSE_IOT_AUTH_HOST", Value: "iot-auth-service.$(KUBERNETES_NAMESPACE).svc"},
			{Name: "ENMASSE_IOT_AUTH_VALIDATION_SHARED_SECRET", Value: *config.Status.AuthenticationServicePSK},

			{Name: "ENMASSE_IOT_TENANT_ENDPOINT_AMQP_NATIVE_TLS_REQUIRED", Value: "false"},
		}

		if err := AppendTrustStores(config, container, []string{"ENMASSE_IOT_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)

		// apply container options

		applyContainerConfig(container, config.Spec.ServicesConfig.Tenant.Container)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// volumes

	install.ApplyConfigMapVolume(deployment, "config", nameTenantService+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(config, deployment, nameTenantService); err != nil {
		return err
	}

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileTenantServiceService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "iot", service.Name)

	if len(service.Spec.Ports) != 1 {
		service.Spec.Ports = make([]corev1.ServicePort, 1)
	}

	service.Spec.Ports[0].Name = "amqps"
	service.Spec.Ports[0].Port = 5671
	service.Spec.Ports[0].TargetPort = intstr.FromInt(5671)
	service.Spec.Ports[0].Protocol = corev1.ProtocolTCP

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	if err := ApplyInterServiceForService(config, service, nameTenantService); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileTenantServiceConfigMap(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	if configMap.Data["logback-spring.xml"] == "" {
		configMap.Data["logback-spring.xml"] = DefaultLogbackConfig
	}

	return nil
}
