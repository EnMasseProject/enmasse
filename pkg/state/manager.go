/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"fmt"
	"sync"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
)

type manager struct {
	infraStates    map[StateKey]InfraState
	tenantStates   map[StateKey]TenantState
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
		infraStates:    make(map[StateKey]InfraState),
		tenantStates:   make(map[StateKey]TenantState),
		resyncInterval: 300 * time.Second,
		lock:           &sync.Mutex{},
	}
}

// Signal that an instance of infrastructure is updated
func (m *manager) GetOrCreateInfra(i *v1beta2.MessagingInfra) InfraState {
	m.lock.Lock()
	defer m.lock.Unlock()
	key := StateKey{Name: i.Name, Namespace: i.Namespace}
	state, exists := m.infraStates[key]
	if !exists {
		infraState := &infra{
			routers:            make(map[string]*RouterState, 0),
			brokers:            make(map[string]*BrokerState, 0),
			routerStateFactory: NewRouterState,
			brokerStateFactory: NewBrokerState,
			selector:           i.Spec.Selector,
			lock:               &sync.Mutex{},
		}
		m.infraStates[key] = infraState
		state = infraState
	}
	return state
}

func (m *manager) DeleteInfra(infra *v1beta2.MessagingInfra) error {
	m.lock.Lock()
	defer m.lock.Unlock()
	key := StateKey{Name: infra.Name, Namespace: infra.Namespace}
	state, exists := m.infraStates[key]
	if !exists {
		return nil
	}

	// Make sure we cannot delete infra if it is in use
	for tenantKey, tenantState := range m.tenantStates {
		if tenantState.GetInfra() == state {
			return fmt.Errorf("Infrastructure in use by tenant %s/%s", tenantKey.Name, tenantKey.Namespace)
		}
	}
	err := state.Shutdown()
	if err != nil {
		return err
	}
	delete(m.infraStates, key)
	return nil
}

func (m *manager) GetOrCreateTenant(t *v1beta2.MessagingTenant) TenantState {
	m.lock.Lock()
	defer m.lock.Unlock()
	key := StateKey{Name: t.Name, Namespace: t.Namespace}
	state, exists := m.tenantStates[key]
	if !exists {
		tenantState := &tenant{
			infraState: nil,
		}
		m.tenantStates[key] = tenantState
		state = tenantState
	}

	if state.GetInfra() == nil {
		// Use existing persisted binding if found
		for infraKey, infraState := range m.infraStates {
			if t.Status.MessagingInfraRef != nil && t.Status.MessagingInfraRef.Name == infraKey.Name && t.Status.MessagingInfraRef.Namespace == infraKey.Namespace {

				state.BindInfra(infraKey, infraState)
				return state
			}
		}
	}
	return state
}

func (m *manager) BindTenantToInfra(t *v1beta2.MessagingTenant) error {
	m.lock.Lock()
	defer m.lock.Unlock()

	key := StateKey{Name: t.Name, Namespace: t.Namespace}
	state, exists := m.tenantStates[key]
	if !exists {
		return fmt.Errorf("Unable to find tenant state for %s/%s", t.Namespace, t.Name)
	}

	// Only bind if not yet bound
	if state.GetInfra() == nil {
		// TODO: Use infra ref on tenant and selector of infras to determine which infra should
		// serve the tenant. For now, take the first one available
		infraKey, infraState := m.findMatchLocked(t)
		if infraState != nil {
			state.BindInfra(infraKey, infraState)
			return nil
		}
		return fmt.Errorf("Unable to find infra to bind tenant to")
	}
	return nil
}

func (m *manager) findMatchLocked(t *v1beta2.MessagingTenant) (StateKey, InfraState) {
	var bestMatchKey *StateKey
	var bestMatchSelector *v1beta2.Selector
	for key, infra := range m.infraStates {
		selector := infra.GetSelector()
		// If there is a global one without a selector, use it
		if selector == nil && bestMatchKey == nil {
			bestMatchKey = &key
		} else if selector != nil {
			// If selector is applicable to this tenant
			matched := false
			for _, ns := range selector.Namespaces {
				if ns == t.Namespace {
					matched = true
					break
				}
			}

			// Check if this selector is better than the previous (aka. previous was either not set or global)
			if matched && bestMatchSelector == nil {
				bestMatchKey = &key
				bestMatchSelector = selector
			}

			// TODO: Support more advanced selection mechanism based on namespace labels
		}
	}

	// No match
	if bestMatchKey == nil {
		return StateKey{}, nil
	}
	return *bestMatchKey, m.infraStates[*bestMatchKey]
}

func (m *manager) DeleteTenant(t *v1beta2.MessagingTenant) error {
	m.lock.Lock()
	defer m.lock.Unlock()
	key := StateKey{Name: t.Name, Namespace: t.Namespace}
	state, exists := m.tenantStates[key]
	if !exists {
		return nil
	}
	err := state.Shutdown()
	if err != nil {
		return err
	}
	delete(m.tenantStates, key)
	return nil
}
