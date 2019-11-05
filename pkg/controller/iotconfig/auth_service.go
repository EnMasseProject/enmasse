/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"encoding/json"
	"strconv"

	"github.com/enmasseproject/enmasse/pkg/util/cchange"

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

	configCtx := cchange.NewRecorder()

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameAuthService+"-config", config, false, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {
			return r.reconcileAuthServiceConfigMap(config, configMap, configCtx)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameAuthService, config, false, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
			return r.reconcileAuthServiceDeployment(config, deployment, configCtx)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameAuthService, config, false, r.reconcileAuthServiceService)
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileAuthServiceDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, configCtx *cchange.ConfigChangeRecorder) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	service := config.Spec.ServicesConfig.Authentication
	applyDefaultDeploymentConfig(deployment, service.ServiceConfig, configCtx)

	err := install.ApplyDeploymentContainerWithError(deployment, "auth-service", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-auth-service", config); err != nil {
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
			{Name: "amqps", ContainerPort: 5671, Protocol: corev1.ProtocolTCP},
		}

		SetHonoProbes(container)

		// environment

		container.Env = []corev1.EnvVar{
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: "authentication-impl"},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: &corev1.EnvVarSource{FieldRef: &corev1.ObjectFieldSelector{FieldPath: "metadata.namespace"}}},

			{Name: "HONO_AUTH_SVC_SIGNING_SHARED_SECRET", Value: *config.Status.AuthenticationServicePSK},
			{Name: "HONO_AUTH_AMQP_NATIVE_TLS_REQUIRED", Value: strconv.FormatBool(service.IsNativeTlsRequired(config))},
		}
		if err := AppendTrustStores(config, container, []string{"HONO_AUTH_AMQP_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		AppendStandardHonoJavaOptions(container)

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "tls", "/etc/tls", true)

		// apply container options

		applyContainerConfig(container, service.Container)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	// volumes

	install.ApplyConfigMapVolume(deployment, "config", nameAuthService+"-config")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(config, deployment, nameAuthService); err != nil {
		return err
	}

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

	if err := ApplyInterServiceForService(config, service, nameAuthService); err != nil {
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileAuthServiceConfigMap(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap, configCtx *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	// create config map data

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
  auth:
    amqp:
      bindAddress: 0.0.0.0
      keyPath: /etc/tls/tls.key
      certPath: /etc/tls/tls.crt
      keyFormat: PEM
      trustStoreFormat: PEM
    svc:
      permissionsPath: file:///etc/config/permissions.json
`

	// create permissions files

	permissions, err := generatePermissions(config, adapters)
	if err != nil {
		return err
	}
	configMap.Data["permissions.json"] = permissions

	// record for config hash

	configCtx.AddString(configMap.Data["application.yml"])
	configCtx.AddString(configMap.Data["permissions.json"])

	return nil
}

func generatePermissions(config *iotv1alpha1.IoTConfig, adapters []adapter) (string, error) {

	result := `
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
				"operation":"tenant:get",
				"activities":["EXECUTE"]
			},
			{
				"resource": "device_con/*",
				"activities": [ "READ", "WRITE" ]
			},
			{
				"operation": "device_con/*:*",
				"activities": [ "EXECUTE" ]
			}
		]
	},
	"users":{
`

	// append snippets for adapters

	for _, a := range adapters {

		if !a.IsEnabled(config) {
			continue
		}

		encodedPassword, err := json.Marshal(config.Status.Adapters[a.Name].InterServicePassword)
		if err != nil {
			return "", err
		}

		result += `		"` + a.Name + `-adapter@HONO":{
			"mechanism":"PLAIN",
			"password":` + string(encodedPassword) + `,
			"authorities":["protocol-adapter"]
		},
`

	}

	// append device registry snippet

	result += `		"device-registry":{
			"mechanism":"EXTERNAL",
			"authorities":[]
		}
	}
}
`

	return result, nil

}
