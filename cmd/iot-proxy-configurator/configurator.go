/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"fmt"
	"time"

	"github.com/enmasseproject/enmasse/pkg/apis/iot/v1"

	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/util/runtime"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/tools/cache"
	"k8s.io/client-go/util/workqueue"

	clientset "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned"
	iotinformers "github.com/enmasseproject/enmasse/pkg/client/informers/externalversions/iot/v1"

	iotlisters "github.com/enmasseproject/enmasse/pkg/client/listers/iot/v1"

	"github.com/enmasseproject/enmasse/pkg/qdr"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type Configurator struct {
	enmasseclientset clientset.Interface

	tenantLister  iotlisters.IoTTenantLister
	tenantsSynced cache.InformerSynced

	workqueue workqueue.RateLimitingInterface

	manage *qdr.Manage
}

func NewConfigurator(
	iotclientset clientset.Interface,
	tenantInformer iotinformers.IoTTenantInformer,
) *Configurator {

	controller := &Configurator{
		enmasseclientset: iotclientset,

		tenantLister:  tenantInformer.Lister(),
		tenantsSynced: tenantInformer.Informer().HasSynced,

		workqueue: workqueue.NewNamedRateLimitingQueue(workqueue.DefaultControllerRateLimiter(), "IoTProjects"),
		manage:    qdr.NewManage(),
	}

	log.Info("Setting up event handlers")

	// listen for events on the project resource
	tenantInformer.Informer().AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc: func(obj interface{}) {
			log.V(2).Info("Add", "object", obj)
			controller.enqueueTenant(obj, nil)
		},
		UpdateFunc: func(old, new interface{}) {
			log.V(2).Info("Update", "old", old, "new", new)
			controller.enqueueTenant(new, old)
		},
		DeleteFunc: func(obj interface{}) {
			log.V(2).Info("Deleted", "object", obj)
			controller.enqueueTenant(obj, nil)
		},
	})

	return controller
}

func (c *Configurator) enqueueTenant(obj, old interface{}) {

	key, err := cache.MetaNamespaceKeyFunc(obj)

	if err != nil {
		runtime.HandleError(err)
		return
	}

	result := shouldQueue(obj, old)

	log.Info(fmt.Sprintf("Should queue %v -> %v", key, result))

	if !result {
		return
	}

	c.workqueue.AddRateLimited(key)
}

func shouldQueue(obj, old interface{}) bool {

	if old == nil {
		// we only have the current state, no diff ... so enqueue it
		return true
	}

	prj, ok := obj.(*v1.IoTTenant)
	if !ok {
		runtime.HandleError(fmt.Errorf("can only handle objects of type *IoTTenant, was: %T", obj))
		return false
	}

	oldprj, ok := old.(*v1.IoTTenant)
	if !ok {
		runtime.HandleError(fmt.Errorf("'old' must be of type *IoTTenant, was: %T", old))
		return false
	}

	if prj.Status.Phase != oldprj.Status.Phase {
		// always re-queue on phase change
		return true
	}

	// requeue
	return true

}

// Run main controller
//
// This will run until the `stopCh` is closed, which will then shutdown the
// workqueue, wait for workers to complete, and then return.
func (c *Configurator) Run(threadiness int, stopCh <-chan struct{}) error {
	defer runtime.HandleCrash()
	defer c.workqueue.ShutDown()

	log.Info("Starting IoTTenant controller")

	// prepare the caches

	log.Info("Waiting for informer caches to sync")
	if ok := cache.WaitForCacheSync(stopCh, c.tenantsSynced); !ok {
		return fmt.Errorf("failed to wait for caches to sync")
	}

	// start the workers

	log.Info("Starting workers", "numberOfWorkers", threadiness)
	for i := 0; i < threadiness; i++ {
		go wait.Until(c.runWorker, time.Second, stopCh)
	}

	// wait for shutdown

	log.Info("Started workers")
	<-stopCh
	log.Info("Shutting down workers")

	// return

	return nil
}

// fetch any process work
func (c *Configurator) runWorker() {
	for c.processNextWorkItem() {
	}
}

// fetch and process the next work item
func (c *Configurator) processNextWorkItem() bool {
	obj, shutdown := c.workqueue.Get()

	if shutdown {
		return false
	}

	// scope next section in order to use "defer", poor man's try-finally
	err := func(obj interface{}) error {

		// by the end of the function, we are done with this item
		// either we Forget() about it, or re-queue it
		defer c.workqueue.Done(obj)

		// try-cast to string
		key, ok := obj.(string)

		// the work queue should only contain strings
		if !ok {
			// if it doesn't, drop the item
			c.workqueue.Forget(obj)
			runtime.HandleError(fmt.Errorf("expected string in workqueue but got %v", obj))
			return nil
		}

		// try-sync change event, on error -> re-queue

		if err := c.syncHandler(key); err != nil {
			c.workqueue.AddRateLimited(key)
			log.Error(err, "Error syncing project", "key", key)
			return fmt.Errorf("error syncing '%v': %v, requeuing", key, err.Error())
		}

		// handled successfully, drop from work queue

		c.workqueue.Forget(obj)
		log.V(2).Info("Successfully synced", "key", key)

		return nil
	}(obj)

	// if an error occurred ...
	if err != nil {
		// ... handle error ...
		runtime.HandleError(err)
		// ... and continue processing
		return true
	}

	// return, indicating that we want more
	return true
}

// Synchronize the requested state with the actual state
func (c *Configurator) syncHandler(key string) error {

	// parse into namespace + name
	namespace, name, err := cache.SplitMetaNamespaceKey(key)
	if err != nil {
		runtime.HandleError(fmt.Errorf("invalid resource key: %v", key))
		// don't re-queue, so error must be "nil"
		return nil
	}

	log := log.WithValues("namespace", namespace, "name", name)

	// read requested state
	tenant, err := c.tenantLister.IoTTenants(namespace).Get(name)
	if err != nil {

		// something went wrong

		if errors.IsNotFound(err) {

			// we didn't find the object

			log.Info("Item got deleted. Deleting configuration.")

			// if something went wrong deleting, then returning
			// an error will re-queue the item

			return c.deleteTenant(&metav1.ObjectMeta{
				Namespace: namespace,
				Name:      name,
			})

		}

		log.Error(err, "Failed to read IoTTenant")

		return err
	}

	// check if we are scheduled for deletion ...
	if tenant.DeletionTimestamp != nil {

		// ... yes we are, go ahead and delete

		log.Info("Item scheduled for deletion. Deleting configuration.")

		return c.deleteTenant(&metav1.ObjectMeta{
			Namespace: namespace,
			Name:      name,
		})

	}

	log.Info("Change on IoTTenant: " + tenant.Namespace + "." + tenant.Name)

	if tenant.Status.Phase != v1.TenantPhaseActive {
		log.Info("Tenant is not ready yet", "Phase", tenant.Status.Phase)
		// tenant is not ready yet
		return nil
	}

	// sync add or update
	_, err = c.syncTenant(tenant)

	// something went wrong syncing the tenant
	// we will re-queue this by returning the error state
	if err != nil {
		log.Error(err, "Failed to sync IoTTenant with QDR")
		return err
	}

	return nil
}
