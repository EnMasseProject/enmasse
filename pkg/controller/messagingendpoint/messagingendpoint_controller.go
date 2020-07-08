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

	amqp "github.com/enmasseproject/enmasse/pkg/amqpcommand"
	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/common"
	"github.com/enmasseproject/enmasse/pkg/state"
	stateerrors "github.com/enmasseproject/enmasse/pkg/state/errors"
	"github.com/enmasseproject/enmasse/pkg/util"
	utilerrors "github.com/enmasseproject/enmasse/pkg/util/errors"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	"github.com/go-logr/logr"

	routev1 "github.com/openshift/api/route/v1"
	corev1 "k8s.io/api/core/v1"
	netv1beta1 "k8s.io/api/networking/v1beta1"

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
	client         client.Client
	reader         client.Reader
	recorder       record.EventRecorder
	scheme         *runtime.Scheme
	clientManager  state.ClientManager
	certController *cert.CertController
	namespace      string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

const (
	FINALIZER_NAME                              = "enmasse.io/operator"
	openShiftServiceCAAnnotationServingCertBeta = "service.beta.openshift.io/serving-cert-secret-name"
)

var (
	protocolToPortName = map[v1.MessagingEndpointProtocol]string{
		v1.MessagingProtocolAMQP:    strings.ToLower(string(v1.MessagingProtocolAMQP)),
		v1.MessagingProtocolAMQPS:   strings.ToLower(string(v1.MessagingProtocolAMQPS)),
		v1.MessagingProtocolAMQPWS:  strings.ToLower(string(v1.MessagingProtocolAMQPWS)),
		v1.MessagingProtocolAMQPWSS: strings.ToLower(string(v1.MessagingProtocolAMQPWSS)),
	}

	portNameToProtocol = map[string]v1.MessagingEndpointProtocol{
		strings.ToLower(string(v1.MessagingProtocolAMQP)):    v1.MessagingProtocolAMQP,
		strings.ToLower(string(v1.MessagingProtocolAMQPS)):   v1.MessagingProtocolAMQPS,
		strings.ToLower(string(v1.MessagingProtocolAMQPWS)):  v1.MessagingProtocolAMQPWS,
		strings.ToLower(string(v1.MessagingProtocolAMQPWSS)): v1.MessagingProtocolAMQPWSS,
	}
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
		// TODO: Make expiry configurable
		certController: cert.NewCertController(mgr.GetClient(), mgr.GetScheme(), 24*30*time.Hour, 24*time.Hour),
		namespace:      namespace,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingEndpoint) error {

	c, err := controller.New("messagingendpoint-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1.MessagingEndpoint{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	return err
}

func (r *ReconcileMessagingEndpoint) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	logger.Info("Reconciling MessagingEndpoint")

	found := &v1.MessagingEndpoint{}
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

	rc.Process(func(endpoint *v1.MessagingEndpoint) (processorResult, error) {
		// First set configuring state if not set to indicate we are processing the endpoint.
		if endpoint.Status.Phase == "" {
			endpoint.Status.Phase = v1.MessagingEndpointConfiguring
		}
		if endpoint.Spec.Cluster != nil {
			endpoint.Status.Type = v1.MessagingEndpointTypeCluster
		} else if endpoint.Spec.NodePort != nil {
			endpoint.Status.Type = v1.MessagingEndpointTypeNodePort
		} else if endpoint.Spec.LoadBalancer != nil {
			endpoint.Status.Type = v1.MessagingEndpointTypeLoadBalancer
		} else if endpoint.Spec.Route != nil {
			endpoint.Status.Type = v1.MessagingEndpointTypeRoute
		} else if endpoint.Spec.Ingress != nil {
			endpoint.Status.Type = v1.MessagingEndpointTypeIngress
		}
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointFoundProject)
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointConfiguredTls)
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointAllocatedPorts)
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointCreated)
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointServiceCreated)
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointReady)
		return processorResult{}, nil
	})

	// Initialize and process finalizer
	result, err := rc.Process(func(endpoint *v1.MessagingEndpoint) (processorResult, error) {
		return r.reconcileFinalizer(ctx, logger, endpoint)
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Retrieve the MessagingInfra for this MessagingEndpoint
	var infra *v1.MessagingInfrastructure
	result, err = rc.Process(func(endpoint *v1.MessagingEndpoint) (processorResult, error) {
		_, i, err := messaginginfra.LookupInfra(ctx, r.client, found.Namespace)
		if err != nil && (k8errors.IsNotFound(err) || utilerrors.IsNotBound(err)) {
			endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointFoundProject).SetStatus(corev1.ConditionFalse, "", err.Error())
			endpoint.Status.Message = err.Error()
			return processorResult{RequeueAfter: 10 * time.Second}, nil
		}
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointFoundProject).SetStatus(corev1.ConditionTrue, "", "")
		infra = i
		return processorResult{}, err

	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Ensure endpoint exists for project
	client := r.clientManager.GetClient(infra)
	result, err = rc.Process(func(endpoint *v1.MessagingEndpoint) (processorResult, error) {
		if len(endpoint.Spec.Protocols) == 0 {
			err := fmt.Errorf("must specify at least 1 protocol")
			endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointAllocatedPorts).SetStatus(corev1.ConditionFalse, "", err.Error())
			endpoint.Status.Message = err.Error()
			return processorResult{}, err
		}
		originalStatus := endpoint.Status.DeepCopy()
		err := client.AllocatePorts(endpoint, endpoint.Spec.Protocols)
		if err != nil {
			endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointAllocatedPorts).SetStatus(corev1.ConditionFalse, "", err.Error())
			endpoint.Status.Message = err.Error()
			if errors.Is(err, stateerrors.NotInitializedError) || errors.Is(err, amqp.RequestTimeoutError) || errors.Is(err, stateerrors.NotSyncedError) {
				return processorResult{RequeueAfter: 10 * time.Second}, nil
			}
			client.FreePorts(endpoint)
			return processorResult{}, err
		}
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointAllocatedPorts).SetStatus(corev1.ConditionTrue, "", "")

		return processorResult{
			Requeue: !reflect.DeepEqual(originalStatus.InternalPorts, endpoint.Status.InternalPorts),
		}, nil

	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Create the Service object used by the endpoint
	result, err = rc.Process(func(endpoint *v1.MessagingEndpoint) (processorResult, error) {
		result, err := r.reconcileEndpointService(ctx, logger, infra, endpoint)
		if err != nil {
			endpoint.Status.Message = err.Error()
			endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointServiceCreated).SetStatus(corev1.ConditionFalse, "", err.Error())
			return result, err
		}
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointServiceCreated).SetStatus(corev1.ConditionTrue, "", "")
		return result, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Ensure TLS configuration is applied
	result, err = rc.Process(func(endpoint *v1.MessagingEndpoint) (processorResult, error) {
		result, err := r.reconcileEndpointTls(ctx, logger, infra, endpoint)
		if err != nil {
			endpoint.Status.Message = err.Error()
			endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointConfiguredTls).SetStatus(corev1.ConditionFalse, "", err.Error())
			return result, err
		}

		if endpoint.Spec.Tls != nil {
			// TODO: This ensures that router pods have picked up the new cert. However, it is not as reliable as it should be,
			// so some detection mechanism is needed to check that the SHA256 sum of the router pods annotations got updated.
			// Since a simpler approach would be to wait a few seconds, which would work in 99% of the cases, do that if it its the
			// first time the endpoint is being reconciled (phase not yet Active)
			if endpoint.Status.Phase != v1.MessagingEndpointActive {
				time.Sleep(5 * time.Second)
			}
		}

		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointConfiguredTls).SetStatus(corev1.ConditionTrue, "", "")
		return result, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Ensure endpoint exists
	result, err = rc.Process(func(endpoint *v1.MessagingEndpoint) (processorResult, error) {
		err := client.SyncEndpoint(endpoint)
		if err != nil {
			endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointCreated).SetStatus(corev1.ConditionFalse, "", err.Error())
			endpoint.Status.Message = err.Error()
			if errors.Is(err, stateerrors.NotInitializedError) || errors.Is(err, amqp.RequestTimeoutError) || errors.Is(err, stateerrors.NotSyncedError) {
				return processorResult{RequeueAfter: 10 * time.Second}, nil
			}
			return processorResult{}, err
		}

		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointCreated).SetStatus(corev1.ConditionTrue, "", "")
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Mark endpoint status as all-OK
	result, err = rc.Process(func(endpoint *v1.MessagingEndpoint) (processorResult, error) {
		if endpoint.Status.Host == "" {
			err := fmt.Errorf("hostname is not yet defined")
			endpoint.Status.Message = err.Error()
			endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointReady).SetStatus(corev1.ConditionFalse, "", err.Error())
			return processorResult{RequeueAfter: 5 * time.Second}, nil
		}

		originalStatus := endpoint.Status.DeepCopy()
		endpoint.Status.Phase = v1.MessagingEndpointActive
		endpoint.Status.Message = ""
		endpoint.Status.GetMessagingEndpointCondition(v1.MessagingEndpointReady).SetStatus(corev1.ConditionTrue, "", "")
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

// Reconcile the finalizer for the messaging endpoint, deleting the resources if the endpoint is being deleted.
func (r *ReconcileMessagingEndpoint) reconcileFinalizer(ctx context.Context, logger logr.Logger, endpoint *v1.MessagingEndpoint) (processorResult, error) {
	// Handle finalizing an deletion state first
	if endpoint.DeletionTimestamp != nil && endpoint.Status.Phase != v1.MessagingEndpointTerminating {
		endpoint.Status.Phase = v1.MessagingEndpointTerminating
		err := r.client.Status().Update(ctx, endpoint)
		return processorResult{Requeue: true}, err
	}

	original := endpoint.DeepCopy()
	result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, endpoint, []finalizer.Finalizer{
		{
			Name: FINALIZER_NAME,
			Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
				_, ok := c.Object.(*v1.MessagingEndpoint)
				if !ok {
					return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingEndpoint")
				}

				_, infra, err := messaginginfra.LookupInfra(ctx, r.client, endpoint.Namespace)
				if err != nil {
					// Not bound - allow dropping finalizer
					if utilerrors.IsNotBound(err) || utilerrors.IsNotFound(err) {
						logger.Info("[Finalizer] Messaging project not found or bound, ignoring!")
						return reconcile.Result{}, nil
					}
					logger.Info("[Finalizer] Error looking up infra")
					return reconcile.Result{}, err
				}
				client := r.clientManager.GetClient(infra)
				err = client.DeleteEndpoint(endpoint)
				if err != nil {
					if errors.Is(err, stateerrors.ResourceInUseError) {
						logger.Info("[Finalizer] Endpoint is still in use, rescheduling")
						return reconcile.Result{RequeueAfter: 10 * time.Second}, nil
					} else {
						return reconcile.Result{}, err
					}
				}

				err = r.certController.DeleteEndpointCert(ctx, logger, infra, endpoint)
				if err != nil {
					return reconcile.Result{}, err
				}

				serviceName := getServiceName(endpoint)
				meta := metav1.ObjectMeta{Namespace: infra.Namespace, Name: serviceName}
				if endpoint.Spec.Ingress != nil {
					ingress := &netv1beta1.Ingress{
						ObjectMeta: meta,
					}
					err = r.client.Delete(ctx, ingress)
					if err != nil && !k8errors.IsNotFound(err) {
						return reconcile.Result{}, err
					}
				}

				if endpoint.Spec.Route != nil {
					route := &routev1.Route{
						ObjectMeta: meta,
					}
					err = r.client.Delete(ctx, route)
					if err != nil && !k8errors.IsNotFound(err) {
						return reconcile.Result{}, err
					}
				}

				service := &corev1.Service{
					ObjectMeta: meta,
				}
				err = r.client.Delete(ctx, service)
				if err != nil && !k8errors.IsNotFound(err) {
					return reconcile.Result{}, err
				}

				logger.Info("[Finalizer] Deleted endpoint", "err", err)
				return reconcile.Result{}, nil
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
	return processorResult{Requeue: result.Requeue, RequeueAfter: result.RequeueAfter}, nil
}

// Reconcile the service and external resources for a given endpoint.
func (r *ReconcileMessagingEndpoint) reconcileEndpointService(ctx context.Context, logger logr.Logger, infra *v1.MessagingInfrastructure, endpoint *v1.MessagingEndpoint) (processorResult, error) {
	// Reconcile service
	serviceName := getServiceName(endpoint)
	service := &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: serviceName},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, service, func() error {
		install.ApplyServiceDefaults(service, "endpoint", endpoint.Name)

		// For NodePort, Cluster and LoadBalancer types, use the service directly
		if endpoint.Spec.NodePort != nil {
			service.Spec.Type = corev1.ServiceTypeNodePort
		} else if endpoint.Spec.LoadBalancer != nil {
			service.Spec.Type = corev1.ServiceTypeLoadBalancer
		} else {
			// For Route and Ingress and Cluster, use a ClusterIP service
			service.Spec.Type = corev1.ServiceTypeClusterIP
		}

		// Apply annotations
		if endpoint.Spec.Annotations != nil {
			for key, value := range endpoint.Spec.Annotations {
				service.Annotations[key] = value
			}
		}

		// Set annotation
		if endpoint.Spec.Tls != nil && endpoint.Spec.Tls.Openshift != nil {
			service.Annotations[openShiftServiceCAAnnotationServingCertBeta] = getOpenShiftCertSecret(serviceName)
		}

		service.Spec.Selector = map[string]string{
			common.LABEL_INFRA: infra.Name,
			"component":        "router",
		}

		if len(service.Spec.Ports) == 0 {
			service.Spec.Ports = make([]corev1.ServicePort, 0)
		}

		for _, internalPort := range endpoint.Status.InternalPorts {
			exists := false
			for _, servicePort := range service.Spec.Ports {
				if servicePort.Name == protocolToPortName[internalPort.Protocol] {
					exists = true
					break
				}
			}
			if exists {
				continue
			}
			if internalPort.Protocol == v1.MessagingProtocolAMQP {
				service.Spec.Ports = append(service.Spec.Ports, corev1.ServicePort{
					Port:       5672,
					Protocol:   corev1.ProtocolTCP,
					TargetPort: intstr.FromInt(internalPort.Port),
					Name:       protocolToPortName[internalPort.Protocol],
				})
			} else if internalPort.Protocol == v1.MessagingProtocolAMQPS {
				service.Spec.Ports = append(service.Spec.Ports, corev1.ServicePort{
					Port:       5671,
					Protocol:   corev1.ProtocolTCP,
					TargetPort: intstr.FromInt(internalPort.Port),
					Name:       protocolToPortName[internalPort.Protocol],
				})
			} else if internalPort.Protocol == v1.MessagingProtocolAMQPWS {
				service.Spec.Ports = append(service.Spec.Ports, corev1.ServicePort{
					Port:       80,
					Protocol:   corev1.ProtocolTCP,
					TargetPort: intstr.FromInt(internalPort.Port),
					Name:       protocolToPortName[internalPort.Protocol],
				})
			} else if internalPort.Protocol == v1.MessagingProtocolAMQPWSS {
				service.Spec.Ports = append(service.Spec.Ports, corev1.ServicePort{
					Port:       433,
					Protocol:   corev1.ProtocolTCP,
					TargetPort: intstr.FromInt(internalPort.Port),
					Name:       protocolToPortName[internalPort.Protocol],
				})
			}
		}
		return nil
	})
	if err != nil {
		return processorResult{}, err
	}

	// Reconcile external endpoints if enabled
	if endpoint.Spec.Route != nil || endpoint.Spec.Ingress != nil {
		result, err := r.reconcileEndpointExternal(ctx, logger, infra, endpoint)
		if err != nil {
			return result, err
		}
	}

	var hostname string
	// If hostname is set, use that
	if endpoint.Spec.Host != nil {
		hostname = *endpoint.Spec.Host
	} else {
		// If not, set hostname based on endpoint type
		if endpoint.Spec.NodePort != nil || endpoint.Spec.Cluster != nil {
			hostname = fmt.Sprintf("%s.%s.svc", serviceName, infra.Namespace)
		} else if endpoint.Spec.LoadBalancer != nil {
			// Lookup service to determine hostname
			service := &corev1.Service{}
			err := r.client.Get(ctx, types.NamespacedName{Name: serviceName, Namespace: infra.Namespace}, service)
			if err != nil {
				return processorResult{}, err
			}

			// Use the first host or IP we can find as the hostname
			// Should we support multiple hosts? (I.e. make .spec.hosts instead of .spec.host?)
			for _, ingress := range service.Status.LoadBalancer.Ingress {
				if ingress.Hostname != "" {
					hostname = ingress.Hostname
					break
				} else if ingress.IP != "" {
					hostname = ingress.IP
					break
				}
			}
		} else if endpoint.Spec.Route != nil {
			if !util.IsOpenshift() {
				return processorResult{}, fmt.Errorf("route endpoints are only supported on OpenShift")
			}

			route := &routev1.Route{}
			// Lookup service to determine hostname
			err := r.client.Get(ctx, types.NamespacedName{Name: serviceName, Namespace: infra.Namespace}, route)
			if err != nil {
				return processorResult{}, err
			}
			hostname = route.Spec.Host
		} else if endpoint.Spec.Ingress != nil {
			// Should never happen as this is checked in reconcileEndpointExternal
			if endpoint.Spec.Host == nil {
				return processorResult{}, fmt.Errorf("ingress endpoints require host to be set")
			}
		}

	}

	endpoint.Status.Host = hostname

	return r.reconcileEndpointPorts(ctx, logger, infra, endpoint)
}

// Reconcile the external endpoints of a service (Route or Ingress)
func (r *ReconcileMessagingEndpoint) reconcileEndpointExternal(ctx context.Context, logger logr.Logger, infra *v1.MessagingInfrastructure, endpoint *v1.MessagingEndpoint) (processorResult, error) {
	serviceName := getServiceName(endpoint)
	service := &corev1.Service{}
	err := r.client.Get(ctx, types.NamespacedName{Name: serviceName, Namespace: infra.Namespace}, service)
	if err != nil {
		return processorResult{}, err
	}

	if len(endpoint.Spec.Protocols) > 1 {
		return processorResult{}, fmt.Errorf("route and ingress endpoints can only handle 1 protocol")
	}

	protocol := endpoint.Spec.Protocols[0]
	if protocol == v1.MessagingProtocolAMQP {
		return processorResult{}, fmt.Errorf("route and ingress endpoints can only handle TLS-enabled AMQP protocol")
	}

	if endpoint.Spec.Route != nil {
		if !util.IsOpenshift() {
			return processorResult{}, fmt.Errorf("route endpoints are only supported on OpenShift")
		}
		route := &routev1.Route{
			ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: serviceName},
		}
		_, err = controllerutil.CreateOrUpdate(ctx, r.client, route, func() error {
			install.ApplyDefaultLabels(&route.ObjectMeta, "endpoint", serviceName)

			// Apply annotations
			if route.Annotations == nil {
				route.Annotations = make(map[string]string, 0)
			}
			if endpoint.Spec.Annotations != nil {
				for key, value := range endpoint.Spec.Annotations {
					route.Annotations[key] = value
				}
			}

			if endpoint.Spec.Host != nil {
				route.Spec.Host = *endpoint.Spec.Host
			}

			route.Spec.To = routev1.RouteTargetReference{
				Kind: "Service",
				Name: serviceName,
			}

			// Route TLS cert configuration applies to the internal router TLS, not the route TLS configuration.
			// However, the route TLS termination cannot be freely set for some protocols.
			if endpoint.Spec.Tls != nil {
				termination := routev1.TLSTerminationPassthrough
				if endpoint.Spec.Route.TlsTermination != nil {
					termination = *endpoint.Spec.Route.TlsTermination
				}

				if protocol == v1.MessagingProtocolAMQPS && termination != routev1.TLSTerminationPassthrough {
					return fmt.Errorf("route endpoints require passthrough termination for AMQPS protocol")
				}

				if route.Spec.TLS == nil {
					route.Spec.TLS = &routev1.TLSConfig{
						Termination: termination,
					}
				}

			} else if protocol == v1.MessagingProtocolAMQPWSS {
				if route.Spec.TLS == nil {
					route.Spec.TLS = &routev1.TLSConfig{
						Termination: routev1.TLSTerminationEdge,
					}
				}
			}

			route.Spec.Port = &routev1.RoutePort{
				TargetPort: intstr.FromString(protocolToPortName[protocol]),
			}
			return nil
		})
		if err != nil {
			return processorResult{}, err
		}

	} else if endpoint.Spec.Ingress != nil {
		if endpoint.Spec.Host == nil {
			return processorResult{}, fmt.Errorf("ingress endpoints require host to be set")
		}

		ingress := &netv1beta1.Ingress{
			ObjectMeta: metav1.ObjectMeta{Namespace: infra.Namespace, Name: serviceName},
		}

		_, err = controllerutil.CreateOrUpdate(ctx, r.client, ingress, func() error {
			install.ApplyDefaultLabels(&ingress.ObjectMeta, "endpoint", serviceName)

			// Apply annotations
			if ingress.Annotations == nil {
				ingress.Annotations = make(map[string]string, 0)
			}
			if endpoint.Spec.Annotations != nil {
				for key, value := range endpoint.Spec.Annotations {
					ingress.Annotations[key] = value
				}
			}

			ingress.Spec.Rules = []netv1beta1.IngressRule{
				{
					Host: *endpoint.Spec.Host,
					IngressRuleValue: netv1beta1.IngressRuleValue{
						HTTP: &netv1beta1.HTTPIngressRuleValue{
							Paths: []netv1beta1.HTTPIngressPath{
								{
									Path: "/",
									Backend: netv1beta1.IngressBackend{
										ServiceName: serviceName,
										ServicePort: intstr.FromString(protocolToPortName[protocol]),
									},
								},
							},
						},
					},
				},
			}

			if endpoint.Spec.Tls != nil {
				ingress.Spec.TLS = []netv1beta1.IngressTLS{
					{
						Hosts: []string{*endpoint.Spec.Host},
					},
				}
			}
			return nil
		})
		if err != nil {
			return processorResult{}, err
		}
	}
	return processorResult{}, nil
}

// Reconcile the TLS configuration of an endpoint. Ensure that certificates get provisioned and setup based on the configuration.
func (r *ReconcileMessagingEndpoint) reconcileEndpointTls(ctx context.Context, logger logr.Logger, infra *v1.MessagingInfrastructure, endpoint *v1.MessagingEndpoint) (processorResult, error) {
	// Ensure TLS configuration is specified if we require it
	if endpoint.Spec.Tls == nil {
		needTls := false
		for _, protocol := range endpoint.Spec.Protocols {
			if protocol == v1.MessagingProtocolAMQPS || (protocol == v1.MessagingProtocolAMQPWSS && !endpoint.IsEdgeTerminated()) {
				needTls = true
				break
			}
		}

		if needTls {
			return processorResult{}, fmt.Errorf("protocol requiring TLS is enabled but no TLS configuration specified")
		}
		return processorResult{}, nil
	}

	if endpoint.Spec.Tls.Selfsigned != nil {
		if endpoint.Status.Host == "" {
			logger.Info("endpoint host not yet determined")
			return processorResult{RequeueAfter: 5 * time.Second}, nil
		}

		// Configure self signed certificates
		certInfo, err := r.certController.ReconcileEndpointCert(ctx, logger, infra, endpoint)
		if err != nil {
			return processorResult{}, err
		}

		caSecretName := cert.GetProjectCaSecretName(endpoint.Namespace)
		secret := &corev1.Secret{}
		err = r.client.Get(ctx, types.NamespacedName{Name: caSecretName, Namespace: infra.Namespace}, secret)
		if err != nil {
			return processorResult{}, err
		}

		endpoint.Status.Tls = &v1.MessagingEndpointStatusTls{
			CaCertificate: string(secret.Data["tls.crt"]),
			CertificateValidity: &v1.MessagingEndpointCertValidity{
				NotBefore: metav1.Time{
					Time: certInfo.NotBefore,
				},
				NotAfter: metav1.Time{
					Time: certInfo.NotAfter,
				},
			},
		}

		// Update route with TLS config if terminated with reencrypt. This has to be done after the route have been
		// initially created, because we need to know the route hostname before we can generate the cert.
		if util.IsOpenshift() && endpoint.Spec.Route != nil && endpoint.Spec.Route.TlsTermination != nil && *endpoint.Spec.Route.TlsTermination == routev1.TLSTerminationReencrypt {
			route := &routev1.Route{}
			err = r.client.Get(ctx, types.NamespacedName{Namespace: infra.Namespace, Name: getServiceName(endpoint)}, route)
			if err != nil {
				return processorResult{}, err
			}
			if route.Spec.TLS != nil && route.Spec.TLS.DestinationCACertificate == "" {
				route.Spec.TLS.DestinationCACertificate = endpoint.Status.Tls.CaCertificate
				err := r.client.Update(ctx, route)
				if err != nil {
					return processorResult{}, err
				}
				return processorResult{Requeue: true}, nil
			}
		}
	} else if endpoint.Spec.Tls.Openshift != nil {
		serviceName := getServiceName(endpoint)
		secret := &corev1.Secret{}
		err := r.client.Get(ctx, types.NamespacedName{Name: getOpenShiftCertSecret(serviceName), Namespace: infra.Namespace}, secret)
		if err != nil {
			return processorResult{}, err
		}

		if secret.Data == nil || secret.Data["tls.key"] == nil || secret.Data["tls.crt"] == nil {
			return processorResult{}, fmt.Errorf("secret not yet poulated with certificates")
		}

		err = r.certController.ReconcileEndpointCertFromValues(ctx, logger, infra, endpoint, secret.Data["tls.key"], secret.Data["tls.crt"])
		if err != nil {
			return processorResult{}, err
		}

		certInfo, err := cert.GetCertInfo(secret.Data["tls.crt"], secret.Data["tls.key"])
		if err != nil {
			return processorResult{}, err
		}
		endpoint.Status.Tls = &v1.MessagingEndpointStatusTls{
			// TODO: Get OpenShift CA? Is it necessary?
			CertificateValidity: &v1.MessagingEndpointCertValidity{
				NotBefore: metav1.Time{
					Time: certInfo.NotBefore,
				},
				NotAfter: metav1.Time{
					Time: certInfo.NotAfter,
				},
			},
		}
		return processorResult{}, nil
	} else if endpoint.Spec.Tls.External != nil {
		key, err := r.getInputValue(ctx, logger, endpoint, &endpoint.Spec.Tls.External.Key)
		if err != nil {
			return processorResult{}, err
		}

		value, err := r.getInputValue(ctx, logger, endpoint, &endpoint.Spec.Tls.External.Certificate)
		if err != nil {
			return processorResult{}, err
		}

		err = r.certController.ReconcileEndpointCertFromValues(ctx, logger, infra, endpoint, key, value)
		if err != nil {
			return processorResult{}, err
		}

		certInfo, err := cert.GetCertInfo(value, key)
		if err != nil {
			return processorResult{}, err
		}
		endpoint.Status.Tls = &v1.MessagingEndpointStatusTls{
			CertificateValidity: &v1.MessagingEndpointCertValidity{
				NotBefore: metav1.Time{
					Time: certInfo.NotBefore,
				},
				NotAfter: metav1.Time{
					Time: certInfo.NotAfter,
				},
			},
		}
		return processorResult{}, nil
	} else {
		return processorResult{}, fmt.Errorf("TLS configuration is missing certificate configuration")
	}
	return processorResult{}, nil
}

func (r *ReconcileMessagingEndpoint) getInputValue(ctx context.Context, logger logr.Logger, endpoint *v1.MessagingEndpoint, input *v1.InputValue) ([]byte, error) {
	if len(input.Value) > 0 {
		return []byte(input.Value), nil
	}

	if input.ValueFromSecret == nil {
		return nil, fmt.Errorf("missing secret reference")
	}

	secret := &corev1.Secret{}
	err := r.client.Get(ctx, types.NamespacedName{Namespace: endpoint.Namespace, Name: input.ValueFromSecret.Name}, secret)
	if err != nil {
		return nil, err
	}

	return secret.Data[input.ValueFromSecret.Key], nil
}

// Update the ports of endpoint (its status section) based on that has been created.
func (r *ReconcileMessagingEndpoint) reconcileEndpointPorts(ctx context.Context, logger logr.Logger, infra *v1.MessagingInfrastructure, endpoint *v1.MessagingEndpoint) (processorResult, error) {
	endpoint.Status.Ports = make([]v1.MessagingEndpointPort, 0)
	if endpoint.Spec.NodePort != nil || endpoint.Spec.Cluster != nil || endpoint.Spec.LoadBalancer != nil {
		serviceName := getServiceName(endpoint)
		service := &corev1.Service{}
		err := r.client.Get(ctx, types.NamespacedName{Name: serviceName, Namespace: infra.Namespace}, service)
		if err != nil {
			return processorResult{}, err
		}

		if len(service.Spec.Ports) == 0 {
			return processorResult{}, fmt.Errorf("no service ports found")
		}
		for _, servicePort := range service.Spec.Ports {
			port := servicePort.Port
			if endpoint.Spec.NodePort != nil {
				if servicePort.NodePort == 0 {
					return processorResult{}, fmt.Errorf("nodePort not yet set for service")
				}
				port = servicePort.NodePort
			}

			// Use nodePort if set for load balancer
			if endpoint.Spec.LoadBalancer != nil && servicePort.NodePort != 0 {
				port = servicePort.NodePort
			}

			endpoint.Status.Ports = append(endpoint.Status.Ports, v1.MessagingEndpointPort{
				Name:     servicePort.Name,
				Protocol: portNameToProtocol[servicePort.Name],
				Port:     int(port),
			})
		}
	} else if endpoint.Spec.Route != nil || endpoint.Spec.Ingress != nil {
		for _, protocol := range endpoint.Spec.Protocols {
			ingressPort := 443
			if protocol == v1.MessagingProtocolAMQPWS {
				ingressPort = 80
			}
			endpoint.Status.Ports = append(endpoint.Status.Ports, v1.MessagingEndpointPort{
				Name:     protocolToPortName[protocol],
				Protocol: protocol,
				Port:     ingressPort,
			})
		}
	}
	return processorResult{}, nil
}

func getServiceName(endpoint *v1.MessagingEndpoint) string {
	return fmt.Sprintf("%s-%s", endpoint.Namespace, endpoint.Name)
}

func getOpenShiftCertSecret(serviceName string) string {
	return fmt.Sprintf("%s-openshift-cert", serviceName)
}

/*
 * Automatically handle status update of the resource after running some reconcile logic.
 */
type resourceContext struct {
	ctx      context.Context
	client   client.Client
	status   *v1.MessagingEndpointStatus
	endpoint *v1.MessagingEndpoint
}

type processorResult struct {
	Requeue      bool
	RequeueAfter time.Duration
	Return       bool
}

func (r *resourceContext) Process(processor func(endpoint *v1.MessagingEndpoint) (processorResult, error)) (processorResult, error) {
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
