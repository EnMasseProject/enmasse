/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"sync"
	"time"
)

type infraKey struct {
	Name      string
	Namespace string
}

type manager struct {
	infraStates    map[infraKey]InfraState
	resyncInterval time.Duration
	lock           *sync.Mutex
}

var stateManager StateManager
var once sync.Once

func GetStateManager() StateManager {
	once.Do(func() {
		stateManager = NewStateManager()
	})
	return stateManager
}

func NewStateManager() StateManager {
	return &manager{
		infraStates:    make(map[infraKey]InfraState),
		resyncInterval: 300 * time.Second,
		lock:           &sync.Mutex{},
	}
}

// Signal that an instance of infrastructure is updated
func (m *manager) GetOrCreateInfra(name string, namespace string) InfraState {
	m.lock.Lock()
	defer m.lock.Unlock()
	key := infraKey{Name: name, Namespace: namespace}
	state, exists := m.infraStates[key]
	if !exists {
		infraState := &infra{
			routers:            make(map[string]*RouterState, 0),
			brokers:            make(map[string]*BrokerState, 0),
			routerStateFactory: NewRouterState,
			brokerStateFactory: NewBrokerState,
			lock:               &sync.Mutex{},
		}
		m.infraStates[key] = infraState
		state = infraState
	}
	return state
}

func (m *manager) DeleteInfra(name string, namespace string) error {
	m.lock.Lock()
	defer m.lock.Unlock()
	key := infraKey{Name: name, Namespace: namespace}
	state, exists := m.infraStates[key]
	if !exists {
		return nil
	}
	err := state.Shutdown()
	if err != nil {
		return err
	}
	delete(m.infraStates, key)
	return nil
}

func (m *manager) GetOrCreateTenant(name string, namespace string) TenantState {
	return nil
}

func (m *manager) DeleteTenant(name string, namespace string) error {
	return nil
}
