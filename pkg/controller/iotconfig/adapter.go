/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"fmt"
	"os"
	"strings"

	"github.com/enmasseproject/enmasse/pkg/util"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

type adapter struct {
	Name                  string
	AdapterConfigProvider func(*iotv1alpha1.IoTConfig) *iotv1alpha1.AdapterConfig
}

var adapters = []adapter{
	{
		Name: "mqtt",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.AdapterConfig {
			return &config.Spec.AdaptersConfig.MqttAdapterConfig.AdapterConfig
		},
	},
	{
		Name: "http",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.AdapterConfig {
			return &config.Spec.AdaptersConfig.HttpAdapterConfig.AdapterConfig
		},
	},
	{
		Name: "lorawan",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.AdapterConfig {
			return &config.Spec.AdaptersConfig.LoraWanAdapterConfig.AdapterConfig
		},
	},
	{
		Name: "sigfox",
		AdapterConfigProvider: func(config *iotv1alpha1.IoTConfig) *iotv1alpha1.AdapterConfig {
			return &config.Spec.AdaptersConfig.SigfoxAdapterConfig.AdapterConfig
		},
	},
}

func (a adapter) IsEnabled(config *iotv1alpha1.IoTConfig) bool {

	// find adapter config

	adapterConfig := a.AdapterConfigProvider(config)

	if adapterConfig != nil && adapterConfig.Enabled != nil {
		return *adapterConfig.Enabled
	}

	// return setting from env-var

	return globalIsAdapterEnabled(a.Name)
}

func findAdapter(name string) adapter {
	for _, a := range adapters {
		if a.Name == name {
			return a
		}
	}

	panic(fmt.Errorf("failed to find adapter '%s'", name))
}

func (r *ReconcileIoTConfig) addQpidProxySetup(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, containers iotv1alpha1.CommonAdapterContainers) error {

	err := install.ApplyContainerWithError(deployment, "qdr-cfg", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "iot-proxy-configurator", config); err != nil {
			return err
		}

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(64*1024*1024 /* 64Mi */, resource.BinarySI),
			},
		}

		if len(container.VolumeMounts) != 1 {
			container.VolumeMounts = make([]corev1.VolumeMount, 1)
		}

		container.VolumeMounts[0].Name = "qdr-tmp-certs"
		container.VolumeMounts[0].MountPath = "/var/qdr-certs"
		container.VolumeMounts[0].ReadOnly = false

		// apply container options

		applyContainerConfig(container, containers.ProxyConfigurator)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	err = install.ApplyContainerWithError(deployment, "qdr-proxy", func(container *corev1.Container) error {

		if err := install.SetContainerImage(container, "router", config); err != nil {
			return err
		}

		container.Args = []string{"/sbin/qdrouterd", "-c", "/etc/qdr/config/qdrouterd.conf"}

		// set default resource limits

		container.Resources = corev1.ResourceRequirements{
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: *resource.NewQuantity(128*1024*1024 /* 128Mi */, resource.BinarySI),
			},
		}

		if len(container.VolumeMounts) != 2 {
			container.VolumeMounts = make([]corev1.VolumeMount, 2)
		}

		container.VolumeMounts[0].Name = "qdr-tmp-certs"
		container.VolumeMounts[0].MountPath = "/var/qdr-certs"
		container.VolumeMounts[0].ReadOnly = true

		container.VolumeMounts[1].Name = "qdr-proxy-config"
		container.VolumeMounts[1].MountPath = "/etc/qdr/config"
		container.VolumeMounts[1].ReadOnly = true

		// apply container options

		applyContainerConfig(container, containers.Proxy)

		// return

		return nil
	})

	if err != nil {
		return err
	}

	install.ApplyConfigMapVolume(deployment, "qdr-proxy-config", "qdr-proxy-configurator")
	install.ApplyEmptyDirVolume(deployment, "qdr-tmp-certs")

	return nil
}

func AppendHonoAdapterEnvs(config *iotv1alpha1.IoTConfig, container *corev1.Container, adapter adapter) error {

	username := adapter.Name + "-adapter@HONO"
	password := config.Status.Adapters[adapter.Name].InterServicePassword

	container.Env = append(container.Env, []corev1.EnvVar{
		{Name: "HONO_MESSAGING_HOST", Value: "localhost"},
		{Name: "HONO_MESSAGING_PORT", Value: "5672"},
		{Name: "HONO_COMMAND_HOST", Value: "localhost"},
		{Name: "HONO_COMMAND_PORT", Value: "5672"},

		{Name: "HONO_REGISTRATION_HOST", Value: FullHostNameForEnvVar("iot-device-registry")},
		{Name: "HONO_REGISTRATION_USERNAME", Value: username},
		{Name: "HONO_REGISTRATION_PASSWORD", Value: password},
		{Name: "HONO_CREDENTIALS_HOST", Value: FullHostNameForEnvVar("iot-device-registry")},
		{Name: "HONO_CREDENTIALS_USERNAME", Value: username},
		{Name: "HONO_CREDENTIALS_PASSWORD", Value: password},
		{Name: "HONO_DEVICE_CONNECTION_HOST", Value: FullHostNameForEnvVar("iot-device-registry")},
		{Name: "HONO_DEVICE_CONNECTION_USERNAME", Value: username},
		{Name: "HONO_DEVICE_CONNECTION_PASSWORD", Value: password},
		{Name: "HONO_TENANT_HOST", Value: FullHostNameForEnvVar("iot-tenant-service")},
		{Name: "HONO_TENANT_USERNAME", Value: username},
		{Name: "HONO_TENANT_PASSWORD", Value: password},
	}...)

	if err := AppendTrustStores(config, container, []string{
		"HONO_CREDENTIALS_TRUST_STORE_PATH",
		"HONO_DEVICE_CONNECTION_TRUST_STORE_PATH",
		"HONO_REGISTRATION_TRUST_STORE_PATH",
		"HONO_TENANT_TRUST_STORE_PATH",
	}); err != nil {
		return err
	}

	return nil

}

func (r *ReconcileIoTConfig) processQdrProxyConfig(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processConfigMap(ctx, "qdr-proxy-configurator", config, false, func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error {

			if configMap.Data == nil {
				configMap.Data = make(map[string]string)
			}

			configMap.Data["qdrouterd.conf"] = `
router {
  mode: standalone
  id: Router.Proxy
}

listener {
  host: localhost
  port: 5672
  saslMechanisms: ANONYMOUS
}
`
			return nil
		})
	})

	return rc.Result()
}

func hasEndpointKeyAndCert(endpoint *iotv1alpha1.AdapterEndpointConfig) bool {
	return endpoint != nil &&
		endpoint.KeyCertificateStrategy != nil &&
		endpoint.KeyCertificateStrategy.Key != nil &&
		endpoint.KeyCertificateStrategy.Certificate != nil
}

func applyAdapterEndpointDeployment(endpoint *iotv1alpha1.AdapterEndpointConfig, deployment *appsv1.Deployment, endpointSecretName string) error {
	if endpoint != nil && endpoint.SecretNameStrategy != nil {

		// use provided secret

		install.ApplySecretVolume(deployment, "tls", endpoint.SecretNameStrategy.TlsSecretName)

	} else if endpoint != nil && endpoint.KeyCertificateStrategy != nil {

		install.ApplySecretVolume(deployment, "tls", endpointSecretName+"-"+endpoint.KeyCertificateStrategy.HashString())

	} else {

		// use service CA as fallback

		if !util.IsOpenshift() {
			return fmt.Errorf("not running in OpenShift, unable to use service CA, you need to provide a protocol adapter endpoint key/certificate")
		}

		install.ApplySecretVolume(deployment, "tls", endpointSecretName+"-tls")
	}

	return nil
}

func applyAdapterEndpointService(endpoint *iotv1alpha1.AdapterEndpointConfig, service *corev1.Service, endpointSecretName string) error {

	if service.Annotations != nil {
		delete(service.Annotations, "service.alpha.openshift.io/serving-cert-secret-name")
	}

	if endpoint != nil && endpoint.SecretNameStrategy != nil {

		// use provided secret

	} else if endpoint != nil && endpoint.KeyCertificateStrategy != nil {

		// use provided key/cert

	} else {

		if !util.IsOpenshift() {
			return fmt.Errorf("not running in OpenShift, unable to use service CA, you need to provide a protocol adapter endpoint key/certificate")
		}

		// use service CA as fallback

		if service.Annotations == nil {
			service.Annotations = make(map[string]string)
		}

		service.Annotations["service.alpha.openshift.io/serving-cert-secret-name"] = endpointSecretName + "-tls"
	}

	return nil
}

func (r *ReconcileIoTConfig) reconcileEndpointKeyCertificateSecret(ctx context.Context, config *iotv1alpha1.IoTConfig, endpoint *iotv1alpha1.AdapterEndpointConfig, adapterName string, delete bool) error {

	if delete || !hasEndpointKeyAndCert(endpoint) {

		// cleanup previous secrets
		return r.cleanupSecrets(ctx, config, adapterName)

	}

	kc := endpoint.KeyCertificateStrategy
	name := adapterName + "-" + kc.HashString()
	return r.processSecret(ctx, name, config, false, func(config *iotv1alpha1.IoTConfig, secret *corev1.Secret) error {

		// cleanup previous secrets
		if err := r.cleanupSecrets(ctx, config, adapterName); err != nil {
			return err
		}

		install.ApplyDefaultLabels(&secret.ObjectMeta, "iot", adapterName+"tls")
		install.ApplyTlsSecret(secret, kc.Key, kc.Certificate)
		return nil
	})

}

func globalIsAdapterEnabled(name string) bool {
	v := os.Getenv("IOT_ADAPTER_" + strings.ToUpper(name) + "_ENABLED")
	return v == "" || v == "true"
}
