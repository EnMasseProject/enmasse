/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingaddress

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

var log = logf.Log.WithName("controller_messagingaddress")
var _ reconcile.Reconciler = &ReconcileMessagingAddress{}

type ReconcileMessagingAddress struct {
	client       client.Client
	reader       client.Reader
	recorder     record.EventRecorder
	scheme       *runtime.Scheme
	stateManager state.StateManager
	namespace    string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

/**
 * TODO MessagingAddress
 *
 * - Infra selector
 * - Plan selector
 * - Auth selector
 */

const (
	TENANT_RESOURCE_NAME = "default"
	FINALIZER_NAME       = "enmasse.io/operator"
)

func newReconciler(mgr manager.Manager) *ReconcileMessagingAddress {
	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	stateManager := state.GetStateManager()
	return &ReconcileMessagingAddress{
		client:       mgr.GetClient(),
		stateManager: stateManager,
		reader:       mgr.GetAPIReader(),
		recorder:     mgr.GetEventRecorderFor("messagingaddress"),
		scheme:       mgr.GetScheme(),
		namespace:    namespace,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingAddress) error {

	c, err := controller.New("messagingaddress-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1beta2.MessagingAddress{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	return err
}

func (r *ReconcileMessagingAddress) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	logger.Info("Reconciling MessagingAddress")

	found := &v1beta2.MessagingAddress{}
	err := r.reader.Get(ctx, request.NamespacedName, found)
	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingAddress resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingAddress")
		return reconcile.Result{}, err
	}

	/*
		fresult, err := r.reconcileFinalizers(ctx, logger, address, tenant)
		if err != nil || fresult.Requeue || fresult.Return {
			return reconcile.Result{Requeue: fresult.Requeue}, err
		}
	*/

	ac := addressContext{
		address: found,
		status:  found.Status.DeepCopy(),
		ctx:     ctx,
		client:  r.client,
	}

	result, err := ac.Process(func(address *v1beta2.MessagingAddress) (reconcile.Result, error) {
		if address.Status.Phase == "" {
			address.Status.Phase = v1beta2.MessagingAddressConfiguring
		}
		return reconcile.Result{}, nil
	})
	if needReturn(result, err) {
		return result, err
	}

	tenant := &v1beta2.MessagingTenant{}
	result, err = ac.Process(func(address *v1beta2.MessagingAddress) (reconcile.Result, error) {
		err = r.client.Get(ctx, types.NamespacedName{Name: TENANT_RESOURCE_NAME, Namespace: address.Namespace}, tenant)
		if err != nil {
			if k8errors.IsNotFound(err) {
				ready := address.Status.GetMessagingAddressCondition(v1beta2.MessagingAddressReady)
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

	tenantState := r.stateManager.GetOrCreateTenant(tenant)
	result, err = ac.Process(func(address *v1beta2.MessagingAddress) (reconcile.Result, error) {
		tenantStatus := tenantState.GetStatus()
		if !tenantStatus.Bound {
			ready := address.Status.GetMessagingAddressCondition(v1beta2.MessagingAddressReady)
			ready.SetStatus(corev1.ConditionFalse, "", "MessagingTenant is not yet bound")
			return reconcile.Result{RequeueAfter: 10 * time.Second}, nil
		}
		return reconcile.Result{}, err
	})
	if needReturn(result, err) {
		return result, err
	}

	// Schedule address. Scheduling and creation of addresses are separated and each step is persisted. This is to avoid
	// the case where a scheduled address is forgotten if the operator crashes. Once persisted, the operator will
	// be able to reconcile the broker state as specified in the address status.
	result, err = ac.Process(func(address *v1beta2.MessagingAddress) (reconcile.Result, error) {

		err = tenantState.ScheduleAddress(address)
		if err != nil {
			return reconcile.Result{}, err
		}

		// Signal requeue so that status gets persisted
		return reconcile.Result{Requeue: true}, nil
	})
	if needReturn(result, err) {
		return result, err
	}

	// Ensure address exists. We know where the address should exist now, so just ensure it is done.
	result, err = ac.Process(func(address *v1beta2.MessagingAddress) (reconcile.Result, error) {
		err = tenantState.EnsureAddress(address)
		return reconcile.Result{}, err
	})
	if needReturn(result, err) {
		return result, err
	}

	// Update address status and schedule requeue. Requeue will trigger status update
	result, err = ac.Process(func(address *v1beta2.MessagingAddress) (reconcile.Result, error) {
		ready := address.Status.GetMessagingAddressCondition(v1beta2.MessagingAddressReady)
		ready.SetStatus(corev1.ConditionTrue, "", "")
		return reconcile.Result{RequeueAfter: 30 * time.Second}, nil
	})
	return result, err
}

type finalizerResult struct {
	Requeue bool
	Return  bool
}

func (r *ReconcileMessagingAddress) reconcileFinalizers(ctx context.Context, logger logr.Logger, address *v1beta2.MessagingAddress, tenant *v1beta2.MessagingTenant) (finalizerResult, error) {
	// Handle finalizing an deletion state first
	if address.DeletionTimestamp != nil && address.Status.Phase != v1beta2.MessagingAddressTerminating {
		address.Status.Phase = v1beta2.MessagingAddressTerminating
		err := r.client.Status().Update(ctx, address)
		return finalizerResult{Requeue: true}, err
	}

	original := address.DeepCopy()
	result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, address, []finalizer.Finalizer{
		finalizer.Finalizer{
			Name: FINALIZER_NAME,
			Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
				address, ok := c.Object.(*v1beta2.MessagingAddress)
				if !ok {
					return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingAddress")
				}

				if tenant != nil {
					tenantState := r.stateManager.GetOrCreateTenant(tenant)
					err := tenantState.DeleteAddress(address)
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
 * Automatically handle status update of the address after running some reconcile logic.
 */
type addressContext struct {
	ctx     context.Context
	client  client.Client
	status  *v1beta2.MessagingAddressStatus
	address *v1beta2.MessagingAddress
}

func (a *addressContext) Process(processor func(address *v1beta2.MessagingAddress) (reconcile.Result, error)) (reconcile.Result, error) {
	result, err := processor(a.address)
	if !reflect.DeepEqual(a.status, a.address.Status) {
		if err != nil || result.Requeue || result.RequeueAfter > 0 {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			statuserr := a.client.Status().Update(a.ctx, a.address)
			if statuserr != nil {
				// If this fails, report the status error if everything else whent ok, otherwise report the original error
				log.Error(statuserr, "Status update failed", "address", a.address.Name)
				if err == nil {
					err = statuserr
				}
			} else {
				a.status = a.address.Status.DeepCopy()
			}
			return result, err
		}
	}
	return result, err
}

func needReturn(result reconcile.Result, err error) bool {
	return err != nil || result.Requeue || result.RequeueAfter > 0
}
