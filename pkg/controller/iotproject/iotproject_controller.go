/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/loghandler"
	"k8s.io/client-go/tools/record"
	"reflect"

	corev1 "k8s.io/api/core/v1"

	"github.com/enmasseproject/enmasse/pkg/util/finalizer"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
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

const ControllerName = "iotproject-controller"

var log = logf.Log.WithName("controller_iotproject")

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

func newReconciler(mgr manager.Manager) *ReconcileIoTProject {
	return &ReconcileIoTProject{
		client:   mgr.GetClient(),
		reader:   mgr.GetAPIReader(),
		scheme:   mgr.GetScheme(),
		recorder: mgr.GetEventRecorderFor(ControllerName),
	}
}

func add(mgr manager.Manager, r *ReconcileIoTProject) error {

	// Create a new controller
	c, err := controller.New(ControllerName, mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource IoTProject
	err = c.Watch(&source.Kind{Type: &iotv1alpha1.IoTProject{}}, loghandler.New(&handler.EnqueueRequestForObject{}, log, "IoTProject"))
	if err != nil {
		return err
	}

	// watch for messaging users

	// FIXME: need a solution
	/*
		err = c.Watch(&source.Kind{Type: &userv1beta1.MessagingUser{}}, loghandler.New(&handler.EnqueueRequestForOwner{
			IsController: true,
			OwnerType:    &iotv1alpha1.IoTProject{},
		}, log.V(2), "MessagingUser"))
		if err != nil {
			return err
		}
	*/

	// watch for addresses

	err = c.Watch(&source.Kind{Type: &enmassev1.MessagingAddress{}}, loghandler.New(&handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &iotv1alpha1.IoTProject{},
	}, log.V(2), "Address"))
	if err != nil {
		return err
	}

	return nil
}

var _ reconcile.Reconciler = &ReconcileIoTProject{}

type ReconcileIoTProject struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client   client.Client
	reader   client.Reader
	scheme   *runtime.Scheme
	recorder record.EventRecorder
}

func (r *ReconcileIoTProject) updateProjectStatus(ctx context.Context, originalProject *iotv1alpha1.IoTProject, reconciledProject *iotv1alpha1.IoTProject, rc *recon.ReconcileContext) (reconcile.Result, error) {

	reqLogger := log.WithValues("Request.Namespace", originalProject.Namespace, "Request.Name", originalProject.Name)

	newProject := reconciledProject.DeepCopy()

	// get conditions for ready

	resourcesCreatedCondition := newProject.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesCreated)
	resourcesReadyCondition := newProject.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesReady)
	readyCondition := newProject.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeReady)

	if reconciledProject.DeletionTimestamp != nil {

		newProject.Status.Phase = iotv1alpha1.ProjectPhaseTerminating
		newProject.Status.Message = "Project deleted"
		readyCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Project is being deleted")
		resourcesCreatedCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Project is being deleted")
		resourcesReadyCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Project is being deleted")

	} else {

		// eval ready state

		if rc.Error() == nil &&
			resourcesCreatedCondition.IsOk() &&
			resourcesReadyCondition.IsOk() {

			newProject.Status.Phase = iotv1alpha1.ProjectPhaseActive
			newProject.Status.Message = ""

		} else {

			newProject.Status.Phase = iotv1alpha1.ProjectPhaseConfiguring

		}

		// fill main "ready" condition

		var reason = ""
		var message = ""
		var status = corev1.ConditionUnknown
		if rc.Error() != nil {
			reason = "ProcessingError"
			message = rc.Error().Error()
		}
		if newProject.Status.Phase == iotv1alpha1.ProjectPhaseActive {
			status = corev1.ConditionTrue
		} else {
			status = corev1.ConditionFalse
		}
		readyCondition.SetStatus(status, reason, message)

	}

	// update status

	if !reflect.DeepEqual(newProject, originalProject) {

		reqLogger.Info("Project changed, updating status")
		err := r.client.Status().Update(ctx, newProject)

		// when something changed, we never ask for being re-queued
		// but we do return the update error, which might re-trigger us

		return reconcile.Result{}, err

	} else {

		reqLogger.Info("No project change", "needRequeue", rc.NeedRequeue())

		if rc.Error() != nil && util.OnlyNonRecoverableErrors(rc.Error()) {

			// we cannot recover based on the error, so don't report back that error, but record it

			reqLogger.Info("Only non-recoverable errors", "error", rc.Error())
			r.recorder.Event(originalProject, corev1.EventTypeWarning, "ReconcileError", rc.Error().Error())
			return rc.PlainResult(), nil

		} else {

			// no change, just return the result, may re-queue

			return rc.Result()

		}

	}

}

// Reconcile by reading the IoT project spec and making required changes
//
// returning an error will get the request re-queued
func (r *ReconcileIoTProject) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.V(2).Info("Reconciling IoTProject")

	ctx := context.Background()

	// Get project
	project := &iotv1alpha1.IoTProject{}
	err := r.client.Get(ctx, request.NamespacedName, project)

	if err != nil {

		if errors.IsNotFound(err) {

			reqLogger.Info("Project was not found")

			// Request object not found, could have been deleted after reconcile request.
			// Owned objects are automatically garbage collected. For additional cleanup logic use finalizers.
			// Return and don't requeue
			return reconcile.Result{}, nil
		}

		// Error reading the object - requeue the request.
		return reconcile.Result{}, err
	}

	// make copy for change detection

	original := project.DeepCopy()

	// start reconcile process

	rc := &recon.ReconcileContext{}

	if project.DeletionTimestamp != nil && project.Status.Phase != iotv1alpha1.ProjectPhaseTerminating {
		reqLogger.Info("Re-queue after setting phase to terminating")
		return r.updateProjectStatus(ctx, original, project, &recon.ReconcileContext{})
	}

	// process finalizers

	rc.Process(func() (result reconcile.Result, e error) {
		return finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, project, finalizers)
	})

	if rc.Error() != nil {
		reqLogger.Error(rc.Error(), "Failed to process finalizers")
		// processing finalizers failed
		return rc.Result()
	}

	if rc.NeedRequeue() {
		// persist changes from finalizers, this is signaled to use via "need requeue"
		// Note: we cannot use "updateProjectStatus" here, as we don't update the status section
		reqLogger.Info("Re-queue after processing finalizers")
		if !reflect.DeepEqual(project, original) {
			err := r.client.Update(ctx, project)
			return reconcile.Result{}, err
		} else {
			return rc.Result()
		}
		// processing the finalizers required a persist step, and so we stop early
		// the call to Update will re-trigger us, and we don't need to set "requeue"
	}

	rc.ProcessSimple(func() error {
		return project.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeConfigurationAccepted).
			RunWith("ConfigurationNotAccepted", func() error {
				return r.acceptConfiguration(project)
			})
	})

	// lookup infrastructure

	// FIXME: the following has to be removed once we no longer need an active endpoint
	project.Status.MessagingInfrastructurePrefix = ""
	rc.ProcessSimple(func() error {
		endpoint, err := findFirstActiveEndpoint(ctx, r.client, project)
		if endpoint != nil {
			project.Status.MessagingInfrastructurePrefix = endpoint.Status.Host
		}
		return err
	})

	rc.Process(func() (result reconcile.Result, e error) {
		return r.reconcileManaged(ctx, project)
	})

	return r.updateProjectStatus(ctx, original, project, rc)

}

// get the first, active endpoint for an IoTProject
// returns an error if there is one
func findFirstActiveEndpoint(ctx context.Context, c client.Client, project *iotv1alpha1.IoTProject) (*enmassev1.MessagingEndpoint, error) {
	endpoints := &enmassev1.MessagingEndpointList{}
	if err := c.List(ctx, endpoints, client.InNamespace(project.Namespace)); err != nil {
		return nil, err
	}

	for _, e := range endpoints.Items {
		if !e.IsActive() {
			continue
		}
		return &e, nil
	}

	return nil, util.NewConfigurationError("No active endpoint found in namespace '%s'", project.Namespace)
}
