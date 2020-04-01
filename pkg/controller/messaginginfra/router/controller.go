/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package router

import (
	"context"
	"fmt"

	logr "github.com/go-logr/logr"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/common"
	"github.com/enmasseproject/enmasse/pkg/util/install"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	intstr "k8s.io/apimachinery/pkg/util/intstr"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

type RouterController struct {
	client         client.Client
	scheme         *runtime.Scheme
	certController *cert.CertController
}

func NewRouterController(client client.Client, scheme *runtime.Scheme, certController *cert.CertController) *RouterController {
	return &RouterController{
		client:         client,
		scheme:         scheme,
		certController: certController,
	}
}

/*
 * Reconciles the router instances for an instance of shared infrastructure.
 */
func (r *RouterController) ReconcileRouters(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra) error {

	logger.Info("Reconciling routers", "router", infra.Spec.Router)

	setDefaultRouterScalingStrategy(&infra.Spec.Router)

	routerInfraName := getRouterInfraName(infra)
	statefulset := &appsv1.StatefulSet{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: routerInfraName},
	}
	certSecretName := cert.GetCertSecretName(statefulset)

	// Update router condition
	routersCreated := infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraRoutersCreated)
	routersCreated.SetStatus(corev1.ConditionTrue, "", "")

	return common.WithConditionUpdate(routersCreated, func() error {
		_, err := controllerutil.CreateOrUpdate(ctx, r.client, statefulset, func() error {
			if err := controllerutil.SetControllerReference(infra, statefulset, r.scheme); err != nil {
				return err
			}

			install.ApplyStatefulSetDefaults(statefulset, "router", infra.Name)
			statefulset.Labels["infra"] = infra.Name
			statefulset.Spec.Template.Labels["infra"] = infra.Name

			applyScalingStrategy(infra.Spec.Router.ScalingStrategy, statefulset)

			containers, err := install.ApplyContainerWithError(statefulset.Spec.Template.Spec.Containers, "router", func(container *corev1.Container) error {
				err := install.ApplyContainerImage(container, "router", infra.Spec.Router.Image)
				if err != nil {
					return err
				}

				install.ApplyVolumeMountSimple(container, "certs", "/etc/enmasse-certs", false)
				install.ApplyVolumeMountSimple(container, "config", "/etc/qpid-dispatch/config", false)

				install.ApplyEnvSimple(container, "INFRA_NAME", infra.Name)
				install.ApplyEnvSimple(container, "QDROUTERD_CONF", "/etc/qpid-dispatch/config/qdrouterd.json")
				install.ApplyEnvSimple(container, "QDROUTERD_CONF_TYPE", "json")
				install.ApplyEnvSimple(container, "QDROUTERD_AUTO_MESH_DISCOVERY", "INFER")
				install.ApplyEnvSimple(container, "QDROUTERD_AUTO_MESH_SERVICE_NAME", routerInfraName)

				container.Ports = []corev1.ContainerPort{{
					ContainerPort: 55672,
					Name:          "inter-router",
				}}

				return nil
			})
			if err != nil {
				return err
			}
			statefulset.Spec.Template.Spec.Containers = containers
			statefulset.Spec.ServiceName = routerInfraName

			install.ApplyConfigMapVolume(&statefulset.Spec.Template.Spec, "config", routerInfraName)
			install.ApplySecretVolume(&statefulset.Spec.Template.Spec, "certs", certSecretName)
			return nil
		})

		if err != nil {
			return err
		}

		service := &corev1.Service{
			ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: routerInfraName},
		}

		_, err = controllerutil.CreateOrUpdate(ctx, r.client, service, func() error {
			if err := controllerutil.SetControllerReference(infra, service, r.scheme); err != nil {
				return err
			}
			install.ApplyServiceDefaults(service, "router", infra.Name)
			service.Spec.ClusterIP = "None"
			service.Spec.Selector = statefulset.Spec.Template.Labels
			service.Spec.Ports = []corev1.ServicePort{
				{
					Port:       55672,
					Protocol:   corev1.ProtocolTCP,
					TargetPort: intstr.FromString("inter-router"),
					Name:       "inter-router",
				},
			}

			return nil
		})
		if err != nil {
			return err
		}

		routerConfig := generateConfig(&infra.Spec.Router)
		routerConfigBytes, err := serializeConfig(&routerConfig)
		if err != nil {
			return err
		}
		config := &corev1.ConfigMap{
			ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: routerInfraName},
			Data: map[string]string{
				"qdrouterd.json": string(routerConfigBytes),
			},
		}

		_, err = controllerutil.CreateOrUpdate(ctx, r.client, config, func() error {
			if err := controllerutil.SetControllerReference(infra, config, r.scheme); err != nil {
				return err
			}
			return nil
		})
		if err != nil {
			return err
		}

		_, err = r.certController.ReconcileCert(ctx, logger, infra, statefulset)
		if err != nil {
			return err
		}
		return nil
	})
}

func getRouterInfraName(infra *v1beta2.MessagingInfra) string {
	return fmt.Sprintf("router-%s", infra.Name)
}

func setDefaultRouterScalingStrategy(router *v1beta2.MessagingInfraSpecRouter) {
	if router.ScalingStrategy == nil {
		// Set static scaler by default
		router.ScalingStrategy = &v1beta2.MessagingInfraSpecRouterScalingStrategy{
			Static: &v1beta2.MessagingInfraSpecRouterScalingStrategyStatic{
				Replicas: 1,
			},
		}
	}
}

func int32ptr(val int32) *int32 {
	return &val
}

func applyScalingStrategy(strategy *v1beta2.MessagingInfraSpecRouterScalingStrategy, set *appsv1.StatefulSet) {
	if strategy.Static != nil {
		set.Spec.Replicas = int32ptr(strategy.Static.Replicas)
	}
}
