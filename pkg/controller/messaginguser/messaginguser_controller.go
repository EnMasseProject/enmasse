/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messaginguser

import (
	"context"
	"reflect"
	"time"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/state"
	"github.com/enmasseproject/enmasse/pkg/util"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/tools/record"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_messaginguser")
var _ reconcile.Reconciler = &ReconcileMessagingUser{}

type ReconcileMessagingUser struct {
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

func newReconciler(mgr manager.Manager) *ReconcileMessagingUser {
	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	clientManager := state.GetClientManager()
	return &ReconcileMessagingUser{
		client:        mgr.GetClient(),
		clientManager: clientManager,
		reader:        mgr.GetAPIReader(),
		recorder:      mgr.GetEventRecorderFor("messaginguser"),
		scheme:        mgr.GetScheme(),
		namespace:     namespace,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingUser) error {

	c, err := controller.New("messaginguser-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1.MessagingUser{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	return err
}

func (r *ReconcileMessagingUser) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	logger.Info("Reconciling MessagingUser")

	found := &v1.MessagingUser{}
	err := r.reader.Get(ctx, request.NamespacedName, found)

	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingUser resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingUser")
		return reconcile.Result{}, err
	}

	rc := resourceContext{
		endpoint: found,
		status:   found.Status.DeepCopy(),
		ctx:      ctx,
		client:   r.client,
	}

	rc.Process(func(identityProvider *v1.MessagingUser) (processorResult, error) {
		// First set configuring state if not set to indicate we are processing the messagingUser.
		if identityProvider.Status.Phase == "" {
			identityProvider.Status.Phase = v1.MessagingUserPhaseConfiguring
		}
		return processorResult{}, nil
	})

	// Initialize and process finalizer
	//result, err := rc.Process(func(identityProvider *v1.IdentityProvider) (processorResult, error) {
	//	return processorResult{}, nil
	//	//return r.reconcileFinalizer(ctx, logger, endpoint)
	//})
	//if result.ShouldReturn(err) {
	//	return result.Result(), err
	//}

	// Mark identity provider status as all-OK
	result, err := rc.Process(func(identityProvider *v1.MessagingUser) (processorResult, error) {

		originalStatus := identityProvider.Status.DeepCopy()
		identityProvider.Status.Phase = v1.MessagingUserPhaseActive
		if !reflect.DeepEqual(originalStatus, identityProvider.Status) {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			err := r.client.Status().Update(ctx, identityProvider)
			return processorResult{}, err
		} else {
			return processorResult{}, nil
		}
	})
	return result.Result(), err
}

/*
 * Automatically handle status update of the resource after running some reconcile logic.
 */
type resourceContext struct {
	ctx      context.Context
	client   client.Client
	status   *v1.MessagingUserStatus
	endpoint *v1.MessagingUser
}

type processorResult struct {
	Requeue      bool
	RequeueAfter time.Duration
	Return       bool
}

func (r *resourceContext) Process(processor func(messagingUser *v1.MessagingUser) (processorResult, error)) (processorResult, error) {
	result, err := processor(r.endpoint)
	if !reflect.DeepEqual(r.status, r.endpoint.Status) {
		if err != nil || result.Requeue || result.RequeueAfter > 0 {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			statuserr := r.client.Status().Update(r.ctx, r.endpoint)
			if statuserr != nil {
				// If this fails, report the status error if everything else went ok, otherwise report the original error
				log.Error(statuserr, "Status update failed", "messagingUser", r.endpoint.Name)
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
