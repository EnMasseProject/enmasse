/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package test

import (
	"github.com/enmasseproject/enmasse/pkg/state"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
)

type FakeState struct {
	Routers []string
	Brokers []string
}

var _ state.InfraState = &FakeState{}
var _ state.StateManager = &FakeManager{}

type FakeManager struct {
	Infras map[string]*FakeState
}

func NewFakeManager() *FakeManager {
	return &FakeManager{
		Infras: make(map[string]*FakeState),
	}
}

func (m *FakeManager) GetOrCreateInfra(infra *v1beta2.MessagingInfra) state.InfraState {
	state, exists := m.Infras[infra.Name]
	if !exists {
		infraState := &FakeState{
			Routers: make([]string, 0),
			Brokers: make([]string, 0),
		}
		m.Infras[infra.Name] = infraState
		state = infraState
	}
	return state
}

func (m *FakeManager) GetOrCreateTenant(_ *v1beta2.MessagingTenant) state.TenantState {
	return nil
}

func (m *FakeManager) BindTenantToInfra(_ *v1beta2.MessagingTenant) error {
	return nil
}

func (m *FakeManager) DeleteInfra(infra *v1beta2.MessagingInfra) error {
	delete(m.Infras, infra.Name)
	return nil
}

func (m *FakeManager) DeleteTenant(_ *v1beta2.MessagingTenant) error {
	return nil
}

func (i *FakeState) UpdateRouters(hosts []string) {
	i.Routers = hosts
}

func (i *FakeState) GetOrCreateTenant(name string) (state.TenantState, error) {
	return nil, nil
}

func (i *FakeState) UpdateBrokers(hosts []string) {
	i.Brokers = hosts
}

func (i *FakeState) Sync() error {
	return nil
}

func (i *FakeState) Shutdown() error {
	return nil
}

func (i *FakeState) GetSelector() *v1beta2.Selector {
	return nil
}

func (i *FakeState) GetStatus() (state.InfraStatus, error) {
	return state.InfraStatus{}, nil
}
