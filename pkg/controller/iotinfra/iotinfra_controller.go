/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	"github.com/go-logr/logr"
	"time"

	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/util/iot"
	"github.com/enmasseproject/enmasse/pkg/util/loghandler"
	"github.com/pkg/errors"

	promv1 "github.com/coreos/prometheus-operator/pkg/apis/monitoring/v1"

	"reflect"

	"k8s.io/client-go/tools/record"

	"github.com/enmasseproject/enmasse/pkg/util/cchange"

	"github.com/enmasseproject/enmasse/pkg/util/install"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	"github.com/enmasseproject/enmasse/pkg/util"

	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"

	routev1 "github.com/openshift/api/route/v1"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

const ControllerName = "iotinfra-controller"

const iotPrefix = "iot.enmasse.io"
const iotServiceCaConfigMapName = "iot-service-ca"

const DeviceConnectionTypeAnnotation = iotPrefix + "/deviceConnection.type"

const EventReasonInfrastructureTermination = "InfrastructureTermination"

const RegistryTypeAnnotation = iotPrefix + "/registry.type"

var log = logf.Log.WithName("controller_iotinfra")

var finalizers []finalizer.Finalizer

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	name, err := iot.GetIoTInfrastructureName()
	if err != nil {
		return errors.Wrap(err, "Failed to get IoTInfrastructure name")
	}

	namespace, err := util.GetInfrastructureNamespace()
	if err != nil {
		return errors.Wrap(err, "Infrastructure namespace not set")
	}

	return add(mgr, newReconciler(
		mgr,
		namespace,
		name,
	))
}

func newReconciler(mgr manager.Manager, infraNamespace string, infrastructureName string) *ReconcileIoTInfrastructure {

	certController := cert.NewCertController(
		mgr.GetClient(),
		mgr.GetScheme(),
		24*30*time.Hour, /* CA expiry, which don't use */
		24*time.Hour,    /* client cert expiry */ // FIXME: make configurable
	)

	return &ReconcileIoTInfrastructure{
		client:             mgr.GetClient(),
		reader:             mgr.GetAPIReader(),
		scheme:             mgr.GetScheme(),
		namespace:          infraNamespace,
		infrastructureName: infrastructureName,
		recorder:           mgr.GetEventRecorderFor(ControllerName),
		certController:     certController,
	}
}

type watching struct {
	obj       runtime.Object
	openshift bool
}

func add(mgr manager.Manager, r *ReconcileIoTInfrastructure) error {

	// Create a new controller
	c, err := controller.New(ControllerName, mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource IoTInfrastructure
	err = c.Watch(&source.Kind{Type: &iotv1.IoTInfrastructure{}}, loghandler.New(&handler.EnqueueRequestForObject{}, log, "IoTInfrastructure"))
	if err != nil {
		return err
	}

	// watch owned objects
	ownerEventLog := log.V(2)

	for _, w := range []watching{
		{&appsv1.Deployment{}, false},
		{&appsv1.StatefulSet{}, false},
		{&corev1.Service{}, false},
		{&corev1.ConfigMap{}, false},
		{&corev1.Secret{}, false},
		{&corev1.PersistentVolumeClaim{}, false},
		{&enmassev1.MessagingInfrastructure{}, false},

		{&routev1.Route{}, true},
	} {

		if w.openshift && !util.IsOpenshift() {
			// requires openshift, but we don't have it
			continue
		}

		err = c.Watch(&source.Kind{Type: w.obj}, loghandler.New(&handler.EnqueueRequestForOwner{
			OwnerType:    &iotv1.IoTInfrastructure{},
			IsController: true,
		}, ownerEventLog, w.obj.GetObjectKind().GroupVersionKind().Kind))
		if err != nil {
			return err
		}

	}

	// watch secrets referenced by infrastructure

	s := NewSecretHandler(r.client, r.infrastructureName)
	if err := c.Watch(&source.Kind{Type: &corev1.Secret{}}, s); err != nil {
		return err
	}

	// done

	return nil
}

// ensure we are implementing the interface
var _ reconcile.Reconciler = &ReconcileIoTInfrastructure{}

type ReconcileIoTInfrastructure struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client   client.Client
	reader   client.Reader
	scheme   *runtime.Scheme
	recorder record.EventRecorder

	// Manages certificates for messaging infrastructure.
	certController *cert.CertController

	// The name of the configuration we are watching
	// we are watching only one infra, in our own namespace
	infrastructureName string
	// The name of the infrastructure namespace
	namespace string
}

type configTrackerAdapter struct {
	username string
	password string

	recorder *cchange.ConfigChangeRecorder
}

type configTracker struct {
	qdrProxyConfigCtx *cchange.ConfigChangeRecorder
	authServicePskCtx *cchange.ConfigChangeRecorder
	adapters          map[string]*configTrackerAdapter
}

func (c *configTracker) RecordAdapterPassword(a adapter, username []byte, password []byte) {
	if c.adapters[a.Name] == nil {
		c.adapters[a.Name] = &configTrackerAdapter{
			recorder: cchange.NewRecorder(),
		}
	}

	ac := c.adapters[a.Name]

	// record for auth service

	ac.username = string(username)
	ac.password = string(password)

	// record for hash

	ac.recorder.AddBytes(username)
	ac.recorder.AddBytes(password)
}

func NewConfigTracker() *configTracker {
	return &configTracker{
		qdrProxyConfigCtx: cchange.NewRecorder(),
		authServicePskCtx: cchange.NewRecorder(),
		adapters:          make(map[string]*configTrackerAdapter),
	}
}

// Reconcile
//
// returning an error will get the request re-queued
func (r *ReconcileIoTInfrastructure) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling IoTInfrastructure")

	ctx := context.Background()

	// Get infra
	infra := &iotv1.IoTInfrastructure{}
	err := r.client.Get(ctx, request.NamespacedName, infra)

	if err != nil {

		if apierrors.IsNotFound(err) {

			reqLogger.Info("Infrastructure was not found")

			// Request object not found, could have been deleted after reconcile request.
			// Owned objects are automatically garbage collected. For additional cleanup logic use finalizers.
			// Return and don't requeue
			return reconcile.Result{}, nil
		}

		// Error reading the object - requeue the request.
		return reconcile.Result{}, err
	}

	// check of unique name
	// FIXME: drop requirement in a future version

	if infra.Name != r.infrastructureName {
		return r.failWrongInfrastructureName(ctx, infra)
	}

	rc := &recon.ReconcileContext{}

	// check for deconstruction

	if infra.DeletionTimestamp != nil && infra.Status.Phase != iotv1.InfrastructurePhaseTerminating {
		reqLogger.Info("Re-queue after setting phase to terminating")
		return r.markTerminating(ctx, infra)
	}
	rc.Process(func() (reconcile.Result, error) {
		return r.checkDeconstruct(ctx, reqLogger, infra)
	})
	if rc.NeedRequeue() || rc.Error() != nil {
		return rc.Result()
	}

	if infra.DeletionTimestamp != nil {
		// we are deleted and must stop here
		return reconcile.Result{}, nil
	}

	// we can start

	original := infra.DeepCopy()

	// get messaging infrastructure

	infraName := infra.Spec.MessagingInfrastructure
	if infraName == "" {
		infraName = infra.Name
	}
	messagingInfra := &enmassev1.MessagingInfrastructure{}
	if err := r.client.Get(ctx, client.ObjectKey{Namespace: infra.Namespace, Name: infraName}, messagingInfra); err != nil {
		if apierrors.IsNotFound(err) {
			// FIXME: properly handle status section
			// we cannot proceed without a messaging infrastructure
			rc.ProcessSimple(func() error {
				return util.NewConfigurationError("Unable to find MessagingInfrastructure named '%s' in namespace '%s'", infraName, infra.Namespace)
			})
			return r.updateFinalStatus(ctx, original, infra, rc)
		} else {
			// expect a recoverable error ... try again
			return reconcile.Result{}, err
		}
	}

	// prepare the adapter status section

	prepareAdapterStatus(infra)

	// pre-req reconcile

	configTracker := NewConfigTracker()

	rc.Process(func() (reconcile.Result, error) {
		return r.processQdrProxyConfig(ctx, infra, messagingInfra, configTracker.qdrProxyConfigCtx)
	})
	rc.ProcessSimple(func() error {
		return r.processAuthServicePskSecret(ctx, infra, configTracker.authServicePskCtx)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processAdapterPskCredentials(ctx, infra, configTracker)
	})
	rc.ProcessSimple(func() error {
		return r.processInterServiceCAConfigMap(ctx, infra)
	})
	rc.ProcessSimple(func() error {
		return r.processSharedInfraSecrets(ctx, infra, messagingInfra)
	})

	// start normal reconcile

	rc.Process(func() (reconcile.Result, error) {
		return r.processServiceMesh(ctx, infra)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processAuthService(ctx, infra, configTracker)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processTenantService(ctx, infra, configTracker.authServicePskCtx)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processDeviceConnection(ctx, infra, configTracker.authServicePskCtx)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processDeviceRegistry(ctx, infra, configTracker.authServicePskCtx)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processAmqpAdapter(ctx, infra, messagingInfra, configTracker.qdrProxyConfigCtx)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processHttpAdapter(ctx, infra, messagingInfra, configTracker.qdrProxyConfigCtx)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processMqttAdapter(ctx, infra, messagingInfra, configTracker.qdrProxyConfigCtx)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processSigfoxAdapter(ctx, infra, messagingInfra, configTracker.qdrProxyConfigCtx)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processLoraWanAdapter(ctx, infra, messagingInfra, configTracker.qdrProxyConfigCtx)
	})
	rc.Process(func() (reconcile.Result, error) {
		return r.processMonitoring(ctx, infra)
	})

	return r.updateFinalStatus(ctx, original, infra, rc)
}

func (r *ReconcileIoTInfrastructure) updateStatus(ctx context.Context, original *iotv1.IoTInfrastructure, infra *iotv1.IoTInfrastructure, err error) error {

	// update the status condition

	r.updateConditions(ctx, infra, err)

	// record state change in event log

	if infra.Status.Phase != original.Status.Phase {
		switch infra.Status.Phase {
		case iotv1.InfrastructurePhaseActive:
			// record event that the infrastructure was successfully activated
			r.recorder.Eventf(infra, corev1.EventTypeNormal, "Activated", "IoT infrastructure successfully activated")
		case iotv1.InfrastructurePhaseFailed:
			// record event that the infrastructure failed
			r.recorder.Eventf(infra, corev1.EventTypeWarning, "Failed", "IoT infrastructure failed")
		}
	}

	// update status

	if !reflect.DeepEqual(infra, original) {
		log.Info("IoTInfrastructure changed, updating status")
		return r.client.Status().Update(ctx, infra)
	}

	// no update needed

	return nil
}

func (r *ReconcileIoTInfrastructure) updateFinalStatus(ctx context.Context, original *iotv1.IoTInfrastructure, infra *iotv1.IoTInfrastructure, rc *recon.ReconcileContext) (reconcile.Result, error) {

	// do a status update

	err := r.updateStatus(ctx, original, infra, rc.Error())

	// check if error is non-recoverable

	if rc.Error() != nil && util.OnlyNonRecoverableErrors(rc.Error()) {
		r.recorder.Eventf(infra, corev1.EventTypeWarning, "NonRecoverableError", "Configuration failed: %v", rc.Error())
		log.Error(rc.Error(), "All non-recoverable errors. Stop trying to reconcile.")
		// strip away error, only return update error
		return rc.PlainResult(), err
	} else {
		// return result ... including status update error
		if err != nil {
			rc.AddResult(reconcile.Result{}, err)
		}
		return rc.Result()
	}
}

func (r *ReconcileIoTInfrastructure) failWrongInfrastructureName(ctx context.Context, Infra *iotv1.IoTInfrastructure) (reconcile.Result, error) {

	Infra.Status.Phase = iotv1.InfrastructurePhaseFailed
	Infra.Status.Message = "The name of the resource must be 'default'"

	Infra.Status.GetInfrastructureCondition(iotv1.InfrastructureConditionTypeReady).SetStatusError("WrongName", Infra.Status.Message)

	if err := r.client.Status().Update(ctx, Infra); err != nil {
		return reconcile.Result{}, err
	}

	return reconcile.Result{}, nil
}

func (r *ReconcileIoTInfrastructure) processDeployment(ctx context.Context, name string, infra *iotv1.IoTInfrastructure, delete bool, manipulator func(config *iotv1.IoTInfrastructure, deployment *appsv1.Deployment) error) error {

	deployment := &appsv1.Deployment{
		ObjectMeta: v1.ObjectMeta{Namespace: infra.Namespace, Name: name},
	}

	if delete {
		return install.DeleteIgnoreNotFound(ctx, r.client, deployment, client.PropagationPolicy(v1.DeletePropagationForeground))
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, deployment, func() error {
		if err := controllerutil.SetControllerReference(infra, deployment, r.scheme); err != nil {
			return err
		}

		return manipulator(infra, deployment)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) processStatefulSet(ctx context.Context, name string, infra *iotv1.IoTInfrastructure, delete bool, manipulator func(config *iotv1.IoTInfrastructure, deployment *appsv1.StatefulSet) error) error {

	statefulSet := &appsv1.StatefulSet{
		ObjectMeta: v1.ObjectMeta{Namespace: infra.Namespace, Name: name},
	}

	if delete {
		return install.DeleteIgnoreNotFound(ctx, r.client, statefulSet, client.PropagationPolicy(v1.DeletePropagationForeground))
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, statefulSet, func() error {
		if err := controllerutil.SetControllerReference(infra, statefulSet, r.scheme); err != nil {
			return err
		}

		return manipulator(infra, statefulSet)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) processService(ctx context.Context, name string, infra *iotv1.IoTInfrastructure, delete bool, manipulator func(config *iotv1.IoTInfrastructure, service *corev1.Service) error) error {

	service := &corev1.Service{
		ObjectMeta: v1.ObjectMeta{Namespace: infra.Namespace, Name: name},
	}

	if delete {
		return install.DeleteIgnoreNotFound(ctx, r.client, service, client.PropagationPolicy(v1.DeletePropagationForeground))
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, service, func() error {
		if err := controllerutil.SetControllerReference(infra, service, r.scheme); err != nil {
			return err
		}

		return manipulator(infra, service)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) processConfigMap(ctx context.Context, name string, infra *iotv1.IoTInfrastructure, delete bool, manipulator func(config *iotv1.IoTInfrastructure, configMap *corev1.ConfigMap) error) error {

	cm := &corev1.ConfigMap{
		ObjectMeta: v1.ObjectMeta{
			Namespace: infra.Namespace,
			Name:      name,
		},
	}

	if delete {
		return install.DeleteIgnoreNotFound(ctx, r.client, cm, client.PropagationPolicy(v1.DeletePropagationForeground))
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, cm, func() error {
		if err := controllerutil.SetControllerReference(infra, cm, r.scheme); err != nil {
			return err
		}

		return manipulator(infra, cm)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) processSecret(ctx context.Context, name string, infra *iotv1.IoTInfrastructure, delete bool, manipulator func(config *iotv1.IoTInfrastructure, secret *corev1.Secret) error) error {

	secret := &corev1.Secret{
		ObjectMeta: v1.ObjectMeta{
			Namespace: infra.Namespace,
			Name:      name,
		},
	}

	if delete {
		return install.DeleteIgnoreNotFound(ctx, r.client, secret, client.PropagationPolicy(v1.DeletePropagationForeground))
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, secret, func() error {
		if err := controllerutil.SetControllerReference(infra, secret, r.scheme); err != nil {
			return err
		}

		return manipulator(infra, secret)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) processPersistentVolumeClaim(ctx context.Context, name string, infra *iotv1.IoTInfrastructure, delete bool, manipulator func(config *iotv1.IoTInfrastructure, service *corev1.PersistentVolumeClaim) error) error {

	pvc := &corev1.PersistentVolumeClaim{
		ObjectMeta: v1.ObjectMeta{
			Namespace: infra.Namespace,
			Name:      name,
		},
	}

	if delete {
		return install.DeleteIgnoreNotFound(ctx, r.client, pvc, client.PropagationPolicy(v1.DeletePropagationForeground))
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, pvc, func() error {
		if err := controllerutil.SetControllerReference(infra, pvc, r.scheme); err != nil {
			return err
		}

		return manipulator(infra, pvc)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) processPrometheusRule(ctx context.Context, name string, infra *iotv1.IoTInfrastructure, delete bool, manipulator func(config *iotv1.IoTInfrastructure, rule *promv1.PrometheusRule) error) error {

	pr := &promv1.PrometheusRule{
		ObjectMeta: v1.ObjectMeta{
			Namespace: infra.Namespace,
			Name:      name,
		},
	}

	if delete {
		return install.DeleteIgnoreNotFound(ctx, r.client, pr, client.PropagationPolicy(v1.DeletePropagationForeground))
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, pr, func() error {
		if err := controllerutil.SetControllerReference(infra, pr, r.scheme); err != nil {
			return err
		}

		return manipulator(infra, pr)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) processRoute(ctx context.Context, name string, infra *iotv1.IoTInfrastructure, delete bool, endpointStatus *iotv1.EndpointStatus, manipulator func(config *iotv1.IoTInfrastructure, service *routev1.Route, endpointStatus *iotv1.EndpointStatus) error) error {

	route := &routev1.Route{
		ObjectMeta: v1.ObjectMeta{
			Namespace: infra.Namespace,
			Name:      name,
		},
	}

	if delete {
		endpointStatus.URI = ""
		return install.DeleteIgnoreNotFound(ctx, r.client, route, client.PropagationPolicy(v1.DeletePropagationForeground))
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, route, func() error {
		if err := controllerutil.SetControllerReference(infra, route, r.scheme); err != nil {
			return err
		}

		return manipulator(infra, route, endpointStatus)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return err
	}

	return nil
}

func (r *ReconcileIoTInfrastructure) checkDeconstruct(ctx context.Context, reqLogger logr.Logger, infra *iotv1.IoTInfrastructure) (reconcile.Result, error) {

	rc := recon.ReconcileContext{}
	original := infra.DeepCopy()

	// process finalizers

	rc.Process(func() (result reconcile.Result, e error) {
		return finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, infra, finalizers)
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
		if !reflect.DeepEqual(infra, original) {
			err := r.client.Update(ctx, infra)
			return reconcile.Result{}, err
		} else {
			return rc.Result()
		}
		// processing the finalizers required a persist step, and so we stop early
		// the call to Update will re-trigger us, and we don't need to set "requeue"
	}

	return rc.Result()

}

func (r *ReconcileIoTInfrastructure) markTerminating(ctx context.Context, infra *iotv1.IoTInfrastructure) (reconcile.Result, error) {
	// set phase and message

	infra.Status.Phase = iotv1.InfrastructurePhaseTerminating
	infra.Status.Message = "Infrastructure deleted"

	// set ready condition to false

	readyCondition := infra.Status.GetInfrastructureCondition(iotv1.InfrastructureConditionTypeReady)
	readyCondition.SetStatus(corev1.ConditionFalse, "Deconstructing", "Infrastructure is being deleted")

	// record event

	r.recorder.Event(infra, corev1.EventTypeNormal, EventReasonInfrastructureTermination, "Deconstructing IoT infrastructure")

	// store and re-queue

	return reconcile.Result{Requeue: true}, r.client.Status().Update(ctx, infra)
}
