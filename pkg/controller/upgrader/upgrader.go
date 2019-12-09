/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package upgrader

import (
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"

	adminv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	adminv1beta1_client "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/admin/v1beta1"
	userv1beta1_client "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/user/v1beta1"
	deployer "github.com/enmasseproject/enmasse/pkg/controller/address_space_controller"
	"github.com/enmasseproject/enmasse/pkg/keycloak"
	"github.com/enmasseproject/enmasse/pkg/util"

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

	log.Info("Deleting v1beta1.user.enmasse.io api service")
	err = deleteApiServiceIfPresent(apiServiceClient, "v1beta1.user.enmasse.io")
	if err != nil {
		return err
	}

	log.Info("Deleting v1alpha1.user.enmasse.io api service")
	err = deleteApiServiceIfPresent(apiServiceClient, "v1alpha1.user.enmasse.io")
	if err != nil {
		return err
	}

	// Create MessagingUser CRs for all users in all realms
	err = u.convertMessagingUsers()
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

	// Delete api-server deployment
	err = u.delete("api-server")
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

func (u *Upgrader) convertMessagingUsers() error {
	adminClient, err := adminv1beta1_client.NewForConfig(u.config)
	if err != nil {
		return err
	}
	userClient, err := userv1beta1_client.NewForConfig(u.config)
	if err != nil {
		return err
	}
	secretClient := u.client.CoreV1().Secrets(u.namespace)
	if err != nil {
		return err
	}
	list, err := adminClient.AuthenticationServices(u.namespace).List(metav1.ListOptions{})
	if err != nil {
		return err
	}
	for _, authenticationService := range list.Items {
		if authenticationService.Spec.Type == adminv1beta1.Standard {
			var ca []byte
			if authenticationService.Status.CaCertSecret != nil {
				caCertSecret, err := secretClient.Get(authenticationService.Status.CaCertSecret.Name, metav1.GetOptions{})
				if err != nil {
					log.Error(err, "Getting authentication service CA")
					return err
				}
				ca = caCertSecret.Data["tls.crt"]
			}

			credentials, err := secretClient.Get(authenticationService.Spec.Standard.CredentialsSecret.Name, metav1.GetOptions{})
			if err != nil {
				log.Error(err, "Getting authentication service credentials")
				return err
			}

			adminUser := credentials.Data["admin.username"]
			adminPassword := credentials.Data["admin.password"]
			host := authenticationService.Status.Host
			// Handle wrong host for previous auth service
			if !strings.HasSuffix(host, fmt.Sprintf("%s.svc", authenticationService.Namespace)) {
				host += "." + authenticationService.Namespace + ".svc"
			}
			kcClient, err := keycloak.NewClient(host, 8443, string(adminUser), string(adminPassword), ca)
			if err != nil {
				log.Error(err, "Creating keycloak client")
				return err
			}

			realms, err := kcClient.GetRealms()
			if err != nil {
				log.Error(err, "Getting realms from keycloak")
				return err
			}

			for _, realm := range realms {
				if realm != "master" {
					log.Info("Migrating users in authentication service", "realm", realm)
					users, err := kcClient.GetUsers(realm, func(name string, values []string) bool {
						return name != keycloak.ATTR_FROM_CRD
					})
					if err != nil {
						log.Error(err, "Getting users from keycloak", "realm", realm)
						return err
					}
					for _, user := range users {
						log.Info("Migrating user", "name", user.Name, "namespace", user.Namespace)
						_, err = userClient.MessagingUsers(user.Namespace).Get(user.Name, metav1.GetOptions{})
						if err != nil {
							if k8errors.IsNotFound(err) {
								_, err = userClient.MessagingUsers(user.Namespace).Create(user)
								if err != nil {
									log.Error(err, "Error creating messaginguser")
									return err
								}
								log.Info("User migrated successfully", "name", user.Name, "namespace", user.Namespace)
							} else {
								log.Error(err, "Error getting messaginguser")
								return err
							}
						} else {
							log.Info("User already migrated! Skipping...", "name", user.Name, "namespace", user.Namespace)
						}
					}
				}
			}

		}
	}
	return nil
}

func (u *Upgrader) delete(name string) error {
	deploymentClient := u.client.AppsV1().Deployments(u.namespace)
	propagationPolicy := metav1.DeletePropagationBackground
	err := deploymentClient.Delete(name, &metav1.DeleteOptions{
		PropagationPolicy: &propagationPolicy,
	})
	if err != nil {
		return err
	}

	serviceClient := u.client.CoreV1().Services(u.namespace)
	err = serviceClient.Delete(name, &metav1.DeleteOptions{
		PropagationPolicy: &propagationPolicy,
	})

	return err
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
