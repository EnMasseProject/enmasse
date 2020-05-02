/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingaddress

import (
	"context"
	"fmt"
	"reflect"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/controller/messaginginfra"
	"github.com/enmasseproject/enmasse/pkg/state"
	"github.com/enmasseproject/enmasse/pkg/util"
	utilerrors "github.com/enmasseproject/enmasse/pkg/util/errors"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"

	// logr "github.com/go-logr/logr"
	corev1 "k8s.io/api/core/v1"

	k8errors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime"

	"k8s.io/client-go/tools/record"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_messagingaddress")
var _ reconcile.Reconciler = &ReconcileMessagingAddress{}

type ReconcileMessagingAddress struct {
	client        client.Client
	reader        client.Reader
	recorder      record.EventRecorder
	scheme        *runtime.Scheme
	clientManager state.ClientManager
	namespace     string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

/**
 * TODO - Add support for topic addresses
 * TODO - Add support for scheduling based on broker load
 * TODO - Add support for subscription addresses
 * TODO - Add support for deadLetter addresses
 * TODO - Add support for per-address limits based on MessagingAddressPlan
 * TODO - Add support for migrating queues to different brokers based on scheduling decision
 */

const (
	FINALIZER_NAME = "enmasse.io/operator"
)

func newReconciler(mgr manager.Manager) *ReconcileMessagingAddress {
	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	clientManager := state.GetClientManager()
	return &ReconcileMessagingAddress{
		client:        mgr.GetClient(),
		clientManager: clientManager,
		reader:        mgr.GetAPIReader(),
		recorder:      mgr.GetEventRecorderFor("messagingaddress"),
		scheme:        mgr.GetScheme(),
		namespace:     namespace,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingAddress) error {

	c, err := controller.New("messagingaddress-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1beta2.MessagingAddress{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	return err
}

/*
 * Very dumb scheduler that doesn't look at broker capacity.
 */
type DummyScheduler struct {
}

var _ state.Scheduler = &DummyScheduler{}

func (s *DummyScheduler) ScheduleAddress(address *v1beta2.MessagingAddress, brokers []*state.BrokerState) error {
	if len(brokers) > 0 {
		broker := brokers[0]
		address.Status.Brokers = append(address.Status.Brokers, v1beta2.MessagingAddressBroker{
			State: v1beta2.MessagingAddressBrokerScheduled,
			Host:  broker.Host,
		})
	} else {
		return fmt.Errorf("no available broker")
	}
	return nil
}

func (r *ReconcileMessagingAddress) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	logger.Info("Reconciling MessagingAddress")

	found := &v1beta2.MessagingAddress{}
	err := r.reader.Get(ctx, request.NamespacedName, found)
	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingAddress resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingAddress")
		return reconcile.Result{}, err
	}

	rc := resourceContext{
		address: found,
		status:  found.Status.DeepCopy(),
		ctx:     ctx,
		client:  r.client,
	}

	// Initialize phase and conditions and type
	var foundTenant *v1beta2.MessagingAddressCondition
	var scheduled *v1beta2.MessagingAddressCondition
	var created *v1beta2.MessagingAddressCondition
	var ready *v1beta2.MessagingAddressCondition
	rc.Process(func(address *v1beta2.MessagingAddress) (processorResult, error) {
		if address.Status.Phase == "" {
			address.Status.Phase = v1beta2.MessagingAddressConfiguring
		}
		if address.Spec.Queue != nil {
			address.Status.Type = v1beta2.MessagingAddressTypeQueue
		} else if address.Spec.Anycast != nil {
			address.Status.Type = v1beta2.MessagingAddressTypeAnycast
		} else if address.Spec.Multicast != nil {
			address.Status.Type = v1beta2.MessagingAddressTypeMulticast
		} else if address.Spec.Topic != nil {
			address.Status.Type = v1beta2.MessagingAddressTypeTopic
		} else if address.Spec.Subscription != nil {
			address.Status.Type = v1beta2.MessagingAddressTypeSubscription
		} else if address.Spec.DeadLetter != nil {
			address.Status.Type = v1beta2.MessagingAddressTypeDeadLetter
		}
		foundTenant = address.Status.GetMessagingAddressCondition(v1beta2.MessagingAddressFoundTenant)
		scheduled = address.Status.GetMessagingAddressCondition(v1beta2.MessagingAddressScheduled)
		created = address.Status.GetMessagingAddressCondition(v1beta2.MessagingAddressCreated)
		ready = address.Status.GetMessagingAddressCondition(v1beta2.MessagingAddressReady)
		return processorResult{}, nil
	})

	// Initialize and process finalizer
	result, err := rc.Process(func(address *v1beta2.MessagingAddress) (processorResult, error) {

		// Handle finalizing an deletion state first
		if address.DeletionTimestamp != nil && address.Status.Phase != v1beta2.MessagingAddressTerminating {
			address.Status.Phase = v1beta2.MessagingAddressTerminating
			err := r.client.Status().Update(ctx, address)
			return processorResult{Requeue: true}, err
		}

		original := address.DeepCopy()
		result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, address, []finalizer.Finalizer{
			finalizer.Finalizer{
				Name: FINALIZER_NAME,
				Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
					_, ok := c.Object.(*v1beta2.MessagingAddress)
					if !ok {
						return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingAddress")
					}

					infra, err := messaginginfra.LookupInfra(ctx, r.client, address.Namespace)
					if err != nil {
						// Not bound - allow dropping finalizer
						if utilerrors.IsNotBound(err) || utilerrors.IsNotFound(err) {
							logger.Info("[Finalizer] Messaging tenant not found or bound, ignoring!")
							return reconcile.Result{}, nil
						}
						logger.Info("[Finalizer] Error looking up infra")
						return reconcile.Result{}, err
					}
					client := r.clientManager.GetClient(infra)
					err = client.DeleteAddress(address)
					logger.Info("[Finalizer] Deleted address", "err", err)
					return reconcile.Result{}, err
				},
			},
		})
		if err != nil {
			return processorResult{}, err
		}

		if result.Requeue {
			// Update and requeue if changed
			if !reflect.DeepEqual(original, address) {
				err := r.client.Update(ctx, address)
				return processorResult{Return: true}, err
			}
		}
		return processorResult{Requeue: result.Requeue}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	var infra *v1beta2.MessagingInfra
	// Retrieve the MessagingInfra for this MessagingAddress
	result, err = rc.Process(func(address *v1beta2.MessagingAddress) (processorResult, error) {
		i, err := messaginginfra.LookupInfra(ctx, r.client, found.Namespace)
		if err != nil && (k8errors.IsNotFound(err) || utilerrors.IsNotBound(err)) {
			foundTenant.SetStatus(corev1.ConditionFalse, "", err.Error())
			address.Status.Message = err.Error()
			return processorResult{RequeueAfter: 10 * time.Second}, nil
		}
		foundTenant.SetStatus(corev1.ConditionTrue, "", "")
		infra = i
		return processorResult{}, err

	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Schedule address. Scheduling and creation of addresses are separated and each step is persisted. This is to avoid
	// the case where a scheduled address is forgotten if the operator crashes. Once persisted, the operator will
	// be able to reconcile the broker state as specified in the address status.
	result, err = rc.Process(func(address *v1beta2.MessagingAddress) (processorResult, error) {
		// TODO: Handle changes to partitions etc.
		if len(address.Status.Brokers) > 0 {
			// We're already scheduled so don't change
			return processorResult{}, nil
		}

		// These addresses don't require scheduling
		if address.Spec.Anycast != nil || address.Spec.Multicast != nil {
			return processorResult{}, nil
		}

		// TODO: Make configurable and a better scheduler
		scheduler := &DummyScheduler{}

		client := r.clientManager.GetClient(infra)
		err := client.ScheduleAddress(address, scheduler)
		if err != nil {
			scheduled.SetStatus(corev1.ConditionFalse, "", err.Error())
			address.Status.Message = err.Error()
			if _, ok := err.(*state.NotInitializedError); ok {
				return processorResult{RequeueAfter: 10 * time.Second}, nil
			}
			return processorResult{}, err
		}
		scheduled.SetStatus(corev1.ConditionTrue, "", "")

		// Signal requeue so that status gets persisted
		return processorResult{Requeue: true}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	client := r.clientManager.GetClient(infra)
	// Ensure address exists for all known endpoints. We know where the address should exist now, so just ensure it is done.
	result, err = rc.Process(func(address *v1beta2.MessagingAddress) (processorResult, error) {
		err := client.SyncAddress(address)
		if err != nil {
			created.SetStatus(corev1.ConditionFalse, "", err.Error())
			address.Status.Message = err.Error()
			if _, ok := err.(*state.NotInitializedError); ok {
				return processorResult{RequeueAfter: 10 * time.Second}, nil
			}
		} else {
			for i, _ := range address.Status.Brokers {
				address.Status.Brokers[i].State = v1beta2.MessagingAddressBrokerActive
			}
			created.SetStatus(corev1.ConditionTrue, "", "")
		}
		return processorResult{}, err
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Update address status
	result, err = rc.Process(func(address *v1beta2.MessagingAddress) (processorResult, error) {
		originalStatus := address.Status.DeepCopy()
		address.Status.Phase = v1beta2.MessagingAddressActive
		address.Status.Message = ""
		ready.SetStatus(corev1.ConditionTrue, "", "")
		if !reflect.DeepEqual(originalStatus, address.Status) {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			err := r.client.Status().Update(ctx, address)
			return processorResult{}, err
		} else {
			return processorResult{}, nil
		}
	})
	return result.Result(), err
}

/*
 * Automatically handle status update of the resource after running some reconcile logic.
 */
type resourceContext struct {
	ctx     context.Context
	client  client.Client
	status  *v1beta2.MessagingAddressStatus
	address *v1beta2.MessagingAddress
}

type processorResult struct {
	Requeue      bool
	RequeueAfter time.Duration
	Return       bool
}

func (r *resourceContext) Process(processor func(address *v1beta2.MessagingAddress) (processorResult, error)) (processorResult, error) {
	result, err := processor(r.address)
	if !reflect.DeepEqual(r.status, r.address.Status) {
		if err != nil || result.Requeue || result.RequeueAfter > 0 {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			statuserr := r.client.Status().Update(r.ctx, r.address)
			if statuserr != nil {
				// If this fails, report the status error if everything else whent ok, otherwise report the original error
				log.Error(statuserr, "Status update failed", "address", r.address.Name)
				if err == nil {
					err = statuserr
				}
			} else {
				r.status = r.address.Status.DeepCopy()
			}
			return result, err
		}
	}
	return result, err
}

func (r *processorResult) ShouldReturn(err error) bool {
	return err != nil || r.Requeue || r.RequeueAfter > 0 || r.Return
}

func (r *processorResult) Result() reconcile.Result {
	return reconcile.Result{Requeue: r.Requeue, RequeueAfter: r.RequeueAfter}
}
