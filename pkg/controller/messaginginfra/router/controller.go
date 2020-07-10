/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package router

import (
	"context"
	"fmt"
	"sort"

	"crypto/sha256"
	"encoding/hex"

	logr "github.com/go-logr/logr"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/common"
	. "github.com/enmasseproject/enmasse/pkg/state/common"
	"github.com/enmasseproject/enmasse/pkg/util/install"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	intstr "k8s.io/apimachinery/pkg/util/intstr"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

const ANNOTATION_ROUTER_CONFIG_DIGEST = "enmasse.io/router-config-digest"

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
func (r *RouterController) ReconcileRouters(ctx context.Context, logger logr.Logger, infra *v1.MessagingInfrastructure) ([]Host, error) {

	setDefaultRouterScalingStrategy(&infra.Spec.Router)

	logger.Info("Reconciling routers", "router", infra.Spec.Router)

	routerInfraName := infra.GetRouterInfraName()
	certSecretName := cert.GetCertSecretName(routerInfraName)

	// Reconcile static router config
	routerConfig := generateConfig(infra, &infra.Spec.Router)
	routerConfigBytes, err := serializeConfig(&routerConfig)
	if err != nil {
		return nil, err
	}
	config := &corev1.ConfigMap{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: routerInfraName},
	}

	_, err = controllerutil.CreateOrUpdate(ctx, r.client, config, func() error {
		if err := controllerutil.SetControllerReference(infra, config, r.scheme); err != nil {
			return err
		}
		config.Data = map[string]string{
			"qdrouterd.json": string(routerConfigBytes),
		}
		return nil
	})
	if err != nil {
		return nil, err
	}

	rawSha := sha256.Sum256(routerConfigBytes)
	routerConfigSha := hex.EncodeToString(rawSha[:])

	// Reconcile the project certificate secret. This secret is created by infra, but is updated by the endpoints controller.
	projectSecretName := cert.GetProjectSecretName(infra.Name)
	projectSecret := &corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: projectSecretName},
	}

	certSha := sha256.New()

	_, err = controllerutil.CreateOrUpdate(ctx, r.client, projectSecret, func() error {
		if err := controllerutil.SetControllerReference(infra, projectSecret, r.scheme); err != nil {
			return err
		}
		keys := make([]string, 0, len(projectSecret.Data))
		for key, _ := range projectSecret.Data {
			keys = append(keys, key)
		}
		sort.Strings(keys)
		for _, key := range keys {
			value := projectSecret.Data[key]
			_, err := certSha.Write([]byte(key))
			if err != nil {
				return err
			}
			_, err = certSha.Write(value)
			if err != nil {
				return err
			}
		}
		return nil
	})
	if err != nil {
		return nil, err
	}

	certShaSum := make([]byte, 0, certSha.Size())
	certShaSum = certSha.Sum(certShaSum)
	routerCertSha := hex.EncodeToString(certShaSum[:])

	meshServiceName := fmt.Sprintf("%s", routerInfraName)

	// Reconcile statefulset of the router
	statefulset := &appsv1.StatefulSet{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: routerInfraName},
	}
	_, err = controllerutil.CreateOrUpdate(ctx, r.client, statefulset, func() error {
		if err := controllerutil.SetControllerReference(infra, statefulset, r.scheme); err != nil {
			return err
		}

		install.ApplyStatefulSetDefaults(statefulset, "router", infra.Name)
		statefulset.Labels[common.LABEL_INFRA] = infra.Name
		statefulset.Spec.Template.Labels[common.LABEL_INFRA] = infra.Name
		statefulset.Spec.Template.Annotations[common.ANNOTATION_INFRA_NAME] = infra.Name
		statefulset.Spec.Template.Annotations[common.ANNOTATION_INFRA_NAMESPACE] = infra.Namespace

		statefulset.Annotations[common.ANNOTATION_INFRA_NAME] = infra.Name
		statefulset.Annotations[common.ANNOTATION_INFRA_NAMESPACE] = infra.Namespace
		statefulset.Annotations[ANNOTATION_ROUTER_CONFIG_DIGEST] = routerConfigSha

		applyScalingStrategy(infra.Spec.Router.ScalingStrategy, statefulset)

		containers, err := install.ApplyContainerWithError(statefulset.Spec.Template.Spec.Containers, "router", func(container *corev1.Container) error {
			err := install.ApplyContainerImage(container, "router", infra.Spec.Router.Image)
			if err != nil {
				return err
			}

			install.ApplyVolumeMountSimple(container, "certs", "/etc/enmasse-certs", false)
			install.ApplyVolumeMountSimple(container, "config", "/etc/qpid-dispatch/config", false)
			install.ApplyVolumeMountSimple(container, "project-certs", "/etc/enmasse-project-certs", false)

			install.ApplyEnvSimple(container, "INFRA_NAME", infra.Name)
			install.ApplyEnvSimple(container, "QDROUTERD_CONF", "/etc/qpid-dispatch/config/qdrouterd.json")
			install.ApplyEnvSimple(container, "QDROUTERD_CONF_TYPE", "json")
			install.ApplyEnvSimple(container, "QDROUTERD_AUTO_MESH_DISCOVERY", "INFER")
			install.ApplyEnvSimple(container, "QDROUTERD_AUTO_MESH_SERVICE_NAME", meshServiceName)

			container.Ports = []corev1.ContainerPort{
				{
					ContainerPort: 55672,
					Name:          "inter-router",
				},
				{
					ContainerPort: 55671,
					Name:          "operator",
				},
				{
					ContainerPort: 55667,
					Name:          "cluster",
				},
				{
					ContainerPort: 7777,
					Name:          "management",
				},
				{
					ContainerPort: 7778,
					Name:          "liveness",
				},
				{
					ContainerPort: 7779,
					Name:          "readiness",
				},
			}

			container.LivenessProbe = &corev1.Probe{
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Path:   "/healthz",
						Scheme: corev1.URISchemeHTTP,
						Port:   intstr.FromString("liveness"),
					},
				},
				InitialDelaySeconds: 30,
			}

			container.ReadinessProbe = &corev1.Probe{
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Path:   "/healthz",
						Scheme: corev1.URISchemeHTTP,
						Port:   intstr.FromString("readiness"),
					},
				},
				InitialDelaySeconds: 10,
			}

			for i := 40000; i < 40100; i++ {
				container.Ports = append(container.Ports, corev1.ContainerPort{
					ContainerPort: int32(i),
					Name:          fmt.Sprintf("project%d", i),
				})
			}

			return nil
		})
		if err != nil {
			return err
		}
		statefulset.Spec.Template.Spec.Containers = containers
		statefulset.Spec.ServiceName = meshServiceName

		install.ApplyConfigMapVolume(&statefulset.Spec.Template.Spec, "config", routerInfraName)
		install.ApplySecretVolume(&statefulset.Spec.Template.Spec, "certs", certSecretName)
		install.ApplySecretVolume(&statefulset.Spec.Template.Spec, "project-certs", projectSecretName)
		return nil
	})

	if err != nil {
		return nil, err
	}

	// Reconcile router service
	meshService := &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: meshServiceName},
	}
	_, err = controllerutil.CreateOrUpdate(ctx, r.client, meshService, func() error {
		if err := controllerutil.SetControllerReference(infra, meshService, r.scheme); err != nil {
			return err
		}
		install.ApplyServiceDefaults(meshService, "router", infra.Name)
		meshService.Spec.ClusterIP = "None"
		meshService.Spec.Selector = statefulset.Spec.Template.Labels
		meshService.Spec.Ports = []corev1.ServicePort{
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
		return nil, err
	}

	// Reconcile internal cluster router service
	internalClusterServiceName := infra.GetInternalClusterServiceName()
	internalClusterService := &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: internalClusterServiceName},
	}
	_, err = controllerutil.CreateOrUpdate(ctx, r.client, internalClusterService, func() error {
		if err := controllerutil.SetControllerReference(infra, internalClusterService, r.scheme); err != nil {
			return err
		}
		install.ApplyServiceDefaults(internalClusterService, "router", infra.Name)
		internalClusterService.Spec.Type = corev1.ServiceTypeClusterIP
		internalClusterService.Spec.Selector = statefulset.Spec.Template.Labels
		internalClusterService.Spec.Ports = []corev1.ServicePort{
			{
				Port:       55667,
				Protocol:   corev1.ProtocolTCP,
				TargetPort: intstr.FromString("cluster"),
				Name:       "cluster",
			},
		}

		return nil
	})
	if err != nil {
		return nil, err
	}

	// Reconcile router certificate
	_, err = r.certController.ReconcileCert(ctx, logger, infra, statefulset,
		fmt.Sprintf("%s", meshService.Name),
		fmt.Sprintf("%s.%s.svc", meshService.Name, meshService.Namespace),
		fmt.Sprintf("*.%s.%s.svc", meshService.Name, meshService.Namespace),
		fmt.Sprintf("%s", internalClusterService.Name),
		fmt.Sprintf("%s.%s.svc", internalClusterService.Name, internalClusterService.Namespace),
		fmt.Sprintf("*.%s.%s.svc", internalClusterService.Name, internalClusterService.Namespace))
	if err != nil {
		return nil, err
	}

	// Annotate pods with sha to trigger their update without redeploying them
	pods := &corev1.PodList{}
	err = r.client.List(ctx, pods, client.MatchingLabels(statefulset.Spec.Template.Labels), client.InNamespace(infra.Namespace))
	if err != nil {
		return nil, err
	}

	for _, pod := range pods.Items {
		if pod.Annotations[common.ANNOTATION_CERT_DIGEST] != routerCertSha {
			logger.Info("Patching pod with updated cert digest", "pod", pod.Name, "oldsha256", pod.Annotations[common.ANNOTATION_CERT_DIGEST], "sha256", routerCertSha)
			pod.Annotations[common.ANNOTATION_CERT_DIGEST] = routerCertSha
			err := r.client.Update(ctx, &pod)
			if err != nil {
				return nil, err
			}
		}

	}

	// Update expected routers
	expectedPods := int(*statefulset.Spec.Replicas)
	allHosts := make([]Host, 0)
	for i := 0; i < expectedPods; i++ {
		podIp := ""
		expectedHost := fmt.Sprintf("%s-%d.%s.%s.svc", statefulset.Name, i, statefulset.Name, statefulset.Namespace)
		for _, pod := range pods.Items {
			if pod.Status.Phase == corev1.PodRunning && pod.Status.PodIP != "" {
				host := fmt.Sprintf("%s.%s.%s.svc", pod.Name, statefulset.Name, statefulset.Namespace)
				if host == expectedHost {
					podIp = pod.Status.PodIP
					break
				}
			}
		}
		allHosts = append(allHosts, Host{Hostname: expectedHost, Ip: podIp})
	}

	return allHosts, nil
}

func setDefaultRouterScalingStrategy(router *v1.MessagingInfrastructureSpecRouter) {
	if router.ScalingStrategy == nil {
		// Set static scaler by default
		router.ScalingStrategy = &v1.MessagingInfrastructureSpecRouterScalingStrategy{
			Static: &v1.MessagingInfrastructureSpecRouterScalingStrategyStatic{
				Replicas: 1,
			},
		}
	}
}

func int32ptr(val int32) *int32 {
	return &val
}

func applyScalingStrategy(strategy *v1.MessagingInfrastructureSpecRouterScalingStrategy, set *appsv1.StatefulSet) {
	if strategy.Static != nil {
		set.Spec.Replicas = int32ptr(strategy.Static.Replicas)
	}
}
