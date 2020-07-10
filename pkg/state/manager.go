/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"sync"
	"time"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/state/broker"
	"github.com/enmasseproject/enmasse/pkg/state/router"
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

type systemClock struct{}

func (s *systemClock) Now() time.Time {
	return time.Now()
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

const (
	PORT_RANGE_START = 40000
	PORT_RANGE_END   = 50000
)

// Signal that an instance of infrastructure is updated
func (m *manager) GetClient(i *v1.MessagingInfrastructure) InfraClient {
	m.lock.Lock()
	defer m.lock.Unlock()
	key := clientKey{Name: i.Name, Namespace: i.Namespace}
	client, exists := m.clients[key]
	if !exists {
		client = NewInfra(i.Name, i.Namespace, router.NewRouterState, broker.NewBrokerState, &systemClock{})
		m.clients[key] = client
		client.Start()
	}
	return client
}

func (m *manager) DeleteClient(infra *v1.MessagingInfrastructure) error {
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
