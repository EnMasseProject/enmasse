/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

// Code generated by client-gen. DO NOT EDIT.

package fake

import (
	v1beta2 "github.com/enmasseproject/enmasse/pkg/client/clientset/versioned/typed/enmasse/v1beta2"
	rest "k8s.io/client-go/rest"
	testing "k8s.io/client-go/testing"
)

type FakeEnmasseV1beta2 struct {
	*testing.Fake
}

func (c *FakeEnmasseV1beta2) MessagingAddresses(namespace string) v1beta2.MessagingAddressInterface {
	return &FakeMessagingAddresses{c, namespace}
}

func (c *FakeEnmasseV1beta2) MessagingAddressPlans(namespace string) v1beta2.MessagingAddressPlanInterface {
	return &FakeMessagingAddressPlans{c, namespace}
}

func (c *FakeEnmasseV1beta2) MessagingEndpoints(namespace string) v1beta2.MessagingEndpointInterface {
	return &FakeMessagingEndpoints{c, namespace}
}

func (c *FakeEnmasseV1beta2) MessagingInfrastructures(namespace string) v1beta2.MessagingInfrastructureInterface {
	return &FakeMessagingInfrastructures{c, namespace}
}

func (c *FakeEnmasseV1beta2) MessagingPlans(namespace string) v1beta2.MessagingPlanInterface {
	return &FakeMessagingPlans{c, namespace}
}

func (c *FakeEnmasseV1beta2) MessagingTenants(namespace string) v1beta2.MessagingTenantInterface {
	return &FakeMessagingTenants{c, namespace}
}

// RESTClient returns a RESTClient that is used to communicate
// with API server by this client implementation.
func (c *FakeEnmasseV1beta2) RESTClient() rest.Interface {
	var ret *rest.RESTClient
	return ret
}
