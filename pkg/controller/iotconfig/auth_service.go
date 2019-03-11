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

const nameAuthService = "iot-auth-service"

func (r *ReconcileIoTConfig) processAuthService(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameAuthService, config, r.reconcileAuthServiceDeployment)
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameAuthService, config, r.reconcileAuthServiceService)
	})
	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameAuthService+"-config", config, r.reconcileAuthServiceConfigMap)
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileAuthServiceDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	deployment.Spec.Replicas = nil

	err := install.ApplyContainerWithError(deployment, "auth-service", func(container *corev1.Container) error {
		if err := install.SetContainerImage(container, "iot-auth-service", config); err != nil {
			return err
		}

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
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: "authentication-impl"},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: &corev1.EnvVarSource{FieldRef: &corev1.ObjectFieldSelector{FieldPath: "metadata.namespace"}}},
		}

		// volume mounts

		if len(container.VolumeMounts) != 2 {
			container.VolumeMounts = make([]corev1.VolumeMount, 2)
		}

		container.VolumeMounts[0].Name = "conf"
		container.VolumeMounts[0].MountPath = "/etc/config"
		container.VolumeMounts[0].ReadOnly = false

		container.VolumeMounts[1].Name = "tls"
		container.VolumeMounts[1].MountPath = "/etc/tls"
		container.VolumeMounts[1].ReadOnly = true

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// volumes

	install.ApplyConfigMapVolume(deployment, "conf", nameAuthService+"-config")
	install.ApplySecretVolume(deployment, "tls", "iot-auth-service-tls")

	// return

	return nil
}

func (r *ReconcileIoTConfig) reconcileAuthServiceService(config *iotv1alpha1.IoTConfig, service *corev1.Service) error {

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

	// FIXME: remove OpenShift specific feature
	service.Annotations["service.alpha.openshift.io/serving-cert-secret-name"] = nameAuthService + "-tls"

	return nil
}

func (r *ReconcileIoTConfig) reconcileAuthServiceConfigMap(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {

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
    amqp:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls/tls.key
      certPath: /etc/tls/tls.crt
      keyFormat: PEM
      trustStorePath: /var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt
      trustStoreFormat: PEM
    svc:
      permissionsPath: file:///etc/config/permissions.json
`

	configMap.Data["permissions.json"] = `
{
	"roles":{
		"protocol-adapter":[
			{
				"resource":"telemetry/*",
				"activities":["WRITE"]
			},
			{
				"resource":"event/*",
				"activities":["WRITE"]
			},
			{
				"resource":"registration/*",
				"activities":["READ","WRITE"]
			},
			{
				"operation":"registration/*:assert",
				"activities":["EXECUTE"]
			},
			{
				"resource":"credentials/*",
				"activities":["READ","WRITE"]
			},
			{
				"operation":"credentials/*:get",
				"activities":["EXECUTE"]
			},
			{
				"resource":"tenant",
				"activities":["READ","WRITE"]
			},
			{
				"operation":"tenant/*:*",
				"activities":["EXECUTE"]
			}
		]
	},
	"users":{
		"amqp-adapter@HONO":{
			"mechanism":"PLAIN",
			"password":"amqp-secret",
			"authorities":["hono-component","protocol-adapter"]
		},
		"http-adapter@HONO":{
			"mechanism":"PLAIN",
			"password":"http-secret",
			"authorities":["hono-component","protocol-adapter"]
		},
		"mqtt-adapter@HONO":{
			"mechanism":"PLAIN",
			"password":"mqtt-secret",
			"authorities":["hono-component","protocol-adapter"]
		},
		"device-registry":{
			"mechanism":"EXTERNAL",
			"authorities":[]
		}
	}
}
`

	return nil
}
