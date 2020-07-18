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

func TestPlain_InitResponseSuccessfulAuthWithoutAuthzId(t *testing.T) {
	ic := getIncomingConn(WithSASLPlain(func(user, pass string) (bool, error) {
		return user == "tim" && pass == "tanstaaftanstaaf", nil
	}))
	next := ic.saslMechanisms[saslMechanismPLAIN]
	assert.NotNil(t, next)

	next, challenge, err := next("", []byte("\x00tim\x00tanstaaftanstaaf"))
	assert.NoError(t, err)
	assert.Equal(t, 0, len(challenge))
	assert.Nil(t, next)

	assert.NotNil(t, ic.saslOutcome)
	assert.Equal(t, codeSASLOK, ic.saslOutcome.Code)
	assert.Equal(t, "tim", ic.saslAuthenticatedIdentity)
}

func TestPlain_InitResponseWrongPassword(t *testing.T) {
	ic := getIncomingConn(WithSASLPlain(func(user, pass string) (bool, error) {
		return user == "tim" && pass == "tanstaaftanstaaf", nil
	}))
	next := ic.saslMechanisms[saslMechanismPLAIN]
	assert.NotNil(t, next)

	next, challenge, err := next("", []byte("\x00tim\x00wrong"))
	assert.NoError(t, err)
	assert.Equal(t, 0, len(challenge))
	assert.Nil(t, next)

	assert.NotNil(t, ic.saslOutcome)
	assert.Equal(t, codeSASLAuth, ic.saslOutcome.Code)
	assert.Equal(t, "", ic.saslAuthenticatedIdentity)
}

func TestPlain_InitResponseSuccessfulAuthMatchingAuthzId(t *testing.T) {
	ic := getIncomingConn(WithSASLPlain(func(user, pass string) (bool, error) {
		return user == "Kurt" && pass == "xipj3plmqtim", nil
	}))
	next := ic.saslMechanisms[saslMechanismPLAIN]
	assert.NotNil(t, next)

	next, challenge, err := next("", []byte("Kurt\x00Kurt\x00xipj3plmqtim"))
	assert.NoError(t, err)
	assert.Equal(t, 0, len(challenge))
	assert.Nil(t, next)

	assert.NotNil(t, ic.saslOutcome)
	assert.Equal(t, codeSASLOK, ic.saslOutcome.Code)
	assert.Equal(t, "Kurt", ic.saslAuthenticatedIdentity)
}

func TestPlain_InitResponseSuccessfulAuthMismatchingAuthzId(t *testing.T) {
	ic := getIncomingConn(WithSASLPlain(func(user, pass string) (bool, error) {
		t.Fail()
		return false, nil
	}))
	next := ic.saslMechanisms[saslMechanismPLAIN]
	assert.NotNil(t, next)

	next, challenge, err := next("", []byte("Ursel\x00Kurt\x00xipj3plmqtim"))
	assert.NoError(t, err)
	assert.Equal(t, 0, len(challenge))
	assert.Nil(t, next)

	assert.NotNil(t, ic.saslOutcome)
	assert.Equal(t, codeSASLAuth, ic.saslOutcome.Code)
	assert.Equal(t, "", ic.saslAuthenticatedIdentity)
}

func TestPlain_MalformedResponse(t *testing.T) {
	ic := getIncomingConn(WithSASLPlain(func(user, pass string) (bool, error) {
		t.Fail()
		return false, nil
	}))
	next := ic.saslMechanisms[saslMechanismPLAIN]
	assert.NotNil(t, next)

	next, _, err := next("", []byte("\x00\x00\x00"))
	assert.Error(t, err)
	assert.Equal(t, codeSASLSysPerm, ic.saslOutcome.Code)
	assert.Nil(t, next)
}

func TestPlain_InitialResponseEmpty_SubsequentResponseSuccessfulAuth(t *testing.T) {
	ic := getIncomingConn(WithSASLPlain(func(user, pass string) (bool, error) {
		return user == "tim" && pass == "tanstaaftanstaaf", nil
	}))
	next := ic.saslMechanisms[saslMechanismPLAIN]
	assert.NotNil(t, next)

	next, challenge, err := next("", []byte{})
	assert.NoError(t, err)
	assert.Equal(t, 0, len(challenge))
	assert.NotNil(t, next)

	next, challenge, err = next("", []byte("\x00tim\x00tanstaaftanstaaf"))
	assert.NoError(t, err)
	assert.Equal(t, 0, len(challenge))
	assert.Nil(t, next)

	assert.NotNil(t, ic.saslOutcome)
	assert.Equal(t, codeSASLOK, ic.saslOutcome.Code)
	assert.Equal(t, "tim", ic.saslAuthenticatedIdentity)
}

func TestPlain_InitialResponseEmpty_SubsequentResponseEmpty(t *testing.T) {
	ic := getIncomingConn(WithSASLPlain(func(user, pass string) (bool, error) {
		t.Fail()
		return false, nil
	}))
	next := ic.saslMechanisms[saslMechanismPLAIN]
	assert.NotNil(t, next)

	next, challenge, err := next("", []byte{})
	assert.NoError(t, err)
	assert.Equal(t, 0, len(challenge))
	assert.NotNil(t, next)

	next, challenge, err = next("", []byte{})
	assert.Error(t, err)
	assert.Equal(t, codeSASLSysPerm, ic.saslOutcome.Code)
	assert.Nil(t, next)
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
