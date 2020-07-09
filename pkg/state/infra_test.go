/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"context"
	"crypto/tls"
	"fmt"
	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"testing"
	"time"

	fakecommand "github.com/enmasseproject/enmasse/pkg/amqpcommand/test"
	. "github.com/enmasseproject/enmasse/pkg/state/broker"
	. "github.com/enmasseproject/enmasse/pkg/state/common"
	. "github.com/enmasseproject/enmasse/pkg/state/router"
	"github.com/stretchr/testify/assert"
	"pack.ag/amqp"
)

type testClock struct {
	now time.Time
}

func (t *testClock) Now() time.Time {
	return t.now
}

func setUp(t *testing.T) (*infraClient, *fakecommand.FakeClient, *fakecommand.FakeClient) {
	rclient := fakecommand.NewFakeClient()
	bclient := fakecommand.NewFakeClient()
	ic := NewInfra("testinfa", "ns", func(host Host, port int32, _ *tls.Config) *RouterState {
		return NewTestRouterState(host, port, rclient)
	}, func(host Host, port int32, _ *tls.Config) *BrokerState {
		return NewTestBrokerState(host, port, bclient)
	}, &testClock{})
	assert.NotNil(t, ic)
	return ic, rclient, bclient
}

func TestUpdateRouters(t *testing.T) {
	i, _, _ := setUp(t)

	i.updateRouters([]Host{{Hostname: "r1.example.com", Ip: "10.0.0.1"}, {Hostname: "r2.example.com", Ip: "10.0.0.2"}})
	assert.Equal(t, 2, len(i.routers))
	assertRouter(t, i, "r1.example.com", "10.0.0.1")
	assertRouter(t, i, "r2.example.com", "10.0.0.2")

	i.updateRouters([]Host{{Hostname: "r1.example.com", Ip: "10.0.0.3"}, {Hostname: "r2.example.com", Ip: "10.0.0.2"}})
	assertRouter(t, i, "r1.example.com", "10.0.0.3")
	assertRouter(t, i, "r2.example.com", "10.0.0.2")
}

func TestUpdateBrokers(t *testing.T) {
	i, _, _ := setUp(t)

	err := i.updateBrokers(context.TODO(), []Host{{Hostname: "b1.example.com", Ip: "10.0.0.1"}, {Hostname: "b2.example.com", Ip: "10.0.0.2"}}, true)
	assert.Nil(t, err)
	assert.Equal(t, 2, len(i.brokers))
	assert.Equal(t, 2, len(i.hostMap))
	assertBroker(t, i, "b1.example.com", "10.0.0.1")
	assertBroker(t, i, "b2.example.com", "10.0.0.2")

	err = i.updateBrokers(context.TODO(), []Host{{Hostname: "b1.example.com", Ip: "10.0.0.3"}, {Hostname: "b2.example.com", Ip: "10.0.0.2"}}, true)
	assert.Nil(t, err)
	assertBroker(t, i, "b1.example.com", "10.0.0.3")
	assertBroker(t, i, "b2.example.com", "10.0.0.2")
}

func assertRouter(t *testing.T, i *infraClient, host string, ip string) {
	_, exists := i.routers[Host{Hostname: host, Ip: ip}]
	entry, existsMapping := i.hostMap[host]
	assert.True(t, exists)
	assert.True(t, existsMapping)
	assert.Equal(t, host, entry.Hostname)
	assert.Equal(t, ip, entry.Ip)
}

func assertBroker(t *testing.T, i *infraClient, host string, ip string) {
	_, exists := i.brokers[Host{Hostname: host, Ip: ip}]
	entry, existsMapping := i.hostMap[host]
	assert.True(t, exists)
	assert.True(t, existsMapping)
	assert.Equal(t, host, entry.Hostname)
	assert.Equal(t, ip, entry.Ip)
}

func TestSyncConnectors(t *testing.T) {
	i, rclient, bclient := setUp(t)

	bclient.Handler = func(req *amqp.Message) (*amqp.Message, error) {
		op := req.ApplicationProperties["_AMQ_OperationName"]
		if op == "getAddressSettingsAsJSON" {
			return &amqp.Message{
				ApplicationProperties: map[string]interface{}{"_AMQ_OperationSucceeded": true},
				Value:                 "[]",
			}, nil
		} else {
			return &amqp.Message{
				ApplicationProperties: map[string]interface{}{"_AMQ_OperationSucceeded": true},
				Value:                 "[[\"q1\",\"q2\"]]",
			}, nil
		}
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

	statuses, err := i.SyncAll(
		[]Host{{Hostname: "r1.example.com", Ip: "10.0.0.1"}, {Hostname: "r2.example.com", Ip: "10.0.0.2"}},
		[]Host{{Hostname: "b1.example.com", Ip: "10.0.0.3"}, {Hostname: "b2.example.com", Ip: "10.0.0.4"}}, nil)
	assert.Nil(t, err)
	assert.Equal(t, 2, len(i.routers[Host{Hostname: "r1.example.com", Ip: "10.0.0.1"}].Entities()[RouterConnectorEntity]))
	assert.Equal(t, 2, len(i.routers[Host{Hostname: "r2.example.com", Ip: "10.0.0.2"}].Entities()[RouterConnectorEntity]))
	assert.Equal(t, 4, len(statuses))
}

func TestBuildRouterEndpointEntities_SingleEndpointPlainAMQP(t *testing.T) {

	i, _, _ := setUp(t)

	endpoint := &v1.MessagingEndpoint{
		Status: v1.MessagingEndpointStatus{
			InternalPorts: []v1.MessagingEndpointPort{
				{
					Name:     "test",
					Protocol: v1.MessagingProtocolAMQP,
					Port:     5672,
				},
			},
		},
	}

	expectedAuthServiceHost := fmt.Sprintf("access-control-%s.%s.svc.cluster.local", i.infrastructureName, i.infrastructureNamespace)
	list, err := i.buildRouterEndpointEntities(endpoint)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(list))
	listenerEntity := findEntity("listener-test", list)
	assert.NotNil(t, listenerEntity)

	assert.Equal(t, &RouterListener{
		Name:               "listener-test",
		Host:               "0.0.0.0",
		Port:               "5672",
		Role:               "normal",
		IdleTimeoutSeconds: 16,
		MultiTenant:        true,
		AuthenticatePeer:   true,
		SaslPlugin:         expectedAuthServiceHost + ":5671",
	}, listenerEntity)
}

func findEntity(name string, entities []RouterEntity) RouterEntity {
	return FindFirst(func(e RouterEntity) bool {
		return e.GetName() == name
	}, entities)
}
