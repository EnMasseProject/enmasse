/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iottenant

import (
	"context"
	"fmt"
	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	corev1 "k8s.io/api/core/v1"
	"time"

	"github.com/enmasseproject/enmasse/pkg/util"

	batchv1 "k8s.io/api/batch/v1"
	"k8s.io/apimachinery/pkg/api/errors"

	"github.com/enmasseproject/enmasse/pkg/util/install"

	"sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util/finalizer"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

var finalizers = []finalizer.Finalizer{
	{
		Name:        "iot.enmasse.io/resources",
		Deconstruct: deconstructResources,
	},
	{
		Name:        "iot.enmasse.io/deviceRegistryCleanup",
		Deconstruct: cleanupDeviceRegistry,
	},
	{
		Name:        "iot.enmasse.io/trust-anchors",
		Deconstruct: cleanupTrustAnchors,
	},
}

// region Resources

func deconstructResources(ctx finalizer.DeconstructorContext) (reconcile.Result, error) {

	tenant, ok := ctx.Object.(*iotv1.IoTTenant)

	if !ok {
		return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports IoTTenant")
	}

	return cleanupManagedResources(ctx.Context, ctx.Client, tenant)
}

// delete all managed resources by the tenant
func cleanupManagedResources(ctx context.Context, c client.Client, tenant *iotv1.IoTTenant) (reconcile.Result, error) {

	rc := recon.ReconcileContext{}

	for _, a := range Addresses {
		rc.Process(func() (result reconcile.Result, e error) {

			addressName := util.AddressName(tenant, a)
			addressMetaName := util.EncodeAddressSpaceAsMetaName(addressName)

			return cleanupResource(ctx, c, tenant, client.ObjectKey{
				Namespace: tenant.Namespace,
				Name:      addressMetaName,
			}, &enmassev1.MessagingAddress{})

		})
	}

	return rc.Result()

}

func cleanupResource(ctx context.Context, c client.Client, tenant *iotv1.IoTTenant, key client.ObjectKey, obj runtime.Object) (reconcile.Result, error) {

	err := c.Get(ctx, key, obj)

	if err != nil {

		if errors.IsNotFound(err) {
			// if the object is not found, we got rid of it already
			return reconcile.Result{}, nil
		} else {
			return reconcile.Result{}, err
		}

	}

	deleted, err := install.IsDeleted(obj)
	if err != nil {
		// failed to check if the object was deleted: re-try
		return reconcile.Result{}, err
	}
	if deleted {
		// object was already deleted: we are done here
		return reconcile.Result{}, nil
	}

	result, err := install.RemoveAsOwner(tenant, obj, false)

	if err != nil {
		return reconcile.Result{}, err
	}

	switch result {
	case install.Found:
		// resource was found, but some other owner still exists
		return reconcile.Result{}, c.Update(ctx, obj)
	case install.FoundAndEmpty:
		// The resource was found, and ownership is empty now.
		// We re-queue, to verify later on that the resource was in fact deleted.
		// Deleting an address space might take a bit longer.
		return reconcile.Result{RequeueAfter: 30 * time.Second}, c.Delete(ctx, obj)
	default:
		// currently this could only be "Not Found", which couldn't be
		// the case here. But go cannot check switch statements for completeness,
		// so we resort to "default" here.
		return reconcile.Result{}, nil
	}

}

// endregion

// region Device Registry

func cleanupDeviceRegistry(ctx finalizer.DeconstructorContext) (reconcile.Result, error) {

	tenant, ok := ctx.Object.(*iotv1.IoTTenant)

	if !ok {
		return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports IoTTenant")
	}

	config, err := getIoTConfigInstance(ctx.Context, ctx.Reader)
	if err != nil {
		return reconcile.Result{}, err
	}

	// process

	job, err := createIoTTenantCleanerJob(&ctx, tenant, config)

	// failed to create job

	if err != nil {
		return reconcile.Result{}, err
	}

	// eval job

	if job == nil {
		ctx.Recorder.Event(tenant, corev1.EventTypeNormal, EventReasonTenantTermination, "No need for special tenant cleanup")
		return reconcile.Result{}, nil
	}

	// eval job status

	if isComplete(job) {
		// done
		err := deleteJob(ctx, job)
		log.Info("Tenant cleanup job completed", "Delete job error", err)
		ctx.Recorder.Event(tenant, corev1.EventTypeNormal, "TenantCleanupJobComplete", "Tenant cleanup job completed successfully")
		// remove finalizer
		return reconcile.Result{}, err
	} else if isFailed(job) {
		// restart
		err := deleteJob(ctx, job)
		// keep finalizer
		log.Info("Re-queue: tenant cleanup job failed")
		ctx.Recorder.Event(tenant, corev1.EventTypeNormal, "TenantCleanupJobFailed", "Tenant cleanup job completed failed")
		return reconcile.Result{Requeue: true}, err
	} else {
		// wait
		log.V(2).Info("Tenant cleanup job still running. Wait one more minute.")
		return reconcile.Result{RequeueAfter: time.Minute}, nil
	}

}

func deleteJob(ctx finalizer.DeconstructorContext, job *batchv1.Job) error {
	p := metav1.DeletePropagationBackground
	return install.DeleteIgnoreNotFound(ctx.Context, ctx.Client, job, &client.DeleteOptions{
		PropagationPolicy: &p,
	})
}

// endregion

// region Trust Anchors

func cleanupTrustAnchors(ctx finalizer.DeconstructorContext) (reconcile.Result, error) {

	tenant, ok := ctx.Object.(*iotv1.IoTTenant)

	namespace := util.MustGetInfrastructureNamespace()

	if !ok {
		return reconcile.Result{}, fmt.Errorf("provided wrong object type to finalizer, only supports IoTTenant")
	}

	err := ctx.Client.DeleteAllOf(ctx.Context, &corev1.ConfigMap{},
		client.InNamespace(namespace),
		client.MatchingLabels{
			labelTenantOwnerNamespace: tenant.Namespace,
			labelTenantOwnerName:      tenant.Name,
		},
	)

	// done

	return reconcile.Result{}, err

}

// endregion
