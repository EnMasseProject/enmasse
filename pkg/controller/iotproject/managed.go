/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	"fmt"

	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

const AddressNameTelemetry = "telemetry"
const AddressNameEvent = "event"
const AddressNameCommand = "control"

func (r *ReconcileIoTProject) reconcileManaged(ctx context.Context, request *reconcile.Request, project *iotv1alpha1.IoTProject) (*iotv1alpha1.ExternalDownstreamStrategy, error) {

	log.Info("Reconcile project with managed strategy")

	strategy := project.Spec.DownstreamStrategy.ManagedDownstreamStrategy

	// reconcile address space

	addressSpace := &enmassev1beta1.AddressSpace{
		ObjectMeta: v1.ObjectMeta{Namespace: project.Namespace, Name: strategy.AddressSpace.Name},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, addressSpace, func(existing runtime.Object) error {
		existingAddressSpace := existing.(*enmassev1beta1.AddressSpace)

		if err := r.ensureOwnerIsSet(project, existingAddressSpace); err != nil {
			return err
		}

		log.Info("Reconcile address space", "AddressSpace", existingAddressSpace)

		return r.reconcileAddressSpace(project, strategy, existingAddressSpace)
	})

	if err != nil {
		log.Error(err, "Failed calling CreateOrUpdate")
		return nil, err
	}

	// create a set of addresses

	err = r.reconcileAddressSet(ctx, project, strategy)

	if err != nil {
		log.Error(err, "Failed to create addresses")
	}

	// create a new user for protocol adapters

	credentials, err := r.reconcileAdapterUser(ctx, project, strategy)
	if err != nil {
		log.Error(err, "failed to create adapter user")
		return nil, err
	}

	// extract endpoint information

	forceTls := true
	return extractEndpointInformation("messaging", iotv1alpha1.Service, "amqps", credentials, addressSpace, &forceTls)
}

func (r *ReconcileIoTProject) reconcileAdapterUser(ctx context.Context, project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy) (*iotv1alpha1.Credentials, error) {

	adapterUserName := "adapter"
	adapterUser := &userv1beta1.MessagingUser{
		ObjectMeta: v1.ObjectMeta{Namespace: project.Namespace, Name: strategy.AddressSpace.Name + "." + adapterUserName},
	}

	credentials := &iotv1alpha1.Credentials{
		Username: adapterUserName,
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, adapterUser, func(existing runtime.Object) error {
		existingUser := existing.(*userv1beta1.MessagingUser)

		log.Info("Reconcile messaging user", "MessagingUser", existingUser)

		return r.reconcileAdapterMessagingUser(project, credentials, existingUser)
	})

	return credentials, err
}

func (r *ReconcileIoTProject) reconcileAddress(project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy, addressName string, plan string, typeName string, existing *enmassev1beta1.Address) error {

	if err := r.ensureControllerOwnerIsSet(project, existing); err != nil {
		return err
	}

	existing.Spec.Address = addressName
	existing.Spec.Plan = plan
	existing.Spec.Type = typeName

	return nil
}

func (r *ReconcileIoTProject) createOrUpdateAddress(ctx context.Context, project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy, addressBaseName string, plan string, typeName string) error {

	addressName := util.AddressName(project, addressBaseName)
	addressMetaName := util.EncodeAddressSpaceAsMetaName(strategy.AddressSpace.Name, addressName)

	log.Info("Creating/updating address", "basename", addressBaseName, "name", addressName, "metaname", addressMetaName)

	address := &enmassev1beta1.Address{
		ObjectMeta: v1.ObjectMeta{Namespace: project.Namespace, Name: addressMetaName},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, address, func(existing runtime.Object) error {
		existingAddress := existing.(*enmassev1beta1.Address)

		return r.reconcileAddress(project, strategy, addressName, plan, typeName, existingAddress)
	})

	return err
}

func (r *ReconcileIoTProject) reconcileAddressSet(ctx context.Context, project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy) error {

	mt := util.MultiTool{}

	if strategy.Addresses.Telemetry.Plan == "" {
		return fmt.Errorf("'addresses.telemetry.plan' must not be empty")
	}
	if strategy.Addresses.Event.Plan == "" {
		return fmt.Errorf("'addresses.event.plan' must not be empty")
	}
	if strategy.Addresses.Command.Plan == "" {
		return fmt.Errorf("'addresses.command.plan' must not be empty")
	}

	mt.Run(func() error {
		return r.createOrUpdateAddress(ctx, project, strategy, AddressNameTelemetry,
			strategy.Addresses.Telemetry.Plan,
			StringOrDefault(strategy.Addresses.Telemetry.Type, "anycast"),
		)
	})
	mt.Run(func() error {
		return r.createOrUpdateAddress(ctx, project, strategy, AddressNameEvent,
			strategy.Addresses.Event.Plan,
			StringOrDefault(strategy.Addresses.Event.Type, "queue"),
		)
	})
	mt.Run(func() error {
		return r.createOrUpdateAddress(ctx, project, strategy, AddressNameCommand,
			strategy.Addresses.Command.Plan,
			StringOrDefault(strategy.Addresses.Command.Type, "anycast"),
		)
	})

	return mt.Error

}

func (r *ReconcileIoTProject) reconcileAddressSpace(project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy, existing *enmassev1beta1.AddressSpace) error {

	if existing.CreationTimestamp.IsZero() {
		existing.ObjectMeta.Labels = project.Labels
	}

	if strategy.AddressSpace.Plan == "" {
		return fmt.Errorf("'addressSpace.plan' must not be empty")
	}

	existing.Spec.Type = StringOrDefault(strategy.AddressSpace.Type, "standard")
	existing.Spec.Plan = strategy.AddressSpace.Plan

	return nil
}

func (r *ReconcileIoTProject) reconcileAdapterMessagingUser(project *iotv1alpha1.IoTProject, credentials *iotv1alpha1.Credentials, existing *userv1beta1.MessagingUser) error {

	if err := r.ensureControllerOwnerIsSet(project, existing); err != nil {
		return err
	}

	username := credentials.Username

	telemetryName := util.AddressName(project, AddressNameTelemetry)
	eventName := util.AddressName(project, AddressNameEvent)
	controlName := util.AddressName(project, AddressNameCommand)

	existing.Spec.Username = username
	existing.Spec.Authentication = userv1beta1.AuthenticationSpec{
		Type: "password",
	}

	// if the status has endpoint information ...
	if project.Status.DownstreamEndpoint != nil {
		// ... we extract the password and return it
		credentials.Password = project.Status.DownstreamEndpoint.Password
	}

	// if the project is ready and password is not set yet ...
	if (project.Status.IsReady && credentials.Password == "") || util.IsNewObject(existing) {
		// ... we generate a new one ...
		log.Info("Generating new password")
		password, err := util.GeneratePassword(32)
		if err != nil {
			return err
		}
		// ... return it in the credentials structure
		credentials.Password = password
		// ... and set it to the messaging user
		existing.Spec.Authentication.Password = []byte(credentials.Password)
	}

	existing.Spec.Authorization = []userv1beta1.AuthorizationSpec{
		{
			Addresses: []string{
				telemetryName,
				telemetryName + "/*",
				eventName,
				eventName + "/*",
				controlName,
				controlName + "/*",
			},
			Operations: []string{
				"send",
				"recv",
			},
		},
	}

	return nil
}
