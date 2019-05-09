/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	"github.com/enmasseproject/enmasse/pkg/util"

	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"

	routev1 "github.com/openshift/api/route/v1"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/apis/meta/v1"

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

var log = logf.Log.WithName("controller_iotconfig")

type DeviceRegistryImplementation int

const (
	DeviceRegistryDefault = iota
	DeviceRegistryIllegal
	DeviceRegistryFileBased
	DeviceRegistryInfinispan
)

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(
		mgr,
		util.GetEnvOrDefault("ENMASSE_IOT_CONFIG_NAME", "default"),
	))
}

func newReconciler(mgr manager.Manager, configName string) *ReconcileIoTConfig {
	return &ReconcileIoTConfig{
		client:     mgr.GetClient(),
		scheme:     mgr.GetScheme(),
		configName: configName,
	}
}

type watching struct {
	obj       runtime.Object
	openshift bool
}

func add(mgr manager.Manager, r *ReconcileIoTConfig) error {

	// Create a new controller
	c, err := controller.New("iotconfig-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource IoTConfig
	err = c.Watch(&source.Kind{Type: &iotv1alpha1.IoTConfig{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	// watch owned objects

	for _, w := range []watching{
		{&appsv1.Deployment{}, false},
		{&corev1.Service{}, false},
		{&corev1.ConfigMap{}, false},
		{&corev1.Secret{}, false},
		{&corev1.PersistentVolumeClaim{}, false},

		{&routev1.Route{}, true},
	} {

		if w.openshift && !util.IsOpenshift() {
			// requires openshift, but we don't have it
			continue
		}

		err = c.Watch(&source.Kind{Type: w.obj}, &handler.EnqueueRequestForOwner{
			OwnerType:    &iotv1alpha1.IoTConfig{},
			IsController: true,
		})
		if err != nil {
			return err
		}

	}

	return nil
}

// ensure we are implementing the interface
var _ reconcile.Reconciler = &ReconcileIoTConfig{}

type ReconcileIoTConfig struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client client.Client
	scheme *runtime.Scheme

	// The name of the configuration we are watching
	// we are watching only one config, in our own namespace
	configName string
}

// Reconcile
//
// returning an error will get the request re-queued
func (r *ReconcileIoTConfig) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling IoTConfig")

	// Get config
	config := &iotv1alpha1.IoTConfig{}
	err := r.client.Get(context.TODO(), request.NamespacedName, config)

	ctx := context.TODO()

	if err != nil {

		if errors.IsNotFound(err) {

			reqLogger.Info("Config was not found")

			// Request object not found, could have been deleted after reconcile request.
			// Owned objects are automatically garbage collected. For additional cleanup logic use finalizers.
			// Return and don't requeue
			return reconcile.Result{}, nil
		}

		// Error reading the object - requeue the request.
		return reconcile.Result{}, err
	}

	if config.Name != r.configName {
		return r.failWrongConfigName(ctx, config)
	}

	rc := &recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.processGeneratedCredentials(ctx, config)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processQdrProxyConfig(ctx, config)
	})
	rc.ProcessSimple(func() error {
		return r.processCollector(ctx, config)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processAuthService(ctx, config)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processTenantService(ctx, config)
	})
	rc.Process(func() (reconcile.Result, error) {
		switch deviceRegistryImplementation(config) {
		case DeviceRegistryInfinispan:
			return r.processInfinispanDeviceRegistry(ctx, config)
		case DeviceRegistryFileBased:
			return r.processFileDeviceRegistry(ctx, config)
		default:
			return reconcile.Result{}, fmt.Errorf("illegal device registry configuration.")
		}
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processHttpAdapter(ctx, config)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processMqttAdapter(ctx, config)
	})

	return r.updateStatus(ctx, config, rc)
}

func (r *ReconcileIoTConfig) updateStatus(ctx context.Context, config *iotv1alpha1.IoTConfig, rc *recon.ReconcileContext) (reconcile.Result, error) {

	// we are initialized when there is no error
	config.Status.Initialized = rc.Error() == nil

	if config.Status.Initialized {
		config.Status.State = iotv1alpha1.ConfigStateRunning
	} else {
		config.Status.State = iotv1alpha1.ConfigStateFailed
	}

	// do a status update

	rc.ProcessSimple(func() error {
		return r.client.Update(ctx, config)
	})

	// return result ... including status update

	return rc.Result()
}

func (r *ReconcileIoTConfig) failWrongConfigName(ctx context.Context, config *iotv1alpha1.IoTConfig) (reconcile.Result, error) {

	config.Status.Initialized = false
	config.Status.State = iotv1alpha1.ConfigStateWrongName

	if err := r.client.Update(ctx, config); err != nil {
		return reconcile.Result{}, err
	}

	return reconcile.Result{}, nil
}

func (r *ReconcileIoTConfig) processDeployment(ctx context.Context, name string, config *iotv1alpha1.IoTConfig, manipulator func(config *iotv1alpha1.IoTConfig, deployment *appsv1.Deployment) error) error {

	deployment := appsv1.Deployment{
		ObjectMeta: v1.ObjectMeta{Namespace: config.Namespace, Name: name},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, &deployment, func(existing runtime.Object) error {
		existingDeployment := existing.(*appsv1.Deployment)

		if err := controllerutil.SetControllerReference(config, existingDeployment, r.scheme); err != nil {
			return err
		}

		return manipulator(config, existingDeployment)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) processService(ctx context.Context, name string, config *iotv1alpha1.IoTConfig, manipulator func(config *iotv1alpha1.IoTConfig, service *corev1.Service) error) error {

	service := corev1.Service{
		ObjectMeta: v1.ObjectMeta{Namespace: config.Namespace, Name: name},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, &service, func(existing runtime.Object) error {
		existingService := existing.(*corev1.Service)

		if err := controllerutil.SetControllerReference(config, existingService, r.scheme); err != nil {
			return err
		}

		return manipulator(config, existingService)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) processConfigMap(ctx context.Context, name string, config *iotv1alpha1.IoTConfig, manipulator func(config *iotv1alpha1.IoTConfig, configMap *corev1.ConfigMap) error) error {

	cm := corev1.ConfigMap{
		ObjectMeta: v1.ObjectMeta{Namespace: config.Namespace, Name: name},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, &cm, func(existing runtime.Object) error {
		existingConfigMap := existing.(*corev1.ConfigMap)

		if err := controllerutil.SetControllerReference(config, existingConfigMap, r.scheme); err != nil {
			return err
		}

		return manipulator(config, existingConfigMap)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) processSecret(ctx context.Context, name string, config *iotv1alpha1.IoTConfig, manipulator func(config *iotv1alpha1.IoTConfig, secret *corev1.Secret) error) error {

	secret := corev1.Secret{
		ObjectMeta: v1.ObjectMeta{Namespace: config.Namespace, Name: name},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, &secret, func(existing runtime.Object) error {
		existingSecret := existing.(*corev1.Secret)

		if err := controllerutil.SetControllerReference(config, existingSecret, r.scheme); err != nil {
			return err
		}

		return manipulator(config, existingSecret)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) processPersistentVolumeClaim(ctx context.Context, name string, config *iotv1alpha1.IoTConfig, manipulator func(config *iotv1alpha1.IoTConfig, service *corev1.PersistentVolumeClaim) error) error {

	pvc := corev1.PersistentVolumeClaim{
		ObjectMeta: v1.ObjectMeta{Namespace: config.Namespace, Name: name},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, &pvc, func(existing runtime.Object) error {
		existingPersistentVolumeClaim := existing.(*corev1.PersistentVolumeClaim)

		if err := controllerutil.SetControllerReference(config, existingPersistentVolumeClaim, r.scheme); err != nil {
			return err
		}

		return manipulator(config, existingPersistentVolumeClaim)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) processRoute(ctx context.Context, name string, config *iotv1alpha1.IoTConfig, manipulator func(config *iotv1alpha1.IoTConfig, service *routev1.Route) error) error {

	route := routev1.Route{
		ObjectMeta: v1.ObjectMeta{Namespace: config.Namespace, Name: name},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, &route, func(existing runtime.Object) error {
		existingRoute := existing.(*routev1.Route)

		if err := controllerutil.SetControllerReference(config, existingRoute, r.scheme); err != nil {
			return err
		}

		return manipulator(config, existingRoute)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTConfig) processGeneratedCredentials(ctx context.Context, config *iotv1alpha1.IoTConfig) error {

	// generate auth service PSK

	if config.Status.AuthenticationServicePSK == nil {
		s, err := util.GeneratePassword(128)
		if err != nil {
			return err
		}
		config.Status.AuthenticationServicePSK = &s
	}

	// ensure we have all adapter status entries we want

	config.Status.Adapters = ensureAdapterStatus(config.Status.Adapters, "mqtt", "http")

	// generate adapter users

	if err := ensureAdapterAuthCredentials(config.Status.Adapters); err != nil {
		return err
	}

	// return

	return nil
}
