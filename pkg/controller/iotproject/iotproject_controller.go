/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	"time"

	"github.com/enmasseproject/enmasse/pkg/util"

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
	return &ReconcileIoTProject{client: mgr.GetClient(), scheme: mgr.GetScheme()}
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

	// Watch for enmasse address space

	ownerHandler := ForkedEnqueueRequestForOwner{
		OwnerType:    &iotv1alpha1.IoTProject{},
		IsController: false,
	}
	// inject schema so that the handlers know the groupKind
	err = ownerHandler.InjectScheme(r.scheme)
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &enmassev1beta1.AddressSpace{}},
		&handler.EnqueueRequestsFromMapFunc{
			ToRequests: handler.ToRequestsFunc(func(a handler.MapObject) []reconcile.Request {

				l := log.WithValues("kind", "AddressSpace", "namespace", a.Meta.GetNamespace(), "name", a.Meta.GetName())

				l.V(2).Info("Change event")

				// check if we have an owner

				result := ownerHandler.GetOwnerReconcileRequest(a.Meta)

				if result != nil && len(result) > 0 {
					l.Info("Owned resource")
					// looks like an owned resource ... take this is a result
					return result
				}

				/*
				 * TODO: at this point we are acitvely searching through all IoT projects
				 *       for all AddressSpaces that change.
				 */

				// we need to actively look for a mapped resource

				// a is the AddressSpace that changed
				addressSpaceNamespace := a.Meta.GetNamespace()
				addressSpaceName := a.Meta.GetName()

				l.Info("Looking up IoT project for un-owned addressspace")

				// look for an iot project, that references this address space

				return convertToRequests(r.findIoTProjectsByMappedAddressSpaces(addressSpaceNamespace, addressSpaceName))
			}),
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
	scheme *runtime.Scheme
}

func (r *ReconcileIoTProject) updateProjectStatusError(ctx context.Context, request *reconcile.Request, project *iotv1alpha1.IoTProject) error {

	newProject := project.DeepCopy()
	newProject.Status.IsReady = false

	return r.client.Update(ctx, newProject)
}

func (r *ReconcileIoTProject) updateProjectStatusReady(ctx context.Context, request *reconcile.Request, project *iotv1alpha1.IoTProject, endpointStatus *iotv1alpha1.ExternalDownstreamStrategy) error {

	newProject := project.DeepCopy()

	newProject.Status.IsReady = true
	newProject.Status.DownstreamEndpoint = endpointStatus.DeepCopy()

	return r.client.Update(ctx, newProject)
}

func (r *ReconcileIoTProject) applyUpdate(ctx context.Context, status *iotv1alpha1.ExternalDownstreamStrategy, err error, request *reconcile.Request, project *iotv1alpha1.IoTProject) (reconcile.Result, error) {

	if err != nil {

		if util.IsNotReadyYetError(err) {
			// if the resource is not ready yet, then we retry after a delay
			return reconcile.Result{Requeue: true, RequeueAfter: time.Minute}, nil
		}

		log.Error(err, "failed to reconcile")
		_ = r.updateProjectStatusError(ctx, request, project)
		return reconcile.Result{}, err
	}

	err = r.updateProjectStatusReady(ctx, request, project, status)
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

	if project.Spec.DownstreamStrategy.ExternalDownstreamStrategy != nil {

		// handling as external
		// all information has to be externally configured, we are not managing anything

		reqLogger.Info("Handle as external")

		status, err := r.reconcileExternal(ctx, &request, project)
		return r.applyUpdate(ctx, status, err, &request, project)

	} else if project.Spec.DownstreamStrategy.ProvidedDownstreamStrategy != nil {

		// handling as provided

		reqLogger.Info("Handle as provided")

		status, err := r.reconcileProvided(ctx, &request, project)
		return r.applyUpdate(ctx, status, err, &request, project)

	} else if project.Spec.DownstreamStrategy.ManagedDownstreamStrategy != nil {

		// handling as managed
		// we create the addressspace, addresses and internal users

		reqLogger.Info("Handle as managed")

		status, err := r.reconcileManaged(ctx, &request, project)
		return r.applyUpdate(ctx, status, err, &request, project)

	} else {

		// unknown strategy, we don't know how to handle this
		// so re-queuing doesn't make any sense

		reqLogger.Info("Missing or unknown downstream strategy")

		err = r.updateProjectStatusError(ctx, &request, project)
		return reconcile.Result{}, err

	}

}
