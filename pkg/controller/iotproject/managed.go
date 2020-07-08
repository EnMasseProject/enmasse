/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	"github.com/enmasseproject/enmasse/pkg/util/recon"
	"sort"

	corev1 "k8s.io/api/core/v1"

	enmassev1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
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

const annotationProject = annotationBase + "/project.name"
const annotationProjectUID = annotationBase + "/project.uid"

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

func updateManagedReadyStatus(m *managedStatus, project *iotv1alpha1.IoTProject) {

	createdCondition := project.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesCreated)
	updateFromMap(m.remainingCreated, &createdCondition.CommonCondition, "Missing resources")

	readyCondition := project.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesReady)
	updateFromMap(m.remainingReady, &readyCondition.CommonCondition, "Non-ready resources")

	if createdCondition.Status == corev1.ConditionTrue && readyCondition.Status == corev1.ConditionTrue {
		project.Status.Phase = iotv1alpha1.ProjectPhaseActive
	} else {
		project.Status.Phase = iotv1alpha1.ProjectPhaseConfiguring
	}

}

func (r *ReconcileIoTProject) reconcileManaged(ctx context.Context, project *iotv1alpha1.IoTProject) (reconcile.Result, error) {

	managedStatus := &managedStatus{
		remainingCreated: map[string]bool{},
		remainingReady:   map[string]bool{},
	}

	// start reconciling

	rc := recon.ReconcileContext{}

	project.Status.Phase = iotv1alpha1.ProjectPhaseConfiguring

	// create a set of addresses

	rc.Process(func() (result reconcile.Result, e error) {
		return r.reconcileAddressSet(ctx, project, managedStatus)
	})

	// update status - no more changes to "managedStatus" beyond this point

	updateManagedReadyStatus(managedStatus, project)

	// return result

	return rc.Result()

}

func (r *ReconcileIoTProject) reconcileAddress(
	project *iotv1alpha1.IoTProject,
	addressName string,
	addressType AddressType,
	existing *enmassev1.MessagingAddress,
) error {

	if err := r.ensureControllerOwnerIsSet(project, existing); err != nil {
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

func (r *ReconcileIoTProject) createOrUpdateAddress(ctx context.Context, project *iotv1alpha1.IoTProject, addressBaseName string, addressType AddressType, managedStatus *managedStatus) error {

	addressName := util.AddressName(project, addressBaseName)
	addressMetaName := util.EncodeAddressSpaceAsMetaName(addressName)

	stateKey := "Address|" + addressName
	managedStatus.remainingCreated[stateKey] = false

	address := &enmassev1.MessagingAddress{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: project.Namespace,
			Name:      addressMetaName,
		},
	}

	rc, err := controllerutil.CreateOrUpdate(ctx, r.client, address, func() error {
		managedStatus.remainingReady[stateKey] = address.Status.Phase == enmassev1.MessagingAddressActive

		return r.reconcileAddress(project, addressName, addressType, address)
	})

	if rc != controllerutil.OperationResultNone {
		log.Info("Created/updated address", "op", rc, "basename", addressBaseName, "name", addressName, "metaname", addressMetaName)
	}

	if err == nil {
		managedStatus.remainingCreated[stateKey] = true
	}

	return err
}

func (r *ReconcileIoTProject) reconcileAddressSet(ctx context.Context, project *iotv1alpha1.IoTProject, managedStatus *managedStatus) (reconcile.Result, error) {

	rc := recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, project, AddressNameTelemetry,
			AddressTypeAnycast,
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, project, AddressNameEvent,
			AddressTypeQueue,
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, project, AddressNameCommand,
			AddressTypeAnycast,
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, project, AddressNameCommandResponse,
			AddressTypeAnycast,
			managedStatus,
		)
	})

	return rc.Result()

}

func (r *ReconcileIoTProject) reconcileAdapterMessagingUser(project *iotv1alpha1.IoTProject, credentials iotv1alpha1.Credentials, existing *userv1beta1.MessagingUser) error {

	if err := r.ensureControllerOwnerIsSet(project, existing); err != nil {
		return err
	}

	existing.Spec.Username = credentials.Username
	existing.Spec.Authentication = userv1beta1.AuthenticationSpec{
		Type: "password",
	}

	if existing.Annotations == nil {
		existing.Annotations = make(map[string]string)
	}

	existing.Annotations[annotationProject] = project.Name
	existing.Annotations[annotationProjectUID] = string(project.UID)

	// set the password
	existing.Spec.Authentication.Password = []byte(credentials.Password)

	// create access rules

	telemetryName := util.AddressName(project, AddressNameTelemetry)
	eventName := util.AddressName(project, AddressNameEvent)

	commandName := util.AddressName(project, AddressNameCommand)
	commandResponseName := util.AddressName(project, AddressNameCommandResponse)

	existing.Spec.Authorization = []userv1beta1.AuthorizationSpec{
		{
			Addresses: []string{
				telemetryName,
				telemetryName + "/*",
				eventName,
				eventName + "/*",
				commandResponseName,
				commandResponseName + "/*",
			},
			Operations: []userv1beta1.AuthorizationOperation{
				userv1beta1.Send,
			},
		},

		{
			Addresses: []string{
				commandName,
				commandName + "/*",
			},
			Operations: []userv1beta1.AuthorizationOperation{
				userv1beta1.Recv,
			},
		},
	}

	return nil
}
