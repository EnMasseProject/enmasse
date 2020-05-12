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
	"k8s.io/apimachinery/pkg/runtime"

	"golang.org/x/sync/errgroup"
)

type routerStateFunc = func(host string, port int32) *RouterState
type brokerStateFunc = func(host string, port int32) *BrokerState

type resourceKey struct {
	Name      string
	Namespace string
}

const syncBufferSize int = 100
const maxBatchSize int = 50

// TODO - Unit test of address and endpoint management
type request struct {
	done     chan error
	resource runtime.Object
}

type infraClient struct {
	// The known routers and brokers. All configuration is synchronized with these. If their connections get reset,
	// state is re-synced. If new routers and brokers are created, their configuration will be synced as well.
	routers     map[string]*RouterState
	brokers     map[string]*BrokerState
	initialized bool

	// Port allocation map for router ports
	ports map[int]*string

	// Channel of resources that are awaiting sync
	syncRequests  chan *request
	syncerStop    chan bool
	syncerStopped chan bool

	// Channel of resources that are awaiting deletion
	deleteRequests chan *request
	deleterStop    chan bool
	deleterStopped chan bool

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
		syncRequests:       make(chan *request, syncBufferSize),
		syncerStop:         make(chan bool),
		syncerStopped:      make(chan bool),
		deleteRequests:     make(chan *request, syncBufferSize),
		deleterStop:        make(chan bool),
		deleterStopped:     make(chan bool),
		clock:              clock,
		resyncInterval:     1800 * time.Second, // TODO: Make configurable
		ports:              portmap,
		routerStateFactory: routerFactory,
		brokerStateFactory: brokerFactory,
		lock:               &sync.Mutex{},
	}
	return client
}

func (i *infraClient) Start() {
	go func() {
		log.Printf("Starting syncer goroutine")
		for {
			select {
			case <-i.syncerStop:
				i.syncerStopped <- true
				return
			default:
				i.doSync()
			}
		}
	}()

	go func() {
		log.Printf("Starting deleter goroutine")
		for {
			select {
			case <-i.deleterStop:
				i.deleterStopped <- true
				return
			default:
				i.doDelete()
			}
		}
	}()
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

/**
 * Calling this function will apply existing configuration resources to all routers and brokers.
 * This is called whenever the routers or brokers change so that they get the correct configuration applied.
 */
func (i *infraClient) syncConfiguration(ctx context.Context) error {
	i.initPorts()
	routerEntities := make([]RouterEntity, 0)
	brokerQueues := make(map[string][]string, 0)

	for _, endpoint := range i.endpoints {
		if endpoint.Status.Phase == v1beta2.MessagingEndpointActive && endpoint.Status.Host != "" {
			for _, address := range i.addresses {
				if endpoint.Namespace == address.Namespace {
					resultRouter, err := i.buildRouterAddressEntities(endpoint, address)
					if err != nil {
						return err
					}

					resultBroker, err := i.buildBrokerAddressEntities(endpoint, address)
					if err != nil {
						return err
					}
					for broker, queues := range resultBroker {
						if brokerQueues[broker] == nil {
							brokerQueues[broker] = queues
						} else {
							brokerQueues[broker] = append(brokerQueues[broker], queues...)
						}
					}
					routerEntities = append(routerEntities, resultRouter...)
				}
			}
			e, err := i.buildRouterEndpointEntities(endpoint)
			if err != nil {
				return err
			}
			routerEntities = append(routerEntities, e...)
		}
	}

	return i.syncEntities(ctx, routerEntities, brokerQueues)
}

func (i *infraClient) ScheduleAddress(address *v1beta2.MessagingAddress, scheduler Scheduler) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NotInitializedError
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

/**
 * Return map of endpoints that are in the active state (phase Active) and hostname set.
 */
func (i *infraClient) getActiveEndpoints() map[string][]*v1beta2.MessagingEndpoint {
	activeEndpoints := make(map[string][]*v1beta2.MessagingEndpoint, 0)
	for _, endpoint := range i.endpoints {
		if isEndpointActive(endpoint) {
			if activeEndpoints[endpoint.Namespace] == nil {
				activeEndpoints[endpoint.Namespace] = make([]*v1beta2.MessagingEndpoint, 0)
			}
			activeEndpoints[endpoint.Namespace] = append(activeEndpoints[endpoint.Namespace], endpoint)
		}
	}
	return activeEndpoints
}

func (i *infraClient) SyncAddress(address *v1beta2.MessagingAddress) error {

	// Request sync
	log.Printf("Syncing address %s/%s", address.Namespace, address.Name)
	req := &request{
		resource: address,
		done:     make(chan error, 1),
	}
	select {
	case i.syncRequests <- req:
		return <-req.done
	default:
		close(req.done)
		return NotSyncedError
	}
}

func (i *infraClient) SyncEndpoint(endpoint *v1beta2.MessagingEndpoint) error {
	log.Printf("Syncing endpoint %s/%s", endpoint.Namespace, endpoint.Name)
	req := &request{
		resource: endpoint,
		done:     make(chan error, 1),
	}
	select {
	case i.syncRequests <- req:
		return <-req.done
	default:
		close(req.done)
		return NotSyncedError
	}
}

func (i *infraClient) AllocatePorts(endpoint *v1beta2.MessagingEndpoint, protocols []v1beta2.MessagingEndpointProtocol) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NotInitializedError
	}

	// Allocate ports for all defined protocols
	for _, protocol := range protocols {
		name := portName(endpoint, protocol)
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

	newPorts := make([]v1beta2.MessagingEndpointPort, 0)
	for _, port := range endpoint.Status.InternalPorts {
		if port.Protocol != v1beta2.MessagingProtocolAMQPWS && port.Protocol != v1beta2.MessagingProtocolAMQPWSS {
			i.freePort(port.Port)
		} else {
			// TODO: Workaround for https://issues.apache.org/jira/browse/DISPATCH-1646, as HTTP listeners can't be deleted. We will ignore the error and
			// keep the entity in the local state.
			newPorts = append(newPorts, port)
		}
	}
	endpoint.Status.InternalPorts = newPorts
}

/**
 * Synchronize resources from incoming requests.
 */
func (i *infraClient) doSync() {
	toSync := i.collectRequests(i.syncRequests)
	if len(toSync) == 0 {
		return
	}
	log.Printf("Going to sync %d resources to infra", len(toSync))

	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		for _, req := range toSync {
			req.done <- NotInitializedError
		}
		return
	}

	builtRequests, routerEntities, brokerQueues := i.buildEntities(toSync)

	log.Printf("Syncing %d router entities", len(routerEntities))
	ctx := context.Background()
	err := i.syncEntities(ctx, routerEntities, brokerQueues)
	for _, req := range builtRequests {
		var key resourceKey
		switch v := req.resource.(type) {
		case *v1beta2.MessagingAddress:
			address := req.resource.(*v1beta2.MessagingAddress)
			key = resourceKey{Name: address.Name, Namespace: address.Namespace}
			if err == nil {
				i.addresses[key] = address
			}
			req.done <- err
		case *v1beta2.MessagingEndpoint:
			endpoint := req.resource.(*v1beta2.MessagingEndpoint)
			key = resourceKey{Name: endpoint.Name, Namespace: endpoint.Namespace}
			if err == nil {
				i.endpoints[key] = endpoint
			}
			req.done <- err
		default:
			req.done <- fmt.Errorf("unknown resource type %T", v)
		}
		log.Printf("State updated to (err %+v) for %s/%s", err, key.Namespace, key.Name)
	}

}

func (i *infraClient) syncEntities(ctx context.Context, entities []RouterEntity, brokerQueues map[string][]string) error {
	// Configure brokers first
	var err error
	if len(brokerQueues) > 0 {
		err = i.applyBrokers(ctx, func(broker *BrokerState) error {
			if len(brokerQueues[broker.Host]) > 0 {
				return broker.EnsureQueues(ctx, brokerQueues[broker.Host])
			}
			return nil
		})
	}
	if err != nil {
		return err
	}

	// Configure all routers
	if len(entities) > 0 {
		err = i.applyRouters(ctx, func(router *RouterState) error {
			return router.EnsureEntities(ctx, entities)
		})
	}
	return err
}

/**
 * Build entities for all requests. Failed requests will be marked as failed immediately, and the list of successful requests along with entities that should
 * be created is returned.
 */
func (i *infraClient) buildEntities(requests []*request) (built []*request, routerEntities []RouterEntity, brokerQueues map[string][]string) {
	activeEndpoints := i.getActiveEndpoints()
	routerEntities = make([]RouterEntity, 0, len(requests))
	brokerQueues = make(map[string][]string, len(i.brokers))
	for _, broker := range i.brokers {
		brokerQueues[broker.Host] = make([]string, 0)
	}

	built = make([]*request, 0, len(requests))
	for _, req := range requests {
		switch v := req.resource.(type) {
		case *v1beta2.MessagingAddress:
			address := req.resource.(*v1beta2.MessagingAddress)
			endpoints := activeEndpoints[address.Namespace]
			if endpoints == nil {
				req.done <- NoEndpointsError
				continue
			}

			failed := false
			for _, endpoint := range endpoints {
				resultRouter, err := i.buildRouterAddressEntities(endpoint, address)
				if err != nil {
					req.done <- err
					failed = true
					break
				}

				resultBroker, err := i.buildBrokerAddressEntities(endpoint, address)
				if err != nil {
					req.done <- err
					failed = true
					break
				}

				routerEntities = append(routerEntities, resultRouter...)
				for broker, queues := range resultBroker {
					if brokerQueues[broker] == nil {
						brokerQueues[broker] = queues
					} else {
						brokerQueues[broker] = append(brokerQueues[broker], queues...)
					}
				}
			}

			if !failed {
				built = append(built, req)
			}

		case *v1beta2.MessagingEndpoint:
			endpoint := req.resource.(*v1beta2.MessagingEndpoint)
			result, err := i.buildRouterEndpointEntities(endpoint)
			if err != nil {
				req.done <- err
				continue
			}
			routerEntities = append(routerEntities, result...)
			built = append(built, req)
		default:
			req.done <- fmt.Errorf("unknown resource type %T", v)
		}

	}

	return built, routerEntities, brokerQueues
}

func isEndpointActive(endpoint *v1beta2.MessagingEndpoint) bool {
	return endpoint.Status.Phase == v1beta2.MessagingEndpointActive && endpoint.Status.Host != ""
}

func (i *infraClient) buildRouterEndpointEntities(endpoint *v1beta2.MessagingEndpoint) ([]RouterEntity, error) {
	// TODO: Make configurable
	// linkCapacity := 20
	idleTimeoutSeconds := 16
	routerEntities := make([]RouterEntity, 0)

	for _, internalPort := range endpoint.Status.InternalPorts {
		var sslProfile *RouterSslProfile
		if internalPort.Protocol == v1beta2.MessagingProtocolAMQPS || (internalPort.Protocol == v1beta2.MessagingProtocolAMQPWSS && !endpoint.IsEdgeTerminated()) {
			sslProfile = &RouterSslProfile{
				Name:           sslProfileName(internalPort.Name),
				CertFile:       fmt.Sprintf("/etc/enmasse-tenant-certs/%s.%s.crt", endpoint.Namespace, endpoint.Name),
				PrivateKeyFile: fmt.Sprintf("/etc/enmasse-tenant-certs/%s.%s.key", endpoint.Namespace, endpoint.Name),
			}

			if endpoint.Spec.Tls != nil {
				if endpoint.Spec.Tls.Protocols != nil {
					sslProfile.Protocols = *endpoint.Spec.Tls.Protocols
				}

				if endpoint.Spec.Tls.Ciphers != nil {
					sslProfile.Ciphers = *endpoint.Spec.Tls.Ciphers
				}

			}
			routerEntities = append(routerEntities, sslProfile)
		}

		websockets := (internalPort.Protocol == v1beta2.MessagingProtocolAMQPWS || internalPort.Protocol == v1beta2.MessagingProtocolAMQPWSS)
		listener := &RouterListener{
			Name:               listenerName(internalPort.Name),
			Host:               "0.0.0.0",
			Port:               fmt.Sprintf("%d", internalPort.Port),
			Role:               "normal",
			RequireSsl:         false,
			IdleTimeoutSeconds: idleTimeoutSeconds,
			// LinkCapacity:       linkCapacity,
			MultiTenant: true,
			Websockets:  websockets,
			Http:        websockets,
			Healthz:     false,
			Metrics:     false,
		}

		if sslProfile != nil {
			listener.SslProfile = sslProfile.Name
			// Do not set require SSL for websockets, due to https://issues.apache.org/jira/browse/DISPATCH-1040
			listener.RequireSsl = !websockets
		}
		routerEntities = append(routerEntities, listener)
	}
	return routerEntities, nil
}

/**
 * Add router entities that should exist for a given address for a given endpoint.
 */
func (i *infraClient) buildRouterAddressEntities(endpoint *v1beta2.MessagingEndpoint, address *v1beta2.MessagingAddress) ([]RouterEntity, error) {
	// Skip endpoints that are not active or do not have hosts defined
	if !isEndpointActive(endpoint) {
		return nil, fmt.Errorf("inactive endpoint")
	}

	routerEntities := make([]RouterEntity, 0)

	tenantId := endpoint.Status.Host

	// Build desired state
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
				return nil, fmt.Errorf("unable to configure address autoLink (tenant %s address %s) for unknown broker %s", tenantId, address.GetAddress(), broker.Host)
			}
			connector := connectorName(brokerState)
			routerEntities = append(routerEntities, &RouterAutoLink{
				Name:            autoLinkName(tenantId, address, broker.Host, "in"),
				Address:         fullAddress,
				Direction:       "in",
				Connection:      connector,
				ExternalAddress: fullAddress,
			})

			routerEntities = append(routerEntities, &RouterAutoLink{
				Name:            autoLinkName(tenantId, address, broker.Host, "out"),
				Address:         fullAddress,
				Direction:       "out",
				Connection:      connector,
				ExternalAddress: fullAddress,
			})
		}
	} else if address.Spec.DeadLetter != nil {
		// TODO
		return nil, fmt.Errorf("unsupported address type 'deadLetter'")
	} else if address.Spec.Topic != nil {
		// TODO
		return nil, fmt.Errorf("unsupported address type 'topic'")
	} else if address.Spec.Subscription != nil {
		// TODO
		return nil, fmt.Errorf("unsupported address type 'subscription'")
	}
	return routerEntities, nil
}

/**
 * Add broker queues to be created for a given addresss
 */
func (i *infraClient) buildBrokerAddressEntities(endpoint *v1beta2.MessagingEndpoint, address *v1beta2.MessagingAddress) (map[string][]string, error) {

	if !isEndpointActive(endpoint) {
		return nil, fmt.Errorf("inactive endpoint")
	}

	brokerQueues := make(map[string][]string, 0)
	tenantId := endpoint.Status.Host

	// Build desired state
	fullAddress := fullAddress(tenantId, address)
	if address.Spec.Queue != nil {
		for _, broker := range address.Status.Brokers {
			brokerState := i.brokers[broker.Host]
			if brokerState == nil {
				return nil, fmt.Errorf("unable to configure queue (tenant %s address %s) for unknown broker %s", tenantId, address.GetAddress(), broker.Host)
			}
			if len(brokerQueues[broker.Host]) == 0 {
				brokerQueues[broker.Host] = make([]string, 0)
			}
			brokerQueues[broker.Host] = append(brokerQueues[broker.Host], fullAddress)
		}
	} else if address.Spec.DeadLetter != nil {
		// TODO
		return nil, fmt.Errorf("unsupported address type 'deadLetter'")
	} else if address.Spec.Topic != nil {
		// TODO
		return nil, fmt.Errorf("unsupported address type 'topic'")
	} else if address.Spec.Subscription != nil {
		// TODO
		return nil, fmt.Errorf("unsupported address type 'subscription'")
	}
	return brokerQueues, nil
}

func (i *infraClient) Shutdown() error {
	i.lock.Lock()
	defer i.lock.Unlock()

	i.syncerStop <- true
	<-i.syncerStopped

	i.deleterStop <- true
	<-i.deleterStopped

	for _, router := range i.routers {
		router.Shutdown()
	}

	for _, broker := range i.brokers {
		broker.Shutdown()
	}

	return nil
}

func (i *infraClient) DeleteEndpoint(endpoint *v1beta2.MessagingEndpoint) error {
	log.Printf("Deleting endpoint %s/%s", endpoint.Namespace, endpoint.Name)
	req := &request{
		resource: endpoint,
		done:     make(chan error, 1),
	}
	select {
	case i.deleteRequests <- req:
		return <-req.done
	default:
		close(req.done)
		return NotDeletedError
	}
}

func (i *infraClient) DeleteAddress(address *v1beta2.MessagingAddress) error {
	log.Printf("Deleting address %s/%s", address.Namespace, address.Name)
	req := &request{
		resource: address,
		done:     make(chan error, 1),
	}
	select {
	case i.deleteRequests <- req:
		return <-req.done
	default:
		close(req.done)
		return NotDeletedError
	}
}
func (i *infraClient) doDelete() {
	toDelete := i.collectRequests(i.deleteRequests)
	if len(toDelete) == 0 {
		return
	}
	log.Printf("Going to delete %d resources in infra", len(toDelete))
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		for _, req := range toDelete {
			req.done <- NotInitializedError
		}
		return
	}

	valid := make([]*request, 0, len(toDelete))
	for _, req := range toDelete {
		switch req.resource.(type) {
		case *v1beta2.MessagingEndpoint:
			endpoint := req.resource.(*v1beta2.MessagingEndpoint)
			var err error
			for key, _ := range i.addresses {
				if endpoint.Namespace == key.Namespace {
					err = fmt.Errorf("endpoint still in use by addresses")
					break
				}
			}
			if err != nil {
				req.done <- err
				continue
			}
			valid = append(valid, req)
		default:
			valid = append(valid, req)
		}
	}

	builtRequests, routerEntities, brokerQueues := i.buildEntities(valid)

	ctx := context.Background()
	err := i.deleteEntities(ctx, routerEntities, brokerQueues)
	for _, req := range builtRequests {
		var key resourceKey
		switch v := req.resource.(type) {
		case *v1beta2.MessagingAddress:
			address := req.resource.(*v1beta2.MessagingAddress)
			key = resourceKey{Name: address.Name, Namespace: address.Namespace}
			if err == nil {
				delete(i.addresses, key)
				log.Printf("Deleted address %s/%s", address.Namespace, address.Name)
			}
			req.done <- err
		case *v1beta2.MessagingEndpoint:
			endpoint := req.resource.(*v1beta2.MessagingEndpoint)
			key = resourceKey{Name: endpoint.Name, Namespace: endpoint.Namespace}

			if err == nil {
				i.freePortsInternal(endpoint)
				delete(i.endpoints, key)
				log.Printf("Deleted endpoint %s/%s", endpoint.Namespace, endpoint.Name)
			}
			req.done <- err
		default:
			req.done <- fmt.Errorf("unknown resource type %T", v)
		}
		log.Printf("State updated to (err %+v) for %s/%s", err, key.Namespace, key.Name)
	}
}

func (i *infraClient) deleteEntities(ctx context.Context, routerEntities []RouterEntity, brokerQueues map[string][]string) error {
	// Delete from routers
	err := i.applyRouters(ctx, func(router *RouterState) error {
		return router.DeleteEntities(ctx, routerEntities)
	})
	if err != nil {
		return err
	}

	// Delete from brokers
	return i.applyBrokers(ctx, func(brokerState *BrokerState) error {
		return brokerState.DeleteQueues(ctx, brokerQueues[brokerState.Host])
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

/**
 * Collect up to maxBatchSize requests from a channel or until 5 seconds without incoming requests has passed.
 */
func (i *infraClient) collectRequests(c chan *request) []*request {
	timeout := 1 * time.Second
	requests := make([]*request, 0, maxBatchSize)
	for {
		select {
		case r := <-c:
			requests = append(requests, r)
			if len(requests) >= maxBatchSize {
				return requests
			}
		// If we would block and have gathered some requests, then return whatever we have, if we have requests
		case <-time.After(timeout):
			return requests
		}
	}
}

func autoLinkName(tenantId string, address *v1beta2.MessagingAddress, host string, direction string) string {
	return fmt.Sprintf("autoLink-%s-%s-%s-%s", tenantId, address.Name, host, direction)
}

func addressName(tenantId string, address *v1beta2.MessagingAddress) string {
	return fmt.Sprintf("address-%s-%s", tenantId, address.Name)
}

func fullAddress(tenantId string, address *v1beta2.MessagingAddress) string {
	return fmt.Sprintf("%s/%s", tenantId, address.GetAddress())
}

func connectorName(broker *BrokerState) string {
	return fmt.Sprintf("connector-%s-%d", broker.Host, broker.Port)
}

func portName(endpoint *v1beta2.MessagingEndpoint, protocol v1beta2.MessagingEndpointProtocol) string {
	return fmt.Sprintf("%s-%s-%s", endpoint.Namespace, endpoint.Name, protocol)
}

func listenerName(portName string) string {
	return fmt.Sprintf("listener-%s", portName)
}

func sslProfileName(portName string) string {
	return fmt.Sprintf("sslProfile-%s", portName)
}
