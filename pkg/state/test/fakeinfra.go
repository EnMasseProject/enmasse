/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package test

import (
	"crypto/tls"

	"github.com/enmasseproject/enmasse/pkg/state"
	. "github.com/enmasseproject/enmasse/pkg/state/common"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
)

type FakeClient struct {
	Routers []Host
	Brokers []Host
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

func (m *FakeManager) GetClient(infra *v1.MessagingInfrastructure) state.InfraClient {
	client, exists := m.Clients[infra.Name]
	if !exists {
		client = &FakeClient{
			Routers: make([]Host, 0),
			Brokers: make([]Host, 0),
		}
		m.Clients[infra.Name] = client
	}
	return client
}

func (m *FakeManager) DeleteClient(infra *v1.MessagingInfrastructure) error {
	delete(m.Clients, infra.Name)
	return nil
}

func (i *FakeClient) Start() {
}

func (i *FakeClient) DeleteBroker(host string) error {
	return nil
}

func (i *FakeClient) SyncAll(routers []Host, brokers []Host, tlsConfig *tls.Config) ([]state.ConnectorStatus, error) {
	i.Routers = routers
	i.Brokers = brokers
	return nil, nil
}

func (i *FakeClient) AllocatePorts(endpoint *v1.MessagingEndpoint, protocols []v1.MessagingEndpointProtocol) error {
	return nil
}

func (i *FakeClient) FreePorts(endpoint *v1.MessagingEndpoint) {
}

func (i *FakeClient) SyncEndpoint(endpoint *v1.MessagingEndpoint) error {
	return nil
}

func (i *FakeClient) DeleteEndpoint(endpoint *v1.MessagingEndpoint) error {
	return nil
}

func (i *FakeClient) ScheduleProject(project *v1.MessagingProject) error {
	return nil
}

func (i *FakeClient) ScheduleAddress(address *v1.MessagingAddress) error {
	return nil
}

func (i *FakeClient) SyncAddress(address *v1.MessagingAddress) error {
	return nil
}

func (i *FakeClient) DeleteAddress(address *v1.MessagingAddress) error {
	return nil
}

func (i *FakeClient) SyncProject(project *v1.MessagingProject) error {
	return nil
}

func (i *FakeClient) DeleteProject(project *v1.MessagingProject) error {
	return nil
}

func (i *FakeClient) Shutdown() error {
	return nil
}
