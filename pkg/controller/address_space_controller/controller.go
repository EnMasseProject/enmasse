/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package address_space_controller

import (
	"context"
	"os"
	"time"

	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/images"
	"github.com/enmasseproject/enmasse/pkg/util/install"

	"github.com/go-logr/logr"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/source"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	resource "k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	runtime "k8s.io/apimachinery/pkg/runtime"
	schema "k8s.io/apimachinery/pkg/runtime/schema"
	controllertypes "k8s.io/apimachinery/pkg/types"
	intstr "k8s.io/apimachinery/pkg/util/intstr"
	kubernetes "k8s.io/client-go/kubernetes"
)

var log = logf.Log.WithName("controller_address_space_controller")

const ADDRESS_SPACE_CONTROLLER_NAME = "address-space-controller"
const ANNOTATION_VERSION = "enmasse.io/version"
const ENV_VERSION = "VERSION"

var _ reconcile.Reconciler = &ReconcileAddressSpaceController{}

type ReconcileAddressSpaceController struct {
	client     client.Client
	kubeClient *kubernetes.Clientset
	scheme     *runtime.Scheme
	namespace  string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	reconciler, err := newReconciler(mgr)
	if err != nil {
		return err
	}
	return add(mgr, reconciler)
}

func newReconciler(mgr manager.Manager) (*ReconcileAddressSpaceController, error) {
	// Create initial deployment if it does not exist
	client, err := kubernetes.NewForConfig(mgr.GetConfig())
	if err != nil {
		log.Error(err, "Error creating kubernetes client")
		return nil, err
	}
	return &ReconcileAddressSpaceController{
		client:     mgr.GetClient(),
		kubeClient: client,
		scheme:     mgr.GetScheme(),
		namespace:  util.GetEnvOrDefault("NAMESPACE", "enmasse-infra"),
	}, nil
}

func add(mgr manager.Manager, r *ReconcileAddressSpaceController) error {

	// Create initial deployment if it does not exist. We cannot yet rely on
	// the controller-runtime client, as the runtime has not yet started.
	deploymentClient := r.kubeClient.AppsV1().Deployments(r.namespace)
	_, err := deploymentClient.Get(ADDRESS_SPACE_CONTROLLER_NAME, metav1.GetOptions{})
	if err != nil {
		if k8errors.IsNotFound(err) {
			_, err = r.Reconcile(reconcile.Request{NamespacedName: controllertypes.NamespacedName{
				Namespace: r.namespace,
				Name:      ADDRESS_SPACE_CONTROLLER_NAME,
			}})
		} else {
			return err
		}
	}

	// Start reconciler for address-space-controller deployment
	c, err := controller.New("address-space-controller-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	return c.Watch(&source.Kind{Type: &appsv1.Deployment{}}, &handler.EnqueueRequestForObject{})
}

func (r *ReconcileAddressSpaceController) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	expectedName := controllertypes.NamespacedName{
		Namespace: r.namespace,
		Name:      ADDRESS_SPACE_CONTROLLER_NAME,
	}

	if expectedName == request.NamespacedName {
		reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
		reqLogger.Info("Reconciling Address Space Controller")

		ctx := context.TODO()

		_, err := r.ensureDeployment(ctx, request, reqLogger)
		if err != nil {
			reqLogger.Error(err, "Error creating address space controller deployment")
			return reconcile.Result{}, err
		}

		_, err = r.ensureService(ctx, request, reqLogger)
		if err != nil {
			reqLogger.Error(err, "Error creating address space controller service")
			return reconcile.Result{}, err
		}
	}
	return reconcile.Result{RequeueAfter: 30 * time.Second}, nil
}

func (r *ReconcileAddressSpaceController) ensureDeployment(ctx context.Context, request reconcile.Request, reqLogger logr.Logger) (reconcile.Result, error) {

	deployment := &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      request.NamespacedName.Name,
			Namespace: request.NamespacedName.Namespace,
		},
	}

	deployment.SetGroupVersionKind(schema.GroupVersionKind{
		Group:   "apps",
		Kind:    "Deployment",
		Version: "v1",
	})

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, deployment, func() error {
		return ApplyDeployment(deployment)
	})

	return reconcile.Result{}, err
}

func ApplyDeployment(deployment *appsv1.Deployment) error {
	install.ApplyDeploymentDefaults(deployment, "address-space-controller", deployment.Name)
	err := install.ApplyContainerWithError(deployment, "address-space-controller", func(container *corev1.Container) error {
		install.ApplyContainerImage(container, "address-space-controller", nil)
		install.ApplyHttpProbe(container.LivenessProbe, 30, "/healthz", 8080)
		install.ApplyHttpProbe(container.ReadinessProbe, 30, "/healthz", 8080)
		container.Ports = []corev1.ContainerPort{
			{Name: "http", ContainerPort: 8080, Protocol: corev1.ProtocolTCP},
		}
		install.ApplyEnvSimple(container, "JAVA_OPTS", "-verbose:gc")
		install.ApplyEnvSimple(container, "ENABLE_EVENT_LOGGER", "true")
		install.ApplyEnvSimple(container, "TEMPLATE_DIR", "/opt/templates")
		install.ApplyEnvSimple(container, "RESOURCES_DIR", "/opt")
		t := true
		install.ApplyEnvConfigMap(container, "WILDCARD_ENDPOINT_CERT_SECRET", "wildcardEndpointCertSecret", "address-space-controller-config", &t)
		install.ApplyEnvConfigMap(container, "RESYNC_INTERVAL", "resyncInterval", "address-space-controller-config", &t)
		install.ApplyEnvConfigMap(container, "RECHECK_INTERVAL", "recheckInterval", "address-space-controller-config", &t)

		install.ApplyEnvSimple(container, "IMAGE_PULL_POLICY", string(images.PullPolicyFromImageName(container.Image)))
		applyImageEnv(container, "ROUTER_IMAGE", "router")
		applyImageEnv(container, "STANDARD_CONTROLLER_IMAGE", "standard-controller")
		applyImageEnv(container, "AGENT_IMAGE", "agent")
		applyImageEnv(container, "BROKER_IMAGE", "broker")
		applyImageEnv(container, "BROKER_PLUGIN_IMAGE", "broker-plugin")
		applyImageEnv(container, "TOPIC_FORWARDER_IMAGE", "topic-forwarder")
		applyImageEnv(container, "MQTT_GATEWAY_IMAGE", "mqtt-gateway")
		applyImageEnv(container, "MQTT_LWT_IMAGE", "mqtt-lwt")

		memoryEnv := util.GetEnvOrDefault("ADDRSS_SPACE_CONTROLLER_MEMORY_LIMIT", "512Mi")
		memoryLimit, err := resource.ParseQuantity(memoryEnv)
		if err != nil {
			return err
		}

		container.Resources = corev1.ResourceRequirements{
			Requests: corev1.ResourceList{
				corev1.ResourceMemory: memoryLimit,
			},
			Limits: corev1.ResourceList{
				corev1.ResourceMemory: memoryLimit,
			},
		}
		return nil
	})
	if err != nil {
		return err
	}
	// This means we are running
	version, ok := os.LookupEnv(ENV_VERSION)
	if ok {
		deployment.Annotations[ANNOTATION_VERSION] = version
	}

	var one int32 = 1
	deployment.Spec.Template.Spec.ServiceAccountName = "address-space-controller"
	deployment.Spec.Replicas = &one
	install.ApplyNodeAffinity(&deployment.Spec.Template, "node-role.enmasse.io/operator-infra")
	install.ApplyFsGroupOverride(deployment)

	deployment.SetGroupVersionKind(schema.GroupVersionKind{
		Group:   "apps",
		Kind:    "Deployment",
		Version: "v1",
	})

	return nil
}

func applyImageEnv(container *corev1.Container, env string, imageName string) error {
	image, err := images.GetImage(imageName)
	if err != nil {
		return err
	}
	install.ApplyEnvSimple(container, env, image)
	return nil
}

func (r *ReconcileAddressSpaceController) ensureService(ctx context.Context, request reconcile.Request, reqLogger logr.Logger) (reconcile.Result, error) {

	service := &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      request.NamespacedName.Name,
			Namespace: request.NamespacedName.Namespace,
		},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, service, func() error {
		return applyService(service)
	})

	return reconcile.Result{}, err
}

func applyService(service *corev1.Service) error {
	install.ApplyServiceDefaults(service, service.Name, service.Name)
	service.Spec.Ports = []corev1.ServicePort{
		{
			Port:       8080,
			Protocol:   corev1.ProtocolTCP,
			TargetPort: intstr.FromString("http"),
			Name:       "health",
		},
	}
	return nil
}
