/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"context"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"

	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"

	"github.com/enmasseproject/enmasse/pkg/qdr"

	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type Configurator struct {
	client client.Client
	manage *qdr.Manage
}

var _ reconcile.Reconciler = &Configurator{}

func add(mgr manager.Manager) error {

	// create

	c, err := controller.New("qdr-proxy", mgr, controller.Options{
		Reconciler: NewConfigurator(mgr),
	})
	if err != nil {
		return err
	}

	// watch

	err = c.Watch(&source.Kind{Type: &iotv1alpha1.IoTProject{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	// done

	return nil

}

func NewConfigurator(mgr manager.Manager) *Configurator {

	return &Configurator{
		client: mgr.GetClient(),
		manage: qdr.NewManage(),
	}

}

func (c *Configurator) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling IoTProject")

	ctx := context.Background()

	// Get config
	tenant := &iotv1alpha1.IoTProject{}
	err := c.client.Get(ctx, request.NamespacedName, tenant)

	if err != nil {

		if apierrors.IsNotFound(err) {

			reqLogger.Info("Tenant was not found")

			// Request object not found, could have been deleted after reconcile request.
			// Owned objects are automatically garbage collected. For additional cleanup logic use finalizers.
			// Return and don't requeue

			err := c.deleteProject(&metav1.ObjectMeta{
				Namespace: request.Namespace,
				Name:      request.Name,
			})

			return reconcile.Result{}, err
		}

		// Error reading the object - requeue the request.
		return reconcile.Result{}, err
	}

	// apply

	if err := c.syncHandler(tenant); err != nil {
		log.Error(err, "Failed to sync with QDR")
		return reconcile.Result{}, err
	}

	// done

	return reconcile.Result{}, nil
}

// Synchronize the requested state with the actual state
func (c *Configurator) syncHandler(tenant *iotv1alpha1.IoTProject) error {

	// check if we are scheduled for deletion ...
	if tenant.DeletionTimestamp != nil {

		// ... yes we are, go ahead and delete

		log.Info("Item scheduled for deletion. Deleting configuration.")

		return c.deleteProject(&metav1.ObjectMeta{
			Namespace: tenant.Namespace,
			Name:      tenant.Name,
		})

	}

	log.Info("Change on IoTProject: " + tenant.Namespace + "." + tenant.Name)

	if tenant.Status.Phase != iotv1alpha1.ProjectPhaseActive {
		log.Info("Project is not ready yet", "Phase", tenant.Status.Phase)
		// project is not ready yet
		return nil
	}

	// sync add or update
	_, err := c.syncProject(tenant)

	// something went wrong syncing the project
	// we will re-queue this by returning the error state
	if err != nil {
		log.Error(err, "Failed to sync IoTProject with QDR")
		return err
	}

	return nil
}
