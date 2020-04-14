/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"fmt"
	"log"
	"sync"
)

type routerStateFunc = func(host string, port int32) *RouterState
type brokerStateFunc = func(host string, port int32) *BrokerState

type infra struct {
	routers            map[string]*RouterState
	brokers            map[string]*BrokerState
	routerStateFactory routerStateFunc
	brokerStateFactory brokerStateFunc
	status             InfraStatus
	lock               *sync.Mutex
}

func (i *infra) UpdateRouters(hosts []string) {
	i.lock.Lock()
	defer i.lock.Unlock()

	toAdd := make(map[string]bool, 0)
	for _, host := range hosts {
		toAdd[host] = true
	}

	toRemove := make(map[string]bool, 0)

	for host, _ := range i.routers {
		found := toAdd[host]

		// Should not longer exist, so shut down clients
		if !found {
			toRemove[host] = true
		} else {
			// We already have a state for it
			delete(toAdd, host)
		}
	}

	// Shutdown and remove unknown hosts
	for host, _ := range toRemove {
		i.routers[host].Shutdown()
		delete(i.routers, host)
	}

	// Create states for new hosts
	for host, _ := range toAdd {
		routerState := i.routerStateFactory(host, 7777)
		i.routers[host] = routerState
	}
}

func (i *infra) UpdateBrokers(hosts []string) {
	i.lock.Lock()
	defer i.lock.Unlock()

	toAdd := make(map[string]bool, 0)
	for _, host := range hosts {
		toAdd[host] = true
	}

	toRemove := make(map[string]bool, 0)

	for host, _ := range i.brokers {
		found := toAdd[host]

		// Should not longer exist, so shut down clients
		if !found {
			toRemove[host] = true
		} else {
			// We already have a state for it so remove it
			delete(toAdd, host)
		}
	}

	// Shutdown and remove unknown hosts
	for host, _ := range toRemove {
		i.brokers[host].Shutdown()
		delete(i.brokers, host)
	}

	// Create states for new hosts
	for host, _ := range toAdd {
		brokerState := i.brokerStateFactory(host, 5671)
		i.brokers[host] = brokerState
	}
}

func (i *infra) Sync() error {
	i.lock.Lock()
	defer i.lock.Unlock()

	// Ensure all routers are initialized
	for _, router := range i.routers {
		err := router.Initialize()
		if err != nil {
			return err
		}
	}

	connectorStatuses := make([]ConnectorStatus, 0)

	// Ensure all routers are connected to all brokers
	for _, broker := range i.brokers {
		connector := &RouterConnector{
			Host:               broker.Host,
			Port:               fmt.Sprintf("%d", broker.Port),
			Role:               "route-container",
			SslProfile:         "infra_tls",
			SaslMechanisms:     "EXTERNAL",
			IdleTimeoutSeconds: 16,
			VerifyHostname:     true,
		}
		for _, router := range i.routers {
			err := router.EnsureConnector(connector)
			if err != nil {
				return err
			}

			// Query for status
			status, err := router.GetConnectorStatus(connector)
			if err != nil {
				return err
			}

			connectorStatuses = append(connectorStatuses, *status)

		}
	}
	log.Printf("State synchronization complete with %d routers and %d brokers", len(i.routers), len(i.brokers))
	i.status.Connectors = connectorStatuses
	return nil
}

func (i *infra) GetStatus() (InfraStatus, error) {
	i.lock.Lock()
	defer i.lock.Unlock()
	return i.status, nil
}

func (i *infra) Shutdown() error {
	i.lock.Lock()
	defer i.lock.Unlock()

	for _, router := range i.routers {
		router.Shutdown()
	}
	return nil
}
