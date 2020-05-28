/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"crypto/sha1"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	"github.com/google/uuid"
	routev1 "github.com/openshift/api/route/v1"
	"golang.org/x/net/context"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"strings"
)

func (r *queryResolver) MessagingEndpoints(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*MessagingEndpointQueryResultConsoleapiEnmasseIoV1beta1, error) {
	requestState := server.GetRequestStateFromContext(ctx)
	viewFilter := requestState.AccessController.ViewFilter()

	filterFunc, keyElements, err := BuildFilter(filter, "$.metadata.namespace")
	if err != nil {
		return nil, err
	}

	orderer, err := BuildOrderer(orderBy)
	if err != nil {
		return nil, err
	}

	addressSpaces, e := r.Cache.Get(cache.PrimaryObjectIndex, fmt.Sprintf("AddressSpace/%s", keyElements), viewFilter)
	if e != nil {
		return nil, e
	}

	endpoints := make([]*v1beta2.MessagingEndpoint, 0)

	for i := range addressSpaces {
		as := addressSpaces[i].(*consolegraphql.AddressSpaceHolder).AddressSpace

		serviceFound := false
		for _, spec := range as.Spec.Endpoints {
			var specTls, statusTls = buildMessagingEndpointSpecTls(spec, as.Status)

			if spec.Name != "" {
				var clusterEndpoint *v1beta2.MessagingEndpoint
				var exposeEndpoint *v1beta2.MessagingEndpoint
				status := findStatus(as.Status, spec.Name)

				if spec.Service != "" && !serviceFound {
					clusterEndpoint = buildClusterEndpoint(as, spec, status, specTls, statusTls)
					serviceFound = true
				}
				if spec.Expose != nil {
					exposeEndpoint = buildExposedEndpoint(as, spec, status, specTls, statusTls)
				}

				if match, err := applyFilter(filterFunc, clusterEndpoint); err != nil {
					return nil, err
				} else if match {
					endpoints = append(endpoints, clusterEndpoint)
				}
				if match, err := applyFilter(filterFunc, exposeEndpoint); err != nil {
					return nil, err
				} else if match {
					endpoints = append(endpoints, exposeEndpoint)
				}
			}
		}
	}

	e = orderer(endpoints)
	if e != nil {
		return nil, e
	}

	lower, upper := CalcLowerUpper(offset, first, len(endpoints))
	paged := endpoints[lower:upper]

	mer := &MessagingEndpointQueryResultConsoleapiEnmasseIoV1beta1{
		Total:              len(endpoints),
		MessagingEndpoints: paged,
	}

	return mer, nil
}

func buildExposedEndpoint(addressSpace v1beta1.AddressSpace, spec v1beta1.EndpointSpec, status *v1beta1.EndpointStatus, specTls *v1beta2.MessagingEndpointSpecTls, statusTls *v1beta2.MessagingEndpointStatusTls) *v1beta2.MessagingEndpoint {
	var exposeEndpoint *v1beta2.MessagingEndpoint
	host := ""
	ports := make([]v1beta2.MessagingEndpointPort, 0)
	protocols := make([]v1beta2.MessagingEndpointProtocol, 0)
	phase := v1beta2.MessagingEndpointConfiguring

	var statusType v1beta2.MessagingEndpointType
	var route *v1beta2.MessagingEndpointSpecRoute
	var loadbalancer *v1beta2.MessagingEndpointSpecLoadBalancer

	if spec.Expose.Type == v1beta1.ExposeTypeRoute {
		statusType = v1beta2.MessagingEndpointTypeRoute
		var termination *routev1.TLSTerminationType
		if spec.Expose.RouteTlsTermination == v1beta1.RouteTlsTerminationReencrypt {
			reencrypt := routev1.TLSTerminationReencrypt
			termination = &reencrypt
		}

		route = &v1beta2.MessagingEndpointSpecRoute{
			TlsTermination: termination,
		}
		if status != nil {
			if status.ExternalHost != "" {
				host = status.ExternalHost
				phase = v1beta2.MessagingEndpointActive
			}

			if len(status.ExternalPorts) > 0 {
				externalPort := status.ExternalPorts[0]
				var protocol v1beta2.MessagingEndpointProtocol
				switch externalPort.Name {
				case "https":
					protocol = v1beta2.MessagingProtocolAMQPWSS
				default:
					protocol = v1beta2.MessagingProtocolAMQPS
				}
				port := v1beta2.MessagingEndpointPort{
					Name:     strings.ToLower(string(protocol)),
					Protocol: protocol,
					Port:     int(externalPort.Port),
				}
				ports = append(ports, port)
				protocols = append(protocols, protocol)
			}
		}
	} else if spec.Expose.Type == v1beta1.ExposeTypeLoadBalancer {
		statusType = v1beta2.MessagingEndpointTypeLoadBalancer

		loadbalancer = &v1beta2.MessagingEndpointSpecLoadBalancer{}

		if status != nil {
			for _, p := range status.ExternalPorts {
				protocol := toMessagingEndpointProtocol(p.Name)
				port := v1beta2.MessagingEndpointPort{
					Name:     p.Name,
					Protocol: protocol,
					Port:     int(p.Port),
				}
				ports = append(ports, port)
				protocols = append(protocols, protocol)
			}
			phase = v1beta2.MessagingEndpointActive
		}
	}

	endpointName := fmt.Sprintf("%s.%s", addressSpace.Name, spec.Name)
	exposeEndpoint = &v1beta2.MessagingEndpoint{
		ObjectMeta: metav1.ObjectMeta{
			Name:      endpointName,
			Namespace: addressSpace.Namespace,
			UID:       createStableUuidV5(addressSpace.ObjectMeta, endpointName),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Route:        route,
			LoadBalancer: loadbalancer,
			Protocols:    protocols,
			Tls:          specTls,
		},
		Status: v1beta2.MessagingEndpointStatus{
			Type:  statusType,
			Phase: phase,
			Host:  host,
			Ports: ports,
			Tls:   statusTls,
		},
	}
	return exposeEndpoint
}

func buildClusterEndpoint(addressSpace v1beta1.AddressSpace, spec v1beta1.EndpointSpec, status *v1beta1.EndpointStatus, specTls *v1beta2.MessagingEndpointSpecTls, statusTls *v1beta2.MessagingEndpointStatusTls) *v1beta2.MessagingEndpoint {
	var clusterEndpoint *v1beta2.MessagingEndpoint
	serviceName := fmt.Sprintf("%s.%s.cluster", addressSpace.Name, spec.Service)
	host := ""
	ports := make([]v1beta2.MessagingEndpointPort, 0)
	protocols := make([]v1beta2.MessagingEndpointProtocol, 0)
	phase := v1beta2.MessagingEndpointConfiguring
	if status != nil {
		if status.ServiceHost != "" {
			host = status.ServiceHost
			phase = v1beta2.MessagingEndpointActive
		}
		for _, p := range status.ServicePorts {
			protocol := toMessagingEndpointProtocol(p.Name)
			port := v1beta2.MessagingEndpointPort{
				Name:     p.Name,
				Protocol: protocol,
				Port:     int(p.Port),
			}
			ports = append(ports, port)
			protocols = append(protocols, protocol)
		}
	}
	clusterEndpoint = &v1beta2.MessagingEndpoint{
		ObjectMeta: metav1.ObjectMeta{
			Name:      serviceName,
			Namespace: addressSpace.Namespace,
			UID:       createStableUuidV5(addressSpace.ObjectMeta, serviceName),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Cluster:   &v1beta2.MessagingEndpointSpecCluster{},
			Protocols: protocols,
			Tls:       specTls,
		},
		Status: v1beta2.MessagingEndpointStatus{
			Type:  v1beta2.MessagingEndpointTypeCluster,
			Phase: phase,
			Host:  host,
			Ports: ports,
			Tls:   statusTls,
		},
	}
	return clusterEndpoint
}

func buildMessagingEndpointSpecTls(endpointSpec v1beta1.EndpointSpec, status v1beta1.AddressSpaceStatus) (*v1beta2.MessagingEndpointSpecTls, *v1beta2.MessagingEndpointStatusTls) {
	var specTls *v1beta2.MessagingEndpointSpecTls
	var statusTls *v1beta2.MessagingEndpointStatusTls

	if endpointSpec.Certificate != nil {

		var tlsSelfSigned *v1beta2.MessagingEndpointSpecTlsSelfsigned
		var tlsOpenshift *v1beta2.MessagingEndpointSpecTlsOpenshift
		var tlsExternal *v1beta2.MessagingEndpointSpecTlsExternal

		switch endpointSpec.Certificate.Provider {
		case v1beta1.CertificateProviderTypeCertSelfsigned:
			tlsSelfSigned = &v1beta2.MessagingEndpointSpecTlsSelfsigned{}
		case v1beta1.CertificateProviderTypeCertOpenshift:
			tlsOpenshift = &v1beta2.MessagingEndpointSpecTlsOpenshift{}
		case v1beta1.CertificateProviderTypeCertBundle:
			var key v1beta2.InputValue
			var cert v1beta2.InputValue
			if len(endpointSpec.Certificate.TlsKey) > 0 && len(endpointSpec.Certificate.TlsCert) > 0 {
				key = v1beta2.InputValue{
					Value: string(endpointSpec.Certificate.TlsKey), // Base64 decode?
				}
				cert = v1beta2.InputValue{
					Value: string(endpointSpec.Certificate.TlsCert),
				}
			}
			tlsExternal = &v1beta2.MessagingEndpointSpecTlsExternal{
				Key:         key,
				Certificate: cert,
			}
		case v1beta1.CertificateProviderTypeWildcard:
			// TODO - wildcard secret name not accessible - it is hardcoded config to the ASC.
		}
		specTls = &v1beta2.MessagingEndpointSpecTls{
			Selfsigned: tlsSelfSigned,
			Openshift:  tlsOpenshift,
			External:   tlsExternal,
		}

		statusTls = &v1beta2.MessagingEndpointStatusTls{
			CaCertificate: string(status.CACertificate),
			// TODO extract cert details.
		}
	}
	return specTls, statusTls
}

func applyFilter(filterFunc cache.ObjectFilter, clusterEndpoint *v1beta2.MessagingEndpoint) (bool, error) {
	if clusterEndpoint == nil {
		return false, nil
	}

	if filterFunc == nil {
		return true, nil
	}

	match, _, err := filterFunc(clusterEndpoint)
	return match, err
}

func toMessagingEndpointProtocol(name string) v1beta2.MessagingEndpointProtocol {
	switch name {
	case "amqp":
		return v1beta2.MessagingProtocolAMQP
	case "amqps":
		return v1beta2.MessagingProtocolAMQPS
	case "amqp-ws":
		return v1beta2.MessagingProtocolAMQP
	case "amqp-wss":
		return v1beta2.MessagingProtocolAMQPWSS
	default:
		return v1beta2.MessagingProtocolAMQP
	}
}

func findStatus(status v1beta1.AddressSpaceStatus, name string) *v1beta1.EndpointStatus {
	for _, status := range status.EndpointStatus {
		if status.Name == name {
			return &status
		}
	}
	return nil
}

func createStableUuidV5(m metav1.ObjectMeta, endpointName string) (uid types.UID) {
	h := sha1.New()
	if m.Namespace != "" {
		h.Write([]byte(m.Namespace))
	}
	if m.UID != "" {
		h.Write([]byte(m.UID))
	}
	h.Write([]byte(endpointName))
	s := h.Sum(nil)
	var type5uuid uuid.UUID
	copy(type5uuid[:], s)
	type5uuid[6] = (type5uuid[6] & 0x0f) | uint8((5&0xf)<<4)
	type5uuid[8] = (type5uuid[8] & 0x3f) | 0x80 // RFC 4122 variant
	return types.UID(type5uuid.String())
}
