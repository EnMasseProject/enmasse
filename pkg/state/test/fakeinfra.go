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
	Routers []string
	Brokers []string
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

func (m *FakeManager) GetClient(infra *v1beta2.MessagingInfra) state.InfraClient {
	client, exists := m.Clients[infra.Name]
	if !exists {
		client = &FakeClient{
			Routers: make([]string, 0),
			Brokers: make([]string, 0),
		}
		m.Clients[infra.Name] = client
	}
	return client
}

func (m *FakeManager) DeleteClient(infra *v1beta2.MessagingInfra) error {
	delete(m.Clients, infra.Name)
	return nil
}

func (i *FakeClient) SyncConnectors(routers []string, brokers []string) ([]state.ConnectorStatus, error) {
	i.Routers = routers
	i.Brokers = brokers
	return nil, nil
}

func (i *FakeClient) Shutdown() error {
	return nil
}
