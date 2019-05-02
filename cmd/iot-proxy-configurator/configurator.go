/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package main

import (
	"fmt"
	"time"

	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/util/runtime"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/cache"
	"k8s.io/client-go/util/workqueue"

	clientset "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned"
	iotinformers "github.com/enmasseproject/enmasse/pkg/client/informers/externalversions/iot/v1alpha1"

	iotlisters "github.com/enmasseproject/enmasse/pkg/client/listers/iot/v1alpha1"

	"github.com/enmasseproject/enmasse/pkg/qdr"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

type Configurator struct {
	ephermalCertBase string

	kubeclientset    kubernetes.Interface
	enmasseclientset clientset.Interface

	projectLister  iotlisters.IoTProjectLister
	projectsSynced cache.InformerSynced

	workqueue workqueue.RateLimitingInterface

	manage *qdr.Manage
}

func NewConfigurator(
	kubeclientset kubernetes.Interface,
	iotclientset clientset.Interface,
	projectInformer iotinformers.IoTProjectInformer,
	ephermalCertBase string,
) *Configurator {

	controller := &Configurator{
		kubeclientset:    kubeclientset,
		enmasseclientset: iotclientset,

		projectLister:  projectInformer.Lister(),
		projectsSynced: projectInformer.Informer().HasSynced,

		workqueue:        workqueue.NewNamedRateLimitingQueue(workqueue.DefaultControllerRateLimiter(), "IoTProjects"),
		manage:           qdr.NewManage(),
		ephermalCertBase: ephermalCertBase,
	}

	log.Info("Setting up event handlers")

	// listen for events on the project resource
	projectInformer.Informer().AddEventHandler(cache.ResourceEventHandlerFuncs{
		AddFunc: func(obj interface{}) {
			log.V(2).Info("Add", "object", obj)
			controller.enqueueProject(obj)
		},
		UpdateFunc: func(old, new interface{}) {
			log.V(2).Info("Update", "old", old, "new", new)
			controller.enqueueProject(new)
		},
		DeleteFunc: func(obj interface{}) {
			log.V(2).Info("Deleted", "object", obj)
			controller.enqueueProject(obj)
		},
	})

	return controller
}

func (c *Configurator) enqueueProject(obj interface{}) {
	if key, err := cache.MetaNamespaceKeyFunc(obj); err != nil {
		runtime.HandleError(err)
		return
	} else {
		c.workqueue.AddRateLimited(key)
	}
}

// Run main controller
//
// This will run until the `stopCh` is closed, which will then shutdown the
// workqueue, wait for workers to complete, and then return.
func (c *Configurator) Run(threadiness int, stopCh <-chan struct{}) error {
	defer runtime.HandleCrash()
	defer c.workqueue.ShutDown()

	log.Info("Starting IoTProjects controller")

	// prepare the caches

	log.Info("Waiting for informer caches to sync")
	if ok := cache.WaitForCacheSync(stopCh, c.projectsSynced); !ok {
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
			return fmt.Errorf("error syncing '%v': %v, requeuing", key, err.Error())
		}

		// handled successfully, drop from work queue

		c.workqueue.Forget(obj)
		log.Info("Successfully synced", "key", key)

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
		return nil
	}

	// read requested state
	project, err := c.projectLister.IoTProjects(namespace).Get(name)
	if err != nil {

		// something went wrong

		if errors.IsNotFound(err) {

			// we didn't find the object

			log.Info("Item got deleted. Deleting configuration.")

			err = c.deleteProject(&metav1.ObjectMeta{
				Namespace: namespace,
				Name:      name,
			})

			// if something went wrong deleting, then returning
			// and error will re-queue the item
			return err
		}

		return err
	}

	if !project.Status.IsReady || project.Status.DownstreamEndpoint == nil {
		// project is not ready yet
		return nil
	}

	// sync add or update
	_, err = c.syncProject(project)

	// something went wrong syncing the project
	// we will re-queue this by returning the error state
	if err != nil {
		return err
	}

	return nil
}
