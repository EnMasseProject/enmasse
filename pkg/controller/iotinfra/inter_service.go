/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	apierrors "k8s.io/apimachinery/pkg/api/errors"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"

	"github.com/enmasseproject/enmasse/pkg/util/install"

	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

const openShiftServiceCAAnnotationServingCertAlpha = "service.alpha.openshift.io/serving-cert-secret-name"
const openShiftServiceCAAnnotationServingCertBeta = "service.beta.openshift.io/serving-cert-secret-name"

const openShiftServiceCAAnnotationInjectBundle = "service.beta.openshift.io/inject-cabundle"

const tlsServiceCAVolumeName = "tls-service-ca"
const tlsServiceKeyVolumeName = "tls"

func keyHashAnnotationName(serviceName string) string {
	return iotPrefix + "/" + serviceName + "_service-key-hash"
}

// process either the path of service ca, or provided secrets
func handleInterService(infra *iotv1.IoTInfrastructure, handlerServiceCA func() error, handlerSecrets func(iotv1.SecretCertificatesStrategy) error) error {

	if infra.Spec.HasNoInterServiceConfig() {
		// no explicit configuration
		if util.IsOpenshift4() {
			return handlerServiceCA()
		} else {
			return util.NewConfigurationError("Implicit inter-service certificate configuration can only be used on OpenShift 4")
		}
	}

	cfg := infra.Spec.InterServiceCertificates

	if cfg.ServiceCAStrategy != nil {
		if !util.IsOpenshift4() {
			return util.NewConfigurationError("Service CA inter-service certificate configuration can only be used on OpenShift 4")
		} else {
			return handlerServiceCA()
		}
	} else if cfg.SecretCertificatesStrategy != nil {
		return handlerSecrets(*cfg.SecretCertificatesStrategy)
	}

	// unknown configuration

	return util.NewConfigurationError("unknown inter service certificates configuration")

}

// the global service CA infra map
func (r *ReconcileIoTInfrastructure) processInterServiceCAConfigMap(ctx context.Context, infra *iotv1.IoTInfrastructure) error {

	// the configmap to create/delete

	cm := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{
			Name:      iotServiceCaConfigMapName,
			Namespace: r.namespace,
		},
	}

	// handle different paths

	return handleInterService(infra,
		func() error {

			_, err := controllerutil.CreateOrUpdate(ctx, r.client, cm, func() error {

				if err := controllerutil.SetControllerReference(infra, cm, r.scheme); err != nil {
					return err
				}

				if cm.Annotations == nil {
					cm.Annotations = make(map[string]string, 0)
				}

				cm.Annotations[openShiftServiceCAAnnotationInjectBundle] = "true"

				// just create an empty infra map, and set ourselves as controller

				return nil
			})
			return err

		}, func(strategy iotv1.SecretCertificatesStrategy) error {

			// everything is provided, we delete our global one
			key, err := client.ObjectKeyFromObject(cm)
			if err != nil {
				return nil
			}
			_, err = install.DeleteIfOwnedBy(ctx, r.client, key, cm, infra, true)
			if apierrors.IsNotFound(err) {
				return nil
			}
			return err

		})
}

// for deployment

// Apply the inter-service certificate configuration for the deployment.
// If the "serviceName" is empty, then the service does not want to expose an internal service and does not
// receive any key/cert for doing so.
func ApplyInterServiceForDeployment(client client.Client, infra *iotv1.IoTInfrastructure, deployment *appsv1.Deployment, volumeName string, serviceName string) error {

	return handleInterService(infra,
		func() error {
			return applyInterServiceForDeploymentServiceCa(client, deployment, volumeName, serviceName)
		},
		func(strategy iotv1.SecretCertificatesStrategy) error {
			return applyInterServiceForDeploymentSecretCertificates(client, deployment, strategy, volumeName, serviceName)
		})

}

// for statefulset

// Apply the inter-service certificate configuration for the stateful set.
// If the "serviceName" is empty, then the service does not want to expose an internal service and does not
// receive any key/cert for doing so.
func ApplyInterServiceForStatefulSet(client client.Client, infra *iotv1.IoTInfrastructure, statefulSet *appsv1.StatefulSet, volumeName string, serviceName string) error {

	return handleInterService(infra,
		func() error {
			return applyInterServiceForStatefulSetServiceCa(client, statefulSet, volumeName, serviceName)
		},
		func(strategy iotv1.SecretCertificatesStrategy) error {
			return applyInterServiceForStatefulSetSecretCertificates(client, statefulSet, strategy, volumeName, serviceName)
		})

}

// for service

// Apply the inter-service certificate configuration for the service.
// If the "serviceName" is empty, then the service does not want to expose an internal service and does not
// receive any key/cert for doing so.
func ApplyInterServiceForService(infra *iotv1.IoTInfrastructure, service *corev1.Service, serviceName string) error {

	if service.Annotations != nil {
		delete(service.Annotations, openShiftServiceCAAnnotationServingCertAlpha)
	}

	return handleInterService(infra,
		func() error {
			return applyInterServiceForServiceServiceCa(service, serviceName)
		},
		func(strategy iotv1.SecretCertificatesStrategy) error {
			return applyInterServiceForServiceSecretCertificates(service, serviceName)
		})

}

func AppendTrustStores(infra *iotv1.IoTInfrastructure, container *corev1.Container, envs []string) error {
	return handleInterService(infra,
		func() error {
			return appendTrustStoresForServiceCa(container, envs)
		}, func(strategy iotv1.SecretCertificatesStrategy) error {
			return appendTrustStoresForSecretCertificates(container, envs)
		})
}

// service CA

func applyInterServiceForServiceServiceCa(service *corev1.Service, serviceName string) error {

	if serviceName == "" {
		return nil
	}

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	service.Annotations[openShiftServiceCAAnnotationServingCertBeta] = serviceName + "-tls"

	return nil
}

func applyInterServiceForPodServiceCa(client client.Client, namespace string, pod *corev1.PodTemplateSpec, volumeName string, serviceName string) error {

	// mount the global service ca configmap
	install.ApplyConfigMapVolume(&pod.Spec, tlsServiceCAVolumeName, iotServiceCaConfigMapName)
	if err := install.ApplyConfigMapHash(client, pod, iotPrefix+"/service-ca-hash", namespace, iotServiceCaConfigMapName); err != nil {
		return err
	}

	if serviceName != "" {
		// mount the key/cert secret
		install.ApplySecretVolume(&pod.Spec, volumeName, serviceName+"-tls")
		if err := install.ApplySecretHash(client, pod, keyHashAnnotationName(serviceName), namespace, serviceName+"-tls"); err != nil {
			return err
		}
	}

	return nil
}

func applyInterServiceForDeploymentServiceCa(client client.Client, deployment *appsv1.Deployment, volumeName string, serviceName string) error {

	return applyInterServiceForPodServiceCa(client, deployment.Namespace, &deployment.Spec.Template, volumeName, serviceName)

}

func applyInterServiceForStatefulSetServiceCa(client client.Client, statefulSet *appsv1.StatefulSet, volumeName string, serviceName string) error {

	return applyInterServiceForPodServiceCa(client, statefulSet.Namespace, &statefulSet.Spec.Template, volumeName, serviceName)

}

func appendTrustStoresForServiceCa(container *corev1.Container, env []string) error {

	for _, e := range env {
		container.Env = append(container.Env, corev1.EnvVar{Name: e, Value: "/etc/tls-service-ca/service-ca.crt"})
	}

	install.ApplyVolumeMountSimple(container, tlsServiceCAVolumeName, "/etc/tls-service-ca", true)

	return nil
}

// secret certificates

func applyInterServiceForServiceSecretCertificates(service *corev1.Service, _ string) error {

	if service.Annotations != nil {
		delete(service.Annotations, openShiftServiceCAAnnotationServingCertBeta)
	}

	return nil
}

func applyInterServiceForPodSecretCertificates(client client.Client, namespace string, pod *corev1.PodTemplateSpec, cfg iotv1.SecretCertificatesStrategy, volumeName string, serviceName string) error {
	if cfg.CASecretName == "" {
		return util.NewConfigurationError("inter service secret CA name must not be empty")
	}

	// mount the global service ca configmap
	install.ApplySecretVolume(&pod.Spec, tlsServiceCAVolumeName, cfg.CASecretName)
	if err := install.ApplySecretHash(client, pod, iotPrefix+"/service-ca-hash", namespace, cfg.CASecretName, "service-ca.crt"); err != nil {
		return err
	}

	if serviceName != "" {

		// map the service name to a secret name
		mappedSecretName := cfg.ServiceSecretNames[serviceName]
		if mappedSecretName == "" {
			return util.NewConfigurationError("secret name %s mapped to an empty secret name", serviceName)
		}

		// mount the secret for key/cert
		install.ApplySecretVolume(&pod.Spec, volumeName, mappedSecretName)
		if err := install.ApplySecretHash(client, pod, keyHashAnnotationName(serviceName), namespace, mappedSecretName, "tls.crt", "tls.key"); err != nil {
			return err
		}

	}

	return nil
}

func applyInterServiceForDeploymentSecretCertificates(client client.Client, deployment *appsv1.Deployment, cfg iotv1.SecretCertificatesStrategy, volumeName string, serviceName string) error {

	return applyInterServiceForPodSecretCertificates(client, deployment.Namespace, &deployment.Spec.Template, cfg, volumeName, serviceName)

}

func applyInterServiceForStatefulSetSecretCertificates(client client.Client, statefulSet *appsv1.StatefulSet, cfg iotv1.SecretCertificatesStrategy, volumeName string, serviceName string) error {

	return applyInterServiceForPodSecretCertificates(client, statefulSet.Namespace, &statefulSet.Spec.Template, cfg, volumeName, serviceName)

}

func appendTrustStoresForSecretCertificates(container *corev1.Container, env []string) error {

	for _, e := range env {
		container.Env = append(container.Env, corev1.EnvVar{Name: e, Value: "/etc/tls-service-ca/service-ca.crt"})
	}

	install.ApplyVolumeMountSimple(container, tlsServiceCAVolumeName, "/etc/tls-service-ca", true)

	return nil
}
