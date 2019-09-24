/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package address_space_controller_deployment

import (
	"errors"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/images"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"os"
	"strconv"
	"time"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	resource "k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	labels "k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	intstr "k8s.io/apimachinery/pkg/util/intstr"
	kubernetes "k8s.io/client-go/kubernetes"
	corev1client "k8s.io/client-go/kubernetes/typed/core/v1"
	rest "k8s.io/client-go/rest"
	aggregatorclient "k8s.io/kube-aggregator/pkg/client/clientset_generated/clientset"
	apiserviceclient "k8s.io/kube-aggregator/pkg/client/clientset_generated/clientset/typed/apiregistration/v1"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
)

var log = logf.Log.WithName("controller_address_space_controller_deployer")

const ADDRESS_SPACE_CONTROLLER_NAME = "address-space-controller"
const ANNOTATION_VERSION = "enmasse.io/version"
const ENV_VERSION = "VERSION"

type AddressSpaceControllerDeployment struct {
	client    *kubernetes.Clientset
	config    *rest.Config
	scheme    *runtime.Scheme
	namespace string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	dep, err := newDeployment(mgr)
	if err != nil {
		return err
	}
	return add(mgr, dep)
}

func newDeployment(mgr manager.Manager) (*AddressSpaceControllerDeployment, error) {
	client, err := kubernetes.NewForConfig(mgr.GetConfig())
	if err != nil {
		log.Error(err, "Error creating kubernetes client")
		return nil, err
	}
	return &AddressSpaceControllerDeployment{client: client, config: mgr.GetConfig(), scheme: mgr.GetScheme(), namespace: util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")}, nil
}

func add(mgr manager.Manager, d *AddressSpaceControllerDeployment) error {

	err := d.ensureDeployment()
	if err != nil {
		log.Error(err, "Error creating address space controller deployment")
		return err
	}

	err = d.ensureService()
	if err != nil {
		log.Error(err, "Error creating address space controller service")
		return err
	}

	return nil
}

func (d *AddressSpaceControllerDeployment) ensureDeployment() error {

	deploymentClient := d.client.AppsV1().Deployments(d.namespace)

	var deployment *appsv1.Deployment
	deployment, err := deploymentClient.Get(ADDRESS_SPACE_CONTROLLER_NAME, metav1.GetOptions{})
	if err != nil && k8errors.IsNotFound(err) {
		deployment := &appsv1.Deployment{
			ObjectMeta: metav1.ObjectMeta{Namespace: d.namespace, Name: ADDRESS_SPACE_CONTROLLER_NAME},
		}
		err = applyDeployment(deployment)
		if err != nil {
			return err
		}
		_, err := deploymentClient.Create(deployment)
		return err
	} else {
		if _, ok := deployment.Annotations[ANNOTATION_VERSION]; !ok {
			log.Info("address-space-controller is missing annotation, initiating upgrade")
			err = d.performUpgrade(deployment)
			if err != nil {
				return err
			}
		}

		err = applyDeployment(deployment)
		if err != nil {
			return err
		}
		log.Info("Updating address-space-controller deployment")
		_, err := deploymentClient.Update(deployment)
		return err
	}
}

func (d *AddressSpaceControllerDeployment) performUpgrade(deployment *appsv1.Deployment) error {

	podClient := d.client.CoreV1().Pods(d.namespace)
	deploymentClient := d.client.AppsV1().Deployments(d.namespace)
	var zero int32 = 0

	// Scale down api-server
	apiServerDeployment, err := deploymentClient.Get("api-server", metav1.GetOptions{})
	if err != nil {
		return err
	}
	log.Info("Downscaling api-server", "deployment", apiServerDeployment.Name)
	apiServerReplicas := *apiServerDeployment.Spec.Replicas
	apiServerDeployment.Spec.Replicas = &zero
	_, err = deploymentClient.Update(apiServerDeployment)
	if err != nil {
		return err
	}
	err = waitForReplicas(podClient, apiServerDeployment, 0)
	if err != nil {
		return err
	}
	log.Info("Api-server scaled down", "deployment", apiServerDeployment.Name)

	// Scale down address-space-controller
	log.Info("Downscaling address-space-controller", "deployment", deployment.Name)
	deployment.Spec.Replicas = &zero
	_, err = deploymentClient.Update(deployment)
	if err != nil {
		return err
	}
	err = waitForReplicas(podClient, deployment, 0)
	if err != nil {
		return err
	}
	log.Info("Address-space-controller scaled down", "deployment", deployment.Name)

	// Scale down admin pods
	admins, err := deploymentClient.List(metav1.ListOptions{
		LabelSelector: "name=admin",
	})
	if err != nil {
		return err
	}
	for _, adminDeployment := range admins.Items {
		adminDeployment.Spec.Replicas = &zero
		log.Info("Downscaling admin", "deployment", adminDeployment.Name)
		_, err = deploymentClient.Update(&adminDeployment)
		if err != nil {
			return err
		}
		err = waitForReplicas(podClient, &adminDeployment, 0)
		if err != nil {
			return err
		}
		log.Info("Admin scaled down", "deployment", adminDeployment.Name)
	}

	// Scale down agent pods
	agents, err := deploymentClient.List(metav1.ListOptions{
		LabelSelector: "role=agent",
	})
	if err != nil {
		return err
	}
	for _, agentDeployment := range agents.Items {
		agentDeployment.Spec.Replicas = &zero
		log.Info("Downscaling agent", "deployment", agentDeployment.Name)
		_, err = deploymentClient.Update(&agentDeployment)
		if err != nil {
			return err
		}
		err = waitForReplicas(podClient, &agentDeployment, 0)
		if err != nil {
			return err
		}
		log.Info("Agent scaled down", "deployment", agentDeployment.Name)
	}

	// Get rid of API service so that it doesn't prevent upgrade
	aggClient, err := aggregatorclient.NewForConfig(d.config)
	if err != nil {
		return err
	}
	apiServiceClient := aggClient.ApiregistrationV1().APIServices()

	log.Info("Deleting v1beta1.enmasse.io api service")
	err = deleteApiServiceIfPresent(apiServiceClient, "v1beta1.enmasse.io")
	if err != nil {
		return err
	}

	log.Info("Deleting v1alpha1.enmasse.io api service")
	err = deleteApiServiceIfPresent(apiServiceClient, "v1alpha1.enmasse.io")
	if err != nil {
		return err
	}

	// Scale api-server back up
	log.Info("Scaling up api-server", "deployment", apiServerDeployment.Name)
	apiServerDeployment.Spec.Replicas = &apiServerReplicas
	_, err = deploymentClient.Update(apiServerDeployment)
	if err != nil {
		return err
	}
	err = waitForReplicas(podClient, apiServerDeployment, int(apiServerReplicas))
	if err != nil {
		return err
	}
	log.Info("Api-server scaled up", "deployment", apiServerDeployment.Name)
	return nil
}

func deleteApiServiceIfPresent(client apiserviceclient.APIServiceInterface, name string) error {
	_, err := client.Get(name, metav1.GetOptions{})
	if err != nil {
		if k8errors.IsNotFound(err) {
			return nil
		}
		return err
	}

	dopts := metav1.DeleteOptions{}
	return client.Delete(name, &dopts)
}

func applyDeployment(deployment *appsv1.Deployment) error {
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
		applyImageEnv(container, "ROUTER_IMAGE", "qdrouterd-base")
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

	return nil
}

func getBooleanAnnotationValue(Annotations map[string]string, name string) bool {
	if Annotations != nil {
		if sval, ok := Annotations[name]; ok {
			if bval, ok := strconv.ParseBool(sval); ok == nil && bval {
				return bval
			}
		}
	}
	return false
}

func applyImageEnv(container *corev1.Container, env string, imageName string) error {
	image, err := images.GetImage(imageName)
	if err != nil {
		return err
	}
	install.ApplyEnvSimple(container, env, image)
	return nil
}

func (d *AddressSpaceControllerDeployment) ensureService() error {

	serviceClient := d.client.CoreV1().Services(d.namespace)

	var service *corev1.Service
	service, err := serviceClient.Get(ADDRESS_SPACE_CONTROLLER_NAME, metav1.GetOptions{})
	if err != nil && k8errors.IsNotFound(err) {
		service := &corev1.Service{
			ObjectMeta: metav1.ObjectMeta{Namespace: d.namespace, Name: ADDRESS_SPACE_CONTROLLER_NAME},
		}
		applyService(service)
		_, err := serviceClient.Create(service)
		return err
	} else {
		applyService(service)
		_, err := serviceClient.Update(service)
		return err
	}
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

func waitForReplicas(podClient corev1client.PodInterface, deployment *appsv1.Deployment, expectedReplicas int) error {

	labelMap, err := metav1.LabelSelectorAsMap(deployment.Spec.Selector)
	if err != nil {
		return err
	}
	selector := labels.SelectorFromSet(labelMap).String()

	log.Info(fmt.Sprintf("Looking for pods with label selector %s", selector))
	opts := metav1.ListOptions{
		LabelSelector: selector,
	}

	// Set a long timeout like 15 minutes to prevent from looping forever in case of errors
	var timeout time.Duration = 15 * time.Minute
	now := time.Now()
	end := now.Add(timeout)

	for now.Before(end) {
		list, err := podClient.List(opts)
		if err == nil && len(list.Items) == expectedReplicas {
			return nil
		}
		log.Info(fmt.Sprintf("Found %d pods (expected %d)", len(list.Items), expectedReplicas))
		time.Sleep(5 * time.Second)
		now = time.Now()
	}

	list, err := podClient.List(opts)
	if err != nil {
		return err
	}
	if len(list.Items) != expectedReplicas {
		return errors.New(fmt.Sprintf("Deployment %s has %d replicas. Expected: %d", deployment.Name, len(list.Items), expectedReplicas))
	}
	return nil
}
