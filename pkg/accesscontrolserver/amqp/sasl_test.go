/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package amqp

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestAnonymous_InitResponseWithTrace(t *testing.T) {
	ic := getIncomingConn(WithSASLAnonymous())
	next := ic.saslMechanisms[saslMechanismANONYMOUS]
	assert.NotNil(t, next)

	next, challenge, err := next("", []byte("user"))
	assert.NoError(t, err)
	assert.Equal(t, 0, len(challenge))
	assert.Nil(t, next)

	assert.NotNil(t, ic.saslOutcome)
	assert.Equal(t, codeSASLOK, ic.saslOutcome.Code)
	assert.Equal(t, "user", ic.saslAuthenticatedIdentity)
}

func TestAnonymous_InitResponseWithoutTrace(t *testing.T) {
	ic := getIncomingConn(WithSASLAnonymous())
	next := ic.saslMechanisms[saslMechanismANONYMOUS]
	assert.NotNil(t, next)

	next, challenge, err := next("", []byte{})
	assert.NoError(t, err)
	assert.Equal(t, 0, len(challenge))
	assert.Nil(t, next)

	assert.NotNil(t, ic.saslOutcome)
	assert.Equal(t, codeSASLOK, ic.saslOutcome.Code)
	assert.Equal(t, "", ic.saslAuthenticatedIdentity)
}

func getIncomingConn(options ...IncomingConnOption) *IncomingConn {
	ic := &IncomingConn{
		saslMechanisms: make(map[Symbol]saslStateFunc),
	}
	for _, option := range options {
		option(ic)
	}
	return ic
}
