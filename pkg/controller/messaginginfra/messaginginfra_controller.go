/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messaginginfra

import (
	"context"
	"reflect"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/broker"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/router"
	"github.com/enmasseproject/enmasse/pkg/util"

	//logr "github.com/go-logr/logr"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	//metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	k8errors "k8s.io/apimachinery/pkg/api/errors"
	//resource "k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/runtime"
	//intstr "k8s.io/apimachinery/pkg/util/intstr"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	//"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_messaginginfra")
var _ reconcile.Reconciler = &ReconcileMessagingInfra{}

type ReconcileMessagingInfra struct {
	client           client.Client
	reader           client.Reader
	scheme           *runtime.Scheme
	certController   *cert.CertController
	routerController *router.RouterController
	brokerController *broker.BrokerController
	namespace        string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

/**
 * TODO MessagingInfra
 *
 * - Make certificate expiry configurable
 * - Add podTemplate configuration to CRD in .spec.router.podTemplate and .spec.broker.podTemplate and apply
 * - Add static router configuration options in .spec.router: thread pool, min available, max unavailable, link capacity, idle timeout
 * - Add static broker configuration options in .spec.broker: minAvailable, maxUnavailable, storage capacity, java options, address full policy, global max size, storage class name, resize persistent volume claim, threadpool config, global address policies
 */

func newReconciler(mgr manager.Manager) *ReconcileMessagingInfra {
	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	// TODO: Make expiry configurable
	certController := cert.NewCertController(mgr.GetClient(), mgr.GetScheme(), 24*30*time.Hour, 24*time.Hour)
	brokerController := broker.NewBrokerController(mgr.GetClient(), mgr.GetScheme(), certController)
	routerController := router.NewRouterController(mgr.GetClient(), mgr.GetScheme(), certController)
	return &ReconcileMessagingInfra{
		client:           mgr.GetClient(),
		certController:   certController,
		routerController: routerController,
		brokerController: brokerController,
		reader:           mgr.GetAPIReader(),
		scheme:           mgr.GetScheme(),
		namespace:        namespace,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingInfra) error {

	c, err := controller.New("messaginginfra-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1beta2.MessagingInfra{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &appsv1.StatefulSet{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &v1beta2.MessagingInfra{},
	})
	return err
}

func (r *ReconcileMessagingInfra) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling MessagingInfra")

	ctx := context.TODO()

	found := &v1beta2.MessagingInfra{}
	err := r.client.Get(ctx, request.NamespacedName, found)
	if err != nil {
		if k8errors.IsNotFound(err) {
			reqLogger.Info("MessagingInfra resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		reqLogger.Error(err, "Failed to get MessagingInfra")
		return reconcile.Result{}, err
	}

	ic := &infraContext{
		ctx:    ctx,
		client: r.client,
		infra:  found,
		status: found.Status.DeepCopy(),
	}

	// Initialize status
	err = ic.ProcessSimple(func(infra *v1beta2.MessagingInfra) error {
		if infra.Status.Phase == "" || infra.Status.Phase == v1beta2.MessagingInfraPending {
			infra.Status.Phase = v1beta2.MessagingInfraConfiguring
		}
		return nil
	})
	if err != nil {
		return reconcile.Result{}, err
	}

	// Reconcile CA
	err = ic.ProcessSimple(func(infra *v1beta2.MessagingInfra) error {
		return r.certController.ReconcileCa(ctx, reqLogger, infra)
	})
	if err != nil {
		return reconcile.Result{}, err
	}

	// Reconcile Routers
	err = ic.ProcessSimple(func(infra *v1beta2.MessagingInfra) error {
		return r.routerController.ReconcileRouters(ctx, reqLogger, infra)
	})
	if err != nil {
		return reconcile.Result{}, err
	}

	// Reconcile Brokers
	err = ic.ProcessSimple(func(infra *v1beta2.MessagingInfra) error {
		return r.brokerController.ReconcileBrokers(ctx, reqLogger, infra)
	})
	if err != nil {
		return reconcile.Result{}, err
	}

	// Update main condition
	err = ic.ProcessSimple(func(infra *v1beta2.MessagingInfra) error {
		routersCreated := infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraRoutersCreated)
		brokersCreated := infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraBrokersCreated)
		caCreated := infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraCaCreated)

		// TODO: Update ready based on actual router and broker state
		ready := infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraReady)
		if routersCreated.Status == corev1.ConditionTrue &&
			brokersCreated.Status == corev1.ConditionTrue &&
			caCreated.Status == corev1.ConditionTrue {

			ready.SetStatus(corev1.ConditionTrue, "", "")
			infra.Status.Phase = v1beta2.MessagingInfraActive

		} else {
			ready.SetStatus(corev1.ConditionFalse, "", "Infrastructure is not created")
		}
		return nil
	})
	if err != nil {
		return reconcile.Result{}, err
	}

	// Update status (if it has changed)
	err = ic.UpdateStatus()

	return reconcile.Result{}, err
}

/*
 * Automatically handle status update of the infra after running processor.
 */
type infraContext struct {
	ctx    context.Context
	client client.Client
	infra  *v1beta2.MessagingInfra
	status *v1beta2.MessagingInfraStatus
}

func (i *infraContext) Process(processor func(infra *v1beta2.MessagingInfra) (reconcile.Result, error)) (reconcile.Result, error) {
	result, err := processor(i.infra)
	if !reflect.DeepEqual(i.status, i.infra.Status) {
		// If there was an error and the status has changed, perform an update so that
		// errors are visible to the user.
		if err != nil {
			_ = i.UpdateStatus()
			return result, err
		}
	}
	return result, err
}

func (i *infraContext) UpdateStatus() error {
	if !reflect.DeepEqual(i.status, i.infra.Status) {
		err := i.client.Status().Update(i.ctx, i.infra)
		if err != nil {
			log.Error(err, "Status update failed", "infra", i.infra.Name)
			// If this fails, at least report the original error in the log
			return err
		}
		i.status = i.infra.Status.DeepCopy()
	}
	return nil
}

func (i *infraContext) ProcessSimple(processor func(infra *v1beta2.MessagingInfra) error) error {
	_, err := i.Process(func(infra *v1beta2.MessagingInfra) (reconcile.Result, error) {
		err := processor(infra)
		return reconcile.Result{}, err
	})
	return err
}
