/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"reflect"
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"
	"golang.org/x/sync/errgroup"

	"pack.ag/amqp"
)

const (
	routerCommandAddress         = "$management"
	routerCommandResponseAddress = "router_command_response"
)

type routerEntity string

const (
	listenerEntity  routerEntity = "org.apache.qpid.dispatch.listener"
	connectorEntity routerEntity = "org.apache.qpid.dispatch.connector"
	addressEntity   routerEntity = "org.apache.qpid.dispatch.router.config.address"
	autoLinkEntity  routerEntity = "org.apache.qpid.dispatch.router.config.autoLink"
)

func NewRouterState(host string, port int32) *RouterState {
	state := &RouterState{
		host:        host,
		port:        port,
		initialized: false,
		commandClient: amqpcommand.NewCommandClient(fmt.Sprintf("amqp://%s:%d", host, port),
			routerCommandAddress,
			routerCommandResponseAddress,
			amqp.ConnConnectTimeout(10*time.Second),
			amqp.ConnProperty("product", "controller-manager")),
	}
	state.commandClient.Start()
	return state
}

func (r *RouterState) Initialize(nextResync time.Time) error {
	if r.initialized {
		return nil
	}

	r.nextResync = nextResync

	log.Printf("[Router %s] Initializing...", r.host)
	connectors, err := r.readConnectors()
	if err != nil {
		return err
	}
	r.connectors = connectors

	addresses, err := r.readAddresses()
	if err != nil {
		return err
	}
	r.addresses = addresses

	autoLinks, err := r.readAutoLinks()
	if err != nil {
		return err
	}
	r.autoLinks = autoLinks

	listeners, err := r.readListeners()
	if err != nil {
		return err
	}
	r.listeners = listeners

	log.Printf("[Router %s] Initialized controller state with %d connectors, %d addresses, %d autoLinks and %d listeners", r.host, len(connectors), len(addresses), len(autoLinks), len(listeners))
	r.initialized = true
	return nil
}

/*
 * Reset router state from router (i.e. drop all internal state and rebuild from actual router state) if it has an initialized state.
 */
func (r *RouterState) Reset() {
	if r.commandClient != nil && r.initialized {
		log.Printf("[Router %s] Resetting connection", r.host)
		r.commandClient.Stop()
		r.initialized = false
		r.commandClient.Start()
	}
}

func (r *RouterState) Shutdown() {
	if r.commandClient != nil {
		r.commandClient.Stop()
	}
}

/*
 * Ensure that a given connector exists.
 */
func (r *RouterState) EnsureConnector(connector *RouterConnector) error {
	for _, existing := range r.connectors {
		// This is the same connector. Report error if settings have changed
		if existing.Name == connector.Name {
			if !reflect.DeepEqual(connector, existing) {
				log.Printf("Changing from '%+v' to '%+v'\n", existing, connector)
				return fmt.Errorf("router connector %s:%s was updated - connector updates are not supported", existing.Host, existing.Port)
			} else {
				return nil
			}
		}
	}

	log.Printf("[Router %s] Creating connector %s", r.host, connector.Name)

	entity, err := entityToMap(connector)
	if err != nil {
		return err
	}

	// No connector found so we need to create it
	err = r.createEntity(connectorEntity, connector.Name, entity, true)
	if err != nil {
		return err
	}

	log.Printf("[Router %s] Connector %s:%s created", r.host, connector.Host, connector.Port)
	r.connectors[connector.Name] = connector
	return nil
}

func (r *RouterState) readConnectors() (map[string]*RouterConnector, error) {
	v, err := r.queryEntities(connectorEntity)
	if err != nil {
		return nil, err
	}

	attributeNames := v["attributeNames"].([]interface{})
	results := v["results"].([]interface{})

	data, err := createMapData(attributeNames, results)
	if err != nil {
		return nil, err
	}

	connectors := make(map[string]*RouterConnector, 0)
	for _, entry := range data {
		out, err := json.Marshal(entry)
		if err != nil {
			return nil, err
		}

		var connector RouterConnector

		err = json.Unmarshal(out, &connector)
		if err != nil {
			return nil, err
		}
		connectors[connector.Name] = &connector
	}

	return connectors, nil
}

/*
 * Ensure that a given listener exists.
 */
func (r *RouterState) EnsureListener(listener *RouterListener) error {
	for _, existing := range r.listeners {
		// This is the same listener. Report error if settings have changed
		if existing.Name == listener.Name {
			if !reflect.DeepEqual(listener, existing) {
				log.Printf("Changing from '%+v' to '%+v'\n", existing, listener)
				return fmt.Errorf("router listener %s:%s was updated - listener updates are not supported", existing.Host, existing.Port)
			} else {
				return nil
			}
		}
	}

	log.Printf("[Router %s] Creating listener %s", r.host, listener.Name)

	entity, err := entityToMap(listener)
	if err != nil {
		return err
	}

	// No listener found so we need to create it
	err = r.createEntity(listenerEntity, listener.Name, entity, true)
	if err != nil {
		return err
	}

	log.Printf("[Router %s] Listener %s:%s created", r.host, listener.Host, listener.Port)
	r.listeners[listener.Name] = listener
	return nil
}

func (r *RouterState) readAddresses() (map[string]*RouterAddress, error) {
	v, err := r.queryEntities(addressEntity)
	if err != nil {
		return nil, err
	}

	attributeNames := v["attributeNames"].([]interface{})
	results := v["results"].([]interface{})

	data, err := createMapData(attributeNames, results)
	if err != nil {
		return nil, err
	}

	addresses := make(map[string]*RouterAddress, 0)
	for _, entry := range data {
		out, err := json.Marshal(entry)
		if err != nil {
			return nil, err
		}

		var address RouterAddress

		err = json.Unmarshal(out, &address)
		if err != nil {
			return nil, err
		}
		addresses[address.Name] = &address
	}

	return addresses, nil
}

func (r *RouterState) readListeners() (map[string]*RouterListener, error) {
	v, err := r.queryEntities(listenerEntity)
	if err != nil {
		return nil, err
	}

	attributeNames := v["attributeNames"].([]interface{})
	results := v["results"].([]interface{})

	data, err := createMapData(attributeNames, results)
	if err != nil {
		return nil, err
	}

	listeners := make(map[string]*RouterListener, 0)
	for _, entry := range data {
		out, err := json.Marshal(entry)
		if err != nil {
			return nil, err
		}

		var listener RouterListener

		err = json.Unmarshal(out, &listener)
		if err != nil {
			return nil, err
		}
		listeners[listener.Name] = &listener
	}

	return listeners, nil
}

func (r *RouterState) readAutoLinks() (map[string]*RouterAutoLink, error) {
	v, err := r.queryEntities(autoLinkEntity)
	if err != nil {
		return nil, err
	}

	attributeNames := v["attributeNames"].([]interface{})
	results := v["results"].([]interface{})

	data, err := createMapData(attributeNames, results)
	if err != nil {
		return nil, err
	}

	autoLinks := make(map[string]*RouterAutoLink, 0)
	for _, entry := range data {
		out, err := json.Marshal(entry)
		if err != nil {
			return nil, err
		}

		var autoLink RouterAutoLink

		err = json.Unmarshal(out, &autoLink)
		if err != nil {
			return nil, err
		}
		autoLinks[autoLink.Name] = &autoLink
	}

	return autoLinks, nil
}

func createMapData(attributeNames []interface{}, results []interface{}) ([]map[string]interface{}, error) {
	list := make([]map[string]interface{}, 0)
	for _, result := range results {
		r := result.([]interface{})
		m := make(map[string]interface{}, 0)
		for i, attribute := range attributeNames {
			a := attribute.(string)
			v := r[i]
			if v != nil {
				m[a] = v
			}
		}
		list = append(list, m)
	}
	return list, nil
}

func (r *RouterState) GetConnectorStatus(connector *RouterConnector) (*ConnectorStatus, error) {
	v, err := r.readEntity(connectorEntity, connector.Name)
	if err != nil {
		return nil, err
	}

	connectionStatus := v["connectionStatus"].(string)
	connectionMsg := v["connectionMsg"].(string)

	status := &ConnectorStatus{
		Router:    r.host,
		Broker:    connector.Host,
		Connected: connectionStatus == "SUCCESS",
		Message:   connectionMsg,
	}

	return status, nil
}

func entityToMap(v interface{}) (map[interface{}]interface{}, error) {

	out, err := json.Marshal(v)
	if err != nil {
		return nil, err
	}

	var f interface{}

	err = json.Unmarshal(out, &f)
	if err != nil {
		return nil, err
	}

	s := f.(map[string]interface{})

	result := map[interface{}]interface{}{}

	for k, v := range s {
		switch t := v.(type) {
		case int32:
			result[k] = t
		case int:
			result[k] = t
		case bool:
			result[k] = t
		case string:
			result[k] = t
		}
	}

	return result, nil
}

func getStatusCode(response *amqp.Message) (int32, error) {
	code := response.ApplicationProperties["statusCode"]
	switch v := code.(type) {
	case int32:
		return code.(int32), nil
	default:
		log.Printf("Response: %+v", response)
		return 0, fmt.Errorf("unexpected value with type %T", v)
	}
}

func getStatusDescription(response *amqp.Message) interface{} {
	return response.ApplicationProperties["statusDescription"]
}

func (r *RouterState) deleteEntity(entity routerEntity, name string, resetOnDisconnect bool) error {
	properties := make(map[string]interface{})
	properties["operation"] = "DELETE"
	properties["type"] = string(entity)
	properties["name"] = name

	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
	}

	response, err := r.doRequest(request, true)
	if err != nil {
		return err
	}

	code, err := getStatusCode(response)
	if err != nil {
		return err
	}

	if code < 200 || code >= 300 {
		return fmt.Errorf("response with status code %d: %+v", code, getStatusDescription(response))
	}
	return nil
}

/**
 * Perform management request against this router. If resetOnDisconnect is set, the router state will be instructed
 * to be reset. This should be set to true if running in a single-threaded context, and the error should be handled
 * by the caller.
 */
func (r *RouterState) doRequest(request *amqp.Message, resetOnDisconnect bool) (*amqp.Message, error) {
	// If by chance we got disconnected while waiting for the request
	response, err := r.commandClient.RequestWithTimeout(request, 10*time.Second)
	if resetOnDisconnect && isConnectionError(err) {
		r.Reset()
	} else {
		log.Printf("Error is: %+v", err)
	}
	return response, err
}

func isConnectionError(err error) bool {
	return errors.Is(err, amqp.ErrConnClosed) || errors.Is(err, amqpcommand.NotConnectedError)
}

func isError(err error, targets ...error) bool {
	for _, target := range targets {
		if errors.Is(err, target) {
			return true
		}
	}
	return false
}

func (r *RouterState) createEntity(entity routerEntity, name string, data map[interface{}]interface{}, resetOnDisconnect bool) error {
	properties := make(map[string]interface{})
	properties["operation"] = "CREATE"
	properties["type"] = string(entity)
	properties["name"] = name
	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
		Value:                 data,
	}

	response, err := r.doRequest(request, resetOnDisconnect)
	if err != nil {
		return err
	}

	code, err := getStatusCode(response)
	if err != nil {
		return err
	}

	if code < 200 || code >= 300 {
		return fmt.Errorf("response with status code %d: %+v", code, getStatusDescription(response))
	}
	return nil
}

func (r *RouterState) readEntity(entity routerEntity, name string) (map[string]interface{}, error) {
	properties := make(map[string]interface{})
	properties["operation"] = "READ"
	properties["type"] = string(entity)
	properties["name"] = name

	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
	}

	response, err := r.doRequest(request, true)
	if err != nil {
		return nil, err
	}

	code, err := getStatusCode(response)
	if err != nil {
		return nil, err
	}

	if code < 200 || code >= 300 {
		return nil, fmt.Errorf("response with status code %d: %+v", code, getStatusDescription(response))
	}

	switch v := response.Value.(type) {
	case map[string]interface{}:
		return response.Value.(map[string]interface{}), nil
	default:
		log.Printf("Response: %+v", response)
		return nil, fmt.Errorf("unexpected value with type %T", v)
	}
}

func (r *RouterState) queryEntities(entity routerEntity, attributes ...string) (map[string]interface{}, error) {
	properties := make(map[string]interface{})
	properties["operation"] = "QUERY"
	properties["entityType"] = string(entity)

	data := make(map[string][]string, 0)
	if len(attributes) > 0 {
		data["attributeNames"] = attributes
	} else {
		data["attributeNames"] = make([]string, 0)
	}

	body, err := entityToMap(data)
	if err != nil {
		return nil, err
	}

	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
		Value:                 body,
	}

	response, err := r.doRequest(request, true)
	if err != nil {
		return nil, err
	}

	// log.Printf("Got response %+v\n", response)

	code, err := getStatusCode(response)
	if err != nil {
		return nil, err
	}

	if code < 200 || code >= 300 {
		return nil, fmt.Errorf("response with status code %d: %+v", code, getStatusDescription(response))
	}

	switch v := response.Value.(type) {
	case map[string]interface{}:
		return response.Value.(map[string]interface{}), nil
	default:
		log.Printf("Response: %+v", response)
		return nil, fmt.Errorf("unexpected value with type %T", v)
	}
}

/*
 * Ensure that a given vhost exists.
 */
func (r *RouterState) EnsureVhost(address *RouterVhost) error {
	// TODO: Implement
	return nil
}

/*
 * Ensure that a given address exists.
 */
func (r *RouterState) EnsureAddresses(addresses []*RouterAddress) error {
	toCreate := make([]*RouterAddress, 0, len(addresses))
	for _, address := range addresses {
		existing, ok := r.addresses[address.Name]
		if ok {
			if !reflect.DeepEqual(address, existing) {
				log.Printf("Changing from '%+v' to '%+v'\n", existing, address)
				return fmt.Errorf("router address %s was updated - address updates are not supported", existing.Name)
			}
		} else {
			toCreate = append(toCreate, address)
		}
	}

	g, _ := errgroup.WithContext(context.Background())
	completed := make(chan *RouterAddress, len(toCreate))
	for _, address := range toCreate {
		a := address
		entity, err := entityToMap(address)
		if err != nil {
			return err
		}
		g.Go(func() error {
			log.Printf("[Router %s] Creating address %s", r.host, a.Name)
			err := r.createEntity(addressEntity, a.Name, entity, false)
			if err != nil {
				return err
			}
			completed <- a
			return nil
		})
	}
	err := g.Wait()
	close(completed)

	// Serialize completed
	for address := range completed {
		r.addresses[address.Name] = address
	}

	if isConnectionError(err) {
		r.Reset()
	}
	return err
}

func (r *RouterState) DeleteAddresses(names []string) error {
	g, _ := errgroup.WithContext(context.Background())
	completed := make(chan string, len(names))
	for _, name := range names {
		n := name
		g.Go(func() error {
			err := r.deleteEntity(addressEntity, n, false)
			if err != nil {
				return err
			}
			completed <- n
			return nil
		})
	}
	err := g.Wait()
	close(completed)

	// Serialize completed
	for name := range completed {
		delete(r.addresses, name)
	}

	if isConnectionError(err) {
		r.Reset()
	}
	return err
}

func (r *RouterState) DeleteAutoLinks(names []string) error {
	g, _ := errgroup.WithContext(context.Background())
	completed := make(chan string, len(names))
	for _, name := range names {
		n := name
		g.Go(func() error {
			err := r.deleteEntity(autoLinkEntity, n, false)
			if err != nil {
				return err
			}
			completed <- n
			return nil
		})
	}
	err := g.Wait()
	close(completed)

	// Serialize completed
	for name := range completed {
		delete(r.autoLinks, name)
	}
	return err
}

func (r *RouterState) DeleteConnector(name string) error {
	err := r.deleteEntity(connectorEntity, name, true)
	if err != nil {
		return err
	}
	delete(r.connectors, name)
	return nil
}

func (r *RouterState) DeleteListener(name string) error {
	err := r.deleteEntity(listenerEntity, name, true)
	if err != nil {
		return err
	}
	delete(r.listeners, name)
	return nil
}

/*
 * Ensure that a given autoLink exists.
 */
func (r *RouterState) EnsureAutoLinks(autoLinks []*RouterAutoLink) error {
	toCreate := make([]*RouterAutoLink, 0, len(autoLinks))
	for _, autoLink := range autoLinks {
		existing, ok := r.autoLinks[autoLink.Name]
		if ok {
			if !reflect.DeepEqual(autoLink, existing) {
				log.Printf("Changing from '%+v' to '%+v'\n", existing, autoLink)
				return fmt.Errorf("router autoLink %s was updated - autoLink updates are not supported", existing.Name)
			}
		} else {
			toCreate = append(toCreate, autoLink)
		}
	}

	g, _ := errgroup.WithContext(context.Background())
	completed := make(chan *RouterAutoLink, len(toCreate))
	for _, autoLink := range toCreate {
		a := autoLink
		entity, err := entityToMap(autoLink)
		if err != nil {
			return err
		}
		g.Go(func() error {
			log.Printf("[Router %s] Creating autoLink %+v", r.host, a.Name)
			err := r.createEntity(autoLinkEntity, a.Name, entity, false)
			if err != nil {
				return err
			}
			completed <- a
			return nil
		})
	}
	err := g.Wait()
	close(completed)

	// Serialize completed
	for autoLink := range completed {
		r.autoLinks[autoLink.Name] = autoLink
	}

	if isConnectionError(err) {
		r.Reset()
	}
	return err
}

/*
 * Ensure that a given linkRoute exists.
 */
func (r *RouterState) EnsureLinkRoute(address *RouterLinkRoute) error {
	// TODO: Implement
	return nil
}
