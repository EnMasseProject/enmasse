/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	fakecommand "github.com/enmasseproject/enmasse/pkg/amqpcommand/test"

	"pack.ag/amqp"
)

func TestInitialize(t *testing.T) {
	client := fakecommand.NewFakeClient()

	state := &RouterState{
		host:        "",
		port:        0,
		initialized: false,
		entities: map[RouterEntityType]map[string]RouterEntity{
			RouterConnectorEntity: make(map[string]RouterEntity, 0),
			RouterListenerEntity:  make(map[string]RouterEntity, 0),
			RouterAddressEntity:   make(map[string]RouterEntity, 0),
			RouterAutoLinkEntity:  make(map[string]RouterEntity, 0),
		},
		commandClient: client,
	}

	client.Handler = func(req *amqp.Message) (*amqp.Message, error) {
		return &amqp.Message{
			ApplicationProperties: map[string]interface{}{
				"statusCode": int32(201),
			},
			Value: map[string]interface{}{
				"attributeNames": []interface{}{"name", "host", "port"},
				"results":        []interface{}{[]interface{}{"conn1", "example.com", "5672"}},
			}}, nil
	}

	err := state.Initialize(time.Time{})
	assert.Nil(t, err)
	assert.NotNil(t, state.entities)
	assert.NotNil(t, state.entities[RouterConnectorEntity])
	assert.Equal(t, 1, len(state.entities[RouterConnectorEntity]))
	assert.Equal(t, "example.com", state.entities[RouterConnectorEntity]["conn1"].(*RouterConnector).Host)

	// Adding connector should not call any command client
	err = state.EnsureEntities(context.TODO(), []RouterEntity{&RouterConnector{
		Name: "conn1",
		Host: "example.com",
		Port: "5672",
	}})
	assert.Nil(t, err)
}

func TestEnsureConnector(t *testing.T) {
	client := fakecommand.NewFakeClient()

	state := &RouterState{
		host:        "",
		port:        0,
		initialized: false,
		entities: map[RouterEntityType]map[string]RouterEntity{
			RouterConnectorEntity: make(map[string]RouterEntity, 0),
			RouterListenerEntity:  make(map[string]RouterEntity, 0),
			RouterAddressEntity:   make(map[string]RouterEntity, 0),
			RouterAutoLinkEntity:  make(map[string]RouterEntity, 0),
		},
		commandClient: client,
	}

	client.Handler = func(req *amqp.Message) (*amqp.Message, error) {
		return &amqp.Message{
			ApplicationProperties: map[string]interface{}{
				"statusCode": int32(201),
			},
			Value: map[string]interface{}{
				"attributeNames": []interface{}{},
				"results":        []interface{}{},
			}}, nil
	}

	err := state.Initialize(time.Time{})
	assert.Nil(t, err)
	assert.NotNil(t, state.entities)
	assert.NotNil(t, state.entities[RouterConnectorEntity])
	assert.Equal(t, 0, len(state.entities[RouterConnectorEntity]))

	// Adding connector should not call any command client
	err = state.EnsureEntities(context.TODO(), []RouterEntity{&RouterConnector{
		Name: "conn1",
		Host: "example.com",
		Port: "5672",
	}})
	assert.Nil(t, err)

	// Adding again should be no problem as long as it is the same
	err = state.EnsureEntities(context.TODO(), []RouterEntity{&RouterConnector{
		Name: "conn1",
		Host: "example.com",
		Port: "5672",
	}})
	assert.Nil(t, err)

	// Adding again is not ok when attributes have changed (not supported by qpid dispatch router)
	err = state.EnsureEntities(context.TODO(), []RouterEntity{&RouterConnector{
		Name: "conn1",
		Host: "example.com",
		Port: "5672",
		Role: "edge",
	}})
	assert.NotNil(t, err)
}
