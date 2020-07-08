/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotconfig

import (
	"context"
	"fmt"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"github.com/enmasseproject/enmasse/pkg/util"
	"go.uber.org/multierr"
	appsv1 "k8s.io/api/apps/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/runtime/schema"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"strings"
)

type componentEnabled func(config *iotv1alpha1.IoTConfig) bool
type componentCheck func(ctx context.Context, client client.Client, config *iotv1alpha1.IoTConfig) (*result, error)

type component struct {
	Type iotv1alpha1.ConfigConditionType

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

	ready := numReady > 0
	degraded := numReady < numTotal

	if err != nil {
		if errors.IsNotFound(err) {
			return componentNotFound(objectKind, namespace, name), nil
		} else {
			return nil, err
		}
	}

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
	return func(ctx context.Context, c client.Client, config *iotv1alpha1.IoTConfig) (*result, error) {
		deployment := &appsv1.Deployment{}
		err := c.Get(ctx, client.ObjectKey{Namespace: namespace, Name: name}, deployment)

		numReady := deployment.Status.ReadyReplicas
		numTotal := deployment.Status.Replicas

		return componentApps(deployment, namespace, name, err, numReady, numTotal)
	}
}

func componentStatefulSet(namespace string, name string) componentCheck {
	return func(ctx context.Context, c client.Client, config *iotv1alpha1.IoTConfig) (*result, error) {
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
	config     *iotv1alpha1.IoTConfig
	components []component
}

func newAggregator(config *iotv1alpha1.IoTConfig) *conditionAggregator {

	// device registry split?

	deviceRegistryAdapter := nameDeviceRegistry
	deviceRegistryManagement := nameDeviceRegistry

	split, _ := isSplitRegistry(config)
	if split {
		deviceRegistryManagement = nameDeviceRegistryManagement
	}

	// add static services

	components := []component{
		{Type: iotv1alpha1.ConfigConditionTypeCommandMeshReady, Enabled: nil, Check: componentStatefulSet(config.Namespace, nameServiceMesh)},
		{Type: iotv1alpha1.ConfigConditionTypeAuthServiceReady, Enabled: nil, Check: componentDeployment(config.Namespace, nameAuthService)},
		{Type: iotv1alpha1.ConfigConditionTypeTenantServiceReady, Enabled: nil, Check: componentDeployment(config.Namespace, nameTenantService)},
		{Type: iotv1alpha1.ConfigConditionTypeDeviceConnectionServiceReady, Enabled: nil, Check: componentDeployment(config.Namespace, nameDeviceConnection)},
		{Type: iotv1alpha1.ConfigConditionTypeDeviceRegistryAdapterServiceReady, Enabled: nil, Check: componentDeployment(config.Namespace, deviceRegistryAdapter)},
		{Type: iotv1alpha1.ConfigConditionTypeDeviceRegistryManagementServiceReady, Enabled: nil, Check: componentDeployment(config.Namespace, deviceRegistryManagement)},
	}

	// add protocol adapters

	for _, a := range adapters {
		components = append(components, component{
			Type: a.ReadyCondition,
			Enabled: func(config *iotv1alpha1.IoTConfig) bool {
				return a.IsEnabled(config)
			},
			Check: componentDeployment(config.Namespace, a.FullName()),
		})
	}

	return &conditionAggregator{config, components}
}

func (c *conditionAggregator) aggregateFromList(condition *iotv1alpha1.CommonCondition, list []string, okValue bool, reason string, theyAre string) {
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

func (c *conditionAggregator) Aggregate(ctx context.Context, client client.Client, config *iotv1alpha1.IoTConfig, reconcileError error) (iotv1alpha1.ConfigPhaseType, string) {

	// merge errors into reconcile error

	var me = reconcileError

	// record non-read and degraded

	var nonReadyList = make([]string, 0)
	var degradedList = make([]string, 0)

	// eval

	for _, component := range c.components {
		if component.Enabled != nil && !component.Enabled(config) {
			// remove
			config.Status.RemoveCondition(component.Type)
			continue
		}

		condition := config.Status.GetConfigCondition(component.Type)
		result, err := component.Check(ctx, client, config)
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
	reconCond := config.Status.GetConfigCondition(iotv1alpha1.ConfigConditionTypeReconciled)
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

	ready := config.Status.GetConfigCondition(iotv1alpha1.ConfigConditionTypeReady)
	c.aggregateFromList(&ready.CommonCondition, nonReadyList, true, "NonReadyComponents", "non-ready")

	degraded := config.Status.GetConfigCondition(iotv1alpha1.ConfigConditionTypeDegraded)
	c.aggregateFromList(&degraded.CommonCondition, degradedList, false, "DegradedComponents", "degraded")

	// done

	if ready.IsOk() {
		return iotv1alpha1.ConfigPhaseActive, ""
	} else if configErr != nil {
		return iotv1alpha1.ConfigPhaseFailed, configErr.Error()
	} else {
		return iotv1alpha1.ConfigPhaseFailed, ready.Message
	}

}

func (r *ReconcileIoTConfig) updateConditions(ctx context.Context, config *iotv1alpha1.IoTConfig, reconcileError error) {

	aggregator := newAggregator(config)
	phase, message := aggregator.Aggregate(ctx, r.client, config, reconcileError)

	config.Status.Phase = phase
	config.Status.Message = message

}
