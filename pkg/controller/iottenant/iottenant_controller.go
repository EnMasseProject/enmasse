/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iottenant

import (
	"context"
	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	"github.com/enmasseproject/enmasse/pkg/util/loghandler"
	"github.com/go-logr/logr"
	"k8s.io/client-go/tools/record"
	"reflect"

	corev1 "k8s.io/api/core/v1"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

const ControllerName = "iottenant-controller"

var log = logf.Log.WithName("controller_iottenant")

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

func newReconciler(mgr manager.Manager) *ReconcileIoTTenant {
	return &ReconcileIoTTenant{
		client:   mgr.GetClient(),
		reader:   mgr.GetAPIReader(),
		scheme:   mgr.GetScheme(),
		recorder: mgr.GetEventRecorderFor(ControllerName),
	}
}

func add(mgr manager.Manager, r *ReconcileIoTTenant) error {

	// Create a new controller
	c, err := controller.New(ControllerName, mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource IoTTenant
	err = c.Watch(&source.Kind{Type: &iotv1alpha1.IoTTenant{}}, loghandler.New(&handler.EnqueueRequestForObject{}, log, "IoTTenant"))
	if err != nil {
		return err
	}

	// watch for messaging users

	// FIXME: need a solution
	/*
		err = c.Watch(&source.Kind{Type: &userv1beta1.MessagingUser{}}, loghandler.New(&handler.EnqueueRequestForOwner{
			IsController: true,
			OwnerType:    &iotv1alpha1.IoTTenant{},
		}, log.V(2), "MessagingUser"))
		if err != nil {
			return err
		}
	*/

	// watch for addresses

	err = c.Watch(&source.Kind{Type: &enmassev1.MessagingAddress{}}, loghandler.New(&handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &iotv1alpha1.IoTTenant{},
	}, log.V(2), "Address"))
	if err != nil {
		return err
	}

	return nil
}

var _ reconcile.Reconciler = &ReconcileIoTTenant{}

type ReconcileIoTTenant struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client   client.Client
	reader   client.Reader
	scheme   *runtime.Scheme
	recorder record.EventRecorder
}

func (r *ReconcileIoTTenant) updateTenantStatus(ctx context.Context, originalTenant *iotv1alpha1.IoTTenant, reconciledTenant *iotv1alpha1.IoTTenant, rc *recon.ReconcileContext) (reconcile.Result, error) {

	reqLogger := log.WithValues("Request.Namespace", originalTenant.Namespace, "Request.Name", originalTenant.Name)

	newTenant := reconciledTenant.DeepCopy()

	// get conditions for ready

	resourcesCreatedCondition := newTenant.Status.GetTenantCondition(iotv1alpha1.TenantConditionTypeResourcesCreated)
	resourcesReadyCondition := newTenant.Status.GetTenantCondition(iotv1alpha1.TenantConditionTypeResourcesReady)
	readyCondition := newTenant.Status.GetTenantCondition(iotv1alpha1.TenantConditionTypeReady)

	if reconciledTenant.DeletionTimestamp != nil {

		newTenant.Status.Phase = iotv1alpha1.TenantPhaseTerminating
		newTenant.Status.Message = "Tenant deleted"
		readyCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Tenant is being deleted")
		resourcesCreatedCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Tenant is being deleted")
		resourcesReadyCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Tenant is being deleted")

	} else {

		// FIXME: clean up this mess
		// eval ready state

		if rc.Error() == nil &&
			resourcesCreatedCondition.IsOk() &&
			resourcesReadyCondition.IsOk() {

			newTenant.Status.Phase = iotv1alpha1.TenantPhaseActive
			newTenant.Status.Message = ""
		} else {
			newTenant.Status.Phase = iotv1alpha1.TenantPhaseConfiguring
		}

		// fill main "ready" condition

		var reason = ""
		var message = ""
		var status = corev1.ConditionUnknown
		if rc.Error() != nil {
			reason = "ProcessingError"
			message = rc.Error().Error()
		}
		if newTenant.Status.Phase == iotv1alpha1.TenantPhaseActive {
			status = corev1.ConditionTrue
			newTenant.Status.Message = ""
		} else {
			status = corev1.ConditionFalse
			newTenant.Status.Message = message
		}
		readyCondition.SetStatus(status, reason, message)

	}

	// update status

	if !reflect.DeepEqual(newTenant, originalTenant) {

		reqLogger.Info("Tenant changed, updating status")
		err := r.client.Status().Update(ctx, newTenant)

		// when something changed, we never ask for being re-queued
		// but we do return the update error, which might re-trigger us

		return reconcile.Result{}, err

	} else {

		reqLogger.Info("No tenant change", "needRequeue", rc.NeedRequeue())

		if rc.Error() != nil && util.OnlyNonRecoverableErrors(rc.Error()) {

			// we cannot recover based on the error, so don't report back that error, but record it

			reqLogger.Info("Only non-recoverable errors", "error", rc.Error())
			r.recorder.Event(originalTenant, corev1.EventTypeWarning, "ReconcileError", rc.Error().Error())
			return rc.PlainResult(), nil

		} else {

			// no change, just return the result, may re-queue

			return rc.Result()

		}

	}

}

// Reconcile by reading the IoT tenant spec and making required changes
//
// returning an error will get the request re-queued
func (r *ReconcileIoTTenant) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.V(2).Info("Reconciling IoTTenant")

	ctx := context.Background()

	// Get tenant
	tenant := &iotv1alpha1.IoTTenant{}
	err := r.client.Get(ctx, request.NamespacedName, tenant)

	if err != nil {

		if errors.IsNotFound(err) {

			reqLogger.Info("Tenant was not found")

			// Request object not found, could have been deleted after reconcile request.
			// Owned objects are automatically garbage collected. For additional cleanup logic use finalizers.
			// Return and don't requeue
			return reconcile.Result{}, nil
		}

		// Error reading the object - requeue the request.
		return reconcile.Result{}, err
	}

	// make copy for change detection

	original := tenant.DeepCopy()

	// start reconcile process

	rc := &recon.ReconcileContext{}

	// check for deconstruction

	if tenant.DeletionTimestamp != nil && tenant.Status.Phase != iotv1alpha1.TenantPhaseTerminating {
		reqLogger.Info("Re-queue after setting phase to terminating")
		return r.updateTenantStatus(ctx, original, tenant, rc)
	}
	rc.Process(func() (reconcile.Result, error) {
		return r.checkDeconstruct(ctx, reqLogger, tenant)
	})
	if rc.NeedRequeue() || rc.Error() != nil {
		return rc.Result()
	}

	if tenant.DeletionTimestamp != nil {
		return reconcile.Result{}, nil
	}

	// start construction

	rc.ProcessSimple(func() error {
		return tenant.Status.GetTenantCondition(iotv1alpha1.TenantConditionTypeConfigurationAccepted).
			RunWith("ConfigurationNotAccepted", func() error {
				return r.acceptConfiguration(tenant)
			})
	})

	rc.Process(func() (result reconcile.Result, e error) {
		return r.reconcileManaged(ctx, tenant)
	})

	return r.updateTenantStatus(ctx, original, tenant, rc)

}

func (r *ReconcileIoTTenant) checkDeconstruct(ctx context.Context, reqLogger logr.Logger, tenant *iotv1alpha1.IoTTenant) (reconcile.Result, error) {

	rc := &recon.ReconcileContext{}
	original := tenant.DeepCopy()

	// process finalizers

	rc.Process(func() (result reconcile.Result, e error) {
		return finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, tenant, finalizers)
	})

	if rc.Error() != nil {
		reqLogger.Error(rc.Error(), "Failed to process finalizers")
		// processing finalizers failed
		return rc.Result()
	}

	if rc.NeedRequeue() {
		// persist changes from finalizers, this is signaled to use via "need requeue"
		// Note: we cannot use "updateTenantStatus" here, as we don't update the status section
		reqLogger.Info("Re-queue after processing finalizers")
		if !reflect.DeepEqual(tenant, original) {
			err := r.client.Update(ctx, tenant)
			return reconcile.Result{}, err
		} else {
			return rc.Result()
		}
		// processing the finalizers required a persist step, and so we stop early
		// the call to Update will re-trigger us, and we don't need to set "requeue"
	}

	return rc.Result()

}
