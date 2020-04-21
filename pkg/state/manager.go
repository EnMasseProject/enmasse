/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"sync"
	"time"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
)

type clientKey struct {
	Name      string
	Namespace string
}

type manager struct {
	clients        map[clientKey]InfraClient
	resyncInterval time.Duration
	lock           *sync.Mutex
}

var clientManager ClientManager
var once sync.Once

/**
 * Make it a singleton so that we share clients in this operator instance.
 */
func GetClientManager() ClientManager {
	once.Do(func() {
		clientManager = NewClientManager()
	})
	return clientManager
}

func NewClientManager() ClientManager {
	return &manager{
		clients:        make(map[clientKey]InfraClient),
		resyncInterval: 300 * time.Second,
		lock:           &sync.Mutex{},
	}
}

// Signal that an instance of infrastructure is updated
func (m *manager) GetClient(i *v1beta2.MessagingInfra) InfraClient {
	m.lock.Lock()
	defer m.lock.Unlock()
	key := clientKey{Name: i.Name, Namespace: i.Namespace}
	client, exists := m.clients[key]
	if !exists {
		client = &infraClient{

			routers:            make(map[string]*RouterState, 0),
			brokers:            make(map[string]*BrokerState, 0),
			routerStateFactory: NewRouterState,
			brokerStateFactory: NewBrokerState,
			lock:               &sync.Mutex{},
		}
		m.clients[key] = client
	}
	return client
}

func (m *manager) DeleteClient(infra *v1beta2.MessagingInfra) error {
	m.lock.Lock()
	defer m.lock.Unlock()
	key := clientKey{Name: infra.Name, Namespace: infra.Namespace}
	client, exists := m.clients[key]
	if !exists {
		return nil
	}

	err := client.Shutdown()
	if err != nil {
		return err
	}
	delete(m.clients, key)
	return nil
}
