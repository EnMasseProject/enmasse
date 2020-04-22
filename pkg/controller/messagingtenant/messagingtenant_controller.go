/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package messagingtenant

import (
	"context"
	"reflect"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/util"

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
 * TODO MessagingTenant
 *
 * - Infra selector
 * - Plan selector
 * - Auth selector
 */

const (
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

	tenant := &v1beta2.MessagingTenant{}
	err := r.reader.Get(ctx, request.NamespacedName, tenant)
	if err != nil {
		if k8errors.IsNotFound(err) {
			logger.Info("MessagingTenant resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		logger.Error(err, "Failed to get MessagingTenant")
		return reconcile.Result{}, err
	}

	originalStatus := tenant.Status.DeepCopy()
	var infra *v1beta2.MessagingInfra

	if tenant.Status.MessagingInfraRef == nil {
		// Find a suiting MessagingInfra to bind to
		infras := &v1beta2.MessagingInfraList{}
		err := r.client.List(ctx, infras)
		if err != nil {
			return reconcile.Result{}, err
		}
		infra = findBestMatch(tenant, infras.Items)
	} else {
		// Lookup infra in case it was deleted while we were reconciled
		err := r.client.Get(ctx, types.NamespacedName{Name: tenant.Status.MessagingInfraRef.Name, Namespace: tenant.Status.MessagingInfraRef.Namespace}, infra)
		if err != nil {
			if k8errors.IsNotFound(err) {
				infra = nil
			} else {
				return reconcile.Result{}, err
			}
		}
	}

	requeueAfter := 0 * time.Second
	ready := tenant.Status.GetMessagingTenantCondition(v1beta2.MessagingTenantReady)
	if infra != nil {
		ready.SetStatus(corev1.ConditionTrue, "", "")
		tenant.Status.Phase = v1beta2.MessagingTenantActive
		tenant.Status.MessagingInfraRef = &v1beta2.MessagingInfraReference{
			Name:      infra.Name,
			Namespace: infra.Namespace,
		}
	} else {
		ready.SetStatus(corev1.ConditionFalse, "", "Not yet bound to infrastructure")
		tenant.Status.Phase = v1beta2.MessagingTenantConfiguring
		requeueAfter = 10 * time.Second
	}

	if !reflect.DeepEqual(originalStatus, tenant.Status) {
		err := r.client.Status().Update(ctx, tenant)
		if err != nil {
			logger.Error(err, "Status update failed")
			return reconcile.Result{}, err
		}
	}

	return reconcile.Result{RequeueAfter: requeueAfter}, nil
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
