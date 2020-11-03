/*
* Copyright 2019, EnMasse authors.
* License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package resolvers

import (
	"context"
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/accesscontroller"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/cache"
	"github.com/enmasseproject/enmasse/pkg/consolegraphql/server"
	v12 "github.com/openshift/api/route/v1"
	"github.com/stretchr/testify/assert"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"testing"
)

func newTestMessagingEndpointResolver(t *testing.T) (*Resolver, context.Context) {
	objectCache, err := cache.CreateObjectCache()
	assert.NoError(t, err, "failed to create object cache")

	resolver := Resolver{}
	resolver.Cache = objectCache

	requestState := &server.RequestState{
		AccessController: accesscontroller.NewAllowAllAccessController(),
	}

	ctx := server.ContextWithRequestState(requestState, context.TODO())
	return &resolver, ctx
}

func TestQueryMessagingEndpointNoEndpoints(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	addressSpace := createAddressSpace("myas", "mynamespace")
	err := r.Cache.Add(addressSpace)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 0, objs.Total, "Unexpected number of endpoints")
}

func TestQueryMessagingEndpointClusterOnly(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	endpointSpec := v1beta1.EndpointSpec{
		Name:    "myendpoint",
		Service: v1beta1.EndpointServiceTypeMessaging,
	}
	endpointStatus := v1beta1.EndpointStatus{
		Name:        "myendpoint",
		ServiceHost: "messaging-queuespace.enmasse-infra.svc",
		ServicePorts: []v1beta1.Port{
			{Name: "amqps", Port: 5671},
		},
	}
	addressSpace := createAddressSpace("myas", "mynamespace", withEndpoint(endpointSpec, endpointStatus))
	err := r.Cache.Add(addressSpace)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 1, objs.Total, "Unexpected number of messagingEndpoints")

	messagingEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging.cluster",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging.cluster"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Cluster:   &v1beta2.MessagingEndpointSpecCluster{},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeCluster,
			Host:  "messaging-queuespace.enmasse-infra.svc",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 5671},
			},
		},
	}
	assert.Equal(t, messagingEndpoint, objs.MessagingEndpoints[0], "Unexpected messagingEndpoint")
}

func TestQueryMessagingEndpointAmqpsRouteAndCluster(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	endpointSpec := v1beta1.EndpointSpec{
		Name:    "messaging",
		Service: v1beta1.EndpointServiceTypeMessaging,
		Expose: &v1beta1.ExposeSpec{
			Type:                v1beta1.ExposeTypeRoute,
			RouteServicePort:    v1beta1.RouteServicePortAmqps,
			RouteTlsTermination: v1beta1.RouteTlsTerminationPassthrough,
		},
	}
	endpointStatus := v1beta1.EndpointStatus{
		Name:        "messaging",
		ServiceHost: "messaging-queuespace.enmasse-infra.svc",
		ServicePorts: []v1beta1.Port{
			{Name: "amqps", Port: 5671},
		},
		ExternalHost: "messaging-queuespace-enmasse-infra.apps-crc.testing",
		ExternalPorts: []v1beta1.Port{
			{Name: "amqps", Port: 443},
		},
	}
	addressSpace := createAddressSpace("myas", "mynamespace", withEndpoint(endpointSpec, endpointStatus))
	err := r.Cache.Add(addressSpace)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 2, objs.Total, "Unexpected number of endpoints")

	clusterEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging.cluster",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging.cluster"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Cluster:   &v1beta2.MessagingEndpointSpecCluster{},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeCluster,
			Host:  "messaging-queuespace.enmasse-infra.svc",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 5671},
			},
		},
	}
	assert.Equal(t, clusterEndpoint, objs.MessagingEndpoints[0], "Unexpected messagingEndpoints")

	routeEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Route:     &v1beta2.MessagingEndpointSpecRoute{},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeRoute,
			Host:  "messaging-queuespace-enmasse-infra.apps-crc.testing",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 443},
			},
		},
	}
	assert.Equal(t, routeEndpoint, objs.MessagingEndpoints[1], "Unexpected messagingEndpoints")
}

func TestQueryMessagingEndpointAmqpWssRouteAndCluster(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	endpointSpec := v1beta1.EndpointSpec{
		Name:    "messaging-wss",
		Service: v1beta1.EndpointServiceTypeMessaging,
		Expose: &v1beta1.ExposeSpec{
			Type:                v1beta1.ExposeTypeRoute,
			RouteServicePort:    v1beta1.RouteServicePortHttps,
			RouteTlsTermination: v1beta1.RouteTlsTerminationReencrypt,
		},
	}
	endpointStatus := v1beta1.EndpointStatus{
		Name:        "messaging-wss",
		ServiceHost: "messaging-queuespace.enmasse-infra.svc",
		ServicePorts: []v1beta1.Port{
			{Name: "amqp-wss", Port: 443},
		},
		ExternalHost: "messaging-wss-queuespace-enmasse-infra.apps-crc.testing",
		ExternalPorts: []v1beta1.Port{
			{Name: "https", Port: 443},
		},
	}
	addressSpace := createAddressSpace("myas", "mynamespace", withEndpoint(endpointSpec, endpointStatus))
	err := r.Cache.Add(addressSpace)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 2, objs.Total, "Unexpected number of endpoints")

	clusterEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging.cluster",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging.cluster"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Cluster:   &v1beta2.MessagingEndpointSpecCluster{},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPWSS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeCluster,
			Host:  "messaging-queuespace.enmasse-infra.svc",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqp-wss", Protocol: v1beta2.MessagingProtocolAMQPWSS, Port: 443},
			},
		},
	}
	assert.Equal(t, clusterEndpoint, objs.MessagingEndpoints[0], "Unexpected messagingEndpoints")

	reencrypt := v12.TLSTerminationReencrypt
	routeEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging-wss",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging-wss"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Route: &v1beta2.MessagingEndpointSpecRoute{
				TlsTermination: &reencrypt,
			},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPWSS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeRoute,
			Host:  "messaging-wss-queuespace-enmasse-infra.apps-crc.testing",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqp-wss", Protocol: v1beta2.MessagingProtocolAMQPWSS, Port: 443},
			},
		},
	}
	assert.Equal(t, routeEndpoint, objs.MessagingEndpoints[1], "Unexpected messagingEndpoints")

}

func TestQueryMessagingEndpointSharedServiceAndTwoRoutes(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	serviceName := v1beta1.EndpointServiceTypeMessaging
	serviceHost := "messaging-queuespace.enmasse-infra.svc"
	servicePorts := []v1beta1.Port{
		{Name: "amqps", Port: 5671},
		{Name: "amqp", Port: 5672},
		{Name: "amqp-wss", Port: 443},
	}

	endpointSpec1 := v1beta1.EndpointSpec{
		Name:    "messaging",
		Service: serviceName,
		Expose: &v1beta1.ExposeSpec{
			Type:                v1beta1.ExposeTypeRoute,
			RouteServicePort:    v1beta1.RouteServicePortAmqps,
			RouteTlsTermination: v1beta1.RouteTlsTerminationPassthrough,
		},
	}
	endpointStatus1 := v1beta1.EndpointStatus{
		Name:         "messaging",
		ServiceHost:  serviceHost,
		ServicePorts: servicePorts,
		ExternalHost: "messaging-queuespace-enmasse-infra.apps-crc.testing",
		ExternalPorts: []v1beta1.Port{
			{Name: "amqps", Port: 443},
		},
	}

	endpointSpec2 := v1beta1.EndpointSpec{
		Name:    "messaging-wss",
		Service: serviceName,
		Expose: &v1beta1.ExposeSpec{
			Type:                "route",
			RouteServicePort:    "https",
			RouteTlsTermination: "reencrypt",
		},
	}
	endpointStatus2 := v1beta1.EndpointStatus{
		Name:         "messaging-wss",
		ServiceHost:  serviceHost,
		ServicePorts: servicePorts,
		ExternalHost: "messaging-wss-queuespace-enmasse-infra.apps-crc.testing",
		ExternalPorts: []v1beta1.Port{
			{Name: "https", Port: 443},
		},
	}

	addressSpace := createAddressSpace("myas", "mynamespace", withEndpoint(endpointSpec1, endpointStatus1), withEndpoint(endpointSpec2, endpointStatus2))
	err := r.Cache.Add(addressSpace)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 3, objs.Total, "Unexpected number of endpoints")

	clusterEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging.cluster",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging.cluster"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Cluster:   &v1beta2.MessagingEndpointSpecCluster{},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS, v1beta2.MessagingProtocolAMQP, v1beta2.MessagingProtocolAMQPWSS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeCluster,
			Host:  serviceHost,
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 5671},
				{Name: "amqp", Protocol: v1beta2.MessagingProtocolAMQP, Port: 5672},
				{Name: "amqp-wss", Protocol: v1beta2.MessagingProtocolAMQPWSS, Port: 443},
			},
		},
	}
	assert.Equal(t, clusterEndpoint, objs.MessagingEndpoints[0], "Unexpected messagingEndpoints")

	amqpsRouteEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Route:     &v1beta2.MessagingEndpointSpecRoute{},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeRoute,
			Host:  "messaging-queuespace-enmasse-infra.apps-crc.testing",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 443},
			},
		},
	}
	assert.Equal(t, amqpsRouteEndpoint, objs.MessagingEndpoints[1], "Unexpected messagingEndpoints")

	reencrypt := v12.TLSTerminationReencrypt
	amqpwssRouteEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging-wss",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging-wss"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Route: &v1beta2.MessagingEndpointSpecRoute{
				TlsTermination: &reencrypt,
			},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPWSS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeRoute,
			Host:  "messaging-wss-queuespace-enmasse-infra.apps-crc.testing",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqp-wss", Protocol: v1beta2.MessagingProtocolAMQPWSS, Port: 443},
			},
		},
	}
	assert.Equal(t, amqpwssRouteEndpoint, objs.MessagingEndpoints[2], "Unexpected messagingEndpoints")

}

func TestQueryMessagingEndpointLoadbalancerAndCluster(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	endpointSpec := v1beta1.EndpointSpec{
		Name:    "messaging",
		Service: v1beta1.EndpointServiceTypeMessaging,
		Expose: &v1beta1.ExposeSpec{
			Type:                     v1beta1.ExposeTypeLoadBalancer,
			LoadBalancerPorts:        []string{"amqp", "amqps"},
			LoadBalancerSourceRanges: []string{"10.0.0.0/8"},
		},
	}
	endpointStatus := v1beta1.EndpointStatus{
		Name:        "messaging",
		ServiceHost: "messaging-queuespace.enmasse-infra.svc",
		ServicePorts: []v1beta1.Port{
			{Name: "amqps", Port: 5671},
		},
		ExternalPorts: []v1beta1.Port{
			{Name: "amqps", Port: 443},
		},
	}
	addressSpace := createAddressSpace("myas", "mynamespace", withEndpoint(endpointSpec, endpointStatus))
	err := r.Cache.Add(addressSpace)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 2, objs.Total, "Unexpected number of endpoints")

	clusterEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging.cluster",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging.cluster"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Cluster:   &v1beta2.MessagingEndpointSpecCluster{},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeCluster,
			Host:  "messaging-queuespace.enmasse-infra.svc",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 5671},
			},
		},
	}
	assert.Equal(t, clusterEndpoint, objs.MessagingEndpoints[0], "Unexpected messagingEndpoints")

	loadbalancerEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			LoadBalancer: &v1beta2.MessagingEndpointSpecLoadBalancer{},
			Protocols:    []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeLoadBalancer,
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 443},
			},
		},
	}
	assert.Equal(t, loadbalancerEndpoint, objs.MessagingEndpoints[1], "Unexpected messagingEndpoints")
}

func TestFilterMessagingEndpoints(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	as1EndpointSpec := v1beta1.EndpointSpec{
		Name:    "messaging",
		Service: v1beta1.EndpointServiceTypeMessaging,
	}
	endpointStatus := v1beta1.EndpointStatus{
		Name:        "messaging",
		ServiceHost: "messaging-queuespace.enmasse-infra.svc",
		ServicePorts: []v1beta1.Port{
			{Name: "amqps", Port: 5671},
		},
	}
	as1 := createAddressSpace("myas1", "mynamespace", withEndpoint(as1EndpointSpec, endpointStatus))
	as2 := createAddressSpace("myas2", "mynamespace", withEndpoint(as1EndpointSpec, endpointStatus))
	err := r.Cache.Add(as1, as2)
	assert.NoError(t, err)

	clusterFilter := fmt.Sprint("`$.metadata.name` LIKE 'myas1.%'")
	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, &clusterFilter, nil)
	assert.NoError(t, err)

	assert.Equal(t, 1, objs.Total, "Unexpected number of endpoints")

}

func TestQueryMessagingEndpointTlsSelfSigned(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	endpointSpec := v1beta1.EndpointSpec{
		Name:    "myendpoint",
		Service: v1beta1.EndpointServiceTypeMessaging,
		Certificate: &v1beta1.CertificateSpec{
			Provider: v1beta1.CertificateProviderTypeCertSelfsigned,
		},
	}
	endpointStatus := v1beta1.EndpointStatus{
		Name:        "myendpoint",
		ServiceHost: "messaging-queuespace.enmasse-infra.svc",
		ServicePorts: []v1beta1.Port{
			{Name: "amqps", Port: 5671},
		},
	}
	addressSpace := createAddressSpace("myas", "mynamespace",
		withEndpoint(endpointSpec, endpointStatus),
		withCACertificate([]byte("cacert")))
	err := r.Cache.Add(addressSpace)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 1, objs.Total, "Unexpected number of messagingEndpoints")

	messagingEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging.cluster",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging.cluster"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Tls: &v1beta2.MessagingEndpointSpecTls{
				Selfsigned: &v1beta2.MessagingEndpointSpecTlsSelfsigned{},
			},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS},
			Cluster:   &v1beta2.MessagingEndpointSpecCluster{},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeCluster,
			Host:  "messaging-queuespace.enmasse-infra.svc",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 5671},
			},
			Tls: &v1beta2.MessagingEndpointStatusTls{
				CaCertificate: "cacert",
			},
		},
	}
	assert.Equal(t, messagingEndpoint, objs.MessagingEndpoints[0], "Unexpected messagingEndpoint")
}

func TestQueryMessagingEndpointTlsOpenShift(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	endpointSpec := v1beta1.EndpointSpec{
		Name:    "myendpoint",
		Service: v1beta1.EndpointServiceTypeMessaging,
		Certificate: &v1beta1.CertificateSpec{
			Provider: v1beta1.CertificateProviderTypeCertOpenshift,
		},
	}
	endpointStatus := v1beta1.EndpointStatus{
		Name:        "myendpoint",
		ServiceHost: "messaging-queuespace.enmasse-infra.svc",
		ServicePorts: []v1beta1.Port{
			{Name: "amqps", Port: 5671},
		},
	}
	addressSpace := createAddressSpace("myas", "mynamespace",
		withEndpoint(endpointSpec, endpointStatus))
	err := r.Cache.Add(addressSpace)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 1, objs.Total, "Unexpected number of messagingEndpoints")

	messagingEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging.cluster",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging.cluster"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Tls: &v1beta2.MessagingEndpointSpecTls{
				Openshift: &v1beta2.MessagingEndpointSpecTlsOpenshift{},
			},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS},
			Cluster:   &v1beta2.MessagingEndpointSpecCluster{},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeCluster,
			Host:  "messaging-queuespace.enmasse-infra.svc",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 5671},
			},
			Tls: &v1beta2.MessagingEndpointStatusTls{},
		},
	}
	assert.Equal(t, messagingEndpoint, objs.MessagingEndpoints[0], "Unexpected messagingEndpoint")
}

func TestQueryMessagingEndpointTlsExternal(t *testing.T) {
	r, ctx := newTestMessagingEndpointResolver(t)

	endpointSpec := v1beta1.EndpointSpec{
		Name:    "myendpoint",
		Service: v1beta1.EndpointServiceTypeMessaging,
		Certificate: &v1beta1.CertificateSpec{
			Provider: v1beta1.CertificateProviderTypeCertBundle,
			TlsKey:   "base64PEM",
			TlsCert:  "base64PEM",
		},
	}
	endpointStatus := v1beta1.EndpointStatus{
		Name:        "myendpoint",
		ServiceHost: "messaging-queuespace.enmasse-infra.svc",
		ServicePorts: []v1beta1.Port{
			{Name: "amqps", Port: 5671},
		},
	}
	addressSpace := createAddressSpace("myas", "mynamespace",
		withEndpoint(endpointSpec, endpointStatus))
	err := r.Cache.Add(addressSpace)
	assert.NoError(t, err)

	objs, err := r.Query().MessagingEndpoints(ctx, nil, nil, nil, nil)
	assert.NoError(t, err)

	assert.Equal(t, 1, objs.Total, "Unexpected number of messagingEndpoints")

	messagingEndpoint := &v1beta2.MessagingEndpoint{
		ObjectMeta: v1.ObjectMeta{
			Namespace: addressSpace.Namespace,
			Name:      "myas.messaging.cluster",
			UID:       createStableUuidV5(addressSpace.ObjectMeta, "myas.messaging.cluster"),
		},
		Spec: v1beta2.MessagingEndpointSpec{
			Tls: &v1beta2.MessagingEndpointSpecTls{
				External: &v1beta2.MessagingEndpointSpecTlsExternal{
					Key: v1beta2.InputValue{
						Value: string(endpointSpec.Certificate.TlsKey),
					},
					Certificate: v1beta2.InputValue{
						Value: string(endpointSpec.Certificate.TlsCert),
					},
				},
			},
			Protocols: []v1beta2.MessagingEndpointProtocol{v1beta2.MessagingProtocolAMQPS},
			Cluster:   &v1beta2.MessagingEndpointSpecCluster{},
		},
		Status: v1beta2.MessagingEndpointStatus{
			Phase: v1beta2.MessagingEndpointActive,
			Type:  v1beta2.MessagingEndpointTypeCluster,
			Host:  "messaging-queuespace.enmasse-infra.svc",
			Ports: []v1beta2.MessagingEndpointPort{
				{Name: "amqps", Protocol: v1beta2.MessagingProtocolAMQPS, Port: 5671},
			},
			Tls: &v1beta2.MessagingEndpointStatusTls{},
		},
	}
	assert.Equal(t, messagingEndpoint, objs.MessagingEndpoints[0], "Unexpected messagingEndpoint")
}
