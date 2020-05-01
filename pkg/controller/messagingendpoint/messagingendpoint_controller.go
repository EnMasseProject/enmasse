/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingendpoint

import (
	"context"
	"errors"
	"fmt"
	"reflect"
	"strings"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/common"
	"github.com/enmasseproject/enmasse/pkg/state"
	"github.com/enmasseproject/enmasse/pkg/util"
	utilerrors "github.com/enmasseproject/enmasse/pkg/util/errors"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	"github.com/enmasseproject/enmasse/pkg/util/install"

	corev1 "k8s.io/api/core/v1"

	k8errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/intstr"

	"k8s.io/client-go/tools/record"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_messagingendpoint")
var _ reconcile.Reconciler = &ReconcileMessagingEndpoint{}

type ReconcileMessagingEndpoint struct {
	client        client.Client
	reader        client.Reader
	recorder      record.EventRecorder
	scheme        *runtime.Scheme
	clientManager state.ClientManager
	namespace     string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

/**
 * TODO - Add support for TLS configuration
 * TODO - Add support for self-signed certificates
 * TODO - Add support for openshift-signed certificates
 * TODO - Add support for externally provided certificates
 * TODO - Add support for route type
 * TODO - Add support for ingress type
 * TODO - Complete LoadBalancer support
 */

const (
	FINALIZER_NAME = "enmasse.io/operator"
)

func newReconciler(mgr manager.Manager) *ReconcileMessagingEndpoint {
	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	clientManager := state.GetClientManager()
	return &ReconcileMessagingEndpoint{
		client:        mgr.GetClient(),
		clientManager: clientManager,
		reader:        mgr.GetAPIReader(),
		recorder:      mgr.GetEventRecorderFor("messagingendpoint"),
		scheme:        mgr.GetScheme(),
		namespace:     namespace,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingEndpoint) error {

	c, err := controller.New("messagingendpoint-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1beta2.MessagingEndpoint{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	return err
}

func (r *ReconcileMessagingEndpoint) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	logger.Info("Reconciling MessagingEndpoint")

	found := &v1beta2.MessagingEndpoint{}
	err := r.reader.Get(ctx, request.NamespacedName, found)
	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingEndpoint resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingEndpoint")
		return reconcile.Result{}, err
	}

	rc := resourceContext{
		endpoint: found,
		status:   found.Status.DeepCopy(),
		ctx:      ctx,
		client:   r.client,
	}

	var foundTenant *v1beta2.MessagingEndpointCondition
	var allocated *v1beta2.MessagingEndpointCondition
	var created *v1beta2.MessagingEndpointCondition
	var serviceCreated *v1beta2.MessagingEndpointCondition
	var ready *v1beta2.MessagingEndpointCondition
	rc.Process(func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error) {
		// First set configuring state if not set to indicate we are processing the endpoint.
		if endpoint.Status.Phase == "" {
			endpoint.Status.Phase = v1beta2.MessagingEndpointConfiguring
		}
		foundTenant = endpoint.Status.GetMessagingEndpointCondition(v1beta2.MessagingEndpointFoundTenant)
		allocated = endpoint.Status.GetMessagingEndpointCondition(v1beta2.MessagingEndpointAllocatedPorts)
		created = endpoint.Status.GetMessagingEndpointCondition(v1beta2.MessagingEndpointCreated)
		serviceCreated = endpoint.Status.GetMessagingEndpointCondition(v1beta2.MessagingEndpointServiceCreated)
		ready = endpoint.Status.GetMessagingEndpointCondition(v1beta2.MessagingEndpointReady)
		return processorResult{}, nil
	})

	// Initialize and process finalizer
	result, err := rc.Process(func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error) {
		// Handle finalizing an deletion state first
		if endpoint.DeletionTimestamp != nil && endpoint.Status.Phase != v1beta2.MessagingEndpointTerminating {
			endpoint.Status.Phase = v1beta2.MessagingEndpointTerminating
			err := r.client.Status().Update(ctx, endpoint)
			return processorResult{Requeue: true}, err
		}

		original := endpoint.DeepCopy()
		result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, endpoint, []finalizer.Finalizer{
			finalizer.Finalizer{
				Name: FINALIZER_NAME,
				Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
					_, ok := c.Object.(*v1beta2.MessagingEndpoint)
					if !ok {
						return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingEndpoint")
					}

					infra, err := messaginginfra.LookupInfra(ctx, r.client, endpoint.Namespace)
					if err != nil {
						// Not bound - allow dropping finalizer
						if utilerrors.IsNotBound(err) || utilerrors.IsNotFound(err) {
							logger.Info("[Finalizer] Messaging tenant not found or bound, ignoring!")
							return reconcile.Result{}, nil
						}
						logger.Info("[Finalizer] Error looking up infra")
						return reconcile.Result{}, err
					}
					client := r.clientManager.GetClient(infra)
					err = client.DeleteEndpoint(endpoint)
					logger.Info("[Finalizer] Deleted endpoint", "err", err)
					return reconcile.Result{}, err
				},
			},
		})
		if err != nil {
			return processorResult{}, err
		}

		if result.Requeue {
			// Update and requeue if changed
			if !reflect.DeepEqual(original, endpoint) {
				err := r.client.Update(ctx, endpoint)
				return processorResult{Return: true}, err
			}
		}
		return processorResult{Requeue: result.Requeue}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	var infra *v1beta2.MessagingInfra
	// Retrieve the MessagingInfra for this MessagingEndpoint
	result, err = rc.Process(func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error) {
		i, err := messaginginfra.LookupInfra(ctx, r.client, found.Namespace)
		if err != nil && (k8errors.IsNotFound(err) || utilerrors.IsNotBound(err)) {
			foundTenant.SetStatus(corev1.ConditionFalse, "", err.Error())
			endpoint.Status.Message = err.Error()
			return processorResult{RequeueAfter: 10 * time.Second}, nil
		}
		foundTenant.SetStatus(corev1.ConditionTrue, "", "")
		infra = i
		return processorResult{}, err

	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	client := r.clientManager.GetClient(infra)

	// Ensure endpoint exists for tenant
	result, err = rc.Process(func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error) {
		originalStatus := endpoint.Status.DeepCopy()
		err := client.AllocatePorts(endpoint, endpoint.Spec.Protocols)
		if err != nil {
			allocated.SetStatus(corev1.ConditionFalse, "", err.Error())
			endpoint.Status.Message = err.Error()
			if errors.Is(err, state.NotInitializedError) {
				return processorResult{RequeueAfter: 10 * time.Second}, nil
			}
			client.FreePorts(endpoint)
			return processorResult{}, err
		}

		allocated.SetStatus(corev1.ConditionTrue, "", "")
		return processorResult{
			Requeue: !reflect.DeepEqual(originalStatus.InternalPorts, endpoint.Status.InternalPorts),
		}, nil

	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Ensure endpoint exists
	result, err = rc.Process(func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error) {
		err := client.SyncEndpoint(endpoint)
		if err != nil {
			created.SetStatus(corev1.ConditionFalse, "", err.Error())
			endpoint.Status.Message = err.Error()
			if errors.Is(err, state.NotInitializedError) {
				return processorResult{RequeueAfter: 10 * time.Second}, nil
			}
			return processorResult{}, err
		}
		created.SetStatus(corev1.ConditionTrue, "", "")
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Create the Service object used by the endpoint
	result, err = rc.Process(func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error) {
		// Reconcile service
		serviceName := getServiceName(endpoint)
		service := &corev1.Service{
			ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: serviceName},
		}
		_, err = controllerutil.CreateOrUpdate(ctx, r.client, service, func() error {
			if err := controllerutil.SetControllerReference(infra, service, r.scheme); err != nil {
				return err
			}
			install.ApplyServiceDefaults(service, "endpoint", endpoint.Name)

			// For NodePort, Cluster and LoadBalancer types, use the service directly
			if endpoint.Spec.NodePort != nil {
				applyNodePort(endpoint, service)
			} else if endpoint.Spec.LoadBalancer != nil {
				applyLoadBalancer(endpoint, service)
			} else {
				// For Route and Ingress and Cluster, use a ClusterIP service
				applyClusterIp(endpoint, service)
			}

			service.Spec.Selector = map[string]string{
				common.LABEL_INFRA: infra.Name,
				"component":        "router",
			}

			if len(service.Spec.Ports) == 0 {
				service.Spec.Ports = make([]corev1.ServicePort, 0)
				for _, internalPort := range endpoint.Status.InternalPorts {
					if internalPort.Protocol == v1beta2.MessagingProtocolAMQP {
						service.Spec.Ports = append(service.Spec.Ports, corev1.ServicePort{
							Port:       5672,
							Protocol:   corev1.ProtocolTCP,
							TargetPort: intstr.FromInt(internalPort.Port),
							Name:       strings.ToLower(string(internalPort.Protocol)),
						})
					}
				}
			}

			return nil
		})
		if err != nil {
			serviceCreated.SetStatus(corev1.ConditionFalse, "", err.Error())
			endpoint.Status.Message = err.Error()
			return processorResult{}, err
		}
		serviceCreated.SetStatus(corev1.ConditionTrue, "", "")

		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Create any public exposable resources
	result, err = rc.Process(func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error) {
		// TODO: Routes and Ingress
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Update endpoint status based on type
	result, err = rc.Process(func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error) {
		originalStatus := endpoint.Status.DeepCopy()

		serviceName := getServiceName(endpoint)
		service := &corev1.Service{
			ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: serviceName},
		}

		endpoint.Status.Ports = make([]v1beta2.MessagingEndpointPort, 0)
		if endpoint.Spec.NodePort != nil {
			err := r.client.Get(ctx, types.NamespacedName{Name: service.Name, Namespace: service.Namespace}, service)
			if err != nil {
				ready.SetStatus(corev1.ConditionFalse, "", err.Error())
				endpoint.Status.Message = err.Error()
				return processorResult{}, err
			}

			// Make sure port is set
			if endpoint.Spec.Host != nil {
				endpoint.Status.Host = *endpoint.Spec.Host
			} else {
				endpoint.Status.Host = fmt.Sprintf("%s.%s.svc", serviceName, infra.Namespace)
			}

			for _, servicePort := range service.Spec.Ports {
				if servicePort.NodePort != 0 {
					endpoint.Status.Ports = append(endpoint.Status.Ports, v1beta2.MessagingEndpointPort{
						Name:     servicePort.Name,
						Protocol: v1beta2.MessagingProtocolAMQP,
						Port:     int(servicePort.NodePort),
					})
				} else {
					msg := "NodePort not yet set for service"
					ready.SetStatus(corev1.ConditionFalse, "", msg)
					endpoint.Status.Message = msg

				}
			}

		} else if endpoint.Spec.LoadBalancer != nil {
			err := r.client.Get(ctx, types.NamespacedName{Name: service.Name, Namespace: service.Namespace}, service)
			if err != nil {
				ready.SetStatus(corev1.ConditionFalse, "", err.Error())
				endpoint.Status.Message = err.Error()
				return processorResult{}, err
			}
			if endpoint.Spec.Host != nil {
				endpoint.Status.Host = *endpoint.Spec.Host
			}
			for _, servicePort := range service.Spec.Ports {
				endpoint.Status.Ports = append(endpoint.Status.Ports, v1beta2.MessagingEndpointPort{
					Name:     servicePort.Name,
					Protocol: v1beta2.MessagingProtocolAMQP,
					Port:     int(servicePort.Port),
				})
			}
		} else if endpoint.Spec.Cluster != nil {
			err := r.client.Get(ctx, types.NamespacedName{Name: service.Name, Namespace: service.Namespace}, service)
			if err != nil {
				ready.SetStatus(corev1.ConditionFalse, "", err.Error())
				endpoint.Status.Message = err.Error()
				return processorResult{}, err
			}
			endpoint.Status.Host = fmt.Sprintf("%s.%s.svc", serviceName, infra.Namespace)
			for _, servicePort := range service.Spec.Ports {
				endpoint.Status.Ports = append(endpoint.Status.Ports, v1beta2.MessagingEndpointPort{
					Name:     servicePort.Name,
					Protocol: v1beta2.MessagingProtocolAMQP,
					Port:     int(servicePort.Port),
				})
			}
		}
		if !reflect.DeepEqual(originalStatus.Ports, endpoint.Status.Ports) {
			logger.Info("Updating status that was changed", "old", originalStatus, "new", endpoint.Status)
			return processorResult{Requeue: true}, nil
		}
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Mark endpoint status as all-OK
	result, err = rc.Process(func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error) {
		originalStatus := endpoint.Status.DeepCopy()
		endpoint.Status.Phase = v1beta2.MessagingEndpointActive
		endpoint.Status.Message = ""
		ready.SetStatus(corev1.ConditionTrue, "", "")
		if !reflect.DeepEqual(originalStatus, endpoint.Status) {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			err := r.client.Status().Update(ctx, endpoint)
			return processorResult{}, err
		} else {
			return processorResult{}, nil
		}
	})
	return result.Result(), err
}

func applyNodePort(endpoint *v1beta2.MessagingEndpoint, service *corev1.Service) {
	service.Spec.Type = corev1.ServiceTypeNodePort
}

func applyLoadBalancer(endpoint *v1beta2.MessagingEndpoint, service *corev1.Service) {
	service.Spec.Type = corev1.ServiceTypeLoadBalancer
}

func applyClusterIp(endpoint *v1beta2.MessagingEndpoint, service *corev1.Service) {
	service.Spec.Type = corev1.ServiceTypeClusterIP
}

func getServiceName(endpoint *v1beta2.MessagingEndpoint) string {
	return fmt.Sprintf("%s-%s", endpoint.Namespace, endpoint.Name)
}

/*
 * Automatically handle status update of the resource after running some reconcile logic.
 */
type resourceContext struct {
	ctx      context.Context
	client   client.Client
	status   *v1beta2.MessagingEndpointStatus
	endpoint *v1beta2.MessagingEndpoint
}

type processorResult struct {
	Requeue      bool
	RequeueAfter time.Duration
	Return       bool
}

func (r *resourceContext) Process(processor func(endpoint *v1beta2.MessagingEndpoint) (processorResult, error)) (processorResult, error) {
	result, err := processor(r.endpoint)
	if !reflect.DeepEqual(r.status, r.endpoint.Status) {
		if err != nil || result.Requeue || result.RequeueAfter > 0 {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			statuserr := r.client.Status().Update(r.ctx, r.endpoint)
			if statuserr != nil {
				// If this fails, report the status error if everything else whent ok, otherwise report the original error
				log.Error(statuserr, "Status update failed", "endpoint", r.endpoint.Name)
				if err == nil {
					err = statuserr
				}
			} else {
				r.status = r.endpoint.Status.DeepCopy()
			}
			return result, err
		}
	}
	return result, err
}

func (r *processorResult) ShouldReturn(err error) bool {
	return err != nil || r.Requeue || r.RequeueAfter > 0 || r.Return
}

func (r *processorResult) Result() reconcile.Result {
	return reconcile.Result{Requeue: r.Requeue, RequeueAfter: r.RequeueAfter}
}
