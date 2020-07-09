/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"context"
	"crypto/tls"
	"fmt"
	"math/rand"
	"sync"
	"time"

	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	. "github.com/enmasseproject/enmasse/pkg/state/broker"
	. "github.com/enmasseproject/enmasse/pkg/state/common"
	. "github.com/enmasseproject/enmasse/pkg/state/errors"
	. "github.com/enmasseproject/enmasse/pkg/state/router"
	sched "github.com/enmasseproject/enmasse/pkg/state/scheduler"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	logf "sigs.k8s.io/controller-runtime/pkg/log"

	"golang.org/x/sync/errgroup"
)

var log = logf.Log.WithName("state")

type routerStateFunc = func(host Host, port int32, tlsConfig *tls.Config) *RouterState
type brokerStateFunc = func(host Host, port int32, tlsConfig *tls.Config) *BrokerState

type resourceKey struct {
	Name      string
	Namespace string
}

const syncBufferSize int = 100
const maxBatchSize int = 50

// TODO - Unit test of address and endpoint management
type request struct {
	done     chan error
	resource metav1.Object
}

var defaultScheduler = sched.NewDummyScheduler()

type infraClient struct {
	infrastructureName      string
	infrastructureNamespace string

	// The known routers and brokers. All configuration is synchronized with these. If their connections get reset,
	// state is re-synced. If new routers and brokers are created, their configuration will be synced as well.
	routers     map[Host]*RouterState
	brokers     map[Host]*BrokerState
	hostMap     map[string]Host
	initialized bool

	// The TLS configuration of the operator to the clients
	tlsConfig *tls.Config

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

	// Projects, endpoints and addresses known to this client. These provide a consistent view of addresses and endpoints
	// between infra, endpoint and address controllers that may attempt to modify the state at the same time.
	addresses map[resourceKey]*v1.MessagingAddress
	endpoints map[resourceKey]*v1.MessagingEndpoint
	projects  map[resourceKey]*v1.MessagingProject

	// Factory classes to make it possible to inject alternative clients for configuring routers and brokers.
	routerStateFactory routerStateFunc
	brokerStateFactory brokerStateFunc

	// Clock to keep track of time
	clock          Clock
	resyncInterval time.Duration

	// Guards all fields in the internal state of the client
	lock           *sync.Mutex
}

func NewInfra(infraName, infraNamespace string, routerFactory routerStateFunc, brokerFactory brokerStateFunc, clock Clock) *infraClient {
	// TODO: Make constants and expand range
	portmap := make(map[int]*string, 0)
	for i := 40000; i < 40100; i++ {
		portmap[i] = nil
	}
	client := &infraClient{
		infrastructureName:      infraName,
		infrastructureNamespace: infraNamespace,

		routers:            make(map[Host]*RouterState, 0),
		brokers:            make(map[Host]*BrokerState, 0),
		hostMap:            make(map[string]Host, 0),
		addresses:          make(map[resourceKey]*v1.MessagingAddress, 0),
		endpoints:          make(map[resourceKey]*v1.MessagingEndpoint, 0),
		projects:           make(map[resourceKey]*v1.MessagingProject, 0),
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
		log.Info("Starting syncer goroutine")
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
		log.Info("Starting deleter goroutine")
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
		if router.NextResync().Before(now) {
			router.Reset()
		}
	}

	for _, broker := range i.brokers {
		if broker.NextResync().Before(now) {
			broker.Reset()
		}
	}
}

func (i *infraClient) updateRouters(hosts []Host) {
	toAdd := make(map[string]Host, 0)
	for _, host := range hosts {
		toAdd[host.Hostname] = host
	}

	toRemove := make(map[string]Host, 0)

	for host, _ := range i.routers {
		entry, found := toAdd[host.Hostname]

		// Should not longer exist, so shut down clients
		if !found {
			toRemove[host.Hostname] = host
		} else if entry.Ip != host.Ip {
			// Ip has changed, we need to delete and create
			toRemove[host.Hostname] = host
		} else {
			// We already have a state for it
			delete(toAdd, host.Hostname)
		}
	}

	// Shutdown and remove unknown hosts
	for hostname, host := range toRemove {
		log.Info(fmt.Sprintf("Removing router %+v", host))
		i.routers[host].Shutdown()
		delete(i.hostMap, hostname)
		delete(i.routers, host)
	}

	// Create states for new hosts
	for hostname, host := range toAdd {
		log.Info(fmt.Sprintf("Adding router %+v", host))
		var routerTlsConfig *tls.Config
		if i.tlsConfig != nil {
			routerTlsConfig = &tls.Config{
				Certificates: i.tlsConfig.Certificates,
				RootCAs:      i.tlsConfig.RootCAs,
				ServerName:   host.Hostname,
			}
		}
		routerState := i.routerStateFactory(host, 55671, routerTlsConfig)
		i.routers[host] = routerState
		i.hostMap[hostname] = host
	}
}

func (i *infraClient) updateBrokers(ctx context.Context, hosts []Host, removeRouterConnectors bool) error {
	toAdd := make(map[string]Host, 0)
	for _, host := range hosts {
		toAdd[host.Hostname] = host
	}

	toRemove := make(map[string]Host, 0)

	for host, _ := range i.brokers {
		entry, found := toAdd[host.Hostname]

		// Should not longer exist, so shut down clients
		if !found {
			toRemove[host.Hostname] = host
		} else if entry.Ip != host.Ip {
			// Ip has changed, we need to delete and create
			toRemove[host.Hostname] = host
		} else {
			// We already have a state for it so remove it
			delete(toAdd, host.Hostname)
		}
	}

	// Shutdown and remove unknown hosts
	for hostname, host := range toRemove {
		log.Info(fmt.Sprintf("Removing broker %+v", host))
		_, foundAdded := toAdd[hostname]
		// If it was found, it means the ip changed, but the host remains stable so there is no need to delete the connector.
		if !foundAdded && removeRouterConnectors {
			err := i.applyRouters(ctx, func(r *RouterState) error {
				router := r
				return router.DeleteEntities(ctx, []RouterEntity{&NamedEntity{EntityType: RouterConnectorEntity, Name: connectorName(i.brokers[host])}})
			})
			if err != nil {
				return err
			}
		}
		i.brokers[host].Shutdown()

		delete(i.hostMap, hostname)
		delete(i.brokers, host)
	}

	// Create states for new hosts
	for hostname, host := range toAdd {
		log.Info(fmt.Sprintf("Adding broker %+v", host))
		var brokerTlsConfig *tls.Config
		if i.tlsConfig != nil {
			brokerTlsConfig = &tls.Config{
				Certificates: i.tlsConfig.Certificates,
				RootCAs:      i.tlsConfig.RootCAs,
				ServerName:   host.Hostname,
			}
		}
		brokerState := i.brokerStateFactory(host, 5671, brokerTlsConfig)
		i.brokers[host] = brokerState
		i.hostMap[hostname] = host
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

func (i *infraClient) DeleteBroker(host string) error {
	i.lock.Lock()
	defer i.lock.Unlock()

	var state *BrokerState
	newBrokers := make([]Host, 0)
	for _, brokerState := range i.brokers {
		if brokerState.Host().Hostname == host {
			state = brokerState
		} else {
			newBrokers = append(newBrokers, brokerState.Host())
		}
	}
	if state == nil {
		return nil
	}

	for _, project := range i.projects {
		if project.Status.Broker != nil && project.Status.Broker.Host == host {
			return BrokerInUseError
		}
	}

	for _, address := range i.addresses {
		for _, broker := range address.Status.Brokers {
			if broker.Host == host {
				return BrokerInUseError
			}
		}
	}

	// Not in use, so we can safely delete the broker
	ctx := context.Background()
	return i.updateBrokers(ctx, newBrokers, false)
}

func (i *infraClient) SyncAll(routers []Host, brokers []Host, tlsConfig *tls.Config) ([]ConnectorStatus, error) {
	log.Info(fmt.Sprintf("Syncing with routers: %+v, and brokers: %+v", routers, brokers))
	i.lock.Lock()
	defer i.lock.Unlock()

	i.tlsConfig = tlsConfig
	ctx := context.Background()

	i.updateRouters(routers)

	err := i.updateBrokers(ctx, brokers, true)
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
			Host:               broker.Host().Hostname,
			Port:               fmt.Sprintf("%d", broker.Port()),
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
			log.Error(err, "Error ensuring connector")
			return err
		}

		// Query for status
		readConnectors, err := router.ReadEntities(ctx, connectors)
		if err != nil {
			log.Error(err, "Error getting connector status")
			return err
		}

		for _, c := range readConnectors {
			connectorStatuses <- connectorToStatus(router.Host().Hostname, c.(*RouterConnector))
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

	// Once everything is synced - create the broker probe entities and router listeners for the readiness probe.
	readinessQueue := &BrokerQueue{
		Name:               "readiness",
		Address:            "readiness",
		MaxConsumers:       1,
		Durable:            false,
		AutoCreateAddress:  true,
		RoutingType:        RoutingTypeAnycast,
		PurgeOnNoConsumers: false,
	}
	err = i.applyBrokers(ctx, func(b *BrokerState) error {
		broker := b
		return broker.EnsureEntities(ctx, []BrokerEntity{readinessQueue})
	})
	if err != nil {
		return nil, err
	}

	readinessListener := &RouterListener{
		Name:               "readiness",
		Role:               "normal",
		Host:               "127.0.0.1",
		Port:               "7779",
		IdleTimeoutSeconds: 16,
		AuthenticatePeer:   false,
		Http:               true,
		Metrics:            true,
		Healthz:            true,
		Websockets:         false,
	}
	err = i.applyRouters(ctx, func(r *RouterState) error {
		router := r
		return router.EnsureEntities(ctx, []RouterEntity{readinessListener})
	})
	if err != nil {
		return nil, err
	}

	log.Info(fmt.Sprintf("State synchronization complete with %d routers and %d brokers", len(i.routers), len(i.brokers)))
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
	brokerEntities := make(map[Host][]BrokerEntity, 0)

	for _, endpoint := range i.endpoints {
		if endpoint.Status.Phase == v1.MessagingEndpointActive && endpoint.Status.Host != "" {
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
					for broker, entities := range resultBroker {
						if brokerEntities[broker] == nil {
							brokerEntities[broker] = entities
						} else {
							brokerEntities[broker] = append(brokerEntities[broker], entities...)
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

	return i.syncEntities(ctx, routerEntities, brokerEntities)
}

func (i *infraClient) ScheduleProject(project *v1.MessagingProject) error {

	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NotInitializedError
	}

	brokers := make([]*BrokerState, 0)
	for _, broker := range i.brokers {
		brokers = append(brokers, broker)
	}

	scheduler := defaultScheduler
	// Use the assigned project broker if set
	return scheduler.ScheduleProject(project, brokers)
}

func (i *infraClient) ScheduleAddress(address *v1.MessagingAddress) error {

	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		return NotInitializedError
	}

	project, ok := i.projects[resourceKey{Name: "default", Namespace: address.GetNamespace()}]
	if !ok {
		return ProjectNotFoundError
	}

	brokers := make([]*BrokerState, 0)
	for _, broker := range i.brokers {
		brokers = append(brokers, broker)
	}

	// Use the assigned project broker if set
	if project.Status.Broker != nil {
		address.Status.Brokers = []v1.MessagingAddressBroker{*project.Status.Broker}
		return nil
	} else {
		scheduler := defaultScheduler
		return scheduler.ScheduleAddress(address, brokers)
	}
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
func (i *infraClient) getActiveEndpoints() map[string][]*v1.MessagingEndpoint {
	activeEndpoints := make(map[string][]*v1.MessagingEndpoint, 0)
	for _, endpoint := range i.endpoints {
		if endpoint.IsActive() {
			if activeEndpoints[endpoint.Namespace] == nil {
				activeEndpoints[endpoint.Namespace] = make([]*v1.MessagingEndpoint, 0)
			}
			activeEndpoints[endpoint.Namespace] = append(activeEndpoints[endpoint.Namespace], endpoint)
		}
	}
	return activeEndpoints
}

func (i *infraClient) requestSync(object metav1.Object) error {
	// Request sync
	req := &request{
		resource: object,
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

func (i *infraClient) SyncAddress(address *v1.MessagingAddress) error {

	// Request sync
	log.Info(fmt.Sprintf("Syncing address %s/%s", address.Namespace, address.Name))
	return i.requestSync(address)
}

func (i *infraClient) SyncEndpoint(endpoint *v1.MessagingEndpoint) error {
	log.Info(fmt.Sprintf("Syncing endpoint %s/%s", endpoint.Namespace, endpoint.Name))
	return i.requestSync(endpoint)
}

func (i *infraClient) SyncProject(project *v1.MessagingProject) error {
	log.Info(fmt.Sprintf("Syncing project %s/%s", project.Namespace, project.Name))
	return i.requestSync(project)
}

func (i *infraClient) AllocatePorts(endpoint *v1.MessagingEndpoint, protocols []v1.MessagingEndpointProtocol) error {
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
			log.Info("Port not found, allocating")
			port, err := i.allocatePort(name)
			if err != nil {
				return err
			}
			endpoint.Status.InternalPorts = append(endpoint.Status.InternalPorts, v1.MessagingEndpointPort{
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

func (i *infraClient) FreePorts(endpoint *v1.MessagingEndpoint) {
	i.lock.Lock()
	defer i.lock.Unlock()

	i.freePortsInternal(endpoint)
}

func (i *infraClient) freePortsInternal(endpoint *v1.MessagingEndpoint) {

	// Not strictly necessary with this guard, but keep it just in case
	if !i.initialized {
		return
	}

	newPorts := make([]v1.MessagingEndpointPort, 0)
	for _, port := range endpoint.Status.InternalPorts {
		if port.Protocol != v1.MessagingProtocolAMQPWS && port.Protocol != v1.MessagingProtocolAMQPWSS {
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
	log.Info(fmt.Sprintf("Going to sync %d resources to infra", len(toSync)))

	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		for _, req := range toSync {
			req.done <- NotInitializedError
		}
		return
	}

	valid := make([]*request, 0, len(toSync))
	for _, req := range toSync {
		switch req.resource.(type) {
		case *v1.MessagingAddress:
			if _, ok := i.projects[resourceKey{Name: "default", Namespace: req.resource.GetNamespace()}]; !ok {
				req.done <- fmt.Errorf("project not synced for %s/%s", req.resource.GetNamespace(), req.resource.GetName())
				continue
			}
			valid = append(valid, req)
		case *v1.MessagingEndpoint:
			if _, ok := i.projects[resourceKey{Name: "default", Namespace: req.resource.GetNamespace()}]; !ok {
				req.done <- fmt.Errorf("project not synced for %s/%s", req.resource.GetNamespace(), req.resource.GetName())
				continue
			}
			valid = append(valid, req)
		default:
			valid = append(valid, req)
		}
	}

	// here
	builtRequests, routerEntities, brokerEntities := i.buildEntities(valid)

	log.Info(fmt.Sprintf("Syncing %d router entities", len(routerEntities)))
	ctx := context.Background()
	err := i.syncEntities(ctx, routerEntities, brokerEntities)
	for _, req := range builtRequests {
		key := resourceKey{Name: req.resource.GetName(), Namespace: req.resource.GetNamespace()}
		switch v := req.resource.(type) {
		case *v1.MessagingAddress:
			if err == nil {
				i.addresses[key] = v
			}
			req.done <- err
		case *v1.MessagingEndpoint:
			if err == nil {
				i.endpoints[key] = v
			}
			req.done <- err
		case *v1.MessagingProject:
			if err == nil {
				i.projects[key] = v
			}
			req.done <- err
		default:
			req.done <- fmt.Errorf("unknown resource type %T", v)
		}
		log.Info(fmt.Sprintf("State updated to (err %+v) for %s/%s", err, key.Namespace, key.Name))
	}

}

func (i *infraClient) syncEntities(ctx context.Context, entities []RouterEntity, brokerEntities map[Host][]BrokerEntity) error {
	// Configure brokers first
	var err error
	if len(brokerEntities) > 0 {
		err = i.applyBrokers(ctx, func(broker *BrokerState) error {
			if len(brokerEntities[broker.Host()]) > 0 {
				return broker.EnsureEntities(ctx, brokerEntities[broker.Host()])
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
func (i *infraClient) buildEntities(requests []*request) (built []*request, routerEntities []RouterEntity, brokerEntities map[Host][]BrokerEntity) {
	activeEndpoints := i.getActiveEndpoints()
	routerEntities = make([]RouterEntity, 0, len(requests))
	brokerEntities = make(map[Host][]BrokerEntity, len(i.brokers))
	for _, broker := range i.brokers {
		brokerEntities[broker.Host()] = make([]BrokerEntity, 0)
	}

	built = make([]*request, 0, len(requests))
	for _, req := range requests {
		switch v := req.resource.(type) {
		case *v1.MessagingAddress:
			address := v
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
				for broker, entities := range resultBroker {
					if brokerEntities[broker] == nil {
						brokerEntities[broker] = entities
					} else {
						brokerEntities[broker] = append(brokerEntities[broker], entities...)
					}
				}
			}

			if !failed {
				built = append(built, req)
			}

		case *v1.MessagingEndpoint:
			endpoint := v
			result, err := i.buildRouterEndpointEntities(endpoint)
			if err != nil {
				req.done <- err
				continue
			}
			routerEntities = append(routerEntities, result...)
			built = append(built, req)
		case *v1.MessagingProject:
			project := v
			result, err := i.buildRouterProjectEntities(project)
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

	return built, routerEntities, brokerEntities
}

func (i *infraClient) buildRouterProjectEntities(project *v1.MessagingProject) ([]RouterEntity, error) {
	routerEntities := make([]RouterEntity, 0)
	// TODO: Create vhost policy based on status (plan settings)
	return routerEntities, nil
}

func (i *infraClient) buildRouterEndpointEntities(endpoint *v1.MessagingEndpoint) ([]RouterEntity, error) {
	// TODO: Make configurable
	// linkCapacity := 20
	idleTimeoutSeconds := 16
	routerEntities := make([]RouterEntity, 0)

	for _, internalPort := range endpoint.Status.InternalPorts {
		var sslProfile *RouterSslProfile
		if internalPort.Protocol == v1.MessagingProtocolAMQPS || (internalPort.Protocol == v1.MessagingProtocolAMQPWSS && !endpoint.IsEdgeTerminated()) {
			sslProfile = &RouterSslProfile{
				Name:           sslProfileName(internalPort.Name),
				CertFile:       fmt.Sprintf("/etc/enmasse-project-certs/%s.%s.crt", endpoint.Namespace, endpoint.Name),
				PrivateKeyFile: fmt.Sprintf("/etc/enmasse-project-certs/%s.%s.key", endpoint.Namespace, endpoint.Name),
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

		// TODO separate authhost per endpoint required (so that the service can apply the correct SASL config) - use global one for now
		authHost := fmt.Sprintf("access-control-%s.%s.svc.cluster.local:5671", i.infrastructureName, i.infrastructureNamespace)
		//authService := &RouterAuthServicePlugin{
		//	Host:       authHost,
		//	Port:       "5671",
		//	Realm:      authHost,
		//	SslProfile: "infra_tls",
		//}

		websockets := (internalPort.Protocol == v1.MessagingProtocolAMQPWS || internalPort.Protocol == v1.MessagingProtocolAMQPWSS)
		listener := &RouterListener{
			Name:               listenerName(internalPort.Name),
			Host:               "0.0.0.0",
			Port:               fmt.Sprintf("%d", internalPort.Port),
			Role:               "normal",
			RequireSsl:         false,
			AuthenticatePeer:   true,
			IdleTimeoutSeconds: idleTimeoutSeconds,
			// LinkCapacity:       TODO: Make configurable?
			MultiTenant: true,
			Websockets:  websockets,
			Http:        websockets,
			Healthz:     false,
			Metrics:     false,
			SaslPlugin:  authHost,
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
func (i *infraClient) buildRouterAddressEntities(endpoint *v1.MessagingEndpoint, address *v1.MessagingAddress) ([]RouterEntity, error) {
	// Skip endpoints that are not active or do not have hosts defined
	if !endpoint.IsActive() {
		return nil, fmt.Errorf("inactive endpoint")
	}

	routerEntities := make([]RouterEntity, 0)

	project := i.projects[resourceKey{Name: "default", Namespace: address.Namespace}]
	projectId := endpoint.Status.Host

	// If transactional, rely on link route to be created
	// TODO: Move this to buildRouterProjectEntities once endpoint is not needed.
	if project.Status.Broker != nil {
		// Transactional means that we create a link route based on the project prefix,
		// and all broker addresses go through that route.
		broker := *project.Status.Broker
		host := i.hostMap[broker.Host]
		brokerState := i.brokers[host]
		if brokerState == nil {
			return nil, fmt.Errorf("unable to configure transactional linkroute (project %s) for unknown broker %s", projectId, broker.Host)
		}
		connector := connectorName(brokerState)
		routerEntities = append(routerEntities, &RouterLinkRoute{
			Name:       globalLinkRouteName(projectId, host.Hostname, "out"),
			Prefix:     projectId,
			Direction:  "out",
			Connection: connector,
		})

		routerEntities = append(routerEntities, &RouterLinkRoute{
			Name:       globalLinkRouteName(projectId, host.Hostname, "in"),
			Prefix:     projectId,
			Direction:  "in",
			Connection: connector,
		})
		return routerEntities, nil
	}

	// Build desired state
	addressName := addressName(projectId, address)
	fullAddress := qualifiedAddress(projectId, address.GetAddress())
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
			host := i.hostMap[broker.Host]
			brokerState := i.brokers[host]
			if brokerState == nil {
				return nil, fmt.Errorf("unable to configure address autoLink (project %s address %s) for unknown broker %s", projectId, address.GetAddress(), broker.Host)
			}
			connector := connectorName(brokerState)
			routerEntities = append(routerEntities, &RouterAutoLink{
				Name:            autoLinkName(projectId, address, broker.Host, "in"),
				Address:         fullAddress,
				Direction:       "in",
				Connection:      connector,
				ExternalAddress: fullAddress,
			})

			routerEntities = append(routerEntities, &RouterAutoLink{
				Name:            autoLinkName(projectId, address, broker.Host, "out"),
				Address:         fullAddress,
				Direction:       "out",
				Connection:      connector,
				ExternalAddress: fullAddress,
			})
		}
	} else if address.Spec.DeadLetter != nil {
		// Deadletter addresses should be read-only and consumable from all known brokers
		routerEntities = append(routerEntities, &RouterAddress{
			Name:         addressName,
			Prefix:       fullAddress,
			Distribution: "balanced",
			Waypoint:     true,
		})

		for _, brokerState := range i.brokers {
			connector := connectorName(brokerState)
			routerEntities = append(routerEntities, &RouterAutoLink{
				Name:            autoLinkName(projectId, address, brokerState.Host().Hostname, "in"),
				Address:         fullAddress,
				Direction:       "in",
				Connection:      connector,
				ExternalAddress: fullAddress,
			})
		}
	} else if address.Spec.Topic != nil {
		// Topic addresses are link routed to a single broker, and they can only be present on one broker. However, the following
		// logic relies on the scheduler to make the decision on which broker they are located.
		routerEntities = append(routerEntities, &RouterAddress{
			Name:         addressName,
			Prefix:       fullAddress,
			Distribution: "balanced",
			Waypoint:     true,
		})

		for _, broker := range address.Status.Brokers {
			host := i.hostMap[broker.Host]
			brokerState := i.brokers[host]
			if brokerState == nil {
				return nil, fmt.Errorf("unable to configure address linkRoute (project %s address %s) for unknown broker %s", projectId, address.GetAddress(), broker.Host)
			}
			connector := connectorName(brokerState)
			routerEntities = append(routerEntities, &RouterLinkRoute{
				Name:       linkRouteName(projectId, address, broker.Host, "out"),
				Prefix:     fullAddress,
				Direction:  "out",
				Connection: connector,
			})
			routerEntities = append(routerEntities, &RouterLinkRoute{
				Name:       linkRouteName(projectId, address, broker.Host, "in"),
				Prefix:     fullAddress,
				Direction:  "in",
				Connection: connector,
			})
		}
	} else if address.Spec.Subscription != nil {
		// Subscription addresses are read only and consumable from only the broker where the subscription is scheduled (same as topic).
		routerEntities = append(routerEntities, &RouterAddress{
			Name:         addressName,
			Prefix:       fullAddress,
			Distribution: "balanced",
			Waypoint:     true,
		})

		for _, broker := range address.Status.Brokers {
			host := i.hostMap[broker.Host]
			brokerState := i.brokers[host]
			if brokerState == nil {
				return nil, fmt.Errorf("unable to configure address autoLink (project %s address %s) for unknown broker %s", projectId, address.GetAddress(), broker.Host)
			}
			connector := connectorName(brokerState)
			routerEntities = append(routerEntities, &RouterAutoLink{
				Name:            autoLinkName(projectId, address, brokerState.Host().Hostname, "in"),
				Address:         fullAddress,
				Direction:       "in",
				Connection:      connector,
				ExternalAddress: fmt.Sprintf("%s::%s", qualifiedAddress(projectId, address.Spec.Subscription.Topic), fullAddress),
			})
		}
	}
	return routerEntities, nil
}

/**
 * Add broker entities to be created for a given addresss
 */
func (i *infraClient) buildBrokerAddressEntities(endpoint *v1.MessagingEndpoint, address *v1.MessagingAddress) (map[Host][]BrokerEntity, error) {

	if !endpoint.IsActive() {
		return nil, fmt.Errorf("inactive endpoint")
	}

	brokerEntities := make(map[Host][]BrokerEntity, 0)
	projectId := endpoint.Status.Host

	// Build desired state
	fullAddress := qualifiedAddress(projectId, address.GetAddress())
	if address.Spec.Queue != nil {
		for _, broker := range address.Status.Brokers {
			host := i.hostMap[broker.Host]
			brokerState := i.brokers[host]
			if brokerState == nil {
				return nil, fmt.Errorf("unable to configure queue (project %s address %s) for unknown broker %s", projectId, address.GetAddress(), broker.Host)
			}
			if len(brokerEntities[host]) == 0 {
				brokerEntities[host] = make([]BrokerEntity, 0)
			}
			brokerEntities[host] = append(brokerEntities[host], &BrokerAddress{
				Name:        fullAddress,
				RoutingType: RoutingTypeAnycast,
			})
			// TODO: Influence setting based on properties and plans
			brokerEntities[host] = append(brokerEntities[host], &BrokerQueue{
				Name:               fullAddress,
				Address:            fullAddress,
				RoutingType:        RoutingTypeAnycast,
				MaxConsumers:       -1,
				Durable:            true,
				PurgeOnNoConsumers: false,
				AutoCreateAddress:  false,
			})

			settings := createDefaultAddressSettings(fullAddress)
			if address.Spec.Queue.DeadLetterAddress != "" {
				settings.DeadLetterAddress = qualifiedAddress(projectId, address.Spec.Queue.DeadLetterAddress)
			}

			if address.Spec.Queue.ExpiryAddress != "" {
				settings.ExpiryAddress = qualifiedAddress(projectId, address.Spec.Queue.ExpiryAddress)
			}

			brokerEntities[host] = append(brokerEntities[host], settings)
		}
	} else if address.Spec.DeadLetter != nil {
		for _, brokerState := range i.brokers {
			host := brokerState.Host()
			if len(brokerEntities[host]) == 0 {
				brokerEntities[host] = make([]BrokerEntity, 0)
			}
			brokerEntities[host] = append(brokerEntities[host], &BrokerAddress{
				Name:        fullAddress,
				RoutingType: RoutingTypeAnycast,
			})
			brokerEntities[host] = append(brokerEntities[host], &BrokerQueue{
				Name:               fullAddress,
				Address:            fullAddress,
				RoutingType:        RoutingTypeAnycast,
				MaxConsumers:       -1,
				Durable:            true,
				PurgeOnNoConsumers: false,
				AutoCreateAddress:  false,
			})
		}
	} else if address.Spec.Topic != nil {
		// TODO: Influence setting based on properties and plans
		for _, broker := range address.Status.Brokers {
			host := i.hostMap[broker.Host]
			brokerState := i.brokers[host]
			if brokerState == nil {
				return nil, fmt.Errorf("unable to configure topic (project %s address %s) for unknown broker %s", projectId, address.GetAddress(), broker.Host)
			}
			if len(brokerEntities[host]) == 0 {
				brokerEntities[host] = make([]BrokerEntity, 0)
			}
			brokerEntities[host] = append(brokerEntities[host], &BrokerAddress{
				Name:        fullAddress,
				RoutingType: RoutingTypeMulticast,
			})
		}
	} else if address.Spec.Subscription != nil {
		// TODO: Influence setting based on properties and plans
		for _, broker := range address.Status.Brokers {
			host := i.hostMap[broker.Host]
			brokerState := i.brokers[host]
			if brokerState == nil {
				return nil, fmt.Errorf("unable to configure subscription (project %s address %s) for unknown broker %s", projectId, address.GetAddress(), broker.Host)
			}
			if len(brokerEntities[host]) == 0 {
				brokerEntities[host] = make([]BrokerEntity, 0)
			}
			brokerEntities[host] = append(brokerEntities[host], &BrokerQueue{
				Name:               fullAddress,
				Address:            qualifiedAddress(projectId, address.Spec.Subscription.Topic),
				RoutingType:        RoutingTypeMulticast,
				MaxConsumers:       1,
				Durable:            true,
				PurgeOnNoConsumers: false,
				AutoCreateAddress:  false,
			})
		}
	}
	return brokerEntities, nil
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

func (i *infraClient) DeleteEndpoint(endpoint *v1.MessagingEndpoint) error {
	log.Info(fmt.Sprintf("Deleting endpoint %s/%s", endpoint.Namespace, endpoint.Name))
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

func (i *infraClient) DeleteAddress(address *v1.MessagingAddress) error {
	log.Info(fmt.Sprintf("Deleting address %s/%s", address.Namespace, address.Name))
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

func (i *infraClient) DeleteProject(project *v1.MessagingProject) error {
	log.Info(fmt.Sprintf("Deleting project %s/%s", project.Namespace, project.Name))
	req := &request{
		resource: project,
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
	log.Info(fmt.Sprintf("Going to delete %d resources in infra", len(toDelete)))
	i.lock.Lock()
	defer i.lock.Unlock()

	if !i.initialized {
		for _, req := range toDelete {
			req.done <- NotInitializedError
		}
		return
	}

	checkDelete := func(resource metav1.Object) error {
		var err error
		for key, _ := range i.addresses {
			if resource.GetNamespace() == key.Namespace {
				err = ResourceInUseError
				break
			}
		}
		return err
	}
	valid := make([]*request, 0, len(toDelete))
	for _, req := range toDelete {
		switch req.resource.(type) {
		case *v1.MessagingProject:
			err := checkDelete(req.resource)
			if err != nil {
				req.done <- err
				continue
			}
			valid = append(valid, req)
		case *v1.MessagingEndpoint:
			err := checkDelete(req.resource)
			if err != nil {
				req.done <- err
				continue
			}
			valid = append(valid, req)
		default:
			valid = append(valid, req)
		}
	}

	builtRequests, routerEntities, brokerEntities := i.buildEntities(valid)

	ctx := context.Background()
	err := i.deleteEntities(ctx, routerEntities, brokerEntities)
	for _, req := range builtRequests {
		key := resourceKey{Name: req.resource.GetName(), Namespace: req.resource.GetNamespace()}
		switch v := req.resource.(type) {
		case *v1.MessagingAddress:
			if err == nil {
				delete(i.addresses, key)
				log.Info(fmt.Sprintf("Deleted address %s/%s", key.Namespace, key.Name))
			}
			req.done <- err
		case *v1.MessagingEndpoint:
			endpoint := v
			if err == nil {
				i.freePortsInternal(endpoint)
				delete(i.endpoints, key)
				log.Info(fmt.Sprintf("Deleted endpoint %s/%s", endpoint.Namespace, endpoint.Name))
			}
			req.done <- err
		case *v1.MessagingProject:
			if err == nil {
				delete(i.projects, key)
				log.Info(fmt.Sprintf("Deleted project %s/%s", key.Namespace, key.Name))
			}
			req.done <- err
		default:
			req.done <- fmt.Errorf("unknown resource type %T", v)
		}
		log.Info(fmt.Sprintf("State updated to (err %+v) for %s/%s", err, key.Namespace, key.Name))
	}
}

func (i *infraClient) deleteEntities(ctx context.Context, routerEntities []RouterEntity, brokerEntities map[Host][]BrokerEntity) error {
	// Delete from routers
	err := i.applyRouters(ctx, func(router *RouterState) error {
		return router.DeleteEntities(ctx, routerEntities)
	})
	if err != nil {
		return err
	}

	// Delete from brokers
	return i.applyBrokers(ctx, func(brokerState *BrokerState) error {
		return brokerState.DeleteEntities(ctx, brokerEntities[brokerState.Host()])
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

func createDefaultAddressSettings(address string) *BrokerAddressSetting {
	return &BrokerAddressSetting{
		Name:                     address,
		ExpiryDelay:              -1,
		DeliveryAttempts:         10,
		MaxSizeBytes:             -1,
		PageSizeBytes:            10485760,
		PageMaxCacheSize:         5,
		RedeliveryDelay:          0,
		RedeliveryMultiplier:     1.0,
		MaxRedeliveryDelay:       10000,
		RedistributionDelay:      -1,
		AddressFullMessagePolicy: AddressFullPolicyFail,
		SlowConsumerThreshold:    -1,
		SlowConsumerCheckPeriod:  -1,
		SlowConsumerPolicy:       SlowConsumerPolicyKill,
	}
}

func autoLinkName(projectId string, address *v1.MessagingAddress, host string, direction string) string {
	return fmt.Sprintf("autoLink-%s-%s-%s-%s", projectId, address.Name, host, direction)
}

func globalLinkRouteName(projectId string, host string, direction string) string {
	return fmt.Sprintf("linkRoute-%s-%s-%s", projectId, host, direction)
}

func linkRouteName(projectId string, address *v1.MessagingAddress, host string, direction string) string {
	return fmt.Sprintf("linkRoute-%s-%s-%s-%s", projectId, address.Name, host, direction)
}

func addressName(projectId string, address *v1.MessagingAddress) string {
	return fmt.Sprintf("address-%s-%s", projectId, address.Name)
}

func qualifiedAddress(projectId string, address string) string {
	return fmt.Sprintf("%s/%s", projectId, address)
}

func connectorName(broker *BrokerState) string {
	return fmt.Sprintf("connector-%s-%d", broker.Host().Hostname, broker.Port())
}

func portName(endpoint *v1.MessagingEndpoint, protocol v1.MessagingEndpointProtocol) string {
	return fmt.Sprintf("%s-%s-%s", endpoint.Namespace, endpoint.Name, protocol)
}

func listenerName(portName string) string {
	return fmt.Sprintf("listener-%s", portName)
}

func sslProfileName(portName string) string {
	return fmt.Sprintf("sslProfile-%s", portName)
}
