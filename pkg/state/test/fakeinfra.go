/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package test

import (
	"github.com/enmasseproject/enmasse/pkg/state"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
)

type FakeClient struct {
	Routers []state.Host
	Brokers []state.Host
}

var _ state.InfraClient = &FakeClient{}
var _ state.ClientManager = &FakeManager{}

type FakeManager struct {
	Clients map[string]*FakeClient
}

func NewFakeManager() *FakeManager {
	return &FakeManager{
		Clients: make(map[string]*FakeClient),
	}
}

func (m *FakeManager) GetClient(infra *v1beta2.MessagingInfrastructure) state.InfraClient {
	client, exists := m.Clients[infra.Name]
	if !exists {
		client = &FakeClient{
			Routers: make([]state.Host, 0),
			Brokers: make([]state.Host, 0),
		}
		m.Clients[infra.Name] = client
	}
	return client
}

func (m *FakeManager) DeleteClient(infra *v1beta2.MessagingInfrastructure) error {
	delete(m.Clients, infra.Name)
	return nil
}

func (i *FakeClient) Start() {
}

func (i *FakeClient) SyncAll(routers []state.Host, brokers []state.Host) ([]state.ConnectorStatus, error) {
	i.Routers = routers
	i.Brokers = brokers
	return nil, nil
}

func (i *FakeClient) AllocatePorts(endpoint *v1beta2.MessagingEndpoint, protocols []v1beta2.MessagingEndpointProtocol) error {
	return nil
}

func (i *FakeClient) FreePorts(endpoint *v1beta2.MessagingEndpoint) {
}

func (i *FakeClient) SyncEndpoint(endpoint *v1beta2.MessagingEndpoint) error {
	return nil
}

func (i *FakeClient) DeleteEndpoint(endpoint *v1beta2.MessagingEndpoint) error {
	return nil
}

func (i *FakeClient) ScheduleAddress(address *v1beta2.MessagingAddress, scheduler state.Scheduler) error {
	return nil
}

func (i *FakeClient) SyncAddress(address *v1beta2.MessagingAddress) error {
	return nil
}

func (i *FakeClient) DeleteAddress(address *v1beta2.MessagingAddress) error {
	return nil
}

func (i *FakeClient) Shutdown() error {
	return nil
}
