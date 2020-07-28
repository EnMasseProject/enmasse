/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"encoding/json"
	"github.com/enmasseproject/enmasse/pkg/util/cchange"
	"github.com/pkg/errors"

	"k8s.io/apimachinery/pkg/util/intstr"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
)

const nameAuthService = "iot-auth-service"

func (r *ReconcileIoTConfig) processAuthService(ctx context.Context, config *iotv1alpha1.IoTConfig, configTracker *configTracker) (reconcile.Result, error) {

	service := config.Spec.ServicesConfig.Authentication

	rc := &recon.ReconcileContext{}
	change := cchange.NewRecorder()

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, nameAuthService+"-config", config, false, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {
			return r.reconcileAuthServiceConfigMap(config, service, configMap, change)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processSecret(ctx, nameAuthService+"-permissions", config, false, func(config *iotv1alpha1.IoTConfig, secret *corev1.Secret) error {
			return r.reconcileAuthServiceSecret(config, secret, change, configTracker)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameAuthService+"-metrics", config, false, r.reconcileMetricsService(nameAuthService))
	})
	rc.ProcessSimple(func() error {
		return r.processDeployment(ctx, nameAuthService, config, false, func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error {
			return r.reconcileAuthServiceDeployment(config, deployment, change, configTracker.authServicePskCtx)
		})
	})
	rc.ProcessSimple(func() error {
		return r.processService(ctx, nameAuthService, config, false, r.reconcileAuthServiceService)
	})

	return rc.Result()
}

func (r *ReconcileIoTConfig) reconcileAuthServiceDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, change *cchange.ConfigChangeRecorder, authServicePsk *cchange.ConfigChangeRecorder) error {

	install.ApplyDeploymentDefaults(deployment, "iot", deployment.Name)

	service := config.Spec.ServicesConfig.Authentication
	applyDefaultDeploymentConfig(deployment, service.ServiceConfig, change)
	cchange.ApplyTo(authServicePsk, "iot.enmasse.io/auth-psk-hash", &deployment.Spec.Template.Annotations)

	var tracingContainer *corev1.Container
	err := install.ApplyDeploymentContainerWithError(deployment, "auth-service", func(container *corev1.Container) error {

		tracingContainer = container

		if err := install.SetContainerImage(container, "iot-auth-service", config); err != nil {
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
			{Name: "SPRING_CONFIG_LOCATION", Value: "file:///etc/config/"},
			{Name: "SPRING_PROFILES_ACTIVE", Value: "authentication-impl"},
			{Name: "LOGGING_CONFIG", Value: "file:///etc/config/logback-spring.xml"},
			{Name: "KUBERNETES_NAMESPACE", ValueFrom: install.FromFieldNamespace()},

			{Name: "HONO_AUTH_SVC_SIGNING_SHARED_SECRET", ValueFrom: install.FromSecret(nameAuthServicePskSecret, keyInterServicePsk)},
		}
		if err := AppendTrustStores(config, container, []string{"HONO_AUTH_AMQP_TRUST_STORE_PATH"}); err != nil {
			return err
		}

		appendCommonHonoJavaEnv(container, "HONO_AUTH_AMQP_", config, &service)

		SetupTracing(config, deployment, container)
		AppendStandardHonoJavaOptions(container)

		// volume mounts

		install.ApplyVolumeMountSimple(container, "config", "/etc/config", true)
		install.ApplyVolumeMountSimple(container, "permissions", "/etc/permissions", true)
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

	SetupTracing(config, deployment, tracingContainer)

	// volumes

	install.ApplyConfigMapVolume(&deployment.Spec.Template.Spec, "config", nameAuthService+"-config")
	install.ApplySecretVolume(&deployment.Spec.Template.Spec, "permissions", nameAuthService+"-permissions")

	// inter service secrets

	if err := ApplyInterServiceForDeployment(r.client, config, deployment, tlsServiceKeyVolumeName, nameAuthService); err != nil {
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

func (r *ReconcileIoTConfig) reconcileAuthServiceConfigMap(config *iotv1alpha1.IoTConfig, service iotv1alpha1.AuthenticationServiceConfig, configMap *corev1.ConfigMap, configCtx *cchange.ConfigChangeRecorder) error {

	install.ApplyDefaultLabels(&configMap.ObjectMeta, "iot", configMap.Name)

	// create config map data

	if configMap.Data == nil {
		configMap.Data = make(map[string]string)
	}

	configMap.Data["logback-spring.xml"] = service.RenderConfiguration(config, logbackDefault, configMap.Data["logback-custom.xml"])

	configMap.Data["application.yml"] = `
hono:
  app:
    maxInstances: 1
  vertx:
    preferNative: true
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
      permissionsPath: file:///etc/permissions/permissions.json
`

	// delete legacy entry

	delete(configMap.Data, "permissions.json")

	// record for config hash

	configCtx.AddStringsFromMap(configMap.Data, "application.yml", "logback-spring.xml")

	// done

	return nil
}

func (r *ReconcileIoTConfig) reconcileAuthServiceSecret(config *iotv1alpha1.IoTConfig, secret *corev1.Secret, configCtx *cchange.ConfigChangeRecorder, configTracker *configTracker) error {

	install.ApplyDefaultLabels(&secret.ObjectMeta, "iot", secret.Name)

	// create config map data

	if secret.Data == nil {
		secret.Data = make(map[string][]byte)
	}

	// create permissions files

	permissions, err := generatePermissions(config, adapters, configTracker)
	if err != nil {
		return err
	}
	secret.Data["permissions.json"] = []byte(permissions)

	// record for config hash

	configCtx.AddBytesFromMap(secret.Data, "permissions.json")

	// done

	return nil
}

func generatePermissions(config *iotv1alpha1.IoTConfig, adapters []adapter, configTracker *configTracker) (string, error) {

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

		username := configTracker.adapters[a.Name].username
		password := configTracker.adapters[a.Name].password

		encodedUsername, err := json.Marshal(username)
		if err != nil {
			return "", errors.Wrap(err, "Failed to JSON encode adapter username")
		}
		encodedPassword, err := json.Marshal(password)
		if err != nil {
			return "", errors.Wrap(err, "Failed to JSON encode adapter password")
		}

		result += `		` + string(encodedUsername) + `:{
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
