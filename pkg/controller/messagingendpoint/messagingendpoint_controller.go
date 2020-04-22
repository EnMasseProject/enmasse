/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingendpoint

import (
	"context"
	"fmt"
	"reflect"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/state"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"

	logr "github.com/go-logr/logr"
	corev1 "k8s.io/api/core/v1"

	k8errors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"

	"k8s.io/client-go/tools/record"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
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
 * TODO MessagingEndpoint
 *
 * - Route
 * - Ingress
 * - LoadBalancer
 * - NodePort
 */

const (
	TENANT_RESOURCE_NAME = "default"
	FINALIZER_NAME       = "enmasse.io/operator"
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

	/*
		fresult, err := r.reconcileFinalizers(ctx, logger, address, tenant)
		if err != nil || fresult.Requeue || fresult.Return {
			return reconcile.Result{Requeue: fresult.Requeue}, err
		}
	*/

	ec := endpointContext{
		endpoint: found,
		status:   found.Status.DeepCopy(),
		ctx:      ctx,
		client:   r.client,
	}

	result, err := ac.Process(func(endpoint *v1beta2.MessagingEndpoint) (reconcile.Result, error) {
		if endpoint.Status.Phase == "" {
			endpoint.Status.Phase = v1beta2.MessagingEndpointConfiguring
		}
		return reconcile.Result{}, nil
	})
	if needReturn(result, err) {
		return result, err
	}

	tenant := &v1beta2.MessagingTenant{}
	result, err = ec.Process(func(endpoint *v1beta2.MessagingEndpoint) (reconcile.Result, error) {
		err = r.client.Get(ctx, types.NamespacedName{Name: TENANT_RESOURCE_NAME, Namespace: endpoint.Namespace}, tenant)
		if err != nil {
			if k8errors.IsNotFound(err) {
				ready := endpoint.Status.GetMessagingEndpointCondition(v1beta2.MessagingEndpointReady)
				ready.SetStatus(corev1.ConditionFalse, "", "Unable to find MessagingTenant in the same namespace")
				return reconcile.Result{RequeueAfter: 10 * time.Second}, nil
			} else {
				logger.Error(err, "Failed to get MessagingTenant")
			}
		}
		return reconcile.Result{}, err
	})
	if needReturn(result, err) {
		return result, err
	}

	tenantState := r.clientManager.GetOrCreateTenant(tenant)
	result, err = ac.Process(func(endpoint *v1beta2.MessagingEndpoint) (reconcile.Result, error) {
		tenantStatus := tenantState.GetStatus()
		if !tenantStatus.Bound {
			ready := endpoint.Status.GetMessagingEndpointCondition(v1beta2.MessagingEndpointReady)
			ready.SetStatus(corev1.ConditionFalse, "", "MessagingTenant is not yet bound")
			return reconcile.Result{RequeueAfter: 10 * time.Second}, nil
		}
		return reconcile.Result{}, err
	})
	if needReturn(result, err) {
		return result, err
	}

	// Ensure endpoint exists.
	result, err = ac.Process(func(endpoint *v1beta2.MessagingEndpoint) (reconcile.Result, error) {
		err = tenantState.EnsureEndpoint(endpoint)
		return reconcile.Result{}, err
	})
	if needReturn(result, err) {
		return result, err
	}

	// Update endpoint status and schedule requeue. Requeue will trigger status update
	result, err = ac.Process(func(endpoint *v1beta2.MessagingEndpoint) (reconcile.Result, error) {
		ready := endpoint.Status.GetMessagingEndpointCondition(v1beta2.MessagingEndpointReady)
		ready.SetStatus(corev1.ConditionTrue, "", "")
		return reconcile.Result{RequeueAfter: 30 * time.Second}, nil
	})
	return result, err
}

type finalizerResult struct {
	Requeue bool
	Return  bool
}

func (r *ReconcileMessagingEndpoint) reconcileFinalizers(ctx context.Context, logger logr.Logger, endpoint *v1beta2.MessagingEndpoint, tenant *v1beta2.MessagingTenant) (finalizerResult, error) {
	// Handle finalizing an deletion state first
	if endpoint.DeletionTimestamp != nil && endpoint.Status.Phase != v1beta2.MessagingEndpointTerminating {
		endpoint.Status.Phase = v1beta2.MessagingEndpointTerminating
		err := r.client.Status().Update(ctx, endpoint)
		return finalizerResult{Requeue: true}, err
	}

	original := endpoint.DeepCopy()
	result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, endpoint, []finalizer.Finalizer{
		finalizer.Finalizer{
			Name: FINALIZER_NAME,
			Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
				endpoint, ok := c.Object.(*v1beta2.MessagingEndpoint)
				if !ok {
					return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingEndpoint")
				}

				if tenant != nil {
					tenantState := r.clientManager.GetOrCreateTenant(tenant)
					err := tenantState.DeleteEndpoint(endpoint)
					return reconcile.Result{}, err
				}
				return reconcile.Result{}, nil
			},
		},
	})
	if err != nil {
		return finalizerResult{}, err
	}

	if result.Requeue {
		// Update and requeue if changed
		if !reflect.DeepEqual(original, tenant) {
			err := r.client.Update(ctx, tenant)
			return finalizerResult{Return: true}, err
		}
	}
	return finalizerResult{Requeue: result.Requeue}, nil
}

/*
 * Automatically handle status update of the endpoint after running some reconcile logic.
 */
type endpointContext struct {
	ctx      context.Context
	client   client.Client
	status   *v1beta2.MessagingEndpointStatus
	endpoint *v1beta2.MessagingEndpoint
}

func (e *endpointContext) Process(processor func(endpoint *v1beta2.MessagingEndpoint) (reconcile.Result, error)) (reconcile.Result, error) {
	result, err := processor(e.endpoint)
	if !reflect.DeepEqual(e.status, e.endpoint.Status) {
		if err != nil || result.Requeue || result.RequeueAfter > 0 {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			statuserr := e.client.Status().Update(e.ctx, e.endpoint)
			if statuserr != nil {
				// If this fails, report the status error if everything else whent ok, otherwise report the original error
				log.Error(statuserr, "Status update failed", "endpoint", e.endpoint.Name)
				if err == nil {
					err = statuserr
				}
			} else {
				e.status = e.endpoint.Status.DeepCopy()
			}
			return result, err
		}
	}
	return result, err
}

func needReturn(result reconcile.Result, err error) bool {
	return err != nil || result.Requeue || result.RequeueAfter > 0
}
