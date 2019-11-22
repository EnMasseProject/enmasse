/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package upgrader

import (
	"errors"
	"fmt"
	deployer "github.com/enmasseproject/enmasse/pkg/controller/address_space_controller"
	"github.com/enmasseproject/enmasse/pkg/util"
	"strconv"
	"time"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	labels "k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	kubernetes "k8s.io/client-go/kubernetes"
	corev1client "k8s.io/client-go/kubernetes/typed/core/v1"
	rest "k8s.io/client-go/rest"
	"k8s.io/client-go/util/retry"
	aggregatorclient "k8s.io/kube-aggregator/pkg/client/clientset_generated/clientset"
	apiserviceclient "k8s.io/kube-aggregator/pkg/client/clientset_generated/clientset/typed/apiregistration/v1"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
)

var log = logf.Log.WithName("upgrader")

const ADDRESS_SPACE_CONTROLLER_NAME = "address-space-controller"
const ANNOTATION_VERSION = "enmasse.io/version"
const ENV_VERSION = "VERSION"

type Upgrader struct {
	client    *kubernetes.Clientset
	config    *rest.Config
	scheme    *runtime.Scheme
	namespace string
}

func New(mgr manager.Manager) (*Upgrader, error) {
	client, err := kubernetes.NewForConfig(mgr.GetConfig())
	if err != nil {
		log.Error(err, "Error creating kubernetes client")
		return nil, err
	}
	return &Upgrader{
		client:    client,
		config:    mgr.GetConfig(),
		scheme:    mgr.GetScheme(),
		namespace: util.GetEnvOrDefault("NAMESPACE", "enmasse-infra"),
	}, nil
}

func (u *Upgrader) Upgrade() error {

	deploymentClient := u.client.AppsV1().Deployments(u.namespace)

	var deployment *appsv1.Deployment
	deployment, err := deploymentClient.Get(ADDRESS_SPACE_CONTROLLER_NAME, metav1.GetOptions{})
	if err != nil && k8errors.IsNotFound(err) {
		return nil
	} else if err != nil {
		return err
	} else {
		if _, ok := deployment.Annotations[ANNOTATION_VERSION]; !ok {
			log.Info("address-space-controller is missing annotation, initiating upgrade")
			err = u.performUpgrade(deployment)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func (u *Upgrader) performUpgrade(addressSpaceControllerDeployment *appsv1.Deployment) error {
	deploymentClient := u.client.AppsV1().Deployments(u.namespace)

	// Scale down iot-operator
	err := u.scale("iot-operator", 0)
	if err != nil {
		return err
	}

	// Scale down api-server
	err = u.scale("api-server", 0)
	if err != nil {
		return err
	}

	// Scale down address-space-controller
	err = u.scaleDeployment(addressSpaceControllerDeployment, 0)
	if err != nil {
		return err
	}

	// Scale down admin pods
	admins, err := deploymentClient.List(metav1.ListOptions{
		LabelSelector: "name=admin",
	})
	if err != nil {
		return err
	}
	for _, adminDeployment := range admins.Items {
		err = u.scaleDeployment(&adminDeployment, 0)
		if err != nil {
			return err
		}
	}

	// Scale down agent pods
	agents, err := deploymentClient.List(metav1.ListOptions{
		LabelSelector: "role=agent",
	})
	if err != nil {
		return err
	}
	for _, agentDeployment := range agents.Items {
		err = u.scaleDeployment(&agentDeployment, 0)
		if err != nil {
			return err
		}
	}

	// Get rid of API service so that it doesn't prevent upgrade
	aggClient, err := aggregatorclient.NewForConfig(u.config)
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
	err = u.scale("api-server", 1)
	if err != nil {
		return err
	}

	// Scale address-space-controller back up. Retry in case the deployment got modified while we were scaling down
	err = retry.RetryOnConflict(retry.DefaultRetry, func() error {
		// Update address-space-controller deployment object to new version
		err = deployer.ApplyDeployment(addressSpaceControllerDeployment)
		if err != nil {
			return err
		}

		// Apply new configuration and wait for it to scale up
		return u.scaleDeployment(addressSpaceControllerDeployment, 1)
	})
	if err != nil {
		return err
	}

	// Scale up iot-operator if deployed
	err = u.scale("iot-operator", 1)
	if err != nil {
		return err
	}
	return nil
}

func (u *Upgrader) scale(deploymentName string, replicas int32) error {
	deploymentClient := u.client.AppsV1().Deployments(u.namespace)

	deployment, err := deploymentClient.Get(deploymentName, metav1.GetOptions{})
	if err != nil && k8errors.IsNotFound(err) {
		return nil
	} else if err != nil {
		return err
	} else {
		return u.scaleDeployment(deployment, replicas)
	}
}

func (u *Upgrader) scaleDeployment(deployment *appsv1.Deployment, replicas int32) error {
	podClient := u.client.CoreV1().Pods(u.namespace)
	deploymentClient := u.client.AppsV1().Deployments(u.namespace)

	log.Info("Downscaling", "deployment", deployment.Name)
	deployment.Spec.Replicas = &replicas
	_, err := deploymentClient.Update(deployment)
	if err != nil {
		return err
	}
	err = waitForReplicas(podClient, deployment, int(replicas))
	if err != nil {
		return err
	}
	log.Info("Scaled down", "deployment", deployment.Name)
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
		numReady := 0
		if err == nil && len(list.Items) == expectedReplicas {
			numReady := 0
			for _, pod := range list.Items {
				if pod.Status.Phase == corev1.PodRunning {
					for _, condition := range pod.Status.Conditions {
						if condition.Type == corev1.PodReady &&
							condition.Status == corev1.ConditionTrue {
							numReady += 1
							break
						}
					}
				}
			}
			if numReady == expectedReplicas {
				return nil
			}
		}
		log.Info(fmt.Sprintf("Found %d ready pods (expected %d)", numReady, expectedReplicas))
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
