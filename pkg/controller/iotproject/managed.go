/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	"fmt"
	"time"

	"github.com/enmasseproject/enmasse/pkg/util/install"

	"github.com/enmasseproject/enmasse/pkg/util/recon"

	corev1 "k8s.io/api/core/v1"

	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	userv1beta1 "github.com/enmasseproject/enmasse/pkg/apis/user/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/util"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

const AddressNameTelemetry = "telemetry"
const AddressNameEvent = "event"
const AddressNameCommandLegacy = "control"
const AddressNameCommand = "command"
const AddressNameCommandResponse = "command_response"

var Addresses = []string{
	AddressNameTelemetry,
	AddressNameEvent,
	AddressNameCommandLegacy,
	AddressNameCommand,
	AddressNameCommandResponse,
}

const resourceTypeAddressSpace = "Address Space"
const resourceTypeAdapterUser = "Adapter User"

const annotationPasswordSyncTime = annotationBase + "/passwordSyncTime"
const annotationProject = annotationBase + "/project.name"
const annotationProjectUID = annotationBase + "/project.uid"

type managedStatus struct {
	remainingCreated map[string]bool
	remainingReady   map[string]bool
	addressSpace     *enmassev1beta1.AddressSpace
}

func updateFromMap(resources map[string]bool, condition *iotv1alpha1.CommonCondition, reason string) {

	message := ""
	for k, v := range resources {
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

func updateManagedStatus(m *managedStatus, project *iotv1alpha1.IoTProject) {

	createdCondition := project.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesCreated)
	updateFromMap(m.remainingCreated, &createdCondition.CommonCondition, "Missing resources")

	readyCondition := project.Status.GetProjectCondition(iotv1alpha1.ProjectConditionTypeResourcesReady)
	updateFromMap(m.remainingReady, &readyCondition.CommonCondition, "Non-ready resources")

	project.Status.IsReady = readyCondition.Status == corev1.ConditionTrue

}

func (r *ReconcileIoTProject) reconcileManaged(ctx context.Context, request *reconcile.Request, project *iotv1alpha1.IoTProject) (reconcile.Result, error) {

	now := time.Now()

	strategy := project.Spec.DownstreamStrategy.ManagedDownstreamStrategy

	managedStatus := &managedStatus{
		remainingCreated: map[string]bool{},
		remainingReady:   map[string]bool{},
	}

	// pre-fill maps for buildings conditions later

	managedStatus.remainingCreated[resourceTypeAdapterUser] = false
	managedStatus.remainingReady[resourceTypeAdapterUser] = false

	managedStatus.remainingCreated[resourceTypeAddressSpace] = false
	managedStatus.remainingReady[resourceTypeAdapterUser] = false

	// defer condition status update

	defer updateManagedStatus(managedStatus, project)

	// start reconciling

	rc := recon.ReconcileContext{}

	// ensure adapter credentials are set

	rc.Process(func() (result reconcile.Result, e error) {
		return r.ensureAdapterCredentials(ctx, project)
	})

	if rc.NeedRequeue() || rc.Error() != nil {
		// return early as we need to be persisted
		return rc.Result()
	}

	project.Status.Phase = "Configuring"

	// reconcile address space

	rc.Process(func() (result reconcile.Result, e error) {
		addressSpace, result, err := r.reconcileAddressSpace(ctx, project, strategy, managedStatus)
		managedStatus.addressSpace = addressSpace
		return result, err
	})

	// create a set of addresses

	rc.Process(func() (result reconcile.Result, e error) {
		return r.reconcileAddressSet(ctx, project, strategy, managedStatus)
	})

	// create a new user for protocol adapters

	rc.Process(func() (result reconcile.Result, e error) {
		return r.reconcileAdapterUser(ctx, project, strategy, managedStatus)
	})

	// extract endpoint information

	currentCredentials := project.Status.DownstreamEndpoint.Credentials.DeepCopy()
	if managedStatus.addressSpace != nil && project.Status.IsReady {

		forceTls := true
		rc.Process(func() (result reconcile.Result, e error) {
			endpoint, err := extractEndpointInformation("messaging", iotv1alpha1.Service, "amqps", currentCredentials, managedStatus.addressSpace, &forceTls)

			if endpoint != nil {
				project.Status.DownstreamEndpoint = endpoint.ConnectionInformation.DeepCopy()
			}

			return reconcile.Result{}, err
		})
	}

	// check and queue password reset

	rc.Process(func() (result reconcile.Result, e error) {

		if val, ok := project.Annotations[annotationBase+"/resetPasswordAfter"]; ok {
			expires, err := time.Parse(time.RFC3339, val)
			if err != nil {
				return reconcile.Result{}, err
			}
			delay := expires.Sub(now)
			if delay.Seconds() > 0 {
				log.Info("Password set to expire in", "delay", delay)
				return reconcile.Result{RequeueAfter: delay}, nil
			}
		}
		return reconcile.Result{}, nil
	})

	// return result

	return rc.Result()

}

func (r *ReconcileIoTProject) ensureAdapterCredentials(ctx context.Context, project *iotv1alpha1.IoTProject) (reconcile.Result, error) {

	if project.Status.DownstreamEndpoint == nil {
		project.Status.DownstreamEndpoint = &iotv1alpha1.ConnectionInformation{}
	}

	if project.Status.Managed == nil {
		project.Status.Managed = &iotv1alpha1.ManagedStatus{}
	}

	//

	changed := false

	// eval address space

	if project.Status.Managed.AddressSpace != project.Spec.DownstreamStrategy.ManagedDownstreamStrategy.AddressSpace.Name {

		if project.Status.Managed.AddressSpace == "" {

			// record change, early store and requeue

			project.Status.Managed.AddressSpace = project.Spec.DownstreamStrategy.ManagedDownstreamStrategy.AddressSpace.Name
			changed = true

		} else {

			// cleanup old address space first

			project.Status.Phase = "Reconfiguring"
			project.Status.PhaseReason = "Address Space changed"

			result, err := cleanupManagedResources(ctx, r.client, project)
			if err != nil {
				// failed to clean up ... return
				return result, err
			}

			// clear out address space, will re-set in the next iteration

			project.Status.Managed.AddressSpace = ""
			changed = true

		}

	}

	// eval password

	if val, ok := project.Annotations[annotationBase+"/resetPasswordAfter"]; ok {
		expires, err := time.Parse(time.RFC3339, val)
		if err != nil {
			return reconcile.Result{}, err
		}
		if time.Now().After(expires) && expires.After(project.Status.Managed.PasswordTime.Time) {
			log.Info("Password expired")
			// reset password
			project.Status.DownstreamEndpoint.Password = ""
		}
	}

	if project.Status.DownstreamEndpoint.Password == "" || project.Status.Managed.PasswordTime.Time.IsZero() {

		project.Status.Managed.PasswordTime.Time = time.Now()

		// ... we generate a new one ...
		log.Info("Generating new password")
		password, err := util.GeneratePassword(32)

		if err != nil {
			return reconcile.Result{}, err
		}

		project.Status.DownstreamEndpoint = &iotv1alpha1.ConnectionInformation{
			Credentials: iotv1alpha1.Credentials{
				Username: "adapter-" + string(project.UID),
				Password: password,
			},
		}

		// re-queue right now to ensure the password is stored

		changed = true

	}

	// proceed

	return reconcile.Result{
		Requeue: changed,
	}, nil

}

func (r *ReconcileIoTProject) reconcileAddressSpace(ctx context.Context, project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy, managedStatus *managedStatus) (*enmassev1beta1.AddressSpace, reconcile.Result, error) {

	addressSpace := &enmassev1beta1.AddressSpace{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: project.Namespace,
			Name:      strategy.AddressSpace.Name,
		},
	}

	var retryDelay time.Duration = 0

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, addressSpace, func(existing runtime.Object) error {
		existingAddressSpace := existing.(*enmassev1beta1.AddressSpace)

		log.V(2).Info("Reconcile address space", "AddressSpace", existingAddressSpace)

		managedStatus.remainingReady[resourceTypeAddressSpace] = existingAddressSpace.Status.IsReady

		// if the address space is not ready yet
		if !existingAddressSpace.Status.IsReady {
			// delay for 30 seconds
			retryDelay = 30 * time.Second
		}

		return r.reconcileManagedAddressSpace(project, strategy, existingAddressSpace)
	})

	if err == nil {
		managedStatus.remainingCreated[resourceTypeAddressSpace] = true
	}

	return addressSpace, reconcile.Result{RequeueAfter: retryDelay}, err

}

func (r *ReconcileIoTProject) reconcileAdapterUser(ctx context.Context, project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy, managedStatus *managedStatus) (reconcile.Result, error) {

	// ensured that this is not nil

	credentials := project.Status.DownstreamEndpoint.Credentials

	// construct object

	adapterUser := &userv1beta1.MessagingUser{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: project.Namespace,
			Name:      strategy.AddressSpace.Name + "." + credentials.Username,
		},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, adapterUser, func(existing runtime.Object) error {
		existingUser := existing.(*userv1beta1.MessagingUser)

		log.V(2).Info("Reconcile messaging user", "MessagingUser", existingUser)

		return r.reconcileAdapterMessagingUser(project, credentials, existingUser)
	})

	if err == nil {
		managedStatus.remainingCreated[resourceTypeAdapterUser] = true
		managedStatus.remainingReady[resourceTypeAdapterUser] = true
	}

	return reconcile.Result{}, err
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

func (r *ReconcileIoTProject) createOrUpdateAddress(ctx context.Context, project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy, addressBaseName string, plan string, typeName string, managedStatus *managedStatus) error {

	addressName := util.AddressName(project, addressBaseName)
	addressMetaName := util.EncodeAddressSpaceAsMetaName(strategy.AddressSpace.Name, addressName)

	log.Info("Creating/updating address", "basename", addressBaseName, "name", addressName, "metaname", addressMetaName)

	stateKey := "Address|" + addressName
	managedStatus.remainingCreated[stateKey] = false

	address := &enmassev1beta1.Address{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: project.Namespace,
			Name:      addressMetaName,
		},
	}

	_, err := controllerutil.CreateOrUpdate(ctx, r.client, address, func(existing runtime.Object) error {
		existingAddress := existing.(*enmassev1beta1.Address)

		managedStatus.remainingReady[stateKey] = existingAddress.Status.IsReady

		return r.reconcileAddress(project, strategy, addressName, plan, typeName, existingAddress)
	})

	if err == nil {
		managedStatus.remainingCreated[stateKey] = true
	}

	return err
}

func (r *ReconcileIoTProject) reconcileAddressSet(ctx context.Context, project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy, managedStatus *managedStatus) (reconcile.Result, error) {

	if strategy.Addresses.Telemetry.Plan == "" {
		return reconcile.Result{}, fmt.Errorf("'addresses.telemetry.plan' must not be empty")
	}
	if strategy.Addresses.Event.Plan == "" {
		return reconcile.Result{}, fmt.Errorf("'addresses.event.plan' must not be empty")
	}
	if strategy.Addresses.Command.Plan == "" {
		return reconcile.Result{}, fmt.Errorf("'addresses.command.plan' must not be empty")
	}

	rc := recon.ReconcileContext{}

	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, project, strategy, AddressNameTelemetry,
			strategy.Addresses.Telemetry.Plan,
			StringOrDefault(strategy.Addresses.Telemetry.Type, "anycast"),
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, project, strategy, AddressNameEvent,
			strategy.Addresses.Event.Plan,
			StringOrDefault(strategy.Addresses.Event.Type, "queue"),
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, project, strategy, AddressNameCommandLegacy,
			strategy.Addresses.Command.Plan,
			StringOrDefault(strategy.Addresses.Command.Type, "anycast"),
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, project, strategy, AddressNameCommand,
			strategy.Addresses.Command.Plan,
			StringOrDefault(strategy.Addresses.Command.Type, "anycast"),
			managedStatus,
		)
	})
	rc.ProcessSimple(func() error {
		return r.createOrUpdateAddress(ctx, project, strategy, AddressNameCommandResponse,
			strategy.Addresses.Command.Plan,
			StringOrDefault(strategy.Addresses.Command.Type, "anycast"),
			managedStatus,
		)
	})

	return rc.Result()

}

func (r *ReconcileIoTProject) reconcileManagedAddressSpace(project *iotv1alpha1.IoTProject, strategy *iotv1alpha1.ManagedDownstreamStrategy, existing *enmassev1beta1.AddressSpace) error {

	// add ourselves to the list of owners

	if err := install.AddOwnerReference(project, existing, r.scheme); err != nil {
		return err
	}

	// we must have a plan

	if strategy.AddressSpace.Plan == "" {
		return fmt.Errorf("'addressSpace.plan' must not be empty")
	}

	// eval type information

	t := StringOrDefault(strategy.AddressSpace.Type, "standard")

	// check if we are the only one using this space

	if len(existing.OwnerReferences) == 1 {

		// if so, we simply set the information

		existing.Spec.Type = t
		existing.Spec.Plan = strategy.AddressSpace.Plan

	} else {

		// otherwise, we must ensure we have the same setting

		if existing.Spec.Type != t {
			return fmt.Errorf("address space is already created using a different type: %s", existing.Spec.Type)
		}
		if existing.Spec.Plan != strategy.AddressSpace.Plan {
			return fmt.Errorf("address space is already created using a different plan: %s", existing.Spec.Plan)
		}

	}

	return nil
}

func (r *ReconcileIoTProject) reconcileAdapterMessagingUser(project *iotv1alpha1.IoTProject, credentials iotv1alpha1.Credentials, existing *userv1beta1.MessagingUser) error {

	if err := r.ensureControllerOwnerIsSet(project, existing); err != nil {
		return err
	}

	managed := project.Status.Managed

	existing.Spec.Username = credentials.Username
	existing.Spec.Authentication = userv1beta1.AuthenticationSpec{
		Type: "password",
	}

	if existing.Annotations == nil {
		existing.Annotations = make(map[string]string)
	}

	existing.Annotations[annotationProject] = project.Name
	existing.Annotations[annotationProjectUID] = string(project.UID)

	// get last password sync

	var syncTime time.Time
	syncTimeString := existing.Annotations[annotationPasswordSyncTime]
	if syncTimeString != "" {
		syncTime, _ = time.Parse(time.RFC3339, syncTimeString)
	}

	// if we missed an update

	if managed.PasswordTime.After(syncTime) {

		log.Info("Updating password for", "user", existing.Name)

		// set the password
		existing.Spec.Authentication.Password = []byte(credentials.Password)
		existing.Annotations[annotationPasswordSyncTime] = managed.PasswordTime.UTC().Format(time.RFC3339)

	}

	// create access rules

	telemetryName := util.AddressName(project, AddressNameTelemetry)
	eventName := util.AddressName(project, AddressNameEvent)

	commandLegacyName := util.AddressName(project, AddressNameCommandLegacy)
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
			Operations: []string{
				"send",
			},
		},

		{
			Addresses: []string{
				commandName,
				commandName + "/*",
			},
			Operations: []string{
				"recv",
			},
		},

		{
			Addresses: []string{
				commandLegacyName,
				commandLegacyName + "/*",
			},
			Operations: []string{
				"send",
				"recv",
			},
		},
	}

	return nil
}
