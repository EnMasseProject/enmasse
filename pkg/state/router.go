/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"encoding/json"
	"fmt"
	"log"
	"reflect"
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"

	"pack.ag/amqp"
)

const routerCommandAddress = "$management"
const routerCommandResponseAddress = "router_command_response"

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

func (r *RouterState) Initialize() error {
	if r.initialized {
		return nil
	}

	connectors, err := r.readConnectors()
	if err != nil {
		return err
	}

	r.connectors = connectors
	r.initialized = true
	return nil
}

/*
 * Reset router state from router (i.e. drop all internal state and rebuild from actual router state)
 */
func (r *RouterState) Reset() {
	if r.commandClient != nil {
		r.commandClient.Stop()
	}
	r.commandClient.Start()
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
	name := fmt.Sprintf("%s_%s", connector.Host, connector.Port)

	for _, existing := range r.connectors {
		// This is the same connector. Report error if settings have changed
		if existing.Name == connector.Name {
			if !reflect.DeepEqual(connector, existing) {
				return fmt.Errorf("Router connector %s:%s was updated - connector updates are not supported", existing.Host, existing.Port)
			} else {
				return nil
			}
		}
	}

	if !r.commandClient.Connected() {
		return &NotConnectedError{router: r.host}
	}
	log.Printf("[Router %s] Creating connector %s", r.host, connector.Name)

	entity, err := entityToMap(connector)
	if err != nil {
		return err
	}

	// No connector found so we need to create it
	err = r.createEntity("connector", name, entity)
	if err != nil {
		return err
	}

	log.Printf("[Router %s] Connector %s:%s created", r.host, connector.Host, connector.Port)
	r.connectors = append(r.connectors, connector)
	return nil
}

func (r *RouterState) readConnectors() ([]*RouterConnector, error) {
	if !r.commandClient.Connected() {
		return nil, &NotConnectedError{router: r.host}
	}
	v, err := r.queryEntities("connector")
	if err != nil {
		return nil, err
	}

	attributeNames := v["attributeNames"].([]interface{})
	results := v["results"].([]interface{})

	data, err := createMapData(attributeNames, results)
	if err != nil {
		return nil, err
	}

	connectors := make([]*RouterConnector, 0)
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
		connectors = append(connectors, &connector)
	}

	return connectors, nil
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
	if !r.commandClient.Connected() {
		return nil, &NotConnectedError{router: r.host}
	}
	name := fmt.Sprintf("%s_%s", connector.Host, connector.Port)
	v, err := r.readEntity("connector", name)
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
		case string:
			result[k] = t
		}
	}

	return result, nil
}

func (r *RouterState) deleteEntity(entity string, name string) error {
	properties := make(map[string]interface{})
	properties["operation"] = "DELETE"
	properties["type"] = entity
	properties["name"] = name

	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
	}

	_, err := r.commandClient.RequestWithTimeout(request, 10*time.Second)
	if err != nil {
		return err
	}
	return nil
}

func (r *RouterState) createEntity(entity string, name string, data map[interface{}]interface{}) error {
	properties := make(map[string]interface{})
	properties["operation"] = "CREATE"
	properties["type"] = entity
	properties["name"] = name
	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
		Value:                 data,
	}

	_, err := r.commandClient.RequestWithTimeout(request, 10*time.Second)
	if err != nil {
		return err
	}
	return nil

}

func (r *RouterState) readEntity(entity string, name string) (map[string]interface{}, error) {
	properties := make(map[string]interface{})
	properties["operation"] = "READ"
	properties["type"] = entity
	properties["name"] = name

	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
	}

	response, err := r.commandClient.RequestWithTimeout(request, 10*time.Second)
	if err != nil {
		return nil, err
	}

	switch v := response.Value.(type) {
	case map[string]interface{}:
		return response.Value.(map[string]interface{}), nil
	default:
		return nil, fmt.Errorf("Unexpected value with type %T", v)
	}
}

func (r *RouterState) queryEntities(entity string, attributes ...string) (map[string]interface{}, error) {
	properties := make(map[string]interface{})
	properties["operation"] = "QUERY"
	properties["entityType"] = entity

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

	response, err := r.commandClient.RequestWithTimeout(request, 10*time.Second)
	if err != nil {
		return nil, err
	}

	switch v := response.Value.(type) {
	case map[string]interface{}:
		return response.Value.(map[string]interface{}), nil
	default:
		return nil, fmt.Errorf("Unexpected value with type %T", v)
	}
}

/*
 * Ensure that a given listener exists.
 */
func (r *RouterState) EnsureListener(address *RouterListener) error {
	// TODO: Implement
	return nil
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
func (r *RouterState) EnsureAddress(address *RouterAddress) error {
	for _, existing := range r.addresses {
		// This is the same address. Report error if settings have changed
		if existing.Name == address.Name {
			if !reflect.DeepEqual(address, existing) {
				return fmt.Errorf("Router address %s was updated - address updates are not supported", existing.Name)
			} else {
				return nil
			}
		}
	}

	if !r.commandClient.Connected() {
		return &NotConnectedError{router: r.host}
	}
	log.Printf("[Router %s] Creating address %s", r.host, address.Name)

	entity, err := entityToMap(address)
	if err != nil {
		return err
	}

	// No address found so we need to create it
	err = r.createEntity("address", address.Name, entity)
	if err != nil {
		return err
	}
	return nil
}

func (r *RouterState) DeleteAddress(name string) error {
	return r.deleteEntity("address", name)
}

func (r *RouterState) DeleteAutoLink(name string) error {
	return r.deleteEntity("autoLink", name)
}

/*
 * Ensure that a given autoLink exists.
 */
func (r *RouterState) EnsureAutoLink(autoLink *RouterAutoLink) error {
	for _, existing := range r.autoLinks {
		// This is the same autoLink. Report error if settings have changed
		if existing.Name == autoLink.Name {
			if !reflect.DeepEqual(autoLink, existing) {
				return fmt.Errorf("Router autoLink %s was updated - autoLink updates are not supported", existing.Name)
			} else {
				return nil
			}
		}
	}

	if !r.commandClient.Connected() {
		return &NotConnectedError{router: r.host}
	}
	log.Printf("[Router %s] Creating autoLink %s", r.host, autoLink.Name)

	entity, err := entityToMap(autoLink)
	if err != nil {
		return err
	}

	// No autoLink found so we need to create it
	err = r.createEntity("autoLink", autoLink.Name, entity)
	if err != nil {
		return err
	}
	return nil
}

/*
 * Ensure that a given linkRoute exists.
 */
func (r *RouterState) EnsureLinkRoute(address *RouterLinkRoute) error {
	// TODO: Implement
	return nil
}
