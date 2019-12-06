/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	"fmt"

	corev1 "k8s.io/api/core/v1"

	"github.com/enmasseproject/enmasse/pkg/util/finalizer"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
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

var log = logf.Log.WithName("controller_iotproject")

const DefaultEndpointName = "messaging"
const DefaultPortName = "amqps"
const DefaultEndpointMode = iotv1alpha1.Service

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

func newReconciler(mgr manager.Manager) *ReconcileIoTProject {
	return &ReconcileIoTProject{
		client: mgr.GetClient(),
		reader: mgr.GetAPIReader(),
		scheme: mgr.GetScheme(),
	}
}

func add(mgr manager.Manager, r *ReconcileIoTProject) error {

	// Create a new controller
	c, err := controller.New("iotproject-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource IoTProject
	err = c.Watch(&source.Kind{Type: &iotv1alpha1.IoTProject{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	// watch for messaging users

	err = c.Watch(&source.Kind{Type: &userv1beta1.MessagingUser{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &iotv1alpha1.IoTProject{},
	})
	if err != nil {
		return err
	}

	// watch for addresses

	err = c.Watch(&source.Kind{Type: &enmassev1beta1.Address{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &iotv1alpha1.IoTProject{},
	})
	if err != nil {
		return err
	}

	// Watch for address spaces

	err = c.Watch(&source.Kind{Type: &enmassev1beta1.AddressSpace{}}, &handler.EnqueueRequestForOwner{
		OwnerType: &iotv1alpha1.IoTProject{},
	})
	if err != nil {
		return err
	}

	return nil
}

var _ reconcile.Reconciler = &ReconcileIoTProject{}

type ReconcileIoTProject struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client client.Client
	reader client.Reader
	scheme *runtime.Scheme
}

func (r *ReconcileIoTProject) updateProjectStatus(ctx context.Context, project *iotv1alpha1.IoTProject, currentError error) (reconcile.Result, error) {

	newProject := project.DeepCopy()

	// get conditions for ready

	resourcesCreatedCondition := newProject.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesCreated)
	resourcesReadyCondition := newProject.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesReady)
	readyCondition := newProject.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeReady)

	if project.DeletionTimestamp != nil {

		newProject.Status.Phase = iotv1alpha1.ProjectPhaseTerminating
		newProject.Status.PhaseReason = "Project deleted"
		readyCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Project is being deleted")
		resourcesCreatedCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Project is being deleted")
		resourcesReadyCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Project is being deleted")

	} else {

		// eval ready state

		if currentError == nil &&
			resourcesCreatedCondition.IsOk() &&
			resourcesReadyCondition.IsOk() &&
			newProject.Status.DownstreamEndpoint != nil {

			newProject.Status.Phase = iotv1alpha1.ProjectPhaseReady
			newProject.Status.PhaseReason = ""

		} else {

			newProject.Status.Phase = iotv1alpha1.ProjectPhaseConfiguring

		}

		// fill main "ready" condition

		var reason = ""
		var message = ""
		var status = corev1.ConditionUnknown
		if currentError != nil {
			reason = "ProcessingError"
			message = currentError.Error()
		}
		if newProject.Status.Phase == iotv1alpha1.ProjectPhaseReady {
			status = corev1.ConditionTrue
		} else {
			status = corev1.ConditionFalse
		}
		readyCondition.SetStatus(status, reason, message)

	}

	// update status

	err := r.client.Status().Update(ctx, newProject)

	// return

	return reconcile.Result{}, err

}

// Reconcile by reading the IoT project spec and making required changes
//
// returning an error will get the request re-queued
func (r *ReconcileIoTProject) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling IoTProject")

	// Get project
	project := &iotv1alpha1.IoTProject{}
	err := r.client.Get(context.TODO(), request.NamespacedName, project)

	ctx := context.TODO()

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

	// start reconcile process

	rc := &recon.ReconcileContext{}

	if project.DeletionTimestamp != nil && project.Status.Phase != iotv1alpha1.ProjectPhaseTerminating {
		rc.Process(func() (result reconcile.Result, e error) {
			return r.updateProjectStatus(ctx, project, nil)
		})
		return rc.Result()
	}

	rc.Process(func() (result reconcile.Result, e error) {
		return finalizer.ProcessFinalizers(ctx, r.client, r.reader, project, finalizers)
	})

	if rc.Error() != nil || rc.NeedRequeue() {
		log.Info("Re-queue after processing finalizers")
		// processing finalizers required to requeue already, or failed
		return rc.Result()
	}

	// set the tenant name in the status section

	project.Status.TenantName = project.TenantName()

	// process different types

	if project.Spec.DownstreamStrategy.ExternalDownstreamStrategy != nil {

		// handling as external
		// all information has to be externally configured, we are not managing anything

		reqLogger.Info("Handle as external")

		rc.Process(func() (result reconcile.Result, e error) {
			return r.reconcileExternal(ctx, &request, project)
		})
		rc.Process(func() (result reconcile.Result, e error) {
			return r.updateProjectStatus(ctx, project, rc.Error())
		})

	} else if project.Spec.DownstreamStrategy.ProvidedDownstreamStrategy != nil {

		// handling as provided

		reqLogger.Info("Handle as provided")

		rc.Process(func() (result reconcile.Result, e error) {
			return r.reconcileProvided(ctx, &request, project)
		})
		rc.Process(func() (result reconcile.Result, e error) {
			return r.updateProjectStatus(ctx, project, rc.Error())
		})

	} else if project.Spec.DownstreamStrategy.ManagedDownstreamStrategy != nil {

		// handling as managed
		// we create the addressspace, addresses and internal users

		reqLogger.Info("Handle as managed")

		rc.Process(func() (result reconcile.Result, e error) {
			return r.reconcileManaged(ctx, &request, project)
		})
		rc.Process(func() (result reconcile.Result, e error) {
			return r.updateProjectStatus(ctx, project, rc.Error())
		})

	} else {

		// unknown strategy, we don't know how to handle this
		// so re-queuing doesn't make any sense

		reqLogger.Info("Missing or unknown downstream strategy")

		rc.Process(func() (result reconcile.Result, e error) {
			err := fmt.Errorf("missing or unknown downstream strategy")
			return r.updateProjectStatus(ctx, project, err)
		})

	}

	if rc.NeedRequeue() {
		log.Info("Re-queue scheduled")
	}

	return rc.Result()

}
