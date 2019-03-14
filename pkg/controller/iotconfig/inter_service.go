/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/util/install"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
)

// for deployment

func ApplyInterServiceForDeployment(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment, secretName string) error {

	if config.Spec.HasNoInterServiceConfig() {
		// no explicit configuration
		if util.IsOpenshift() {
			return applyInterServiceForDeploymentServiceCa(deployment, secretName)
		} else {
			return fmt.Errorf("no inter service certificate configuration, but we need an explicit one")
		}
	}

	cfg := config.Spec.InterServiceCertificates

	if cfg.ServiceCAStrategy != nil {
		return applyInterServiceForDeploymentServiceCa(deployment, secretName)
	} else if cfg.SecretCertificatesStrategy != nil {
		return applyInterServiceForDeploymentSecretCertificates(deployment, cfg.SecretCertificatesStrategy, secretName)
	}

	return fmt.Errorf("unknown inter service certificates configuration")
}

// for service

func ApplyInterServiceForService(config *iotv1alpha1.IoTConfig, service *corev1.Service, secretName string) error {

	if config.Spec.HasNoInterServiceConfig() {
		// no explicit configuration
		if util.IsOpenshift() {
			return applyInterServiceForServiceServiceCa(service, secretName)
		} else {
			return fmt.Errorf("no inter service certificate configuration, but we need an explicit one")
		}
	}

	cfg := config.Spec.InterServiceCertificates

	if cfg.ServiceCAStrategy != nil {
		return applyInterServiceForServiceServiceCa(service, secretName)
	} else if cfg.SecretCertificatesStrategy != nil {
		return applyInterServiceForServiceSecretCertificates(service, cfg.SecretCertificatesStrategy)
	}

	return fmt.Errorf("unknown inter service certificates configuration")
}

func AppendTrustStores(config *iotv1alpha1.IoTConfig, container *corev1.Container, envs []string) error {

	if config.Spec.HasNoInterServiceConfig() {
		// no explicit configuration
		if util.IsOpenshift() {
			return appendTrustStoresForServiceCa(container, envs)
		} else {
			return fmt.Errorf("no inter service certificate configuration, but we need an explicit one")
		}
	}

	cfg := config.Spec.InterServiceCertificates

	if cfg.ServiceCAStrategy != nil {
		return appendTrustStoresForServiceCa(container, envs)
	} else if cfg.SecretCertificatesStrategy != nil {
		return appendTrustStoresForSecretCertificates(container, envs)
	}

	return fmt.Errorf("unknown inter service certificates configuration")
}

// service CA

func applyInterServiceForServiceServiceCa(service *corev1.Service, secretName string) error {
	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	service.Annotations["service.alpha.openshift.io/serving-cert-secret-name"] = secretName

	return nil
}

func applyInterServiceForDeploymentServiceCa(deployment *appsv1.Deployment, secretName string) error {
	install.ApplySecretVolume(deployment, "tls", secretName)
	install.DropVolume(deployment, "tls-service-ca")
	return nil
}

func appendTrustStoresForServiceCa(container *corev1.Container, env []string) error {

	for _, e := range env {
		container.Env = append(container.Env, corev1.EnvVar{Name: e, Value: "/var/run/secrets/kubernetes.io/serviceaccount/service-ca.crt"})
	}

	install.DropVolumeMount(container, "tls-service-ca")

	return nil
}

// secret certificates

func applyInterServiceForServiceSecretCertificates(service *corev1.Service, _ *iotv1alpha1.SecretCertificatesStrategy) error {

	if service.Annotations != nil {
		delete(service.Annotations, "service.alpha.openshift.io/serving-cert-secret-name")
	}

	return nil
}

func applyInterServiceForDeploymentSecretCertificates(deployment *appsv1.Deployment, cfg *iotv1alpha1.SecretCertificatesStrategy, secretName string) error {
	if cfg.CASecretName == "" {
		return fmt.Errorf("inter service secret CA name must not be empty")
	}
	install.ApplySecretVolume(deployment, "tls-service-ca", cfg.CASecretName)

	mappedSecretName := cfg.ServiceSecretNames[secretName]
	if mappedSecretName == "" {
		return fmt.Errorf("secret name %s mapped to an empty secret name", secretName)
	}

	install.ApplySecretVolume(deployment, "tls", mappedSecretName)

	return nil
}

func appendTrustStoresForSecretCertificates(container *corev1.Container, env []string) error {

	for _, e := range env {
		container.Env = append(container.Env, corev1.EnvVar{Name: e, Value: "/etc/tls-service-ca/service-ca.crt"})
	}

	install.AppendVolumeMountSimple(container, "tls-service-ca", "/etc/tls-service-ca", true)

	return nil
}
