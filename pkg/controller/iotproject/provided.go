/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"

	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"k8s.io/apimachinery/pkg/types"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
)

func (r *ReconcileIoTProject) reconcileProvided(ctx context.Context, request *reconcile.Request, project *iotv1alpha1.IoTProject) (*iotv1alpha1.ExternalDownstreamStrategy, error) {

	log.Info("Reconcile project with provided strategy")

	strategy := project.Spec.DownstreamStrategy.ProvidedDownstreamStrategy
	endpointName, portName, endpointMode, err := getOrDefaults(strategy)

	if err != nil {
		return nil, err
	}

	return r.processProvided(strategy, endpointMode, endpointName, portName)
}

func (r *ReconcileIoTProject) processProvided(strategy *iotv1alpha1.ProvidedDownstreamStrategy, endpointMode iotv1alpha1.EndpointMode, endpointName string, portName string) (*iotv1alpha1.ExternalDownstreamStrategy, error) {

	addressSpace := &enmassev1beta1.AddressSpace{}
	err := r.client.Get(context.TODO(), types.NamespacedName{Namespace: strategy.Namespace, Name: strategy.AddressSpaceName}, addressSpace)

	if err != nil {
		log.WithValues("namespace", strategy.Namespace, "name", strategy.AddressSpaceName).Info("Failed to get address space")
		return nil, err
	}

	return extractEndpointInformation(endpointName, endpointMode, portName, &strategy.Credentials, addressSpace, strategy.TLS)
}
