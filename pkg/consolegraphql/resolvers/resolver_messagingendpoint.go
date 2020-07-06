/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"crypto/sha1"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
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

func (r *queryResolver) MessagingEndpoints(ctx context.Context, first *int, offset *int, filter *string, orderBy *string) (*MessagingEndpointQueryResultConsoleapiEnmasseIoV1, error) {
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

	endpoints := make([]*v1.MessagingEndpoint, 0)

	for i := range addressSpaces {
		as := addressSpaces[i].(*consolegraphql.AddressSpaceHolder).AddressSpace

		serviceFound := false
		for _, spec := range as.Spec.Endpoints {
			var specTls, statusTls = buildMessagingEndpointSpecTls(spec, as.Status)

			if spec.Name != "" {
				var clusterEndpoint *v1.MessagingEndpoint
				var exposeEndpoint *v1.MessagingEndpoint
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

	mer := &MessagingEndpointQueryResultConsoleapiEnmasseIoV1{
		Total:              len(endpoints),
		MessagingEndpoints: paged,
	}

	return mer, nil
}

func applyFilter(filterFunc cache.ObjectFilter, clusterEndpoint *v1.MessagingEndpoint) (bool, error) {
	if clusterEndpoint == nil {
		return false, nil
	}

	if filterFunc == nil {
		return true, nil
	}

	match, _, err := filterFunc(clusterEndpoint)
	return match, err
}

func toMessagingEndpointProtocol(name string) v1.MessagingEndpointProtocol {
	switch name {
	case "amqp":
		return v1.MessagingProtocolAMQP
	case "amqps":
		return v1.MessagingProtocolAMQPS
	case "amqp-ws":
		return v1.MessagingProtocolAMQP
	case "amqp-wss":
		return v1.MessagingProtocolAMQPWSS
	default:
		return v1.MessagingProtocolAMQP
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
