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

func NewRouterState(host string, port int32) *RouterState {
	state := &RouterState{
		host:        host,
		port:        port,
		initialized: false,
		entities: map[RouterEntityType]map[string]RouterEntity{
			RouterConnectorEntity: make(map[string]RouterEntity, 0),
			RouterListenerEntity:  make(map[string]RouterEntity, 0),
			RouterAddressEntity:   make(map[string]RouterEntity, 0),
			RouterAutoLinkEntity:  make(map[string]RouterEntity, 0),
		},
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
	totalEntities := 0
	entityTypes := []RouterEntityType{RouterConnectorEntity, RouterListenerEntity, RouterAddressEntity, RouterAutoLinkEntity}
	for _, t := range entityTypes {
		list, err := r.readEntities(t)
		if err != nil {
			log.Printf("[Router %s] Error during initialization: %+v", r.host, err)
			return err
		}
		r.entities[t] = list
		totalEntities += len(list)
	}

	log.Printf("[Router %s] Initialized controller state with %d entities", r.host, totalEntities)
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

func (r *RouterState) readEntities(entityType RouterEntityType) (map[string]RouterEntity, error) {
	v, err := r.queryEntities(entityType)
	if err != nil {
		return nil, err
	}

	attributeNames := v["attributeNames"].([]interface{})
	results := v["results"].([]interface{})

	data, err := createMapData(attributeNames, results)
	if err != nil {
		return nil, err
	}

	entities := make(map[string]RouterEntity, 0)
	for _, entry := range data {
		entity, err := entityType.Decode(entry)
		if err != nil {
			return nil, err
		}
		entities[entity.GetName()] = entity
	}

	return entities, nil
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

func (r *RouterState) deleteEntity(entity RouterEntityType, name string) error {
	properties := make(map[string]interface{})
	properties["operation"] = "DELETE"
	properties["type"] = string(entity)
	properties["name"] = name

	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
	}

	response, err := r.doRequest(request)
	if err != nil {
		return err
	}

	code, err := getStatusCode(response)
	if err != nil {
		return err
	}

	// If the resource is already gone, thats ok
	if (code < 200 || code >= 300) && code != 404 {
		return fmt.Errorf("response with status code %d: %+v", code, getStatusDescription(response))
	}
	return nil
}

/**
 * Perform management request against this router.
 */
func (r *RouterState) doRequest(request *amqp.Message) (*amqp.Message, error) {
	response, err := r.commandClient.RequestWithTimeout(request, 10*time.Second)
	return response, err
}

func isConnectionError(err error) bool {
	return errors.Is(err, amqp.ErrConnClosed) || errors.Is(err, amqpcommand.NotConnectedError)
}

func (r *RouterState) createEntity(entity RouterEntityType, name string, data map[string]interface{}) error {
	properties := make(map[string]interface{})
	properties["operation"] = "CREATE"
	properties["type"] = string(entity)
	properties["name"] = name
	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
		Value:                 data,
	}

	response, err := r.doRequest(request)
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

func (r *RouterState) readEntity(entityType RouterEntityType, name string) (RouterEntity, error) {
	properties := make(map[string]interface{})
	properties["operation"] = "READ"
	properties["type"] = string(entityType)
	properties["name"] = name

	request := &amqp.Message{
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
	}

	response, err := r.doRequest(request)
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
		return entityType.Decode(response.Value.(map[string]interface{}))
	default:
		log.Printf("Response: %+v", response)
		return nil, fmt.Errorf("unexpected value with type %T", v)
	}
}

func (r *RouterState) queryEntities(entity RouterEntityType, attributes ...string) (map[string]interface{}, error) {
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

	response, err := r.doRequest(request)
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

func (r *RouterState) ReadEntities(ctx context.Context, entities []RouterEntity) ([]RouterEntity, error) {
	g, _ := errgroup.WithContext(ctx)
	completed := make(chan RouterEntity, len(entities))
	for _, entity := range entities {
		e := entity
		t := e.Type()
		g.Go(func() error {
			read, err := r.readEntity(t, e.GetName())
			if err != nil {
				return err
			}
			completed <- read
			return nil
		})
	}
	err := g.Wait()
	close(completed)

	if isConnectionError(err) {
		r.Reset()
	}
	if err != nil {
		log.Printf("[Router %s] ReadEntities error: %+v", r.host, err)
		return nil, err
	}

	result := make([]RouterEntity, 0, len(entities))
	// Serialize completed
	for entity := range completed {
		result = append(result, entity)
	}

	return result, err
}

func (r *RouterState) EnsureEntities(ctx context.Context, entities []RouterEntity) error {
	toCreate := make([]RouterEntity, 0, len(entities))
	for _, entity := range entities {
		typeMap := r.entities[entity.Type()]
		existing, ok := typeMap[entity.GetName()]
		if ok {
			if !existing.Equals(entity) {
				log.Printf("Changing from '%+v' to '%+v'\n", existing, entity)
				return fmt.Errorf("router entity %s %s was updated - updates are not supported", entity.Type(), existing.GetName())
			}
		} else {
			toCreate = append(toCreate, entity)
		}

	}

	g, _ := errgroup.WithContext(ctx)
	completed := make(chan RouterEntity, len(toCreate))
	for _, entity := range toCreate {
		e := entity
		t := e.Type()
		value, err := t.Encode(e)
		if err != nil {
			return err
		}
		g.Go(func() error {
			log.Printf("[Router %s] Creating entity %s %s", r.host, e.Type(), e.GetName())
			err := r.createEntity(t, e.GetName(), value)
			if err != nil {
				return err
			}
			completed <- e
			return nil
		})
	}
	err := g.Wait()
	close(completed)

	if isConnectionError(err) {
		r.Reset()
	}
	if err != nil {
		log.Printf("[Router %s] EnsureEntities error: %+v", r.host, err)
		return err
	}

	// Serialize completed
	for entity := range completed {
		r.entities[entity.Type()][entity.GetName()] = entity
	}

	return err
}

func (r *RouterState) DeleteEntities(ctx context.Context, names []RouterEntity) error {
	g, _ := errgroup.WithContext(ctx)
	completed := make(chan RouterEntity, len(names))
	for _, name := range names {
		n := name
		g.Go(func() error {
			log.Printf("[Router %s] Deleting entity %s %s", r.host, n.Type(), n.GetName())
			err := r.deleteEntity(n.Type(), n.GetName())
			if err != nil {
				return err
			}
			completed <- n
			return nil
		})
	}
	err := g.Wait()
	close(completed)

	if isConnectionError(err) {
		r.Reset()
	}
	if err != nil {
		log.Printf("[Router %s] DeleteEntities error: %+v", r.host, err)
		return err
	}

	// Serialize completed
	for entity := range completed {
		delete(r.entities[entity.Type()], entity.GetName())
	}
	return err
}

func (e *RouterConnector) Type() RouterEntityType {
	return RouterConnectorEntity
}

func (e *RouterConnector) GetName() string {
	return e.Name
}

func (e *RouterConnector) Equals(other RouterEntity) bool {
	v, ok := other.(*RouterConnector)
	if !ok {
		return false
	}
	return v.Name == e.Name &&
		v.Host == e.Host &&
		v.Port == e.Port &&
		v.Role == e.Role &&
		v.SslProfile == e.SslProfile &&
		v.SaslMechanisms == e.SaslMechanisms &&
		v.SaslUsername == e.SaslUsername &&
		v.SaslPassword == e.SaslPassword &&
		v.LinkCapacity == e.LinkCapacity &&
		v.IdleTimeoutSeconds == e.IdleTimeoutSeconds &&
		v.VerifyHostname == e.VerifyHostname &&
		v.PolicyVhost == e.PolicyVhost

}

func (e *RouterAddress) Type() RouterEntityType {
	return RouterAddressEntity
}

func (e *RouterAddress) GetName() string {
	return e.Name
}

func (e *RouterAddress) Equals(other RouterEntity) bool {
	return reflect.DeepEqual(e, other)
}

func (e *RouterListener) Type() RouterEntityType {
	return RouterListenerEntity
}

func (e *RouterListener) GetName() string {
	return e.Name
}

func (e *RouterListener) Equals(other RouterEntity) bool {
	return reflect.DeepEqual(e, other)
}

func (e *RouterAutoLink) Type() RouterEntityType {
	return RouterAutoLinkEntity
}

func (e *RouterAutoLink) GetName() string {
	return e.Name
}

func (e *RouterAutoLink) Equals(other RouterEntity) bool {
	return reflect.DeepEqual(e, other)
}

func (e *NamedEntity) Type() RouterEntityType {
	return e.EntityType
}

func (e *NamedEntity) GetName() string {
	return e.Name
}

func (e *NamedEntity) Equals(other RouterEntity) bool {
	v, ok := other.(*NamedEntity)
	if !ok {
		return false
	}
	return v.Name == e.Name && v.EntityType == e.EntityType
}

func (t *RouterEntityType) CanUpdate() bool {
	return false
}
func (c *RouterEntityType) Encode(entity RouterEntity) (map[string]interface{}, error) {
	return entityToMap(entity)
}

func (c *RouterEntityType) Decode(data map[string]interface{}) (entity RouterEntity, err error) {
	switch *c {
	case RouterConnectorEntity:
		entity = &RouterConnector{}
		err = mapToEntity(data, entity)
	case RouterListenerEntity:
		entity = &RouterListener{}
		err = mapToEntity(data, entity)
	case RouterAddressEntity:
		entity = &RouterAddress{}
		err = mapToEntity(data, entity)
	case RouterAutoLinkEntity:
		entity = &RouterAutoLink{}
		err = mapToEntity(data, entity)
	default:
		err = fmt.Errorf("unknown entity %s", *c)
	}
	return
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

func mapToEntity(data map[string]interface{}, entity RouterEntity) error {
	out, err := json.Marshal(data)
	if err != nil {
		return err
	}

	err = json.Unmarshal(out, entity)
	if err != nil {
		return err
	}

	return nil
}

func entityToMap(v interface{}) (map[string]interface{}, error) {

	out, err := json.Marshal(v)
	if err != nil {
		return nil, err
	}

	var f interface{}

	err = json.Unmarshal(out, &f)
	if err != nil {
		return nil, err
	}

	return f.(map[string]interface{}), nil
}
