/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"context"
	"fmt"
	"log"
	"math/rand"
	"sync"
	"time"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"

	"golang.org/x/sync/errgroup"
)

type routerStateFunc = func(host string, port int32) *RouterState
type brokerStateFunc = func(host string, port int32) *BrokerState

type resourceKey struct {
	Name      string
	Namespace string
}

// TODO - Add periodic reset of router and broker state
// TODO - Unit test of address and endpoint management

type infraClient struct {
	// The known routers and brokers. All configuration is synchronized with these. If their connections get reset,
	// state is re-synced. If new routers and brokers are created, their configuration will be synced as well.
	routers     map[string]*RouterState
	brokers     map[string]*BrokerState
	initialized bool

	// Port allocation map for router ports
	ports map[int]*string

	// Endpoints and addresses known to this client. These provide a consistent view of addresses and endpoints
	// between infra, endpoint and address controllers that may attempt to modify the state at the same time.
	addresses map[resourceKey]*v1beta2.MessagingAddress
	endpoints map[resourceKey]*v1beta2.MessagingEndpoint

	// Factory classes to make it possible to inject alternative clients for configuring routers and brokers.
	routerStateFactory routerStateFunc
	brokerStateFactory brokerStateFunc

	// Clock to keep track of time
	clock          Clock
	resyncInterval time.Duration

	// Guards all fields in the internal state of the client
	lock *sync.Mutex
}

func NewInfra(routerFactory routerStateFunc, brokerFactory brokerStateFunc, clock Clock) *infraClient {
	// TODO: Make constants and expand range
	portmap := make(map[int]*string, 0)
	for i := 40000; i < 40100; i++ {
		portmap[i] = nil
	}
	client := &infraClient{
		routers:            make(map[string]*RouterState, 0),
		brokers:            make(map[string]*BrokerState, 0),
		addresses:          make(map[resourceKey]*v1beta2.MessagingAddress, 0),
		endpoints:          make(map[resourceKey]*v1beta2.MessagingEndpoint, 0),
		clock:              clock,
		resyncInterval:     1800 * time.Second, // TODO: Make configurable
		ports:              portmap,
		routerStateFactory: routerFactory,
		brokerStateFactory: brokerFactory,
		lock:               &sync.Mutex{},
	}
	return client
}

func (i *infraClient) randomResync(now time.Time) time.Time {
	min := 10
	max := 30
	return now.Add(i.resyncInterval).Add(time.Duration(rand.Intn(max-min)+min) * time.Second)
}

func (i *infraClient) checkResync() {
	now := i.clock.Now()
	for _, router := range i.routers {
		if router.nextResync.Before(now) {
			router.Reset()
		}
	}

	for _, broker := range i.brokers {
		if broker.nextResync.Before(now) {
			broker.Reset()
		}
	}
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

func (i *infraClient) updateBrokers(ctx context.Context, hosts []string) error {
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
		err := i.applyRouters(ctx, func(router *RouterState) error {
			return router.DeleteEntities(ctx, []RouterEntity{&NamedEntity{EntityType: RouterConnectorEntity, Name: connectorName(i.brokers[host])}})
		})
		if err != nil {
			return err
		}
		i.brokers[host].Shutdown()

		delete(i.brokers, host)
	}

	// Create states for new hosts
	for host, _ := range toAdd {
		brokerState := i.brokerStateFactory(host, 5671)
		i.brokers[host] = brokerState
	}
	return nil
}

func (i *infraClient) initialize(ctx context.Context) error {
	// Ensure all routers are initialized
	now := i.clock.Now()

	err := i.applyRouters(ctx, func(router *RouterState) error {
		return router.Initialize(i.randomResync(now))
	})
	if err != nil {
		return err
	}

	// Ensure all brokers are initialized
	return i.applyBrokers(ctx, func(broker *BrokerState) error {
		return broker.Initialize(i.randomResync(now))
	})
}

func (i *infraClient) SyncAll(routers []string, brokers []string) ([]ConnectorStatus, error) {
	i.lock.Lock()
	defer i.lock.Unlock()

	ctx := context.Background()

	i.updateRouters(routers)

	err := i.updateBrokers(ctx, brokers)
	if err != nil {
		return nil, err
	}

	if i.initialized {
		i.checkResync()
	}

	err = i.initialize(ctx)
	if err != nil {
		return nil, err
	}

	// Sync all known addresses and endpoints
	err = i.syncConfiguration(ctx)
	if err != nil {
		return nil, err
	}

	// Ensure all routers are connected to all brokers
	connectors := make([]RouterEntity, 0, len(i.brokers))
	for _, broker := range i.brokers {
		connectors = append(connectors, &RouterConnector{
			Name:               connectorName(broker),
			Host:               broker.Host,
			Port:               fmt.Sprintf("%d", broker.Port),
			Role:               "route-container",
			SslProfile:         "infra_tls",
			SaslMechanisms:     "EXTERNAL",
			IdleTimeoutSeconds: 16,
			VerifyHostname:     true,
		})
	}

	totalConnectors := len(i.routers) * len(i.brokers)
	connectorStatuses := make(chan ConnectorStatus, totalConnectors)
	err = i.applyRouters(ctx, func(r *RouterState) error {
		router := r
		err := router.EnsureEntities(ctx, connectors)
		if err != nil {
			log.Println("Error ensuring connector", err)
			return err
		}

		// Query for status
		readConnectors, err := router.ReadEntities(ctx, connectors)
		if err != nil {
			log.Println("Error getting connector status", err)
			return err
		}

		for _, c := range readConnectors {
			connectorStatuses <- connectorToStatus(router.host, c.(*RouterConnector))
		}
		return nil
	})
	// Close immediately to avoid leaking channel. Channel is not in use once applyBrokers/applyRouters have returned
	close(connectorStatuses)
	if err != nil {
		return nil, err
	}

	allConnectors := make([]ConnectorStatus, 0)
	for status := range connectorStatuses {
		allConnectors = append(allConnectors, status)
	}
	log.Printf("State synchronization complete with %d routers and %d brokers", len(i.routers), len(i.brokers))
	i.initialized = true
	return allConnectors, nil
}

func (i *infraClient) initPorts() {
	for _, endpoint := range i.endpoints {
		for _, port := range endpoint.Status.InternalPorts {
			i.ports[port.Port] = &port.Name
		}
	}

}

func (i *infraClient) syncConfiguration(ctx context.Context) error {
	i.initPorts()
	for _, endpoint := range i.endpoints {
		if endpoint.Status.Phase == v1beta2.MessagingEndpointActive && endpoint.Status.Host != "" {
			addresses := make([]*v1beta2.MessagingAddress, 0, len(i.addresses))
			for _, address := range i.addresses {
				if endpoint.Namespace == address.Namespace {
					addresses = append(addresses, address)
				}
			}
			err := i.syncAddressesInternal(ctx, endpoint, addresses)
			if err != nil {
				return err
			}
			err = i.syncEndpointInternal(ctx, endpoint)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func (i *infraClient) ScheduleAddress(address *v1beta2.MessagingAddress, scheduler Scheduler) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NewNotInitializedError()
	}

	brokers := make([]*BrokerState, 0)
	for _, broker := range i.brokers {
		brokers = append(brokers, broker)
	}

	return scheduler.ScheduleAddress(address, brokers)
}

func (i *infraClient) applyRouters(ctx context.Context, fn func(router *RouterState) error) error {
	g, ctx := errgroup.WithContext(ctx)

	for _, router := range i.routers {
		r := router
		g.Go(func() error {
			return fn(r)
		})
	}
	return g.Wait()
}

func (i *infraClient) applyBrokers(ctx context.Context, fn func(broker *BrokerState) error) error {
	g, ctx := errgroup.WithContext(ctx)

	for _, broker := range i.brokers {
		b := broker
		g.Go(func() error {
			return fn(b)
		})
	}
	return g.Wait()
}

func (i *infraClient) SyncEndpoint(endpoint *v1beta2.MessagingEndpoint) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NewNotInitializedError()
	}

	ctx := context.Background()
	err := i.syncEndpointInternal(ctx, endpoint)
	if err != nil {
		return err
	}
	i.endpoints[resourceKey{Name: endpoint.Name, Namespace: endpoint.Namespace}] = endpoint
	return nil
}

func (i *infraClient) AllocatePorts(endpoint *v1beta2.MessagingEndpoint, protocols []v1beta2.MessagingEndpointProtocol) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NewNotInitializedError()
	}

	// Allocate ports for all defined protocols
	for _, protocol := range protocols {
		name := listenerName(endpoint, protocol)
		var found bool
		for _, internalPort := range endpoint.Status.InternalPorts {
			if internalPort.Name == name {
				found = true
				break
			}
		}
		if !found {
			log.Println("Port not found, allocating")
			port, err := i.allocatePort(name)
			if err != nil {
				return err
			}
			endpoint.Status.InternalPorts = append(endpoint.Status.InternalPorts, v1beta2.MessagingEndpointPort{
				Name:     name,
				Protocol: protocol,
				Port:     port,
			})
		}
	}
	return nil
}

func (i *infraClient) allocatePort(name string) (int, error) {
	for port, binding := range i.ports {
		if binding != nil && *binding == name {
			return port, nil
		}
	}

	for port, binding := range i.ports {
		if binding == nil {
			i.ports[port] = &name
			return port, nil
		}
	}
	return 0, fmt.Errorf("no ports available")
}

func (i *infraClient) freePort(port int) {
	i.ports[port] = nil
}

func (i *infraClient) FreePorts(endpoint *v1beta2.MessagingEndpoint) {
	i.lock.Lock()
	defer i.lock.Unlock()

	i.freePortsInternal(endpoint)
}

func (i *infraClient) freePortsInternal(endpoint *v1beta2.MessagingEndpoint) {

	// Not strictly necessary with this guard, but keep it just in case
	if !i.initialized {
		return
	}

	for _, port := range endpoint.Status.InternalPorts {
		i.freePort(port.Port)
	}
	endpoint.Status.InternalPorts = make([]v1beta2.MessagingEndpointPort, 0)
}

func (i *infraClient) syncEndpointInternal(ctx context.Context, endpoint *v1beta2.MessagingEndpoint) error {
	listeners := make([]RouterEntity, 0)
	for _, internalPort := range endpoint.Status.InternalPorts {
		if internalPort.Protocol == v1beta2.MessagingProtocolAMQP {
			listeners = append(listeners, &RouterListener{
				Name:               internalPort.Name,
				Host:               "0.0.0.0",
				Port:               fmt.Sprintf("%d", internalPort.Port),
				Role:               "normal",
				IdleTimeoutSeconds: 16,
				MultiTenant:        true,
			})
		} else {
			// TODO
			return fmt.Errorf("%s protocol not yet supported", internalPort.Protocol)
		}
	}

	return i.applyRouters(ctx, func(router *RouterState) error {
		return router.EnsureEntities(ctx, listeners)
	})
}

func connectorToStatus(host string, connector *RouterConnector) ConnectorStatus {
	return ConnectorStatus{
		Router:    host,
		Broker:    connector.Host,
		Connected: connector.ConnectionStatus == "SUCCESS",
		Message:   connector.ConnectionMsg,
	}
}

func (i *infraClient) SyncAddress(address *v1beta2.MessagingAddress) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NewNotInitializedError()
	}
	ctx := context.Background()

	synced := 0
	for _, endpoint := range i.endpoints {
		if endpoint.Status.Phase == v1beta2.MessagingEndpointActive && endpoint.Status.Host != "" {
			err := i.syncAddressesInternal(ctx, endpoint, []*v1beta2.MessagingAddress{address})
			if err != nil {
				return err
			}
			synced++
		}
	}
	if synced == 0 {
		return fmt.Errorf("no active endpoints defined")
	}
	i.addresses[resourceKey{Name: address.Name, Namespace: address.Namespace}] = address
	return nil
}

func (i *infraClient) syncAddressesInternal(ctx context.Context, endpoint *v1beta2.MessagingEndpoint, addresses []*v1beta2.MessagingAddress) error {
	// Skip endpoints that are not active or do not have hosts defined
	if endpoint.Status.Phase != v1beta2.MessagingEndpointActive || endpoint.Status.Host == "" {
		return nil
	}

	tenantId := endpoint.Status.Host

	routerEntities := make([]RouterEntity, 0, len(addresses))
	brokerQueues := make(map[string][]string, len(i.brokers))

	// Build desired state
	for _, address := range addresses {
		addressName := addressName(tenantId, address)
		fullAddress := fullAddress(tenantId, address)
		if address.Spec.Anycast != nil {
			routerEntities = append(routerEntities, &RouterAddress{
				Name:         addressName,
				Prefix:       fullAddress,
				Distribution: "balanced",
				Waypoint:     false,
			})
		} else if address.Spec.Multicast != nil {
			routerEntities = append(routerEntities, &RouterAddress{
				Name:         addressName,
				Prefix:       fullAddress,
				Distribution: "multicast",
				Waypoint:     false,
			})
		} else if address.Spec.Queue != nil {
			routerEntities = append(routerEntities, &RouterAddress{
				Name:         addressName,
				Prefix:       fullAddress,
				Distribution: "balanced",
				Waypoint:     true,
			})

			for _, broker := range address.Status.Brokers {
				brokerState := i.brokers[broker.Host]
				if brokerState == nil {
					return fmt.Errorf("unable to configure address autoLink (tenant %s address %s) for unknown broker %s", tenantId, address.GetAddress(), broker.Host)
				}
				if len(brokerQueues[broker.Host]) == 0 {
					brokerQueues[broker.Host] = make([]string, 0)
				}
				brokerQueues[broker.Host] = append(brokerQueues[broker.Host], fullAddress)
				routerEntities = append(routerEntities, &RouterAutoLink{
					Name:            autoLinkName(tenantId, address, broker.Host, "in"),
					Address:         fullAddress,
					Direction:       "in",
					Connection:      connectorName(brokerState),
					ExternalAddress: fullAddress,
				})

				routerEntities = append(routerEntities, &RouterAutoLink{
					Name:            autoLinkName(tenantId, address, broker.Host, "out"),
					Address:         fullAddress,
					Direction:       "out",
					Connection:      connectorName(brokerState),
					ExternalAddress: fullAddress,
				})
			}
		} else if address.Spec.Topic != nil {
			// TODO
			return fmt.Errorf("unsupported address type 'topic'")
		} else if address.Spec.Subscription != nil {
			// TODO
			return fmt.Errorf("unsupported address type 'subscription'")
		}
	}

	// Configure brokers first
	err := i.applyBrokers(ctx, func(broker *BrokerState) error {
		if len(brokerQueues[broker.Host]) > 0 {
			return broker.EnsureQueues(ctx, brokerQueues[broker.Host])
		}
		return nil
	})

	// Configure all routers
	err = i.applyRouters(ctx, func(router *RouterState) error {
		return router.EnsureEntities(ctx, routerEntities)
	})
	if err != nil {
		return err
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

func (i *infraClient) DeleteEndpoint(endpoint *v1beta2.MessagingEndpoint) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NewNotInitializedError()
	}

	ctx := context.Background()
	toDelete := make([]RouterEntity, 0)
	for _, internalPort := range endpoint.Status.InternalPorts {
		if internalPort.Protocol == v1beta2.MessagingProtocolAMQP {
			toDelete = append(toDelete, &NamedEntity{
				EntityType: RouterListenerEntity,
				Name:       internalPort.Name,
			})
		} else {
			// TODO
			return fmt.Errorf("%s protocol not yet supported", internalPort.Protocol)
		}
	}

	// TODO: Once router supports multiple endpoints per address, addresses should no
	// longer be tied to the endpoint.
	addresses := make([]*v1beta2.MessagingAddress, 0, len(i.addresses))
	for _, address := range i.addresses {
		if endpoint.Namespace == address.Namespace {
			addresses = append(addresses, address)
		}
	}

	if endpoint.Status.Host != "" {
		err := i.deleteAddressesInternal(ctx, endpoint.Status.Host, addresses)
		if err != nil {
			return err
		}
	}

	err := i.applyRouters(ctx, func(router *RouterState) error {
		return router.DeleteEntities(ctx, toDelete)
	})
	if err != nil {
		return err
	}
	i.freePortsInternal(endpoint)

	delete(i.endpoints, resourceKey{Name: endpoint.Name, Namespace: endpoint.Namespace})
	return nil
}

func (i *infraClient) DeleteAddress(address *v1beta2.MessagingAddress) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NewNotInitializedError()
	}

	ctx := context.Background()

	for _, endpoint := range i.endpoints {
		if endpoint.Status.Host != "" {
			err := i.deleteAddressesInternal(ctx, endpoint.Status.Host, []*v1beta2.MessagingAddress{address})
			if err != nil {
				return err
			}
		}
	}
	delete(i.addresses, resourceKey{Name: address.Name, Namespace: address.Namespace})
	return nil
}

func (i *infraClient) deleteAddressesInternal(ctx context.Context, tenantId string, addresses []*v1beta2.MessagingAddress) error {
	routerEntities := make([]RouterEntity, 0, len(addresses))
	brokerQueues := make([]string, 0, len(addresses))
	for _, address := range addresses {
		if address.Spec.Anycast != nil || address.Spec.Multicast != nil || address.Spec.Queue != nil {
			routerEntities = append(routerEntities, &NamedEntity{EntityType: RouterAddressEntity, Name: addressName(tenantId, address)})
		}

		if address.Spec.Queue != nil {
			brokerQueues = append(brokerQueues, fullAddress(tenantId, address))

			for _, broker := range address.Status.Brokers {
				routerEntities = append(routerEntities, &NamedEntity{EntityType: RouterAutoLinkEntity, Name: autoLinkName(tenantId, address, broker.Host, "in")})
				routerEntities = append(routerEntities, &NamedEntity{EntityType: RouterAutoLinkEntity, Name: autoLinkName(tenantId, address, broker.Host, "out")})
			}
		}
	}

	// Delete from routers
	err := i.applyRouters(ctx, func(router *RouterState) error {
		return router.DeleteEntities(ctx, routerEntities)
	})
	if err != nil {
		return err
	}

	// Delete from brokers
	return i.applyBrokers(ctx, func(brokerState *BrokerState) error {
		return brokerState.DeleteQueues(ctx, brokerQueues)
	})
}

func autoLinkName(tenantId string, address *v1beta2.MessagingAddress, host string, direction string) string {
	return fmt.Sprintf("%s-%s-%s-%s", tenantId, address.Name, host, direction)
}

func addressName(tenantId string, address *v1beta2.MessagingAddress) string {
	return fmt.Sprintf("%s-%s", tenantId, address.Name)
}

func fullAddress(tenantId string, address *v1beta2.MessagingAddress) string {
	return fmt.Sprintf("%s/%s", tenantId, address.GetAddress())
}

func connectorName(broker *BrokerState) string {
	return fmt.Sprintf("%s-%d", broker.Host, broker.Port)
}

func listenerName(endpoint *v1beta2.MessagingEndpoint, protocol v1beta2.MessagingEndpointProtocol) string {
	return fmt.Sprintf("%s-%s-%s", endpoint.Namespace, endpoint.Name, protocol)
}
