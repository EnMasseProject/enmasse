/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package amqp

import (
	"bytes"
	"fmt"
)

// SASL Codes
const (
	codeSASLOK      saslCode = iota // Connection authentication succeeded.
	codeSASLAuth                    // Connection authentication failed due to an unspecified problem with the supplied credentials.
	codeSASLSys                     // Connection authentication failed due to a system error.
	codeSASLSysPerm                 // Connection authentication failed due to a system error that is unlikely to be corrected without intervention.
	codeSASLSysTemp                 // Connection authentication failed due to a transient system error.
)

// SASL Mechanisms
const (
	saslMechanismPLAIN     Symbol = "PLAIN"
	saslMechanismANONYMOUS Symbol = "ANONYMOUS"
	saslMechanismXOAUTH2   Symbol = "XOAUTH2"
)

func saslOutcomeOK() *saslOutcome {
	return &saslOutcome{
		Code: codeSASLOK,
	}
}

func saslOutcomeAuth() *saslOutcome {
	return &saslOutcome{
		Code: codeSASLAuth,
	}
}

func saslOutcomeSysPerm() *saslOutcome {
	return &saslOutcome{
		Code: codeSASLSysPerm,
	}
}

type saslCode uint8

func (s saslCode) marshal(wr *buffer) error {
	return marshal(wr, uint8(s))
}

func (s *saslCode) unmarshal(r *buffer) error {
	n, err := readUbyte(r)
	*s = saslCode(n)
	return err
}

func WithSASLAnonymous() IncomingConnOption {
	return func(ic *IncomingConn) error {
		var f saslStateFunc
		f = func(hostname string, r []byte) (next saslStateFunc, c []byte, err error) {
			c = []byte{}
			ic.saslOutcome = saslOutcomeOK()
			ic.saslAuthenticatedIdentity = string(r)
			return
		}
		ic.saslMechanisms[saslMechanismANONYMOUS] = f

		return nil
	}
}

type passwordValidator func(user, pass string) (bool, error)

func WithSASLPlain(pv passwordValidator) IncomingConnOption {
	return func(ic *IncomingConn) error {
		var f saslStateFunc
		loopCount := 0
		f = func(hostname string, r []byte) (next saslStateFunc, c []byte, err error) {

			if len(r) == 0 {
				c = []byte{}
				if loopCount == 0 {
					next = f
					loopCount++
				} else {
					ic.saslOutcome = saslOutcomeSysPerm()
					err = fmt.Errorf("improper SASL plain negotiation")
				}
				return
			}

			parts := bytes.Split(r, []byte{byte(0)})

			if len(parts) != 3 {
				c = []byte{}
				ic.saslOutcome = saslOutcomeSysPerm()
				err = fmt.Errorf("improperly formatted SASL plain message")
				return
			}

			authzid := parts[0]
			authcid := parts[1]
			passwd := parts[2]

			if len(authzid) > 0 && !bytes.Equal(authzid, authcid) {
				// Not authorized to requested authorization identity
				c = []byte{}
				ic.saslOutcome = saslOutcomeAuth()
				return
			}

			outcome, err := pv(string(authcid), string(passwd))
			if err != nil {
				c = []byte{}
			} else if outcome {

				c = []byte{}
				ic.saslOutcome = saslOutcomeOK()
				ic.saslAuthenticatedIdentity = string(authcid)
			} else {
				c = []byte{}
				ic.saslOutcome = saslOutcomeAuth()
			}
			return
		}
		ic.saslMechanisms[saslMechanismPLAIN] = f

		return nil
	}
}
