/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package iotproject

import (
	"context"
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/util"

	"k8s.io/apimachinery/pkg/types"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	enmassev1beta1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	iotv1alpha1 "github.com/enmasseproject/enmasse/pkg/apis/iot/v1alpha1"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

// Convert projects to reconcile requests
func convertToRequests(projects []iotv1alpha1.IoTProject, err error) []reconcile.Request {
	if err != nil {
		return []reconcile.Request{}
	}

	var result []reconcile.Request

	for _, project := range projects {
		result = append(result, reconcile.Request{NamespacedName: types.NamespacedName{
			Namespace: project.Namespace,
			Name:      project.Name,
		}})
	}

	return result
}

func (r *ReconcileIoTProject) findIoTProjectsByPredicate(predicate func(project *iotv1alpha1.IoTProject) bool) ([]iotv1alpha1.IoTProject, error) {

	var result []iotv1alpha1.IoTProject

	opts := &client.ListOptions{}
	list := &iotv1alpha1.IoTProjectList{}

	err := r.client.List(context.TODO(), opts, list)
	if err != nil {
		return nil, err
	}

	for _, item := range list.Items {
		if predicate(&item) {
			result = append(result, item)
		}
	}

	return result, nil
}

func (r *ReconcileIoTProject) findIoTProjectsByMappedAddressSpaces(addressSpaceNamespace string, addressSpaceName string) ([]iotv1alpha1.IoTProject, error) {

	// FIXME: brute force scanning through all IoT projects.
	//        This could be improved if field selectors for CRDs would be implemented, which they
	//        current are not.

	// Look for all IoTProjects where:
	//    spec.downstreamStrategy.providedStrategy.namespace = addressSpaceNamespace
	//      and
	//    spec.downstreamStrategy.providedStrategy.addressSpaceName = addressSpaceName

	return r.findIoTProjectsByPredicate(func(project *iotv1alpha1.IoTProject) bool {
		if project.Spec.DownstreamStrategy.ProvidedDownstreamStrategy == nil {
			return false
		}
		if project.Spec.DownstreamStrategy.ProvidedDownstreamStrategy.Namespace != addressSpaceNamespace {
			return false
		}
		if project.Spec.DownstreamStrategy.ProvidedDownstreamStrategy.AddressSpaceName != addressSpaceName {
			return false
		}
		return true
	})
}

func getOrDefaults(strategy *iotv1alpha1.ProvidedDownstreamStrategy) (string, string, iotv1alpha1.EndpointMode, error) {
	endpointName := strategy.EndpointName
	if len(endpointName) == 0 {
		endpointName = DefaultEndpointName
	}
	portName := strategy.PortName
	if len(portName) == 0 {
		portName = DefaultPortName
	}

	var endpointMode iotv1alpha1.EndpointMode
	if strategy.EndpointMode != nil {
		endpointMode = *strategy.EndpointMode
	} else {
		endpointMode = DefaultEndpointMode
	}

	if len(strategy.Namespace) == 0 {
		return "", "", 0, fmt.Errorf("missing namespace")
	}
	if len(strategy.AddressSpaceName) == 0 {
		return "", "", 0, fmt.Errorf("missing address space name")
	}

	return endpointName, portName, endpointMode, nil
}

func extractEndpointInformation(
	endpointName string,
	endpointMode iotv1alpha1.EndpointMode,
	portName string,
	credentials *iotv1alpha1.Credentials,
	addressSpace *enmassev1beta1.AddressSpace,
	forceTls *bool,
) (*iotv1alpha1.ExternalDownstreamStrategy, error) {

	if !addressSpace.Status.IsReady {
		// not ready, yet … wait
		return nil, util.NewObjectNotReadyYetError(addressSpace)
	}

	endpoint := new(iotv1alpha1.ExternalDownstreamStrategy)

	endpoint.Credentials = *credentials

	foundEndpoint := false
	for _, es := range addressSpace.Status.EndpointStatus {
		if es.Name != endpointName {
			continue
		}

		foundEndpoint = true

		var ports []enmassev1beta1.Port

		switch endpointMode {
		case iotv1alpha1.Service:
			endpoint.Host = es.ServiceHost
			ports = es.ServicePorts
		case iotv1alpha1.External:
			endpoint.Host = es.ExternalHost
			ports = es.ExternalPorts
		}

		log.V(2).Info("Ports to scan", "ports", ports)

		endpoint.Certificate = addressSpace.Status.CACertificate

		foundPort := false
		for _, port := range ports {
			if port.Name == portName {
				foundPort = true

				endpoint.Port = port.Port

				tls, err := isTls(addressSpace, &es, &port, forceTls)
				if err != nil {
					return nil, err
				}
				endpoint.TLS = tls

			}
		}

		if !foundPort {
			return nil, fmt.Errorf("unable to find port: %s for endpoint: %s", portName, endpointName)
		}

	}

	if !foundEndpoint {
		return nil, fmt.Errorf("unable to find endpoint: %s", endpointName)
	}

	return endpoint, nil
}

func findEndpointSpec(addressSpace *enmassev1beta1.AddressSpace, endpointStatus *enmassev1beta1.EndpointStatus) *enmassev1beta1.EndpointSpec {
	for _, end := range addressSpace.Spec.Ednpoints {
		if end.Name != endpointStatus.Name {
			continue
		}
		return &end
	}
	return nil
}

// get a an estimate if TLS should be enabled for a port, or not
func isTls(
	addressSpace *enmassev1beta1.AddressSpace,
	endpointStatus *enmassev1beta1.EndpointStatus,
	_port *enmassev1beta1.Port,
	forceTls *bool) (bool, error) {

	if forceTls != nil {
		return *forceTls, nil
	}

	endpoint := findEndpointSpec(addressSpace, endpointStatus)

	if endpoint == nil {
		return false, fmt.Errorf("failed to locate endpoint named: %v", endpointStatus.Name)
	}

	if endpointStatus.Certificate != nil {
		// if there is a certificate, enable tls
		return true, nil
	}

	if endpoint.Expose != nil {
		// anything set as tls termination counts as tls enabled = true
		return len(endpoint.Expose.RouteTlsTermination) > 0, nil
	}

	return false, nil

}
