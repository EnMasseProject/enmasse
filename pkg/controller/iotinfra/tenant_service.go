/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"k8s.io/apimachinery/pkg/util/intstr"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
)

const nameTenantService = "iot-tenant-service"

func (r *ReconcileIoTInfrastructure) processTenantService(ctx context.Context, infra *iotv1.IoTInfrastructure, authServicePsk *cchange.ConfigChangeRecorder) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}
	change := cchange.NewRecorder()

	service := infra.Spec.ServicesConfig.Tenant

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameTenantService+"-config", infra, false, func(infra *iotv1.IoTInfrastructure, configMap *corev1.ConfigMap) error {
			return r.reconcileTenantServiceConfigMap(infra, service, configMap, change)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameTenantService, infra, false, func(infra *iotv1.IoTInfrastructure, deployment *appsv1.Deployment) error {
			return r.reconcileTenantServiceDeployment(infra, deployment, change, authServicePsk)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameTenantService, infra, false, r.reconcileTenantServiceService)
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameTenantService+"-metrics", infra, false, r.reconcileMetricsService(nameTenantService))
	})

	return rc.Result()
}

func (r *ReconcileIoTInfrastructure) reconcileTenantServiceDeployment(infra *iotv1.IoTInfrastructure, deployment *appsv1.Deployment, change *cchange.ConfigChangeRecorder, authServicePsk *cchange.ConfigChangeRecorder) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	service := infra.Spec.ServicesConfig.Tenant
	deployment.Annotations[util.ConnectsTo] = "iot-auth-service"
	deployment.Spec.Template.Spec.ServiceAccountName = "iot-tenant-service"
	applyDefaultDeploymentConfig(deployment, service.ServiceConfig, change)
	cchange.ApplyTo(authServicePsk, "iot.enmasse.io/auth-psk-hash", &deployment.Spec.Template.Annotations)

	var tracingContainer *corev1.Container
	err := install.ApplyDeploymentContainerWithError(deployment, "tenant-service", func(container *corev1.Container) error {

		tracingContainer = container

		if err := install.SetContainerImage(container, "iot-tenant-service", infra); err != nil {
			return err
		}

		container.Args = nil
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
			{Name: "SPRING_PROFILES_ACTIVE", Value: "prod"},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/infra/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: install.FromFieldNamespace()},

			{Name: "ENMASSE_IOT_AUTH_HOST", Value: FullHostNameForEnvVar("iot-auth-service")},
			{Name: "ENMASSE_IOT_AUTH_VALIDATION_SHARED_SECRET", ValueFrom: install.FromSecret(nameAuthServicePskSecret, keyInterServicePsk)},
		}

		applyServiceConnectionOptions(container, "ENMASSE_IOT_AUTH", infra.Spec.ServicesConfig.Authentication.TlsVersions(infra))

		appendCommonHonoJavaEnv(container, "ENMASSE_IOT_AMQP_", infra, &service.CommonServiceConfig)

		SetupTracing(infra, deployment, container)
		AppendStandardHonoJavaOptions(container)

		if err := AppendTrustStores(infra, container, []string{"ENMASSE_IOT_AUTH_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		// volume mounts

		install.ApplyVolumeMountSimple(container, "infra", "/etc/infra", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)

		// apply container options

		applyContainerConfig(container, service.Container.ContainerConfig)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// reset init containers

	deployment.Spec.Template.Spec.InitContainers = nil

	// tracing

	SetupTracing(infra, deployment, tracingContainer)

	// volumes

	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "infra", nameTenantService+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(r.client, infra, deployment, tlsServiceKeyVolumeName, nameTenantService); err != nil {
		return err
	}

	// return

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileTenantServiceService(infra *iotv1.IoTInfrastructure, service *corev1.Service) error {

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

	if err := ApplyInterServiceForService(infra, service, nameTenantService); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) reconcileTenantServiceConfigMap(infra *iotv1.IoTInfrastructure, service iotv1.TenantServiceConfig, configMap *corev1.ConfigMap, change *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	configMap.Data["logback-spring.xml"] = service.RenderConfiguration(infra, logbackDefault, configMap.Data["logback-custom.xml"])

	change.AddStringsFromMap(configMap.Data, "logback-spring.xml")

	return nil
}
