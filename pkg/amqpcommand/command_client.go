/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package amqpcommand

import (
	"context"
	"fmt"
	"log"
	"net"
	"sync/atomic"
	"time"

	"github.com/google/uuid"
	"pack.ag/amqp"
)

type Client interface {
	Start()
	Stop()
	RequestWithTimeout(message *amqp.Message, timeout time.Duration) (*amqp.Message, error)
	Request(message *amqp.Message) (*amqp.Message, error)
}

var (
	NotConnectedError error = fmt.Errorf("not connected")
)

type CommandClient struct {
	addr           string
	connectOptions []amqp.ConnOption

	stop    chan struct{}
	stopped chan struct{}

	commandAddress         string
	commandResponseAddress string

	lastError error
	connected int32

	request chan *commandRequest
}

var _ Client = &CommandClient{}

type commandRequest struct {
	commandMessage *amqp.Message
	response       chan commandResponse
}

type commandResponse struct {
	responseMessage *amqp.Message
	err             error
}

func NewCommandClient(addr string, commandAddress string, commandResponseAddress string, opts ...amqp.ConnOption) Client {
	return &CommandClient{
		addr:                   addr,
		commandAddress:         commandAddress,
		commandResponseAddress: commandResponseAddress,
		connectOptions:         opts,
		lastError:              nil,
		connected:              0,
	}
}

func (c *CommandClient) Start() {
	c.request = make(chan *commandRequest)
	c.stop = make(chan struct{})
	c.stopped = make(chan struct{})
	c.lastError = nil
	go func() {
		defer close(c.stopped)
		defer log.Printf("Command Client %s - stopped", c.addr)

		for {
			select {
			case <-c.stop:
				return
			default:
				err := c.doProcess()
				atomic.StoreInt32(&c.connected, 0)
				if err != nil {
					c.lastError = err
					backoff := computeBackoff(err)
					log.Printf("Command Client %s - restarting - backoff %s(s), %v", c.addr, backoff, err)
					if backoff > 0 {
						time.Sleep(backoff)
					}
				} else {
					// Shutdown
					return
				}
			}
		}
	}()
}

func (c *CommandClient) drainRequests() bool {
	for {
		select {
		case req := <-c.request:
			if req == nil {
				return false
			}
			req.response <- commandResponse{responseMessage: nil, err: c.lastError}
		default:
			return true
		}
	}
}

func (c *CommandClient) doProcess() error {
	log.Printf("Command Client - connecting %s", c.addr)

	client, err := amqp.Dial(c.addr, c.connectOptions...)
	if err != nil {
		return err
	}
	defer func() {
		_ = client.Close()
	}()
	log.Printf("Command Client - connected %s", c.addr)

	session, err := client.NewSession()
	if err != nil {
		return err
	}
	background := context.Background()
	defer func() { _ = session.Close(background) }()

	sender, err := session.NewSender(
		amqp.LinkTargetAddress(c.commandAddress),
		amqp.LinkSenderSettle(amqp.ModeMixed),
	)
	if err != nil {
		return err
	}
	defer func() { _ = sender.Close(background) }()

	receiver, err := session.NewReceiver(
		amqp.LinkSourceAddress(c.commandResponseAddress),
		amqp.LinkAddressDynamic(),
	)
	if err != nil {
		return err
	}
	replyTo := receiver.Address()

	defer func() { _ = receiver.Close(background) }()

	// Close pending requests
	if c.lastError != nil {
		if !c.drainRequests() {
			return nil
		}
		c.lastError = nil
	}
	atomic.StoreInt32(&c.connected, 1)

	requests := make(map[string]*commandRequest)
	for {
		select {
		case req := <-c.request:
			if req == nil {
				return nil
			}

			msgId := uuid.New().String()
			req.commandMessage.Properties.MessageID = msgId
			req.commandMessage.Properties.ReplyTo = replyTo
			req.commandMessage.Properties.CorrelationID = msgId

			requests[msgId] = req

			err = sender.Send(background, req.commandMessage)
			if err != nil {
				log.Printf("failed to accept command %+v %s", req.commandMessage, err)
				req.response <- commandResponse{responseMessage: nil, err: err}
			}
		default:
			// Receive next message
			msg, err := receiveWithTimeout(background, receiver, 250*time.Millisecond)
			if err != nil {
				return err
			} else if msg != nil {
				err := msg.Accept()
				if err != nil {
					return err
				}

				if key, ok := msg.Properties.CorrelationID.(string); ok {
					if req, present := requests[key]; present {
						delete(requests, key)
						req.response <- commandResponse{responseMessage: msg, err: nil}
						continue
					} else {
						log.Printf("Unable to find request with id %s (ignored)", key)
					}
				}
			}
		}
	}
}

func (c *CommandClient) Stop() {
	close(c.request)
	close(c.stop)
	<-c.stopped
}

func (c *CommandClient) RequestWithTimeout(message *amqp.Message, timeout time.Duration) (*amqp.Message, error) {
	if atomic.LoadInt32(&c.connected) == 0 {
		return nil, NotConnectedError
	}
	response := make(chan commandResponse, 1)
	request := &commandRequest{
		commandMessage: message,
		response:       response,
	}

	c.request <- request

	select {
	case result := <-response:
		if result.err != nil {
			return nil, result.err
		} else {
			return result.responseMessage, nil
		}
	case <-time.After(timeout):
		return nil, fmt.Errorf("timed out waiting for response")
	}
}

func (c *CommandClient) Request(message *amqp.Message) (*amqp.Message, error) {
	if atomic.LoadInt32(&c.connected) == 0 {
		return nil, NotConnectedError
	}
	response := make(chan commandResponse)
	request := &commandRequest{
		commandMessage: message,
		response:       response,
	}

	c.request <- request
	result := <-response
	if result.err != nil {
		return nil, result.err
	} else {
		return result.responseMessage, nil
	}
}

func computeBackoff(err error) time.Duration {
	backoff := 5 * time.Second
	if _, ok := err.(net.Error); ok {
		backoff = 30 * time.Second
	}
	return backoff
}

func receiveWithTimeout(ctx context.Context, receiver *amqp.Receiver, timeout time.Duration) (*amqp.Message, error) {
	ctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	msg, err := receiver.Receive(ctx)
	if err != nil && err != context.DeadlineExceeded {
		return nil, err
	}
	return msg, nil
}
