/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingtenant

import (
	"context"
	"fmt"
	"reflect"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"

	corev1 "k8s.io/api/core/v1"

	k8errors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/types"

	"k8s.io/client-go/tools/record"

	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	"sigs.k8s.io/controller-runtime/pkg/source"
)

var log = logf.Log.WithName("controller_messagingtenant")
var _ reconcile.Reconciler = &ReconcileMessagingTenant{}

type ReconcileMessagingTenant struct {
	client    client.Client
	reader    client.Reader
	recorder  record.EventRecorder
	namespace string
}

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

/**
 * TODO - Referencing a MessagingPlan and applying router vhost configuration with settings from plan.
 * TODO - Referencing a AccessControlService and apply router configuration with settings.
 */

const (
	FINALIZER_NAME       = "enmasse.io/operator"
	TENANT_RESOURCE_NAME = "default"
)

func newReconciler(mgr manager.Manager) *ReconcileMessagingTenant {
	namespace := util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")

	return &ReconcileMessagingTenant{
		client:    mgr.GetClient(),
		reader:    mgr.GetAPIReader(),
		recorder:  mgr.GetEventRecorderFor("messagingtenant"),
		namespace: namespace,
	}
}

func add(mgr manager.Manager, r *ReconcileMessagingTenant) error {

	c, err := controller.New("messagingtenant-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &v1beta2.MessagingTenant{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	return err
}

func (r *ReconcileMessagingTenant) Reconcile(request reconcile.Request) (reconcile.Result, error) {

	logger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)

	ctx := context.Background()

	if request.Name != TENANT_RESOURCE_NAME {
		logger.Info("Unsupported resource name")
		return reconcile.Result{}, nil
	}

	logger.Info("Reconciling MessagingTenant")

	found := &v1beta2.MessagingTenant{}
	err := r.reader.Get(ctx, request.NamespacedName, found)
	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingTenant resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingTenant")
		return reconcile.Result{}, err
	}

	rc := resourceContext{
		tenant: found,
		status: found.Status.DeepCopy(),
		ctx:    ctx,
		client: r.client,
	}

	// Initialize phase and conditions
	var bound *v1beta2.MessagingTenantCondition
	var ready *v1beta2.MessagingTenantCondition
	rc.Process(func(tenant *v1beta2.MessagingTenant) (processorResult, error) {
		if tenant.Status.Phase == "" {
			tenant.Status.Phase = v1beta2.MessagingTenantConfiguring
		}
		bound = tenant.Status.GetMessagingTenantCondition(v1beta2.MessagingTenantBound)
		ready = tenant.Status.GetMessagingTenantCondition(v1beta2.MessagingTenantReady)
		return processorResult{}, nil
	})

	// Initialize and process finalizer
	result, err := rc.Process(func(tenant *v1beta2.MessagingTenant) (processorResult, error) {

		// Handle finalizing an deletion state first
		if tenant.DeletionTimestamp != nil && tenant.Status.Phase != v1beta2.MessagingTenantTerminating {
			tenant.Status.Phase = v1beta2.MessagingTenantTerminating
			err := r.client.Status().Update(ctx, tenant)
			return processorResult{Requeue: true}, err
		}

		original := tenant.DeepCopy()
		result, err := finalizer.ProcessFinalizers(ctx, r.client, r.reader, r.recorder, tenant, []finalizer.Finalizer{
			finalizer.Finalizer{
				Name: FINALIZER_NAME,
				Deconstruct: func(c finalizer.DeconstructorContext) (reconcile.Result, error) {
					_, ok := c.Object.(*v1beta2.MessagingTenant)
					if !ok {
						return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports MessagingTenant")
					}

					endpoints := &v1beta2.MessagingEndpointList{}
					err := r.client.List(ctx, endpoints, client.InNamespace(tenant.Namespace))
					if err != nil {
						return reconcile.Result{}, err
					}

					addresses := &v1beta2.MessagingAddressList{}
					err = r.client.List(ctx, addresses, client.InNamespace(tenant.Namespace))
					if err != nil {
						return reconcile.Result{}, err
					}

					if len(addresses.Items) > 0 || len(endpoints.Items) > 0 {
						return reconcile.Result{}, fmt.Errorf("unable to delete MessagingTenant: waiting for %d addresses and %d endpoints to be deleted", len(addresses.Items), len(endpoints.Items))
					}

					return reconcile.Result{}, nil
				},
			},
		})
		if err != nil {
			return processorResult{}, err
		}

		if result.Requeue {
			// Update and requeue if changed
			if !reflect.DeepEqual(original, tenant) {
				err := r.client.Update(ctx, tenant)
				return processorResult{Return: true}, err
			}
		}
		return processorResult{Requeue: result.Requeue}, nil
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Lookup messaging infra
	infra := &v1beta2.MessagingInfra{}
	result, err = rc.Process(func(tenant *v1beta2.MessagingTenant) (processorResult, error) {
		if tenant.Status.MessagingInfraRef == nil {
			// Find a suiting MessagingInfra to bind to
			infras := &v1beta2.MessagingInfraList{}
			err := r.client.List(ctx, infras)
			if err != nil {
				logger.Info("Error listing infras")
				return processorResult{}, err
			}
			logger.Info("Infras", "infras", infras)
			infra = findBestMatch(tenant, infras.Items)
			return processorResult{}, nil
		} else {
			err := r.client.Get(ctx, types.NamespacedName{Name: tenant.Status.MessagingInfraRef.Name, Namespace: tenant.Status.MessagingInfraRef.Namespace}, infra)
			if err != nil {
				if k8errors.IsNotFound(err) {
					msg := fmt.Sprintf("Infrastructure %s/%s not found!", tenant.Status.MessagingInfraRef.Namespace, tenant.Status.MessagingInfraRef.Name)
					tenant.Status.Message = msg
					bound.SetStatus(corev1.ConditionFalse, "", msg)
					return processorResult{RequeueAfter: 10 * time.Second}, nil
				} else {
					logger.Info("Error reconciling", err)
				}
			}
			return processorResult{}, err
		}
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Update infra reference
	result, err = rc.Process(func(tenant *v1beta2.MessagingTenant) (processorResult, error) {
		if infra != nil {
			bound.SetStatus(corev1.ConditionTrue, "", "")
			tenant.Status.MessagingInfraRef = &v1beta2.MessagingInfraReference{
				Name:      infra.Name,
				Namespace: infra.Namespace,
			}
			return processorResult{}, nil
		} else {
			msg := "Not yet bound to any infrastructure"
			bound.SetStatus(corev1.ConditionFalse, "", msg)
			tenant.Status.Message = msg
			return processorResult{RequeueAfter: 10 * time.Second}, err
		}
	})
	if result.ShouldReturn(err) {
		return result.Result(), err
	}

	// Update tenant status
	result, err = rc.Process(func(tenant *v1beta2.MessagingTenant) (processorResult, error) {
		originalStatus := tenant.Status.DeepCopy()
		tenant.Status.Phase = v1beta2.MessagingTenantActive
		tenant.Status.Message = ""
		ready.SetStatus(corev1.ConditionTrue, "", "")
		if !reflect.DeepEqual(originalStatus, tenant.Status) {
			logger.Info("Tenant has changed")
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			err := r.client.Status().Update(ctx, tenant)
			return processorResult{}, err
		}
		return processorResult{}, nil
	})
	return result.Result(), err
}

func findBestMatch(tenant *v1beta2.MessagingTenant, infras []v1beta2.MessagingInfra) *v1beta2.MessagingInfra {
	var bestMatch *v1beta2.MessagingInfra
	var bestMatchSelector *v1beta2.Selector
	for _, infra := range infras {
		selector := infra.Spec.Selector
		// If there is a global one without a selector, use it
		if selector == nil && bestMatch == nil {
			bestMatch = &infra
		} else if selector != nil {
			// If selector is applicable to this tenant
			matched := false
			for _, ns := range selector.Namespaces {
				if ns == tenant.Namespace {
					matched = true
					break
				}
			}

			// Check if this selector is better than the previous (aka. previous was either not set or global)
			if matched && bestMatchSelector == nil {
				bestMatch = &infra
				bestMatchSelector = selector
			}
			// TODO: Support more advanced selection mechanism based on namespace labels
		}
	}
	return bestMatch
}

/*
 * Automatically handle status update of the resource after running some reconcile logic.
 */
type resourceContext struct {
	ctx    context.Context
	client client.Client
	status *v1beta2.MessagingTenantStatus
	tenant *v1beta2.MessagingTenant
}

type processorResult struct {
	Requeue      bool
	RequeueAfter time.Duration
	Return       bool
}

func (r *resourceContext) Process(processor func(tenant *v1beta2.MessagingTenant) (processorResult, error)) (processorResult, error) {
	result, err := processor(r.tenant)
	if !reflect.DeepEqual(r.status, r.tenant.Status) {
		if err != nil || result.Requeue || result.RequeueAfter > 0 {
			// If there was an error and the status has changed, perform an update so that
			// errors are visible to the user.
			statuserr := r.client.Status().Update(r.ctx, r.tenant)
			if statuserr != nil {
				// If this fails, report the status error if everything else whent ok, otherwise report the original error
				log.Error(statuserr, "Status update failed", "tenant", r.tenant.Name)
				if err == nil {
					err = statuserr
				}
			} else {
				r.status = r.tenant.Status.DeepCopy()
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
