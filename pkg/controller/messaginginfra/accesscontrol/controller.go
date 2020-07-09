/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package accesscontrol

import (
	"context"
	"fmt"
	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/common"
	"github.com/enmasseproject/enmasse/pkg/state"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/go-logr/logr"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

type AccessControlController struct {
	client         client.Client
	scheme         *runtime.Scheme
	certController *cert.CertController
	clientManager  state.ClientManager
}

func NewAccessControlController(client client.Client, scheme *runtime.Scheme, certController *cert.CertController) *AccessControlController {
	clientManager := state.GetClientManager()
	return &AccessControlController{
		client:         client,
		scheme:         scheme,
		certController: certController,
		clientManager:  clientManager,
	}
}

func getAccessControlInfraConfigName(infra *v1.MessagingInfrastructure) string {
	return fmt.Sprintf("access-control-%s", infra.Name)
}

func (c AccessControlController) ReconcileAccessControl(ctx context.Context, logger logr.Logger, infra *v1.MessagingInfrastructure) (interface{}, error) {

	logger.Info("Reconciling access control", "accessControl", infra.Spec.AccessControl)

	accessControlInfraName := getAccessControlInfraConfigName(infra)
	certSecretName := cert.GetCertSecretName(accessControlInfraName)

	accessControlServiceName := fmt.Sprintf("%s", accessControlInfraName)

	// Reconcile deployment
	deployment := &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: accessControlInfraName},
	}
	_, err := controllerutil.CreateOrUpdate(ctx, c.client, deployment, func() error {
		if err := controllerutil.SetControllerReference(infra, deployment, c.scheme); err != nil {
			return err
		}

		install.ApplyDeploymentDefaults(deployment, "accesscontrol", infra.Name)
		deployment.Labels[common.LABEL_INFRA] = infra.Name
		deployment.Spec.Replicas = infra.Spec.AccessControl.Replicas
		deployment.Spec.Template.Labels[common.LABEL_INFRA] = infra.Name
		deployment.Spec.Template.Annotations[common.ANNOTATION_INFRA_NAME] = infra.Name
		deployment.Spec.Template.Annotations[common.ANNOTATION_INFRA_NAMESPACE] = infra.Namespace

		deployment.Annotations[common.ANNOTATION_INFRA_NAME] = infra.Name
		deployment.Annotations[common.ANNOTATION_INFRA_NAMESPACE] = infra.Namespace

		containers, err := install.ApplyContainerWithError(deployment.Spec.Template.Spec.Containers, "access-control-server", func(container *corev1.Container) error {
			err := install.ApplyContainerImage(container, "access-control-server", infra.Spec.Router.Image)
			if err != nil {
				return err
			}

			install.ApplyVolumeMountSimple(container, "certs", "/etc/enmasse-certs", false)
			install.ApplyEnvSimple(container, "TLS_KEY_FILE", "/etc/enmasse-certs/tls.key")
			install.ApplyEnvSimple(container, "TLS_CERT_FILE", "/etc/enmasse-certs/tls.crt")
			install.ApplyEnvSimple(container, "PORT", "5671")
			install.ApplyEnvSimple(container, "BIND_ADDRESS", "0.0.0.0")

			container.Ports = []corev1.ContainerPort{
				{
					ContainerPort: 5671,
					Name:          "amqps-auth",
				},
			}

			// TODO
			//container.LivenessProbe = &corev1.Probe{
			//	Handler: corev1.Handler{
			//		HTTPGet: &corev1.HTTPGetAction{
			//			Path:   "/healthz",
			//			Scheme: corev1.URISchemeHTTP,
			//			Port:   intstr.FromString("liveness"),
			//		},
			//	},
			//	InitialDelaySeconds: 30,
			//}
			//
			//container.ReadinessProbe = &corev1.Probe{
			//	Handler: corev1.Handler{
			//		HTTPGet: &corev1.HTTPGetAction{
			//			Path:   "/healthz",
			//			Scheme: corev1.URISchemeHTTP,
			//			Port:   intstr.FromString("readiness"),
			//		},
			//	},
			//	InitialDelaySeconds: 10,
			//}

			return nil
		})
		if err != nil {
			return err
		}
		deployment.Spec.Template.Spec.Containers = containers

		install.ApplySecretVolume(&deployment.Spec.Template.Spec, "certs", certSecretName)
		return nil
	})

	if err != nil {
		return nil, err
	}

	// Originally I was planning a service per endpoint... but...

	// In order to lookup the messagingendpoint configuration, rather than rely on SNI as described in the
	// design, we could allocate a separate port per endpoint (i.e. allocate a pool here like it already done
	// for router, and then in acccess-control-server use the local end of the TCP connection to map back to
	// the endpoint and tenant.  That would save n certificates and n services (where n is the number of
	// endpoints)

	// Reconcile access-control service
	accessControlService := &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: accessControlServiceName},
	}
	_, err = controllerutil.CreateOrUpdate(ctx, c.client, accessControlService, func() error {
		if err := controllerutil.SetControllerReference(infra, accessControlService, c.scheme); err != nil {
			return err
		}
		install.ApplyServiceDefaults(accessControlService, "accesscontrol", infra.Name)
		accessControlService.Spec.ClusterIP = "None"
		accessControlService.Spec.Selector = deployment.Spec.Template.Labels
		accessControlService.Spec.Ports = []corev1.ServicePort{
			{
				Port:       5671,
				Protocol:   corev1.ProtocolTCP,
				TargetPort: intstr.FromString("amqps-auth"),
				Name:       "amqps-auth",
			},
		}

		return nil
	})
	if err != nil {
		return nil, err
	}

	// Reconcile router certificate
	_, err = c.certController.ReconcileCert(ctx, logger, infra, deployment,
		fmt.Sprintf("%s", accessControlService.Name),
		fmt.Sprintf("%s.%s.svc", accessControlService.Name, accessControlService.Namespace),
		fmt.Sprintf("*.%s.%s.svc", accessControlService.Name, accessControlService.Namespace))
	if err != nil {
		return nil, err
	}

	return nil, nil
}
