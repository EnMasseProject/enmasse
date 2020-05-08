/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messaginginfra

import (
	"context"
	"errors"
	"fmt"
	"reflect"
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"
	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/broker"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/common"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/router"
	"github.com/enmasseproject/enmasse/pkg/controller/messagingtenant"
	"github.com/enmasseproject/enmasse/pkg/state"
	"github.com/enmasseproject/enmasse/pkg/util"
	utilerrors "github.com/enmasseproject/enmasse/pkg/util/errors"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"

	logr "github.com/go-logr/logr"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"

	k8errors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"

	"k8s.io/client-go/tools/record"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/event"
	"sigs.k8s.io/controller-runtime/pkg/predicate"

	//"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_messaginginfra")
var _ reconcile.Reconciler = &ReconcileMessagingInfra{}

const (
	FINALIZER_NAME = "enmasse.io/operator"
)

type ReconcileMessagingInfra struct {
	client           client.Client
	reader           client.Reader
	recorder         record.EventRecorder
	scheme           *runtime.Scheme
	certController   *cert.CertController
	routerController *router.RouterController
	brokerController *broker.BrokerController
	clientManager    state.ClientManager
	namespace        string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

/**
 * - TODO Make certificate expiry configurable
 * - TODO Add podTemplate configuration to CRD in .spec.router.podTemplate and .spec.broker.podTemplate and apply
 * - TODO Add static router configuration options in .spec.router: thread pool, min available, max unavailable, link capacity, idle timeout
 * - TODO Add static broker configuration options in .spec.broker: minAvailable, maxUnavailable, storage capacity, java options, address full policy, global max size, storage class name, resize persistent volume claim, threadpool config, global address policies
 */

func newReconciler(mgr manager.Manager) *ReconcileMessagingInfra {
	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	// TODO: Make expiry configurable
	clientManager := state.GetClientManager()
	certController := cert.NewCertController(mgr.GetClient(), mgr.GetScheme(), 24*30*time.Hour, 24*time.Hour)
	brokerController := broker.NewBrokerController(mgr.GetClient(), mgr.GetScheme(), certController)
	routerController := router.NewRouterController(mgr.GetClient(), mgr.GetScheme(), certController)
	return &ReconcileMessagingInfra{
		client:           mgr.GetClient(),
		certController:   certController,
		routerController: routerController,
		brokerController: brokerController,
		clientManager:    clientManager,
		reader:           mgr.GetAPIReader(),
		recorder:         mgr.GetEventRecorderFor("messaginginfra"),
		scheme:           mgr.GetScheme(),
		namespace:        namespace,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingInfra) error {

	c, err := controller.New("messaginginfra-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1beta2.MessagingInfra{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	// Watch changes to statefulsets for underlying routers and brokers
	err = c.Watch(&source.Kind{Type: &appsv1.StatefulSet{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &v1beta2.MessagingInfra{},
	})
	if err != nil {
		return err
	}

	// Watch pods that map to this infra
	err = c.Watch(&source.Kind{Type: &corev1.Pod{}}, &handler.EnqueueRequestsFromMapFunc{
		ToRequests: handler.ToRequestsFunc(func(o handler.MapObject) []reconcile.Request {
			pod := o.Object.(*corev1.Pod)
			annotations := pod.Annotations
			if annotations == nil {
				return []reconcile.Request{}
			}

			if _, exists := annotations[common.ANNOTATION_INFRA_NAME]; !exists {
				return []reconcile.Request{}
			}

			if _, exists := annotations[common.ANNOTATION_INFRA_NAMESPACE]; !exists {
				return []reconcile.Request{}
			}
			return []reconcile.Request{
				{
					NamespacedName: types.NamespacedName{
						Name:      pod.Annotations[common.ANNOTATION_INFRA_NAME],
						Namespace: pod.Annotations[common.ANNOTATION_INFRA_NAMESPACE],
					},
				},
			}
		}),
	}, &infraPodPredicate{})
	if err != nil {
		return err
	}

	/*
		// Watch changes to tenants to retrigger infra sync
		err = c.Watch(&source.Kind{Type: &v1beta2.MessagingTenant{}}, &handler.EnqueueRequestsFromMapFunc{
			ToRequests: handler.ToRequestsFunc(func(o handler.MapObject) []reconcile.Request {
				tenant := o.Object.(*v1beta2.MessagingTenant)
				if tenant.Status.MessagingInfraRef != nil {
					return []reconcile.Request{
						{
							NamespacedName: types.NamespacedName{
								Name:      tenant.Status.MessagingInfraRef.Name,
								Namespace: tenant.Status.MessagingInfraRef.Namespace,
							},
						},
					}
				} else {
					return []reconcile.Request{}
				}
			}),
		})
		if err != nil {
			return err
		}

		// Watch changes to addresses that map to our infra
		err = c.Watch(&source.Kind{Type: &v1beta2.MessagingAddress{}}, &handler.EnqueueRequestsFromMapFunc{
			ToRequests: handler.ToRequestsFunc(func(o handler.MapObject) []reconcile.Request {
				address := o.Object.(*v1beta2.MessagingAddress)
				tenant := &v1beta2.MessagingTenant{}
				err := r.client.Get(context.Background(), client.ObjectKey{Name: messagingtenant.TENANT_RESOURCE_NAME, Namespace: address.Namespace}, tenant)
				if err != nil || tenant.Status.MessagingInfraRef == nil {
					// Skip triggering if we can't find tenant
					return []reconcile.Request{}
				}

				return []reconcile.Request{
					{
						NamespacedName: types.NamespacedName{
							Namespace: tenant.Status.MessagingInfraRef.Namespace,
							Name:      tenant.Status.MessagingInfraRef.Name,
						},
					},
				}
			}),
		})
		if err != nil {
			return err
		}

		// Watch changes to endpoints that map to our infra
		err = c.Watch(&source.Kind{Type: &v1beta2.MessagingEndpoint{}}, &handler.EnqueueRequestsFromMapFunc{
			ToRequests: handler.ToRequestsFunc(func(o handler.MapObject) []reconcile.Request {
				endpoint := o.Object.(*v1beta2.MessagingEndpoint)
				tenant := &v1beta2.MessagingTenant{}
				err := r.client.Get(context.Background(), client.ObjectKey{Name: messagingtenant.TENANT_RESOURCE_NAME, Namespace: endpoint.Namespace}, tenant)
				if err != nil || tenant.Status.MessagingInfraRef == nil {
					// Skip triggering if we can't find tenant
					return []reconcile.Request{}
				}

				return []reconcile.Request{
					{
						NamespacedName: types.NamespacedName{
							Namespace: tenant.Status.MessagingInfraRef.Namespace,
							Name:      tenant.Status.MessagingInfraRef.Name,
						},
					},
				}
			}),
		})
	*/

	return err
}

type infraPodPredicate struct {
	predicate.Funcs
}

// Predicate that only matches pods that have annotations of their infra set
func (infraPodPredicate) Update(e event.UpdateEvent) bool {
	if e.MetaOld == nil {
		log.Error(nil, "UpdateEvent has no old metadata", "event", e)
		return false
	}

	if e.MetaOld.GetAnnotations() == nil {
		return false
	}
	annotations := e.MetaOld.GetAnnotations()

	if _, exists := annotations[common.ANNOTATION_INFRA_NAME]; !exists {
		return false
	}

	if _, exists := annotations[common.ANNOTATION_INFRA_NAMESPACE]; !exists {
		return false
	}
	return true
}

func (r *ReconcileMessagingInfra) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	logger.Info("Reconciling MessagingInfra")

	found := &v1beta2.MessagingInfra{}
	err := r.reader.Get(ctx, request.NamespacedName, found)
	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingInfra resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingInfra")
		return reconcile.Result{}, err
	}

	fresult, err := r.reconcileFinalizers(ctx, logger, found)
	if err != nil || fresult.Requeue || fresult.Return {
		return reconcile.Result{Requeue: fresult.Requeue}, err
	}

	// Start regular processing loop
	rc := &resourceContext{
		ctx:    ctx,
		client: r.client,
		infra:  found,
		status: found.Status.DeepCopy(),
	}

	var ready *v1beta2.MessagingInfraCondition
	var caCreated *v1beta2.MessagingInfraCondition
	var brokersCreated *v1beta2.MessagingInfraCondition
	var routersCreated *v1beta2.MessagingInfraCondition
	var brokersConnected *v1beta2.MessagingInfraCondition
	var synchronized *v1beta2.MessagingInfraCondition

	// Initialize status
	result, err := rc.Process(func(infra *v1beta2.MessagingInfra) (processorResult, error) {
		if infra.Status.Phase == "" || infra.Status.Phase == v1beta2.MessagingInfraPending {
			infra.Status.Phase = v1beta2.MessagingInfraConfiguring
		}

		ready = infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraReady)
		caCreated = infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraCaCreated)
		brokersCreated = infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraBrokersCreated)
		routersCreated = infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraRoutersCreated)
		brokersConnected = infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraBrokersConnected)
		synchronized = infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraSynchronized)
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Reconcile CA
	result, err = rc.Process(func(infra *v1beta2.MessagingInfra) (processorResult, error) {
		err := r.certController.ReconcileCa(ctx, logger, infra)
		if err != nil {
			infra.Status.Message = err.Error()
			caCreated.SetStatus(corev1.ConditionFalse, "", err.Error())
			return processorResult{}, err
		}
		caCreated.SetStatus(corev1.ConditionTrue, "", "")
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Reconcile Routers
	var routerHosts []string
	result, err = rc.Process(func(infra *v1beta2.MessagingInfra) (processorResult, error) {
		hosts, err := r.routerController.ReconcileRouters(ctx, logger, infra)
		if err != nil {
			infra.Status.Message = err.Error()
			routersCreated.SetStatus(corev1.ConditionFalse, "", err.Error())
			return processorResult{}, err
		}
		routersCreated.SetStatus(corev1.ConditionTrue, "", "")
		routerHosts = hosts
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Reconcile Brokers
	var brokerHosts []string
	result, err = rc.Process(func(infra *v1beta2.MessagingInfra) (processorResult, error) {
		hosts, err := r.brokerController.ReconcileBrokers(ctx, logger, infra)
		if err != nil {
			infra.Status.Message = err.Error()
			brokersCreated.SetStatus(corev1.ConditionFalse, "", err.Error())
			return processorResult{}, err
		}
		brokersCreated.SetStatus(corev1.ConditionTrue, "", "")
		brokerHosts = hosts
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Sync all configuration
	var connectorStatuses []state.ConnectorStatus
	result, err = rc.Process(func(infra *v1beta2.MessagingInfra) (processorResult, error) {
		statuses, err := r.clientManager.GetClient(infra).SyncAll(routerHosts, brokerHosts)
		// Treat as transient error
		if errors.Is(err, amqpcommand.RequestTimeoutError) {
			logger.Info("Error syncing infra", "error", err.Error())
			infra.Status.Message = err.Error()
			synchronized.SetStatus(corev1.ConditionFalse, "", err.Error())
			return processorResult{RequeueAfter: 10 * time.Second}, nil
		}
		if err != nil {
			return processorResult{}, err
		}

		synchronized.SetStatus(corev1.ConditionTrue, "", "")
		connectorStatuses = statuses
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Check status
	result, err = rc.Process(func(infra *v1beta2.MessagingInfra) (processorResult, error) {
		for _, connectorStatus := range connectorStatuses {
			if !connectorStatus.Connected {
				msg := fmt.Sprintf("connection between %s and %s not ready: %s", connectorStatus.Router, connectorStatus.Broker, connectorStatus.Message)
				infra.Status.Message = msg
				brokersConnected.SetStatus(corev1.ConditionFalse, "", msg)
				return processorResult{RequeueAfter: 10 * time.Second}, nil
			}
		}
		brokersConnected.SetStatus(corev1.ConditionTrue, "", "")
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Update main condition
	result, err = rc.Process(func(infra *v1beta2.MessagingInfra) (processorResult, error) {
		originalStatus := infra.Status.DeepCopy()
		infra.Status.Phase = v1beta2.MessagingInfraActive
		infra.Status.Message = ""
		ready.SetStatus(corev1.ConditionTrue, "", "")
		if !reflect.DeepEqual(originalStatus, infra.Status) {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			err := r.client.Status().Update(ctx, infra)
			return processorResult{}, err
		}
		return processorResult{}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Recheck infra state periodically
	return reconcile.Result{RequeueAfter: 30 * time.Second}, nil
}

type finalizerResult struct {
	Requeue bool
	Return  bool
}

func (r *ReconcileMessagingInfra) reconcileFinalizers(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra) (finalizerResult, error) {
	// Handle finalizing an deletion state first
	if infra.DeletionTimestamp != nil && infra.Status.Phase != v1beta2.MessagingInfraTerminating {
		infra.Status.Phase = v1beta2.MessagingInfraTerminating
		err := r.client.Status().Update(ctx, infra)
		return finalizerResult{Requeue: true}, err
	}

	original := infra.DeepCopy()
	result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, infra, []finalizer.Finalizer{
		finalizer.Finalizer{
			Name: FINALIZER_NAME,
			Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
				infra, ok := c.Object.(*v1beta2.MessagingInfra)
				if !ok {
					return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingInfra")
				}

				// Check if its in use by any tenants
				tenants := &v1beta2.MessagingTenantList{}
				err := r.client.List(ctx, tenants)
				if err != nil {
					return reconcile.Result{}, err
				}
				for _, tenant := range tenants.Items {
					if tenant.Status.MessagingInfraRef != nil && tenant.Status.MessagingInfraRef.Name == infra.Name && tenant.Status.MessagingInfraRef.Namespace == infra.Namespace {
						return reconcile.Result{}, fmt.Errorf("unable to delete MessagingInfra %s/%s: in use by MessagingTenant %s/%s", infra.Namespace, infra.Name, tenant.Namespace, tenant.Name)
					}
				}

				err = r.clientManager.DeleteClient(infra)
				return reconcile.Result{}, err
			},
		},
	})
	if err != nil {
		return finalizerResult{}, err
	}

	if result.Requeue {
		// Update and requeue if changed
		if !reflect.DeepEqual(original, infra) {
			err := r.client.Update(ctx, infra)
			return finalizerResult{Return: true}, err
		}
	}
	return finalizerResult{Requeue: result.Requeue}, nil
}

/*
 * Automatically handle status update of the resource after running some reconcile logic.
 */
type resourceContext struct {
	ctx    context.Context
	client client.Client
	status *v1beta2.MessagingInfraStatus
	infra  *v1beta2.MessagingInfra
}

type processorResult struct {
	Requeue      bool
	RequeueAfter time.Duration
	Return       bool
}

func (r *resourceContext) Process(processor func(infra *v1beta2.MessagingInfra) (processorResult, error)) (processorResult, error) {
	result, err := processor(r.infra)
	if !reflect.DeepEqual(r.status, r.infra.Status) {
		if err != nil || result.Requeue || result.RequeueAfter > 0 {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			statuserr := r.client.Status().Update(r.ctx, r.infra)
			if statuserr != nil {
				// If this fails, report the status error if everything else whent ok, otherwise report the original error
				log.Error(statuserr, "Status update failed", "infra", r.infra.Name)
				if err == nil {
					err = statuserr
				}
			} else {
				r.status = r.infra.Status.DeepCopy()
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

// Find the MessagingInfra servicing a given namespace
func LookupInfra(ctx context.Context, c client.Client, namespace string) (*v1beta2.MessagingInfra, error) {
	// Retrieve the MessagingTenant for this namespace
	tenant := &v1beta2.MessagingTenant{}
	err := c.Get(ctx, types.NamespacedName{Name: messagingtenant.TENANT_RESOURCE_NAME, Namespace: namespace}, tenant)
	if err != nil {
		if k8errors.IsNotFound(err) {
			return nil, utilerrors.NewNotFoundError("MessagingTenant", messagingtenant.TENANT_RESOURCE_NAME, namespace)
		}
		return nil, err
	}

	if tenant.Status.MessagingInfraRef == nil {
		return nil, utilerrors.NewNotBoundError(namespace)
	}

	// Retrieve the MessagingInfra for this MessagingTenant
	infra := &v1beta2.MessagingInfra{}
	err = c.Get(ctx, types.NamespacedName{Name: tenant.Status.MessagingInfraRef.Name, Namespace: tenant.Status.MessagingInfraRef.Namespace}, infra)
	if err != nil {
		return nil, err
	}
	return infra, nil
}
