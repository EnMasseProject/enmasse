package amqp

import (
	"fmt"
	"regexp"
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
	saslMechanismPLAIN     symbol = "PLAIN"
	saslMechanismANONYMOUS symbol = "ANONYMOUS"
	saslMechanismXOAUTH2   symbol = "XOAUTH2"
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

// ConnSASLPlain enables SASL PLAIN authentication for the connection.
//
// SASL PLAIN transmits credentials in plain text and should only be used
// on TLS/SSL enabled connection.
func ConnSASLPlain(username, password string) ConnOption {
	// TODO: how widely used is hostname? should it be supported
	return func(c *conn) error {
		// make handlers map if no other mechanism has
		if c.saslHandlers == nil {
			c.saslHandlers = make(map[symbol]stateFunc)
		}

		// add the handler the the map
		c.saslHandlers[saslMechanismPLAIN] = func() stateFunc {
			// send saslInit with PLAIN payload
			c.err = c.writeFrame(frame{
				type_: frameTypeSASL,
				body: &saslInit{
					Mechanism:       "PLAIN",
					InitialResponse: []byte("\x00" + username + "\x00" + password),
					Hostname:        "",
				},
			})
			if c.err != nil {
				return nil
			}

			// go to c.saslOutcome to handle the server response
			return c.saslOutcome
		}
		return nil
	}
}

// ConnSASLAnonymous enables SASL ANONYMOUS authentication for the connection.
func ConnSASLAnonymous() ConnOption {
	return func(c *conn) error {
		// make handlers map if no other mechanism has
		if c.saslHandlers == nil {
			c.saslHandlers = make(map[symbol]stateFunc)
		}

		// add the handler the the map
		c.saslHandlers[saslMechanismANONYMOUS] = func() stateFunc {
			c.err = c.writeFrame(frame{
				type_: frameTypeSASL,
				body: &saslInit{
					Mechanism:       saslMechanismANONYMOUS,
					InitialResponse: []byte("anonymous"),
				},
			})
			if c.err != nil {
				return nil
			}

			// go to c.saslOutcome to handle the server response
			return c.saslOutcome
		}
		return nil
	}
}

// ConnSASLXOAUTH2 enables SASL XOAUTH2 authentication for the connection.
func ConnSASLXOAUTH2(username, bearer string, saslMaxFrameSizeOverride uint32) ConnOption {
	return func(c *conn) error {
		// make handlers map if no other mechanism has
		if c.saslHandlers == nil {
			c.saslHandlers = make(map[symbol]stateFunc)
		}

		response, err := saslXOAUTH2InitialResponse(username, bearer)
		if err != nil {
			return err
		}

		// add the handler the the map
		c.saslHandlers[saslMechanismXOAUTH2] = func() stateFunc {
			originalPeerMaxFrameSize := c.peerMaxFrameSize
			if saslMaxFrameSizeOverride > c.peerMaxFrameSize {
				c.peerMaxFrameSize = saslMaxFrameSizeOverride
			}
			c.err = c.writeFrame(frame{
				type_: frameTypeSASL,
				body: &saslInit{
					Mechanism:       saslMechanismXOAUTH2,
					InitialResponse: response,
				},
			})
			c.peerMaxFrameSize = originalPeerMaxFrameSize
			if c.err != nil {
				return nil
			}

			// go to c.saslXOAUTH2Step to handle the server response
			return c.saslXOAUTH2Step
		}
		return nil
	}
}

func saslXOAUTH2InitialResponse(username string, bearer string) ([]byte, error) {
	re := regexp.MustCompile("^[\x20-\x7E]+$")
	if !re.MatchString(bearer) {
		return []byte{}, fmt.Errorf("unacceptable bearer token")
	}

	return []byte("user=" + username + "\x01auth=Bearer " + bearer + "\x01\x01"), nil
}

func (c *conn) saslXOAUTH2Step() stateFunc {
	// read challenge or outcome frame
	fr, err := c.readFrame()
	if err != nil {
		c.err = err
		return nil
	}

	if so, ok := fr.body.(*saslOutcome); ok {
		// check if auth succeeded
		if so.Code != codeSASLOK {
			c.err = errorErrorf("SASL XOAUTH2 auth failed with code %#00x: %s", so.Code, so.AdditionalData)
			return nil
		}

		// return to c.negotiateProto
		c.saslComplete = true
		return c.negotiateProto
	} else if sc, ok := fr.body.(*saslChallenge); ok {
		debug(1, "SASL XOAUTH2 - the server sent a challenge containing error message :%s", string(sc.Challenge))

		// The SASL protocol requires clients to send an empty response to this challenge.
		c.err = c.writeFrame(frame{
			type_: frameTypeSASL,
			body: &saslResponse{
				Response: []byte{},
			},
		})
		return c.saslXOAUTH2Step
	} else {
		c.err = errorErrorf("unexpected frame type %T", fr.body)
		return nil
	}
}
