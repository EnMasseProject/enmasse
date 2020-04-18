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

type infraClient struct {
	routers            map[string]*RouterState
	brokers            map[string]*BrokerState
	routerStateFactory routerStateFunc
	brokerStateFactory brokerStateFunc
	lock               *sync.Mutex
}

func (i *infraClient) updateRouters(hosts []string) {
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

func (i *infraClient) updateBrokers(hosts []string) {
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

func (i *infraClient) SyncConnectors(routers []string, brokers []string) ([]ConnectorStatus, error) {
	i.lock.Lock()
	defer i.lock.Unlock()

	i.updateRouters(routers)
	i.updateBrokers(brokers)

	// Ensure all routers are initialized
	for _, router := range i.routers {
		err := router.Initialize()
		if err != nil {
			return nil, err
		}
	}

	connectorStatuses := make([]ConnectorStatus, 0)

	// Ensure all routers are connected to all brokers
	for _, broker := range i.brokers {
		connector := &RouterConnector{
			Name:               connectorName(broker),
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
				return nil, err
			}

			// Query for status
			status, err := router.GetConnectorStatus(connector)
			if err != nil {
				return nil, err
			}

			connectorStatuses = append(connectorStatuses, *status)

		}
	}
	log.Printf("State synchronization complete with %d routers and %d brokers", len(i.routers), len(i.brokers))
	return connectorStatuses, nil
}

func (i *infraClient) EnsureAddress(tenant string, address *v1beta1.MessagingAddress) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	// Configure all routers
	for _, router := range i.routers {
		err := router.Initialize()
		if err != nil {
			return err
		}

		addressName := fmt.Sprintf("%s-%s", tenantId, address.Name)
		if address.Spec.Type == v1beta2.MessagingAddressAnycast {
			err = router.EnsureAddress(&RouterAddress{
				Name:         addressName,
				Prefix:       fmt.Sprintf("%s/%s", tenantId, address.GetAddress()),
				Distribution: "balanced",
				Waypoint:     false,
			})
			if err != nil {
				return err
			}
		} else if address.Spec.Type == v1beta2.MessagingAddressMulticast {
			err = router.EnsureAddress(&RouterAddress{
				Name:         addressName,
				Prefix:       fmt.Sprintf("%s/%s", tenantId, address.GetAddress()),
				Distribution: "multicast",
				Waypoint:     false,
			})
			if err != nil {
				return err
			}
		} else if address.Spec.Type == v1beta2.MessagingAddressQueue {
			// Message routed
			err = router.EnsureAddress(&RouterAddress{
				Name:         addressName,
				Prefix:       fmt.Sprintf("%s/%s", tenantId, address.GetAddress()),
				Distribution: "balanced",
				Waypoint:     true,
			})
			if err != nil {
				return err
			}

			for _, broker := range address.Status.Brokers {
				namePrefix := fmt.Sprintf("%s-%s-%s", tenantId, address.Name, broker.Host)
				brokerState := i.brokers[broker.Host]
				if brokerState == nil {
					return fmt.Errorf("Unable to configure address autoLink %s for unknown broker %s", namePrefix, broker.Host)
				}
				err = router.EnsureAutoLink(&RouterAutoLink{
					Name:            fmt.Sprintf("%s-in", namePrefix),
					Address:         fmt.Sprintf("%s/%s", tenantId, address.GetAddress()),
					Direction:       "in",
					Connection:      connectorName(brokerState),
					ExternalAddress: fmt.Sprintf("%s/%s", tenantId, address.GetAddress()),
				})
				if err != nil {
					return err
				}

				err = router.EnsureAutoLink(&RouterAutoLink{
					Name:            fmt.Sprintf("%s-out", namePrefix),
					Address:         fmt.Sprintf("%s/%s", tenantId, address.GetAddress()),
					Direction:       "out",
					Connection:      connectorName(brokerState),
					ExternalAddress: fmt.Sprintf("%s/%s", tenantId, address.GetAddress()),
				})
				if err != nil {
					return err
				}
			}

			// TODO link routed
		} else if address.Spec.Type == v1beta2.MessagingAddressTopic {
			// TODO
		} else if address.Spec.Type == v1beta2.MessagingAddressSubscription {
			// TODO
		} else {
			// TODO
		}
	}

	if address.Spec.Type == v1beta2.MessagingAddressQueue {
		// Configure relevant brokers
		for _, broker := range address.Status.Brokers {
			for _, brokerState := range i.brokers {
				err := brokerState.Initialize()
				if err != nil {
					return err
				}
				if broker.Host == brokerState.Host {
					err := brokerState.EnsureQueue(address)
					if err != nil {
						return err
					}
				}
			}
		}
	} else if address.Spec.Type == v1beta2.MessagingAddressTopic {
	} else if address.Spec.Type == v1beta2.MessagingAddressSubscription {
	}
	return nil
}

func (i *infraClient) Shutdown() error {
	i.lock.Lock()
	defer i.lock.Unlock()

	for _, router := range i.routers {
		router.Shutdown()
	}

	for _, broker := range i.brokers {
		broker.Shutdown()
	}
	return nil
}

func (i *infraClient) DeleteAddress(tenantId string, address *v1beta2.MessagingAddress) error {
	// Delete from routers
	i.lock.Lock()
	defer i.lock.Unlock()

	// Configure all routers
	for _, router := range i.routers {
		err := router.Initialize()
		if err != nil {
			return err
		}

		err = router.DeleteAddress(fmt.Sprintf("%s-%s", tenantId, address.Name))
		if err != nil {
			return err
		}

		// Delete autolinks
		if address.Spec.Type == v1beta2.MessagingAddressQueue {
			for _, broker := range address.Status.Brokers {
				// Message routed
				namePrefix := fmt.Sprintf("%s-%s-%s", tenantId, address.Name, broker.Host)
				err = router.DeleteAutoLink(fmt.Sprintf("%s-in", namePrefix))
				if err != nil {
					return err
				}
				err = router.DeleteAutoLink(fmt.Sprintf("%s-out", namePrefix))
				if err != nil {
					return err
				}
			}
			// TODO link routed
		} else if address.Spec.Type == v1beta2.MessagingAddressTopic {
			// TODO
		} else if address.Spec.Type == v1beta2.MessagingAddressSubscription {
			// TODO
		} else {
			// TODO
		}
	}

	if address.Spec.Type == v1beta2.MessagingAddressQueue {
		// Delete from relevant brokers
		for _, broker := range address.Status.Brokers {
			for _, brokerState := range i.brokers {
				err := brokerState.Initialize()
				if err != nil {
					return err
				}
				if broker.Host == brokerState.Host {
					err := brokerState.DeleteQueue(address)
					if err != nil {
						return err
					}
				}
			}
		}
	} else if address.Spec.Type == v1beta2.MessagingAddressTopic {
	} else if address.Spec.Type == v1beta2.MessagingAddressSubscription {
	}
	return nil
}

func connectorName(broker *BrokerState) string {
	return fmt.Sprintf("%s-%d", broker.Host, broker.Port)
}
