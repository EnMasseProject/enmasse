/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iottenant

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"sort"

	corev1 "k8s.io/api/core/v1"

	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

const AddressNameTelemetry = "telemetry"
const AddressNameEvent = "event"
const AddressNameCommand = "command"
const AddressNameCommandResponse = "command_response"

var Addresses = []string{
	AddressNameTelemetry,
	AddressNameEvent,
	AddressNameCommand,
	AddressNameCommandResponse,
}

type managedStatus struct {
	remainingCreated map[string]bool
	remainingReady   map[string]bool
}

func updateFromMap(resources map[string]bool, condition *iotv1alpha1.CommonCondition, reason string) {

	message := ""

	keys := make([]string, 0, len(resources))
	for k := range resources {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	for _, k := range keys {
		v := resources[k]
		if v {
			continue
		}
		if message != "" {
			message += ", "
		}
		message += k
	}

	if message == "" {
		condition.SetStatusOk()
	} else {
		condition.SetStatus(corev1.ConditionFalse, reason, message)
	}

}

func updateManagedReadyStatus(m *managedStatus, tenant *iotv1alpha1.IoTTenant) {

	createdCondition := tenant.Status.GetTenantCondition(iotv1alpha1.TenantConditionTypeResourcesCreated)
	updateFromMap(m.remainingCreated, &createdCondition.CommonCondition, "Missing resources")

	readyCondition := tenant.Status.GetTenantCondition(iotv1alpha1.TenantConditionTypeResourcesReady)
	updateFromMap(m.remainingReady, &readyCondition.CommonCondition, "Non-ready resources")

	if createdCondition.Status == corev1.ConditionTrue && readyCondition.Status == corev1.ConditionTrue {
		tenant.Status.Phase = iotv1alpha1.TenantPhaseActive
	} else {
		tenant.Status.Phase = iotv1alpha1.TenantPhaseConfiguring
	}

}

func (r *ReconcileIoTTenant) reconcileManaged(ctx context.Context, tenant *iotv1alpha1.IoTTenant) (reconcile.Result, error) {

	managedStatus := &managedStatus{
		remainingCreated: map[string]bool{},
		remainingReady:   map[string]bool{},
	}

	// start reconciling

	rc := recon.ReconcileContext{}

	tenant.Status.Phase = iotv1alpha1.TenantPhaseConfiguring

	// create a set of addresses

	rc.Process(func() (result reconcile.Result, e error) {
		return r.reconcileAddressSet(ctx, tenant, managedStatus)
	})

	// update status - no more changes to "managedStatus" beyond this point

	updateManagedReadyStatus(managedStatus, tenant)

	// return result

	return rc.Result()

}

func (r *ReconcileIoTTenant) reconcileAddress(
	tenant *iotv1alpha1.IoTTenant,
	addressName string,
	addressType AddressType,
	existing *enmassev1.MessagingAddress,
) error {

	if err := r.ensureControllerOwnerIsSet(tenant, existing); err != nil {
		return err
	}

	existing.Spec.Address = &addressName
	switch addressType {
	case AddressTypeAnycast:
		existing.Spec.Anycast = &enmassev1.MessagingAddressSpecAnycast{}
		existing.Spec.DeadLetter = nil
		existing.Spec.Multicast = nil
		existing.Spec.Queue = nil
		existing.Spec.Subscription = nil
		existing.Spec.Topic = nil
	case AddressTypeQueue:
		existing.Spec.Anycast = nil
		existing.Spec.DeadLetter = nil
		existing.Spec.Multicast = nil
		existing.Spec.Queue = &enmassev1.MessagingAddressSpecQueue{}
		existing.Spec.Subscription = nil
		existing.Spec.Topic = nil
	}

	return nil
}

func (r *ReconcileIoTTenant) createOrUpdateAddress(ctx context.Context, tenant *iotv1alpha1.IoTTenant, addressBaseName string, addressType AddressType, managedStatus *managedStatus) error {

	addressName := util.AddressName(tenant, addressBaseName)
	addressMetaName := util.EncodeAddressSpaceAsMetaName(addressName)

	stateKey := "Address|" + addressName
	managedStatus.remainingCreated[stateKey] = false

	address := &enmassev1.MessagingAddress{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: tenant.Namespace,
			Name:      addressMetaName,
		},
	}

	rc, err := controllerutil.CreateOrUpdate(ctx, r.client, address, func() error {
		managedStatus.remainingReady[stateKey] = address.Status.Phase == enmassev1.MessagingAddressActive

		return r.reconcileAddress(tenant, addressName, addressType, address)
	})

	if rc != controllerutil.OperationResultNone {
		log.Info("Created/updated address", "op", rc, "basename", addressBaseName, "name", addressName, "metaname", addressMetaName)
	}

	if err == nil {
		managedStatus.remainingCreated[stateKey] = true
	}

	return err
}

func (r *ReconcileIoTTenant) reconcileAddressSet(ctx context.Context, tenant *iotv1alpha1.IoTTenant, managedStatus *managedStatus) (reconcile.Result, error) {

	rc := recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, tenant, AddressNameTelemetry,
			AddressTypeAnycast,
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, tenant, AddressNameEvent,
			AddressTypeQueue,
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, tenant, AddressNameCommand,
			AddressTypeAnycast,
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, tenant, AddressNameCommandResponse,
			AddressTypeAnycast,
			managedStatus,
		)
	})

	return rc.Result()

}
