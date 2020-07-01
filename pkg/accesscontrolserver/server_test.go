/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package accesscontrolserver

import (
	"github.com/stretchr/testify/assert"
	"pack.ag/amqp"
	"testing"
	"time"
)

var connTimeout = amqp.ConnConnectTimeout(time.Duration(500) * time.Second)
var saslAnon = amqp.ConnSASLAnonymous()

func TestServerStartStop(t *testing.T) {
	server, err := NewServer("localhost", 0)
	assert.NoError(t, err)
	server.Stop()
}

func TestSaslAnonymous(t *testing.T) {
	server, err := NewServer("localhost", 0)
	assert.NoError(t, err)
	defer server.Stop()
	addr := "amqp://" + server.GetAddress()
	client, err := amqp.Dial(addr, saslAnon, connTimeout)
	assert.NoError(t, err)
	if client != nil {
		defer client.Close()
	}
}

func TestNoSaslRefused(t *testing.T) {
	server, err := NewServer("localhost", 0)
	assert.NoError(t, err)
	defer server.Stop()
	addr := "amqp://" + server.GetAddress()
	client, err := amqp.Dial(addr, connTimeout)
	assert.NoError(t, err)
	// Provoke an error by using the connection
	_, err = client.NewSession()
	assert.Error(t, err, "AMQP protocol 0 unexpected at this time - AMQP SASL is required first")
	if client != nil {
		defer client.Close()
	}
}