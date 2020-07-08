/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

package amqp

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
			ic.saslOutcome = &saslOutcome{
				Code: codeSASLOK,
			}
			ic.saslAuthenticatedIdentity = string(r)
			return
		}
		ic.saslMechanisms[saslMechanismANONYMOUS] = f

		return nil
	}
}
