/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package authenticationservice

import (
	"context"
	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	intstr "k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func applyNoneAuthServiceDefaults(ctx context.Context, client client.Client, scheme *runtime.Scheme, authservice *adminv1beta1.AuthenticationService) error {
	if authservice.Spec.None == nil {
		authservice.Spec.None = &adminv1beta1.AuthenticationServiceSpecNone{}
	}
	if authservice.Spec.None.CertificateSecret == nil {
		secretName := "none-authservice-cert"
		authservice.Spec.None.CertificateSecret = &corev1.SecretReference{
			Name: secretName,
		}
		if !util.IsOpenshift() {
			err := util.CreateSecret(ctx, client, scheme, authservice.Namespace, secretName, authservice, func(secret *corev1.Secret) error {
				install.ApplyDefaultLabels(&secret.ObjectMeta, "none-authservice", secretName)
				cn := util.ServiceToCommonName(authservice.Namespace, authservice.Name)
				return util.GenerateSelfSignedCertSecret(cn, nil, nil, secret)
			})
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func applyNoneAuthServiceDeployment(authservice *adminv1beta1.AuthenticationService, deployment *appsv1.Deployment) error {
	install.ApplyDeploymentDefaults(deployment, "none-authservice", authservice.Name)
	install.ApplyContainer(deployment, "none-authservice", func(container *corev1.Container) {
		install.ApplyContainerImage(container, "none-authservice", authservice.Spec.None.Image)
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

		install.ApplyVolumeMountSimple(container, "none-authservice-cert", "/opt/none-authservice/cert", true)
	})

	install.ApplySecretVolume(deployment, "none-authservice-cert", authservice.Spec.None.CertificateSecret.Name)

	return nil
}

func applyNoneAuthServiceService(authservice *adminv1beta1.AuthenticationService, service *corev1.Service) error {
	install.ApplyServiceDefaults(service, "none-authservice", authservice.Name)
	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}
	service.Annotations["service.alpha.openshift.io/serving-cert-secret-name"] = "none-authservice-cert"
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
