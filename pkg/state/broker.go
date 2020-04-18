/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"encoding/json"
	"fmt"
	"log"
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"
	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"

	"pack.ag/amqp"
)

const brokerCommandAddress = "activemq.management"
const brokerCommandResponseAddress = "broker_command_response"

func NewBrokerState(host string, port int32) *BrokerState {
	state := &BrokerState{
		Host:        host,
		Port:        port,
		initialized: false,
		queues:      make(map[string]bool),
		commandClient: amqpcommand.NewCommandClient(fmt.Sprintf("amqp://%s:%d", host, 5672),
			brokerCommandAddress,
			brokerCommandResponseAddress,
			amqp.ConnConnectTimeout(10*time.Second),
			amqp.ConnProperty("product", "controller-manager")),
	}
	state.commandClient.Start()
	return state
}

func (b *BrokerState) Initialize() error {
	if b.initialized {
		return nil
	}

	b.initialized = true

	// TODO: addresses, err := b.readAddresses()
	return nil
}

func (b *BrokerState) EnsureQueue(address *v1beta2.MessagingAddress) error {
	if _, ok := b.queues[address.Name]; !ok {
		message, err := newManagementMessage("broker", "deployQueue", address.GetAddress(), address.Name, nil, false)
		if err != nil {
			return err
		}

		log.Printf("Creating queue %s on %s: %+v", address.Name, b.Host, message)

		_, err = b.commandClient.RequestWithTimeout(message, 10*time.Second)
		if err != nil {
			return err
		}
		log.Printf("Queue %s created successfully on %s\n", address.Name, b.Host)
		b.queues[address.Name] = true
	} else {
		log.Printf("Queue %s already exists on %s\n", address.Name, b.Host)
	}
	return nil
}

func (b *BrokerState) DeleteQueue(address *v1beta2.MessagingAddress) error {
	if _, ok := b.queues[address.Name]; ok {
		message, err := newManagementMessage("broker", "destroyQueue", address.Name, true)
		if err != nil {
			return err
		}

		log.Printf("Destroying queue %s on %s: %+v", address.Name, b.Host, message)

		_, err = b.commandClient.RequestWithTimeout(message, 10*time.Second)
		if err != nil {
			return err
		}
		log.Printf("Queue %s destroyed successfully on %s\n", address.Name, b.Host)
		delete(b.queues, address.Name)
	} else {
		log.Printf("Queue %s does not exists on %s\n", address.Name, b.Host)
	}
	return nil
}

func newManagementMessage(resource string, operation string, attribute string, parameters ...interface{}) (*amqp.Message, error) {
	properties := make(map[string]interface{})
	properties["_AMQ_ResourceName"] = resource
	if operation != "" {
		properties["_AMQ_OperationName"] = operation
	}
	if attribute != "" {
		properties["_AMQ_Attribute"] = attribute
	}

	encoded, err := json.Marshal(parameters)
	if err != nil {
		return nil, err
	}
	return &amqp.Message{
		ApplicationProperties: properties,
		Value:                 encoded,
	}, nil
}

func (b *BrokerState) Shutdown() {
	if b.commandClient != nil {
		b.commandClient.Stop()
	}
}
