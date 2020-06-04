/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package test

import (
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"

	"pack.ag/amqp"
)

type RequestHandler = func(m *amqp.Message) (*amqp.Message, error)

type FakeClient struct {
	Handler RequestHandler
}

var _ amqpcommand.Client = &FakeClient{}

func NewFakeClient() *FakeClient {
	return &FakeClient{}
}

func (c *FakeClient) Start() {
}

func (c *FakeClient) Addr() string {
	return ""
}

func (c *FakeClient) AwaitRunning() {
}

func (c *FakeClient) Stop() {
}

func (c *FakeClient) RequestWithTimeout(message *amqp.Message, timeout time.Duration) (*amqp.Message, error) {
	if c.Handler != nil {
		return c.Handler(message)
	}
	return nil, nil
}

func (c *FakeClient) Request(message *amqp.Message) (*amqp.Message, error) {
	if c.Handler != nil {
		return c.Handler(message)
	}
	return nil, nil
}

func (c *FakeClient) ReconnectCount() int64 {
	return 0
}

func (c *FakeClient) Connected() bool {
	return true
}
