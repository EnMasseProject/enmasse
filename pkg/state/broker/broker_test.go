/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package broker

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	fakecommand "github.com/enmasseproject/enmasse/pkg/amqpcommand/test"
	. "github.com/enmasseproject/enmasse/pkg/state/common"

	"pack.ag/amqp"
)

func TestInitializeBroker(t *testing.T) {
	client := fakecommand.NewFakeClient()

	state := &BrokerState{
		host:          Host{},
		port:          0,
		initialized:   false,
		commandClient: client,
		entities:      make(map[BrokerEntityType]map[string]BrokerEntity, 0),
	}

	client.Handler = func(req *amqp.Message) (*amqp.Message, error) {
		return &amqp.Message{
			ApplicationProperties: map[string]interface{}{"_AMQ_OperationSucceeded": true},
			Value:                 "[[\"queue1\",\"queue2\"]]",
		}, nil
	}

	err := state.Initialize(time.Time{})
	assert.Nil(t, err)
	assert.NotNil(t, state.entities)
	assert.Equal(t, 2, len(state.entities[BrokerQueueEntity]))
	assert.NotNil(t, state.entities[BrokerQueueEntity]["queue1"])
	assert.NotNil(t, state.entities[BrokerQueueEntity]["queue2"])
}
