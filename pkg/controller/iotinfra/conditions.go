/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotinfra

import (
	"context"
	"fmt"
	iotv1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"go.uber.org/multierr"
	appsv1 "k8s.io/api/apps/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"strings"
)

type componentEnabled func(infra *iotv1.IoTInfrastructure) bool
type componentCheck func(ctx context.Context, client client.Client, infra *iotv1.IoTInfrastructure) (*result, error)

type component struct {
	Type iotv1.InfrastructureConditionType

	Enabled componentEnabled
	Check   componentCheck
}

func componentNotFound(objectKind schema.ObjectKind, namespace string, name string) *result {
	return &result{
		Ok:       false,
		Reason:   "ComponentNotFound",
		Message:  fmt.Sprintf("Coulnd't find %s named '%s' in namespace '%s'", objectKind.GroupVersionKind().String(), namespace, name),
		NonReady: true,
		Degraded: true,
	}
}

func componentApps(objectKind schema.ObjectKind, namespace string, name string, err error, numReady int32, numTotal int32) (*result, error) {

	if err != nil {
		if errors.IsNotFound(err) {
			return componentNotFound(objectKind, namespace, name), nil
		} else {
			return nil, err
		}
	}

	ready := numReady > 0
	degraded := numReady < numTotal

	kind := objectKind.GroupVersionKind().Kind
	reason := ""
	message := ""
	if !ready {
		// not ready at all
		reason = "AllNonReadyPods"
		message = fmt.Sprintf("%s %s/%s has no pods that are ready (%d/%d)", kind, namespace, name, numReady, numTotal)
	} else if degraded {
		// ready but degraded
		reason = "SomeNonReadyPods"
		message = fmt.Sprintf("Not all pods of %s %s/%s are ready (%d/%d)", kind, namespace, name, numReady, numTotal)
	} else {
		// ready
		reason = "AsExpected"
		message = fmt.Sprintf("%s %s/%s is ready (%d/%d)", kind, namespace, name, numReady, numTotal)
	}

	return &result{
		Ok:       ready,
		Reason:   reason,
		Message:  message,
		NonReady: !ready,
		Degraded: degraded,
	}, nil
}

func componentDeployment(namespace string, name string) componentCheck {
	return func(ctx context.Context, c client.Client, infra *iotv1.IoTInfrastructure) (*result, error) {
		deployment := &appsv1.Deployment{}
		err := c.Get(ctx, client.ObjectKey{Namespace: namespace, Name: name}, deployment)

		numReady := deployment.Status.ReadyReplicas
		numTotal := deployment.Status.Replicas

		return componentApps(deployment, namespace, name, err, numReady, numTotal)
	}
}

func componentStatefulSet(namespace string, name string) componentCheck {
	return func(ctx context.Context, c client.Client, infra *iotv1.IoTInfrastructure) (*result, error) {
		statefulSet := &appsv1.StatefulSet{}
		err := c.Get(ctx, client.ObjectKey{Namespace: namespace, Name: name}, statefulSet)

		numReady := statefulSet.Status.ReadyReplicas
		numTotal := statefulSet.Status.Replicas

		return componentApps(statefulSet, namespace, name, err, numReady, numTotal)
	}
}

type result struct {
	Ok      bool
	Reason  string
	Message string

	NonReady bool
	Degraded bool
}

type conditionAggregator struct {
	infra      *iotv1.IoTInfrastructure
	components []component
}

func newAggregator(infra *iotv1.IoTInfrastructure) *conditionAggregator {

	// device registry split?

	deviceRegistryAdapter := nameDeviceRegistry
	deviceRegistryManagement := nameDeviceRegistry

	split, _ := isSplitRegistry(infra)
	if split {
		deviceRegistryManagement = nameDeviceRegistryManagement
	}

	// add static services

	components := []component{
		{Type: iotv1.InfrastructureConditionTypeCommandMeshReady, Enabled: nil, Check: componentStatefulSet(infra.Namespace, nameServiceMesh)},
		{Type: iotv1.InfrastructureConditionTypeAuthServiceReady, Enabled: nil, Check: componentDeployment(infra.Namespace, nameAuthService)},
		{Type: iotv1.InfrastructureConditionTypeTenantServiceReady, Enabled: nil, Check: componentDeployment(infra.Namespace, nameTenantService)},
		{Type: iotv1.InfrastructureConditionTypeDeviceConnectionServiceReady, Enabled: nil, Check: componentDeployment(infra.Namespace, nameDeviceConnection)},
		{Type: iotv1.InfrastructureConditionTypeDeviceRegistryAdapterServiceReady, Enabled: nil, Check: componentDeployment(infra.Namespace, deviceRegistryAdapter)},
		{Type: iotv1.InfrastructureConditionTypeDeviceRegistryManagementServiceReady, Enabled: nil, Check: componentDeployment(infra.Namespace, deviceRegistryManagement)},
	}

	// add protocol adapters

	for _, a := range adapters {
		// The next line is important, it makes a copy of the reference to the current item.
		// The reference value "a" points to is not moved to the context of the lambda,
		// so the actual value of "a" will change, to the last element of "adapters" and all
		// lamdbas would use the same, last value of "adapters".
		ax := a
		components = append(components, component{
			Type: ax.ReadyCondition,
			Enabled: func(infra *iotv1.IoTInfrastructure) bool {
				return ax.IsEnabled(infra)
			},
			Check: componentDeployment(infra.Namespace, ax.FullName()),
		})
	}

	return &conditionAggregator{infra, components}
}

func (c *conditionAggregator) aggregateFromList(condition *iotv1.CommonCondition, list []string, okValue bool, reason string, theyAre string) {
	if len(list) > 0 {
		var format string
		if len(list) == 1 {
			format = "%d component out of %d is %s: %s"
		} else {
			format = "%d components out of %d are %s: %s"
		}
		condition.SetStatusAsBoolean(
			!okValue,
			reason,
			fmt.Sprintf(format,
				len(list),
				len(c.components),
				theyAre,
				strings.Join(list, ", ")))
	} else {
		condition.SetStatusAsBoolean(okValue, "AsExpected", "")
	}
}

func (c *conditionAggregator) Aggregate(ctx context.Context, client client.Client, infra *iotv1.IoTInfrastructure, reconcileError error) (iotv1.InfrastructurePhaseType, string) {

	// merge errors into reconcile error

	var me = reconcileError

	// record non-read and degraded

	var nonReadyList = make([]string, 0)
	var degradedList = make([]string, 0)

	// eval

	for _, component := range c.components {
		if component.Enabled != nil && !component.Enabled(infra) {
			// remove
			infra.Status.RemoveCondition(component.Type)
			continue
		}

		condition := infra.Status.GetInfrastructureCondition(component.Type)
		result, err := component.Check(ctx, client, infra)
		if err != nil {
			me = multierr.Append(me, err)
			// we do not update the condition if we failed to evaluate the state
		} else {
			condition.SetStatusAsBoolean(result.Ok, result.Reason, result.Message)
			if result.NonReady {
				nonReadyList = append(nonReadyList, string(component.Type))
			}
			if result.Degraded {
				degradedList = append(degradedList, string(component.Type))
			}
		}

	}

	// add reconcile error

	var configErr error = nil
	reconCond := infra.Status.GetInfrastructureCondition(iotv1.InfrastructureConditionTypeReconciled)
	if me == nil {
		reconCond.SetStatusAsBoolean(true, "AsExpected", "")
	} else {
		if util.OnlyNonRecoverableErrors(me) {
			reconCond.SetStatusAsBoolean(false, "ConfigurationFailed", me.Error())
			configErr = me
		} else {
			reconCond.SetStatusAsBoolean(false, "LoopFailed", me.Error())
		}
		nonReadyList = append(nonReadyList, "Configuration")
		degradedList = append(degradedList, "Configuration")
	}

	// composite ready and degraded state

	ready := infra.Status.GetInfrastructureCondition(iotv1.InfrastructureConditionTypeReady)
	c.aggregateFromList(&ready.CommonCondition, nonReadyList, true, "NonReadyComponents", "non-ready")

	degraded := infra.Status.GetInfrastructureCondition(iotv1.InfrastructureConditionTypeDegraded)
	c.aggregateFromList(&degraded.CommonCondition, degradedList, false, "DegradedComponents", "degraded")

	// done

	if ready.IsOk() {
		return iotv1.InfrastructurePhaseActive, ""
	} else if configErr != nil {
		// configuration error has a higher priority in the user facing message
		return iotv1.InfrastructurePhaseFailed, configErr.Error()
	} else {
		return iotv1.InfrastructurePhaseConfiguring, ready.Message
	}

}

func (r *ReconcileIoTInfrastructure) updateConditions(ctx context.Context, infra *iotv1.IoTInfrastructure, reconcileError error) {

	aggregator := newAggregator(infra)
	phase, message := aggregator.Aggregate(ctx, r.client, infra, reconcileError)

	infra.Status.Phase = phase
	infra.Status.Message = message

}
