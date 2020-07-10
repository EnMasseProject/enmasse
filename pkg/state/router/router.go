/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package router

import (
	"context"
	"crypto/tls"
	"encoding/json"
	"strings"

	//"errors"
	"fmt"
	"reflect"
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"
	. "github.com/enmasseproject/enmasse/pkg/state/common"
	. "github.com/enmasseproject/enmasse/pkg/state/errors"
	"golang.org/x/sync/errgroup"

	logf "sigs.k8s.io/controller-runtime/pkg/log"

	"pack.ag/amqp"
)

var log = logf.Log.WithName("router")

const (
	routerCommandAddress         = "$management"
	routerCommandResponseAddress = "router_command_response"
	maxOrder                     = 2
)

func NewRouterState(host Host, port int32, tlsConfig *tls.Config) *RouterState {
	opts := make([]amqp.ConnOption, 0)
	opts = append(opts, amqp.ConnConnectTimeout(10*time.Second))
	opts = append(opts, amqp.ConnProperty("product", "controller-manager"))

	if tlsConfig != nil {
		opts = append(opts, amqp.ConnSASLExternal())
		opts = append(opts, amqp.ConnTLS(true))
		opts = append(opts, amqp.ConnTLSConfig(tlsConfig))
	}
	state := &RouterState{
		host:        host,
		port:        port,
		initialized: false,
		entities: map[RouterEntityType]map[string]RouterEntity{
			RouterConnectorEntity:  make(map[string]RouterEntity, 0),
			RouterListenerEntity:   make(map[string]RouterEntity, 0),
			RouterAddressEntity:    make(map[string]RouterEntity, 0),
			RouterAutoLinkEntity:   make(map[string]RouterEntity, 0),
			RouterLinkRouteEntity:  make(map[string]RouterEntity, 0),
			RouterSslProfileEntity: make(map[string]RouterEntity, 0),
		},
		commandClient: amqpcommand.NewCommandClient(fmt.Sprintf("amqps://%s:%d", host.Ip, port),
			routerCommandAddress,
			routerCommandResponseAddress,
			opts...),
	}
	state.commandClient.Start()
	state.reconnectCount = state.commandClient.ReconnectCount()
	return state
}

func NewTestRouterState(host Host, port int32, client amqpcommand.Client) *RouterState {
	return &RouterState{
		host:          host,
		port:          port,
		commandClient: client,
		entities:      make(map[RouterEntityType]map[string]RouterEntity, 0),
	}
}

func (r *RouterState) Host() Host {
	return r.host
}

func (r *RouterState) Port() int32 {
	return r.port
}

func (r *RouterState) NextResync() time.Time {
	return r.nextResync
}

func (r *RouterState) Entities() map[RouterEntityType]map[string]RouterEntity {
	return r.entities
}

func (r *RouterState) Initialize(nextResync time.Time) error {
	if r.reconnectCount != r.commandClient.ReconnectCount() {
		r.initialized = false
	}

	if r.initialized {
		return nil
	}

	r.nextResync = nextResync

	log.Info(fmt.Sprintf("[Router %s] Initializing...", r.host))
	r.reconnectCount = r.commandClient.ReconnectCount()
	totalEntities := 0
	entityTypes := []RouterEntityType{RouterConnectorEntity, RouterListenerEntity, RouterAddressEntity, RouterAutoLinkEntity, RouterLinkRouteEntity, RouterSslProfileEntity}
	for _, t := range entityTypes {
		list, err := r.readEntities(t)
		if err != nil {
			return err
		}
		r.entities[t] = list
		totalEntities += len(list)
	}

	log.Info(fmt.Sprintf("[Router %s] Initialized controller state with %d entities", r.host, totalEntities))
	r.initialized = true
	return nil
}

/*
 * Reset router state from router (i.e. drop all internal state and rebuild from actual router state) if it has an initialized state.
 */
func (r *RouterState) Reset() {
	if r.commandClient != nil && r.initialized {
		log.Info(fmt.Sprintf("[Router %s] Resetting connection", r.host))
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
		return v, nil
	default:
		log.Info(fmt.Sprintf("Response: %+v", response))
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
	// TODO: Handle errors that are not strictly connection-related potentially with retries
	return err != nil
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

	if (code < 200 || code >= 300) && code != 404 {
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
		return entityType.Decode(v)
	default:
		log.Info(fmt.Sprintf("Response: %+v", response))
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

	// log.Info(fmt.Sprintf("Got response %+v\n", response))

	code, err := getStatusCode(response)
	if err != nil {
		return nil, err
	}

	if code < 200 || code >= 300 {
		return nil, fmt.Errorf("response with status code %d: %+v", code, getStatusDescription(response))
	}

	switch v := response.Value.(type) {
	case map[string]interface{}:
		return v, nil
	default:
		log.Info(fmt.Sprintf("Response: %+v", response))
		return nil, fmt.Errorf("unexpected value with type %T", v)
	}
}

func (r *RouterState) ReadEntities(ctx context.Context, entities []RouterEntity) ([]RouterEntity, error) {
	if !r.initialized {
		return nil, NotInitializedError
	}
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
		log.Info(fmt.Sprintf("[Router %s] ReadEntities error: %+v", r.host, err))
	}

	result := make([]RouterEntity, 0, len(entities))
	// Serialize completed
	for entity := range completed {
		result = append(result, entity)
	}

	return result, err
}

func (r *RouterState) EnsureEntities(ctx context.Context, entities []RouterEntity) error {
	if !r.initialized {
		return NotInitializedError
	}
	toCreate := make([]RouterEntity, 0, len(entities))
	for _, entity := range entities {
		typeMap := r.entities[entity.Type()]
		existing, ok := typeMap[entity.GetName()]
		if ok {
			if !existing.Equals(entity) {
				log.Info(fmt.Sprintf("Changing from '%+v' to '%+v'\n", existing, entity))
				return fmt.Errorf("router entity %s %s was updated - updates are not supported", entity.Type(), existing.GetName())
			}
		} else {
			toCreate = append(toCreate, entity)
		}

	}

	completed := make(chan RouterEntity, len(toCreate))
	var err error
	for order := 0; order < maxOrder; order++ {
		g, _ := errgroup.WithContext(ctx)
		for _, entity := range toCreate {
			e := entity
			if e.Order() == order {
				t := e.Type()
				value, err := t.Encode(e)
				if err != nil {
					return err
				}
				g.Go(func() error {
					log.Info(fmt.Sprintf("[Router %s] Creating entity %s %s: %+v", r.host, e.Type(), e.GetName(), value))
					err := r.createEntity(t, e.GetName(), value)
					if err != nil {
						return err
					}
					completed <- e
					return nil
				})
			}
		}
		err = g.Wait()
		if err != nil {
			break
		}
	}
	close(completed)

	if isConnectionError(err) {
		r.Reset()
	}
	if err != nil {
		log.Info(fmt.Sprintf("[Router %s] EnsureEntities error: %+v", r.host, err))
	}

	// Serialize completed
	for entity := range completed {
		r.entities[entity.Type()][entity.GetName()] = entity
	}

	return err
}

func (r *RouterState) DeleteEntities(ctx context.Context, names []RouterEntity) error {
	if !r.initialized {
		return NotInitializedError
	}
	g, _ := errgroup.WithContext(ctx)
	completed := make(chan RouterEntity, len(names))
	for _, name := range names {
		n := name
		g.Go(func() error {
			log.Info(fmt.Sprintf("[Router %s] Deleting entity %s %s", r.host, n.Type(), n.GetName()))
			err := r.deleteEntity(n.Type(), n.GetName())
			if err != nil {
				// TODO: Workaround for https://issues.apache.org/jira/browse/DISPATCH-1646, as HTTP listeners can't be deleted. We will ignore the error and
				// keep the entity in the local state.
				if strings.Contains(err.Error(), "HTTP listeners cannot be deleted") {
					return nil
				}
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
		log.Info(fmt.Sprintf("[Router %s] DeleteEntities error: %+v", r.host, err))
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
		reflect.DeepEqual(v.Role, e.Role) &&
		reflect.DeepEqual(v.SslProfile, e.SslProfile) &&
		reflect.DeepEqual(v.SaslMechanisms, e.SaslMechanisms) &&
		reflect.DeepEqual(v.SaslUsername, e.SaslUsername) &&
		reflect.DeepEqual(v.SaslPassword, e.SaslPassword) &&
		// 		reflect.DeepEqual(v.LinkCapacity, e.LinkCapacity) &&
		reflect.DeepEqual(v.IdleTimeoutSeconds, e.IdleTimeoutSeconds) &&
		reflect.DeepEqual(v.VerifyHostname, e.VerifyHostname) &&
		reflect.DeepEqual(v.PolicyVhost, e.PolicyVhost)
}

func (e *RouterConnector) Order() int {
	return 1
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

func (e *RouterAddress) Order() int {
	return 0
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

func (e *RouterListener) Order() int {
	return 1
}

func (e *RouterSslProfile) Type() RouterEntityType {
	return RouterSslProfileEntity
}

func (e *RouterSslProfile) GetName() string {
	return e.Name
}

func (e *RouterSslProfile) Equals(other RouterEntity) bool {
	return reflect.DeepEqual(e, other)
}

func (e *RouterSslProfile) Order() int {
	return 0
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

func (e *RouterAutoLink) Order() int {
	return 0
}

func (e *RouterLinkRoute) Type() RouterEntityType {
	return RouterLinkRouteEntity
}

func (e *RouterLinkRoute) GetName() string {
	return e.Name
}

func (e *RouterLinkRoute) Equals(other RouterEntity) bool {
	return reflect.DeepEqual(e, other)
}

func (e *RouterLinkRoute) Order() int {
	return 0
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

func (e *NamedEntity) Order() int {
	return 0
}

func (e *RouterAuthServicePlugin) Type() RouterEntityType {
	return RouterAuthServicePluginEntity
}

func (e *RouterAuthServicePlugin) GetName() string {
	return fmt.Sprintf("%s:%s", e.Host, e.Port)
}

func (e *RouterAuthServicePlugin) Equals(other RouterEntity) bool {
	return reflect.DeepEqual(e, other)
}

func (e *RouterAuthServicePlugin) Order() int {
	return 0
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
	case RouterLinkRouteEntity:
		entity = &RouterLinkRoute{}
		err = mapToEntity(data, entity)
	case RouterSslProfileEntity:
		entity = &RouterSslProfile{}
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

	data := f.(map[string]interface{})
	converted := make(map[string]interface{}, len(data))

	for k, v := range data {
		switch vt := v.(type) {
		// Conversion is needed as router does not accept float, nor does it use it in any entities so conversion should be safe.
		case float64:
			converted[k] = int(vt)
		default:
			//			log.Info(fmt.Sprintf("Key %s has value type %T", k, vt))
			converted[k] = vt
		}
	}

	return converted, nil
}

func FindFirst(pred func(RouterEntity) bool, entities []RouterEntity) RouterEntity {
	for _, entity := range entities {
		if pred(entity) {
			return entity
		}
	}
	return nil
}
