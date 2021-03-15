/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package authenticationservice

import (
	"fmt"

	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	intstr "k8s.io/apimachinery/pkg/util/intstr"
)

func applyNoneAuthServiceDefaults(authservice *adminv1beta1.AuthenticationService) {
	if authservice.Spec.None == nil {
		authservice.Spec.None = &adminv1beta1.AuthenticationServiceSpecNone{}
	}
	if authservice.Spec.None.CertificateSecret == nil {
		secretName := fmt.Sprintf("%s-cert", authservice.Name)
		authservice.Spec.None.CertificateSecret = &corev1.SecretReference{
			Name: secretName,
		}
	}
}

func applyNoneAuthServiceCert(authservice *adminv1beta1.AuthenticationService, secret *corev1.Secret) error {
	// On OpenShift we use the automatic cluster certificate provider
	install.ApplyDefaultLabels(&secret.ObjectMeta, "none-authservice", secret.Name)
	if !hasEntry(secret, "tls.key") || !hasEntry(secret, "tls.crt") {
		cn := util.ServiceToCommonName(authservice.Namespace, authservice.Name)
		return util.GenerateSelfSignedCertSecret(cn, nil, nil, secret)
	}
	return nil
}

func applyNoneAuthServiceDeployment(authservice *adminv1beta1.AuthenticationService, deployment *appsv1.Deployment) error {
	install.ApplyDeploymentDefaults(deployment, "none-authservice", authservice.Name)
	if err := install.ApplyDeploymentContainerWithError(deployment, "none-authservice", func(container *corev1.Container) error {
		if err := install.ApplyContainerImage(container, "none-authservice", authservice.Spec.None.Image); err != nil {
			return err
		}

		container.Env = []corev1.EnvVar{
			{
				Name:  "LISTENPORT",
				Value: "5671",
			},
			{
				Name:  "HEALTHPORT",
				Value: "8080",
			},
		}

		container.LivenessProbe = &corev1.Probe{
			InitialDelaySeconds: 30,
			Handler: corev1.Handler{
				HTTPGet: &corev1.HTTPGetAction{
					Port:   intstr.FromString("http"),
					Path:   "/healthz",
					Scheme: "HTTP",
				},
			},
		}

		container.Ports = []corev1.ContainerPort{
			{
				ContainerPort: 5671,
				Name:          "amqps",
			},
			{
				ContainerPort: 8080,
				Name:          "http",
			},
		}

		if authservice.Spec.None.Resources != nil {
			container.Resources = *authservice.Spec.None.Resources
		} else {
			container.Resources = corev1.ResourceRequirements{}
		}

		install.ApplyVolumeMountSimple(container, "none-authservice-cert", "/opt/none-authservice/cert", true)

		return nil
	}); err != nil {
		return err
	}

	if authservice.Spec.None.Replicas != nil {
		deployment.Spec.Replicas = authservice.Spec.None.Replicas
	}

	install.ApplySecretVolume(&deployment.Spec.Template.Spec, "none-authservice-cert", authservice.Spec.None.CertificateSecret.Name)

	return nil
}

func applyNoneAuthServiceService(authservice *adminv1beta1.AuthenticationService, service *corev1.Service) error {
	install.ApplyServiceDefaults(service, "none-authservice", authservice.Name)
	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}

	install.ApplyOpenShiftServingCertAnnotation(service.Annotations, authservice.Spec.None.CertificateSecret.Name, util.IsOpenshift, util.IsOpenshift4)

	service.Spec.Ports = []corev1.ServicePort{
		{
			Port:       5671,
			Protocol:   corev1.ProtocolTCP,
			TargetPort: intstr.FromString("amqps"),
			Name:       "amqps",
		},
	}
	return nil
}
