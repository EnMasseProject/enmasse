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
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"

	"golang.org/x/sync/errgroup"

	"pack.ag/amqp"
)

const brokerCommandAddress = "activemq.management"
const brokerCommandResponseAddress = "activemq.management_broker_command_response"

func NewBrokerState(host string, port int32) *BrokerState {
	state := &BrokerState{
		Host:        host,
		Port:        port,
		initialized: false,
		queues:      make(map[string]bool),
		commandClient: amqpcommand.NewCommandClient(fmt.Sprintf("amqp://%s:%d", host, 5672),
			brokerCommandAddress,
			brokerCommandResponseAddress,
			amqp.ConnSASLPlain("admin", "admin"),
			amqp.ConnConnectTimeout(10*time.Second),
			amqp.ConnProperty("product", "controller-manager")),
	}
	state.commandClient.Start()
	return state
}

func (b *BrokerState) Initialize(nextResync time.Time) error {
	if b.initialized {
		return nil
	}

	b.nextResync = nextResync

	log.Printf("[Broker %s] Initializing...", b.Host)

	queues, err := b.readQueues()
	if err != nil {
		return err
	}
	b.queues = queues
	log.Printf("[Broker %s] Initialized controller state with %d queues", b.Host, len(queues))
	b.initialized = true
	return nil
}

/**
 * Perform management request against this broker. If resetOnDisconnect is set, the broker state will be instructed
 * to be reset. This should be set to true if running in a single-threaded context, and the error should be handled
 * by the caller.
 */
func (b *BrokerState) doRequest(request *amqp.Message, resetOnDisconnect bool) (*amqp.Message, error) {
	// If by chance we got disconnected while waiting for the request
	response, err := b.commandClient.RequestWithTimeout(request, 10*time.Second)
	if resetOnDisconnect && errors.Is(err, amqp.ErrConnClosed) {
		b.Reset()
	}
	return response, err
}

func (b *BrokerState) readQueues() (map[string]bool, error) {
	message, err := newManagementMessage("broker", "getQueueNames", "", "ANYCAST")
	if err != nil {
		return nil, err
	}

	result, err := b.doRequest(message, true)
	if err != nil {
		return nil, err
	}
	if !success(result) {
		return nil, fmt.Errorf("error reading queues: %+v", result.Value)
	}

	switch v := result.Value.(type) {
	case string:
		queues := make(map[string]bool, 0)
		var list [][]string
		err := json.Unmarshal([]byte(result.Value.(string)), &list)
		if err != nil {
			return nil, err
		}
		for _, entry := range list {
			for _, name := range entry {
				queues[name] = true
			}
		}
		return queues, nil
	default:
		return nil, fmt.Errorf("unexpected value with type %T", v)
	}
}

func (b *BrokerState) EnsureQueues(queues []string) error {
	g, _ := errgroup.WithContext(context.Background())
	completed := make(chan string, len(queues))
	for _, queue := range queues {
		q := queue
		if _, ok := b.queues[q]; !ok {
			g.Go(func() error {
				message, err := newManagementMessage("broker", "createQueue", "", q, "ANYCAST", q, nil, true, -1, false, true)
				if err != nil {
					return err
				}
				log.Printf("Creating queue %s on %s", q, b.Host)
				response, err := b.doRequest(message, false)
				if err != nil {
					return err
				}
				if !success(response) {
					return fmt.Errorf("error creating queue %s: %+v", q, response.Value)
				}
				log.Printf("Queue %s created successfully on %s\n", q, b.Host)
				completed <- q
				return nil
			})
		}
	}
	err := g.Wait()
	close(completed)
	for queue := range completed {
		b.queues[queue] = true
	}
	if errors.Is(err, amqp.ErrConnClosed) {
		b.Reset()
	}
	return err
}

func (b *BrokerState) DeleteQueues(queues []string) error {
	g, _ := errgroup.WithContext(context.Background())
	completed := make(chan string, len(queues))
	for _, queue := range queues {
		q := queue
		if _, ok := b.queues[q]; ok {
			g.Go(func() error {
				message, err := newManagementMessage("broker", "destroyQueue", "", q, true, true)
				if err != nil {
					return err
				}

				log.Printf("Destroying queue %s on %s", q, b.Host)

				response, err := b.doRequest(message, false)
				if err != nil {
					return err
				}

				if !success(response) {
					return fmt.Errorf("error deleting queue %s: %+v", q, response.Value)
				}

				log.Printf("Queue %s destroyed successfully on %s\n", q, b.Host)
				completed <- q
				return nil
			})
		}
	}

	err := g.Wait()
	close(completed)
	for queue := range completed {
		delete(b.queues, queue)
	}
	if errors.Is(err, amqp.ErrConnClosed) {
		b.Reset()
	}
	return err
}

func success(response *amqp.Message) bool {
	successProp, ok := response.ApplicationProperties["_AMQ_OperationSucceeded"]
	if !ok {
		return false
	}
	return successProp.(bool)
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
		Properties:            &amqp.MessageProperties{},
		ApplicationProperties: properties,
		Value:                 string(encoded),
	}, nil
}

/*
 * Reset broker state from broker (i.e. drop all internal state and rebuild from actual router state)
 */
func (b *BrokerState) Reset() {
	if b.commandClient != nil {
		b.commandClient.Stop()
		b.initialized = false
		b.commandClient.Start()
	}
}

func (b *BrokerState) Shutdown() {
	if b.commandClient != nil {
		b.commandClient.Stop()
	}
}
