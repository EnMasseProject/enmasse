/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package broker

import (
	"context"
	"errors"
	"fmt"
	"strings"

	logr "github.com/go-logr/logr"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/common"
	"github.com/enmasseproject/enmasse/pkg/state"
	. "github.com/enmasseproject/enmasse/pkg/state/common"
	stateerrors "github.com/enmasseproject/enmasse/pkg/state/errors"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/install"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	resource "k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	intstr "k8s.io/apimachinery/pkg/util/intstr"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
)

type BrokerController struct {
	client         client.Client
	scheme         *runtime.Scheme
	certController *cert.CertController
	clientManager  state.ClientManager
}

func NewBrokerController(client client.Client, scheme *runtime.Scheme, certController *cert.CertController) *BrokerController {
	clientManager := state.GetClientManager()
	return &BrokerController{
		client:         client,
		scheme:         scheme,
		certController: certController,
		clientManager:  clientManager,
	}
}

func getBrokerLabels(infra *v1.MessagingInfrastructure) map[string]string {
	labels := make(map[string]string, 0)
	labels[common.LABEL_INFRA] = infra.Name
	labels["component"] = "broker"
	labels["app"] = "enmasse"

	return labels
}

/*
 * Reconciles the broker instances for an instance of shared infrastructure.
 *
 * Each instance of a broker is created as a statefulset. If we want to support HA in the future, each statefulset can use replicas to configure HA.
 */
func (b *BrokerController) ReconcileBrokers(ctx context.Context, logger logr.Logger, infra *v1.MessagingInfrastructure) ([]Host, error) {
	setDefaultBrokerScalingStrategy(&infra.Spec.Broker)
	logger.Info("Reconciling brokers", "broker", infra.Spec.Broker)

	labels := getBrokerLabels(infra)

	brokers := appsv1.StatefulSetList{}
	err := b.client.List(ctx, &brokers, client.InNamespace(infra.Namespace), client.MatchingLabels(labels))
	if err != nil {
		return nil, err
	}

	hosts := make(map[string]bool, 0)
	for _, broker := range brokers.Items {
		err := b.reconcileBroker(ctx, logger, infra, &broker)
		if err != nil {
			return nil, err
		}

		hosts[toHost(&broker)] = true
	}

	toCreate := numBrokersToCreate(infra.Spec.Broker.ScalingStrategy, brokers.Items)
	if toCreate > 0 {
		logger.Info("Creating brokers", "toCreate", toCreate)
		for i := 0; i < toCreate; i++ {
			broker := &appsv1.StatefulSet{
				ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: fmt.Sprintf("broker-%s-%s", infra.Name, util.RandomBrokerName())},
			}
			err = b.reconcileBroker(ctx, logger, infra, broker)
			if err != nil {
				return nil, err
			}
			hosts[toHost(broker)] = true
		}

	}

	infraClient := b.clientManager.GetClient(infra)
	toDelete := numBrokersToDelete(infra.Spec.Broker.ScalingStrategy, brokers.Items)
	newSize := len(brokers.Items) - toDelete
	if toDelete > 0 {
		logger.Info("Removing brokers", "toDelete", toDelete)
		for i := len(brokers.Items) - 1; toDelete > 0 && i > 0; i-- {
			err := infraClient.DeleteBroker(toHost(&brokers.Items[i]))
			if err != nil {
				if errors.Is(err, stateerrors.BrokerInUseError) {
					continue
				}
				return nil, err
			}
			err = b.client.Delete(ctx, &brokers.Items[i])
			if err != nil {
				return nil, err
			}
			delete(hosts, toHost(&brokers.Items[i]))
			toDelete--
		}
	}
	// TODO: Depending on scaling strategy, support migrating queues
	if toDelete > 0 {
		return nil, fmt.Errorf("unable to scale down to %d brokers: %d brokers are still needed", newSize, newSize+toDelete)
	}

	// Update discoverable brokers
	brokerPods := corev1.PodList{}
	err = b.client.List(ctx, &brokerPods, client.InNamespace(infra.Namespace), client.MatchingLabels(labels))
	if err != nil {
		return nil, err
	}

	allHosts := make([]Host, 0)
	for expectedHost, _ := range hosts {
		podIp := ""
		for _, pod := range brokerPods.Items {
			if pod.Status.Phase == corev1.PodRunning && pod.Status.PodIP != "" {
				logger.Info("Found broker pod", "ip", pod.Status.PodIP)
				// Rather than re-constructing the DNS name of the pod, check that it is a prefix of any expected full hostname
				// (which includes pod name, statefulset name and namespace), as this is guaranteed to match only one host.
				if strings.HasPrefix(expectedHost, pod.Name) {
					podIp = pod.Status.PodIP
					break
				}
			}
		}
		allHosts = append(allHosts, Host{Hostname: expectedHost, Ip: podIp})
	}

	return allHosts, nil
}

func toHost(broker *appsv1.StatefulSet) string {
	return fmt.Sprintf("%s-0.%s.%s.svc", broker.Name, broker.Name, broker.Namespace)
}

func toNamespacedHost(broker *appsv1.StatefulSet) string {
	return fmt.Sprintf("%s-0", broker.Name)
}

func (b *BrokerController) reconcileBroker(ctx context.Context, logger logr.Logger, infra *v1.MessagingInfrastructure, statefulset *appsv1.StatefulSet) error {
	logger.Info("Creating broker", "name", statefulset.Name)

	certSecretName := cert.GetCertSecretName(statefulset.Name)

	_, err := controllerutil.CreateOrUpdate(ctx, b.client, statefulset, func() error {
		if err := controllerutil.SetControllerReference(infra, statefulset, b.scheme); err != nil {
			return err
		}

		install.ApplyStatefulSetDefaults(statefulset, "broker", statefulset.Name)
		statefulset.Labels[common.LABEL_INFRA] = infra.Name
		statefulset.Spec.Template.Labels[common.LABEL_INFRA] = infra.Name

		statefulset.Spec.Template.Annotations[common.ANNOTATION_INFRA_NAME] = infra.Name
		statefulset.Spec.Template.Annotations[common.ANNOTATION_INFRA_NAMESPACE] = infra.Namespace

		statefulset.Annotations[common.ANNOTATION_INFRA_NAME] = infra.Name
		statefulset.Annotations[common.ANNOTATION_INFRA_NAMESPACE] = infra.Namespace

		statefulset.Spec.ServiceName = statefulset.Name
		statefulset.Spec.Replicas = int32ptr(1)

		initContainers, err := install.ApplyContainerWithError(statefulset.Spec.Template.Spec.InitContainers, "broker-init", func(container *corev1.Container) error {
			err := install.ApplyContainerImage(container, "broker-plugin", infra.Spec.Broker.InitImage)
			if err != nil {
				return err
			}

			install.ApplyEnvSimple(container, "INFRA_NAME", infra.Name)
			install.ApplyEnvSimple(container, "CERT_DIR", "/etc/enmasse-certs")
			install.ApplyEnvSimple(container, "AMQ_NAME", "data")
			install.ApplyEnvSimple(container, "HOME", "/var/run/artemis")

			// TODO:
			install.ApplyEnvSimple(container, "ADDRESS_SPACE_TYPE", "shared")
			install.ApplyEnvSimple(container, "GLOBAL_MAX_SIZE", "-1")
			install.ApplyEnvSimple(container, "ADDRESS_FULL_POLICY", "FAIL")

			install.ApplyVolumeMountSimple(container, "data", "/var/run/artemis", false)
			install.ApplyVolumeMountSimple(container, "init", "/opt/apache-artemis/custom", false)
			install.ApplyVolumeMountSimple(container, "certs", "/etc/enmasse-certs", false)
			return nil
		})
		if err != nil {
			return err
		}
		statefulset.Spec.Template.Spec.InitContainers = initContainers

		containers, err := install.ApplyContainerWithError(statefulset.Spec.Template.Spec.Containers, "broker", func(container *corev1.Container) error {
			err := install.ApplyContainerImage(container, "broker", infra.Spec.Broker.Image)
			if err != nil {
				return err
			}

			install.ApplyEnvSimple(container, "INFRA_NAME", infra.Name)
			install.ApplyEnvSimple(container, "CERT_DIR", "/etc/enmasse-certs")
			install.ApplyEnvSimple(container, "AMQ_NAME", "data")
			install.ApplyEnvSimple(container, "HOME", "/var/run/artemis")
			container.Command = []string{"/opt/apache-artemis/custom/bin/launch-broker.sh"}

			// TODO: Make these configurable in MessagingInfra
			install.ApplyEnvSimple(container, "ADDRESS_SPACE_TYPE", "shared")
			install.ApplyEnvSimple(container, "GLOBAL_MAX_SIZE", "-1")
			install.ApplyEnvSimple(container, "ADDRESS_FULL_POLICY", "FAIL")

			install.ApplyEnvSimple(container, "PROBE_ADDRESS", "readiness")
			install.ApplyEnvSimple(container, "PROBE_USERNAME", "probe")
			install.ApplyEnvSimple(container, "PROBE_PASSWORD", "probe")
			install.ApplyEnvSimple(container, "PROBE_TIMEOUT", "2s")

			install.ApplyVolumeMountSimple(container, "data", "/var/run/artemis", false)
			install.ApplyVolumeMountSimple(container, "init", "/opt/apache-artemis/custom", false)
			install.ApplyVolumeMountSimple(container, "certs", "/etc/enmasse-certs", false)

			// TODO: Make configurable
			container.Resources = corev1.ResourceRequirements{
				Requests: corev1.ResourceList{corev1.ResourceMemory: *resource.NewScaledQuantity(1, resource.Giga)},
				Limits:   corev1.ResourceList{corev1.ResourceMemory: *resource.NewScaledQuantity(1, resource.Giga)},
			}

			container.Ports = []corev1.ContainerPort{
				{
					ContainerPort: 5671,
					Name:          "amqps",
				},
			}

			container.LivenessProbe = &corev1.Probe{
				Handler: corev1.Handler{
					Exec: &corev1.ExecAction{
						Command: []string{
							"sh",
							"-c",
							"$ARTEMIS_HOME/custom/bin/probe.sh",
						},
					},
				},
				InitialDelaySeconds: 300,
			}

			container.ReadinessProbe = &corev1.Probe{
				Handler: corev1.Handler{
					Exec: &corev1.ExecAction{
						Command: []string{
							"sh",
							"-c",
							"$ARTEMIS_HOME/custom/bin/broker-probe",
						},
					},
				},
				TimeoutSeconds:      3,
				InitialDelaySeconds: 10,
			}

			return nil
		})
		if err != nil {
			return err
		}
		statefulset.Spec.Template.Spec.Containers = containers

		install.ApplyEmptyDirVolume(&statefulset.Spec.Template.Spec, "init")
		install.ApplySecretVolume(&statefulset.Spec.Template.Spec, "certs", certSecretName)

		statefulset.Spec.VolumeClaimTemplates = []corev1.PersistentVolumeClaim{
			corev1.PersistentVolumeClaim{
				ObjectMeta: metav1.ObjectMeta{Name: "data"},
				Spec: corev1.PersistentVolumeClaimSpec{
					AccessModes: []corev1.PersistentVolumeAccessMode{
						corev1.ReadWriteOnce,
					},

					Resources: corev1.ResourceRequirements{
						Requests: map[corev1.ResourceName]resource.Quantity{"storage": *resource.NewScaledQuantity(2, resource.Giga)},
					},
				},
			},
		}
		return nil
	})
	if err != nil {
		return err
	}

	// Reconcile service
	service := &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: statefulset.Name},
	}
	_, err = controllerutil.CreateOrUpdate(ctx, b.client, service, func() error {
		if err := controllerutil.SetControllerReference(infra, service, b.scheme); err != nil {
			return err
		}
		install.ApplyServiceDefaults(service, "broker", infra.Name)
		service.Spec.ClusterIP = "None"
		service.Spec.Selector = statefulset.Spec.Template.Labels
		service.Spec.Ports = []corev1.ServicePort{
			{
				Port:       5671,
				Protocol:   corev1.ProtocolTCP,
				TargetPort: intstr.FromString("amqps"),
				Name:       "amqps",
			},
		}

		return nil
	})
	if err != nil {
		return err
	}

	_, err = b.certController.ReconcileCert(ctx, logger, infra, statefulset, toNamespacedHost(statefulset), toHost(statefulset))
	if err != nil {
		return err
	}

	return nil
}

func int32ptr(v int32) *int32 {
	return &v
}

func setDefaultBrokerScalingStrategy(broker *v1.MessagingInfrastructureSpecBroker) {
	// Set static scaler by default
	if broker.ScalingStrategy == nil {
		broker.ScalingStrategy = &v1.MessagingInfrastructureSpecBrokerScalingStrategy{
			Static: &v1.MessagingInfrastructureSpecBrokerScalingStrategyStatic{
				PoolSize: 1,
			},
		}
	}
}

func numBrokersToCreate(strategy *v1.MessagingInfrastructureSpecBrokerScalingStrategy, brokers []appsv1.StatefulSet) int {
	if strategy.Static != nil {
		if int(strategy.Static.PoolSize) > len(brokers) {
			return int(strategy.Static.PoolSize) - len(brokers)
		}
	}
	// Does not normally happen. If it does make sure nothing gets created.
	return 0
}

func numBrokersToDelete(strategy *v1.MessagingInfrastructureSpecBrokerScalingStrategy, brokers []appsv1.StatefulSet) int {
	if strategy.Static != nil {
		if int(strategy.Static.PoolSize) < len(brokers) {
			return len(brokers) - int(strategy.Static.PoolSize)
		}
	}
	// Does not normally happen. If it does make sure nothing gets deleted.
	return 0
}
