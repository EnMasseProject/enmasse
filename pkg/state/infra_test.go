/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"sync"
	"testing"

	fakecommand "github.com/enmasseproject/enmasse/pkg/amqpcommand/test"
	"github.com/stretchr/testify/assert"
	"pack.ag/amqp"
)

func TestUpdateRouters(t *testing.T) {
	client := fakecommand.NewFakeClient()
	i := &infra{
		routers: make(map[string]*RouterState, 0),
		brokers: make(map[string]*BrokerState, 0),
		routerStateFactory: func(host string, port int32) *RouterState {
			return &RouterState{
				host:          host,
				port:          port,
				commandClient: client,
			}
		},
		lock: &sync.Mutex{},
	}
	assert.NotNil(t, i)

	i.UpdateRouters([]string{"r1.example.com", "r2.example.com"})
	assert.Equal(t, 2, len(i.routers))
}

func TestUpdateBrokers(t *testing.T) {
	i := &infra{
		routers: make(map[string]*RouterState, 0),
		brokers: make(map[string]*BrokerState, 0),
		brokerStateFactory: func(host string, port int32) *BrokerState {
			return &BrokerState{
				Host: host,
				Port: port,
			}
		},
		lock: &sync.Mutex{},
	}
	assert.NotNil(t, i)

	i.UpdateBrokers([]string{"r1.example.com", "r2.example.com"})
	assert.Equal(t, 2, len(i.brokers))
}

func TestSync(t *testing.T) {
	client := fakecommand.NewFakeClient()
	i := &infra{
		routers: make(map[string]*RouterState, 0),
		brokers: make(map[string]*BrokerState, 0),
		routerStateFactory: func(host string, port int32) *RouterState {
			return &RouterState{
				host:          host,
				port:          port,
				commandClient: client,
			}
		},
		brokerStateFactory: func(host string, port int32) *BrokerState {
			return &BrokerState{
				Host: host,
				Port: port,
			}
		},
		lock: &sync.Mutex{},
	}
	assert.NotNil(t, i)

	i.UpdateRouters([]string{"r1.example.com", "r2.example.com"})
	i.UpdateBrokers([]string{"b1.example.com", "b2.example.com"})

	assert.Equal(t, 2, len(i.routers))
	assert.Equal(t, 2, len(i.brokers))

	client.Handler = func(req *amqp.Message) (*amqp.Message, error) {
		return &amqp.Message{
			Value: map[string]interface{}{
				"attributeNames":   []interface{}{},
				"results":          []interface{}{},
				"connectionStatus": "CONNECTED",
				"connectionMsg":    "",
			}}, nil
	}

	err := i.Sync()
	assert.Nil(t, err)
	assert.Equal(t, 2, len(i.routers["r1.example.com"].connectors))
	assert.Equal(t, 2, len(i.routers["r2.example.com"].connectors))
}
