/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messaginginfra

import (
	"context"
	"fmt"
	"reflect"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/broker"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/cert"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra/router"
	"github.com/enmasseproject/enmasse/pkg/state"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"

	logr "github.com/go-logr/logr"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"

	k8errors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"

	"k8s.io/client-go/tools/record"

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

const (
	FINALIZER_NAME = "enmasse.io/messaging-infra"
)

type ReconcileMessagingInfra struct {
	client           client.Client
	reader           client.Reader
	recorder         record.EventRecorder
	scheme           *runtime.Scheme
	certController   *cert.CertController
	routerController *router.RouterController
	brokerController *broker.BrokerController
	stateManager     state.StateManager
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
	stateManager := state.GetStateManager()
	certController := cert.NewCertController(mgr.GetClient(), mgr.GetScheme(), 24*30*time.Hour, 24*time.Hour)
	brokerController := broker.NewBrokerController(mgr.GetClient(), mgr.GetScheme(), certController, stateManager)
	routerController := router.NewRouterController(mgr.GetClient(), mgr.GetScheme(), certController, stateManager)
	return &ReconcileMessagingInfra{
		client:           mgr.GetClient(),
		certController:   certController,
		routerController: routerController,
		brokerController: brokerController,
		stateManager:     stateManager,
		reader:           mgr.GetAPIReader(),
		recorder:         mgr.GetEventRecorderFor("messaginginfra"),
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

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	logger.Info("Reconciling MessagingInfra")

	found := &v1beta2.MessagingInfra{}
	err := r.reader.Get(ctx, request.NamespacedName, found)
	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingInfra resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingInfra")
		return reconcile.Result{}, err
	}

	fresult, err := r.reconcileFinalizers(ctx, logger, found)
	if err != nil || fresult.Requeue || fresult.Return {
		return reconcile.Result{Requeue: fresult.Requeue}, err
	}

	// Start regular processing loop
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
		return r.certController.ReconcileCa(ctx, logger, infra)
	})
	if err != nil {
		return reconcile.Result{}, err
	}

	// Reconcile Routers
	err = ic.ProcessSimple(func(infra *v1beta2.MessagingInfra) error {
		return r.routerController.ReconcileRouters(ctx, logger, infra)
	})
	if err != nil {
		return reconcile.Result{}, err
	}

	// Reconcile Brokers
	err = ic.ProcessSimple(func(infra *v1beta2.MessagingInfra) error {
		return r.brokerController.ReconcileBrokers(ctx, logger, infra)
	})
	if err != nil {
		return reconcile.Result{}, err
	}

	// Sync state
	result, err := ic.Process(func(infra *v1beta2.MessagingInfra) (reconcile.Result, error) {
		err := r.stateManager.GetOrCreateInfra(infra).Sync()
		// Treat as transient error
		if err, ok := err.(*state.NotConnectedError); ok {
			logger.Info("Error syncing connectors", "error", err)
			return reconcile.Result{RequeueAfter: 10 * time.Second}, nil
		}
		return reconcile.Result{}, err
	})
	if err != nil || result.Requeue || result.RequeueAfter > 0 {
		return result, err
	}

	// Check status
	err = ic.ProcessSimple(func(infra *v1beta2.MessagingInfra) error {
		infraStatus, err := r.stateManager.GetOrCreateInfra(infra).GetStatus()
		if err != nil {
			return err
		}

		brokersConnected := infra.Status.GetMessagingInfraCondition(v1beta2.MessagingInfraBrokersConnected)
		for _, connectorStatus := range infraStatus.Connectors {
			if !connectorStatus.Connected {
				brokersConnected.SetStatus(corev1.ConditionFalse, "", fmt.Sprintf("Connection between %s and %s not ready: %s", connectorStatus.Router, connectorStatus.Broker, connectorStatus.Message))
				return nil
			}
		}
		brokersConnected.SetStatus(corev1.ConditionTrue, "", "")
		return nil
	})

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

	return reconcile.Result{RequeueAfter: 30 * time.Second}, err
}

type finalizerResult struct {
	Requeue bool
	Return  bool
}

func (r *ReconcileMessagingInfra) reconcileFinalizers(ctx context.Context, logger logr.Logger, infra *v1beta2.MessagingInfra) (finalizerResult, error) {
	// Handle finalizing an deletion state first
	if infra.DeletionTimestamp != nil && infra.Status.Phase != v1beta2.MessagingInfraTerminating {
		infra.Status.Phase = v1beta2.MessagingInfraTerminating
		err := r.client.Status().Update(ctx, infra)
		return finalizerResult{Requeue: true}, err
	}

	original := infra.DeepCopy()
	result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, infra, []finalizer.Finalizer{
		finalizer.Finalizer{
			Name: FINALIZER_NAME,
			Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
				infra, ok := c.Object.(*v1beta2.MessagingInfra)
				if !ok {
					return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingInfra")
				}

				err := r.stateManager.DeleteInfra(infra)
				return reconcile.Result{}, err
			},
		},
	})
	if err != nil {
		return finalizerResult{}, err
	}

	if result.Requeue {
		// Update and requeue if changed
		if !reflect.DeepEqual(original, infra) {
			err := r.client.Update(ctx, infra)
			return finalizerResult{Return: true}, err
		}
	}
	return finalizerResult{Requeue: result.Requeue}, nil
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
