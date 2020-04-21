/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingtenant

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

	"k8s.io/client-go/tools/record"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_messagingtenant")
var _ reconcile.Reconciler = &ReconcileMessagingTenant{}

type ReconcileMessagingTenant struct {
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
 * TODO MessagingTenant
 *
 * - Infra selector
 * - Plan selector
 * - Auth selector
 */

const (
	TENANT_RESOURCE_NAME = "default"
	FINALIZER_NAME       = "enmasse.io/messaging-infra"
)

func newReconciler(mgr manager.Manager) *ReconcileMessagingTenant {
	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	stateManager := state.GetStateManager()
	return &ReconcileMessagingTenant{
		client:       mgr.GetClient(),
		stateManager: stateManager,
		reader:       mgr.GetAPIReader(),
		recorder:     mgr.GetEventRecorderFor("messagingtenant"),
		scheme:       mgr.GetScheme(),
		namespace:    namespace,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingTenant) error {

	c, err := controller.New("messagingtenant-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1beta2.MessagingTenant{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	return err
}

func (r *ReconcileMessagingTenant) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	if request.Name != TENANT_RESOURCE_NAME {
		logger.Info("Unsupported resource name")
		return reconcile.Result{}, nil
	}

	logger.Info("Reconciling MessagingTenant")

	tenant := &v1beta2.MessagingTenant{}
	err := r.reader.Get(ctx, request.NamespacedName, tenant)
	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingTenant resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingTenant")
		return reconcile.Result{}, err
	}

	fresult, err := r.reconcileFinalizers(ctx, logger, tenant)
	if err != nil || fresult.Requeue || fresult.Return {
		return reconcile.Result{Requeue: fresult.Requeue}, err
	}

	// Make sure tenant state is created
	tenantState := r.stateManager.GetOrCreateTenant(tenant)

	err = r.stateManager.BindTenantToInfra(tenant)
	if err != nil {
		// We don't need to handle this error. It will be reflected in the status and it will be retried
		logger.Info("Unable to bind tenant to a shared infrastructure")
	}

	originalStatus := tenant.Status.DeepCopy()

	// TODO: Check status and update conditions
	status := tenantState.GetStatus()

	requeueAfter := 0 * time.Second
	ready := tenant.Status.GetMessagingTenantCondition(v1beta2.MessagingTenantReady)
	if status.Bound {
		ready.SetStatus(corev1.ConditionTrue, "", "")
		tenant.Status.Phase = v1beta2.MessagingTenantActive
		tenant.Status.MessagingInfraRef = &v1beta2.MessagingInfraReference{
			Name:      status.InfraName,
			Namespace: status.InfraNamespace,
		}
	} else {
		ready.SetStatus(corev1.ConditionFalse, "", "Not yet bound to infrastructure")
		tenant.Status.Phase = v1beta2.MessagingTenantConfiguring
		requeueAfter = 10 * time.Second
	}

	if !reflect.DeepEqual(originalStatus, tenant.Status) {
		err := r.client.Status().Update(ctx, tenant)
		if err != nil {
			logger.Error(err, "Status update failed")
			return reconcile.Result{}, err
		}
	}

	return reconcile.Result{RequeueAfter: requeueAfter}, nil
}

type finalizerResult struct {
	Requeue bool
	Return  bool
}

func (r *ReconcileMessagingTenant) reconcileFinalizers(ctx context.Context, logger logr.Logger, tenant *v1beta2.MessagingTenant) (finalizerResult, error) {
	// Handle finalizing an deletion state first
	if tenant.DeletionTimestamp != nil && tenant.Status.Phase != v1beta2.MessagingTenantTerminating {
		tenant.Status.Phase = v1beta2.MessagingTenantTerminating
		err := r.client.Status().Update(ctx, tenant)
		return finalizerResult{Requeue: true}, err
	}

	original := tenant.DeepCopy()
	result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, tenant, []finalizer.Finalizer{
		finalizer.Finalizer{
			Name: FINALIZER_NAME,
			Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
				tenant, ok := c.Object.(*v1beta2.MessagingTenant)
				if !ok {
					return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingTenant")
				}

				err := r.stateManager.DeleteTenant(tenant)
				return reconcile.Result{}, err
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
