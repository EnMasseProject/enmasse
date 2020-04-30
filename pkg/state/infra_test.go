/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"testing"
	"time"

	fakecommand "github.com/enmasseproject/enmasse/pkg/amqpcommand/test"
	"github.com/stretchr/testify/assert"
	"pack.ag/amqp"
)

type testClock struct {
	now time.Time
}

func (t *testClock) Now() time.Time {
	return t.now
}

func TestSyncConnectors(t *testing.T) {
	rclient := fakecommand.NewFakeClient()
	bclient := fakecommand.NewFakeClient()
	i := NewInfra(func(host string, port int32) *RouterState {
		return &RouterState{
			host:          host,
			port:          port,
			connectors:    make(map[string]*RouterConnector, 0),
			commandClient: rclient,
		}
	}, func(host string, port int32) *BrokerState {
		return &BrokerState{
			Host:          host,
			Port:          port,
			commandClient: bclient,
		}
	}, &testClock{})
	assert.NotNil(t, i)

	bclient.Handler = func(req *amqp.Message) (*amqp.Message, error) {
		return &amqp.Message{
			ApplicationProperties: map[string]interface{}{"_AMQ_OperationSucceeded": true},
			Value:                 "[[\"q1\",\"q2\"]]",
		}, nil
	}

	rclient.Handler = func(req *amqp.Message) (*amqp.Message, error) {
		return &amqp.Message{
			ApplicationProperties: map[string]interface{}{
				"statusCode": int32(201),
			},
			Value: map[string]interface{}{
				"attributeNames":   []interface{}{},
				"results":          []interface{}{},
				"connectionStatus": "CONNECTED",
				"connectionMsg":    "",
			}}, nil
	}

	statuses, err := i.SyncAll([]string{"r1.example.com", "r2.example.com"}, []string{"b1.example.com", "b2.example.com"})
	assert.Nil(t, err)
	assert.Equal(t, 2, len(i.routers["r1.example.com"].connectors))
	assert.Equal(t, 2, len(i.routers["r2.example.com"].connectors))
	assert.Equal(t, 4, len(statuses))
}
