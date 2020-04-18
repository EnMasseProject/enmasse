/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	fakecommand "github.com/enmasseproject/enmasse/pkg/amqpcommand/test"

	"pack.ag/amqp"
)

func TestInitializeBroker(t *testing.T) {
	client := fakecommand.NewFakeClient()

	state := &BrokerState{
		Host:          "",
		Port:          0,
		initialized:   false,
		commandClient: client,
	}

	client.Handler = func(req *amqp.Message) (*amqp.Message, error) {
		return &amqp.Message{
			ApplicationProperties: map[string]interface{}{"_AMQ_OperationSucceeded": true},
			Value:                 "[[\"queue1\",\"queue2\"]]",
		}, nil
	}

	err := state.Initialize(time.Time{})
	assert.Nil(t, err)
	assert.NotNil(t, state.queues)
	assert.Equal(t, 2, len(state.queues))
	assert.True(t, state.queues["queue1"])
	assert.True(t, state.queues["queue2"])
}
