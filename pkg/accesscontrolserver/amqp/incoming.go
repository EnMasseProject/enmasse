/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 *
 */

/* Reuses implementation from https://github.com/vcabbage/amqp */

package amqp

import (
	"bytes"
	"errors"
	"fmt"
	"github.com/google/uuid"
	"io"
	"log"
	"math"
	"net"
	"time"
)

var (
	ErrTimeout = errors.New("amqp: timeout waiting for response")

	// ErrConnClosed is propagated to Session and Senders/Receivers
	// when Conn.Close() is called or the server closes the connection
	// without specifying an error.
	ErrConnClosed = errors.New("amqp: connection closed")
)

type protoID uint8

// protocol IDs received in protoHeaders
const (
	protoAMQP protoID = 0x0
	protoTLS  protoID = 0x2
	protoSASL protoID = 0x3
)

const (
	DefaultMaxFrameSize = 512
)

type IncomingConnOption func(incomingConn *IncomingConn) error

// IncomingConn represents an incoming OPEN request.
type IncomingConn struct {
	net            net.Conn      // underlying connection
	connectTimeout time.Duration // time to wait for reads/writes during conn establishment

	// SASL
	saslMechanisms            map[Symbol]saslStateFunc
	saslOutcome               *saslOutcome
	saslAuthenticatedIdentity string

	// local settings
	maxFrameSize uint32 // max frame size to accept

	// remote settings
	peerMaxFrameSize        uint32  // maximum frame size peer will accept
	peerDesiredCapabilities Symbols // capabilities that the peer desires

	// conn state
	err     error         // error to return, only valid after done closes.
	done    chan struct{} // indicates the connection is done
	connErr chan error    // connReader/Writer notifications of an error

	// connReader
	rxProto chan protoHeader // protoHeaders received by connReader
	rxFrame chan frame       // AMQP frames received by connReader
	rxDone  chan struct{}

	// connWriter
	txFrame chan frame    // AMQP frames to be sent by connWriter
	txBuf   buffer        // buffer for marshaling frames before transmitting
	txStop  chan *Error   // Tell writer to write final close
	txDone  chan struct{} // Closed when writer is done
}

type stateFunc func() stateFunc
type saslStateFunc func(string, []byte) (saslStateFunc, []byte, error)

func NewIncoming(netConn net.Conn, opts ...IncomingConnOption) (ic *IncomingConn, err error) {
	defer netConn.Close()
	ic = &IncomingConn{
		saslMechanisms:   make(map[Symbol]saslStateFunc, 0),
		net:              netConn,
		maxFrameSize:     DefaultMaxFrameSize,
		peerMaxFrameSize: DefaultMaxFrameSize,
		done:             make(chan struct{}),
		connErr:          make(chan error, 2), // buffered to ensure connReader/Writer won't leak
		rxProto:          make(chan protoHeader),
		rxFrame:          make(chan frame),
		rxDone:           make(chan struct{}),
		txFrame:          make(chan frame),
		txStop:           make(chan *Error),
		txDone:           make(chan struct{}),
		connectTimeout:   time.Second * 5,
	}

	for _, f := range opts {
		f(ic)
	}

	err = ic.process(ic.negotiateProto)
	if err != nil {
		return nil, err
	}
	return ic, err
}

// negotiateProto does server side protocol negotiation.
func (ic *IncomingConn) negotiateProto() stateFunc {
	var p protoHeader
	if p, ic.err = ic.readProtoHeader(); ic.err != nil {
		return nil
	}
	switch p.ProtoID {
	case protoTLS:
		ic.err = fmt.Errorf("AMQP protocol %d unsupported by this server", p.ProtoID)
		return nil
	case protoSASL:
		if ic.err = ic.writeProtoHeader(p.ProtoID); ic.err != nil {
			return nil
		}
		return ic.saslStart
	case protoAMQP:
		if ic.err = ic.writeProtoHeader(p.ProtoID); ic.err != nil {
			return nil
		}
		if _, ic.err = ic.recvOpen(); ic.err != nil {
			return nil
		}
		if ic.err = ic.sendOpen(); ic.err != nil {
			return nil
		}
		if ic.saslOutcome == nil {
			// SASL not done - organise close in error

			saslStepAbsent := fmt.Errorf("AMQP protocol %d unexpected at this time - AMQP SASL is required first", p.ProtoID)
			amqpError := Error{
				Condition:   "amqp:connection:forced",
				Description: saslStepAbsent.Error(),
			}

			ic.txStop <- &amqpError
			ic.err = saslStepAbsent
			return nil
		}
		return nil
	default:
		log.Printf("unxpected %v", p.ProtoID)
		ic.err = errorErrorf("unknown protocol ID %#02x", p.ProtoID)
		return nil
	}
}

func (ic *IncomingConn) saslStart() stateFunc {
	mechanisms := make([]Symbol, 0)
	for mechanism, _ := range ic.saslMechanisms {
		mechanisms = append(mechanisms, mechanism)
	}
	err := ic.writeFrame(frame{
		type_: frameTypeSASL,
		body: &saslMechanisms{
			Mechanisms: mechanisms,
		},
	})
	if err != nil {
		ic.err = err
		return nil
	}

	return ic.saslNegotiate
}

func (ic *IncomingConn) saslNegotiate() stateFunc {
	fr, err := ic.readFrame()
	if err != nil {
		ic.err = err
		return nil
	}

	si, ok := fr.body.(*saslInit)
	if !ok {
		ic.err = errorErrorf("unexpected frame type %T when expecting saslInit", fr.body)
		return nil
	}

	mechFunc := ic.saslMechanisms[si.Mechanism]
	if mechFunc == nil {
		ic.err = errorErrorf("unsupported auth mechanism selected: %s", si.Mechanism)
		return nil
	}

	response := si.InitialResponse
	for {
		next, challenge, err := mechFunc(si.Hostname, response)
		if err != nil {
			ic.err = errorErrorf("SASL error whilst negotiating %s : %v", si.Mechanism, err)
			return nil
		}
		mechFunc = next

		if len(challenge) == 0 && next == nil {
			if ic.saslOutcome == nil {
				ic.err = errorErrorf("SASL negotiation ended without providing outcome")
				return nil
			}
			break
		}

		err = ic.writeFrame(frame{
			type_: frameTypeSASL,
			body: &saslChallenge{
				Challenge: challenge,
			},
		})
		if err != nil {
			ic.err = err
			return nil
		}

		fr, err := ic.readFrame()
		if err != nil {
			ic.err = err
			return nil
		}

		sr, ok := fr.body.(*saslResponse)
		if !ok {
			ic.err = errorErrorf("unexpected frame type %T when expecting saslResponse", fr.body)
			return nil
		}

		response = sr.Response
	}

	err = ic.writeFrame(frame{
		type_: frameTypeSASL,
		body:  ic.saslOutcome,
	})
	if err != nil {
		ic.err = err
		return nil
	}

	return ic.negotiateProto
}

func (ic *IncomingConn) process(state stateFunc) error {
	defer ic.close()
	go ic.connReader()
	go ic.connWriter()

	// run connection establishment state machine
	for state != nil {
		state = state()
	}

	return ic.err
}

// connReader reads from the net.Conn, decodes frames, and passes them
// up via the conn.rxFrame and conn.rxProto channels.
func (ic *IncomingConn) connReader() {
	defer close(ic.rxDone)

	buf := new(buffer)

	var (
		negotiating     = true      // true during conn establishment, check for protoHeaders
		currentHeader   frameHeader // keep track of the current header, for frames split across multiple TCP packets
		frameInProgress bool        // true if in the middle of receiving data for currentHeader
	)

	for {
		switch {
		// Cheaply reuse free buffer space when fully read.
		case buf.len() == 0:
			buf.reset()

		// Prevent excessive/unbounded growth by shifting data to beginning of buffer.
		case int64(buf.i) > int64(ic.maxFrameSize):
			buf.reclaim()
		}

		// need to read more if buf doesn't contain the complete frame
		// or there's not enough in buf to parse the header
		if frameInProgress || buf.len() < frameHeaderSize {
			err := buf.readFromOnce(ic.net)
			if err != nil {
				select {
				// check if error was due to close in progress
				case <-ic.done:
					return

				default:
					ic.connErr <- err
					return
				}
			} else {
			}
		}

		// read more if buf doesn't contain enough to parse the header
		if buf.len() < frameHeaderSize {
			continue
		}

		// during negotiation, check for proto frames
		if negotiating && bytes.Equal(buf.bytes()[:4], []byte{'A', 'M', 'Q', 'P'}) {
			p, err := parseProtoHeader(buf)
			if err != nil {
				ic.connErr <- err
				return
			}

			// negotiation is complete once an AMQP proto frame is received
			if p.ProtoID == protoAMQP {
				negotiating = false
			}

			// send proto header
			select {
			case <-ic.done:
				return
			case ic.rxProto <- p:
			}

			continue
		}

		// parse the header if a frame isn't in progress
		if !frameInProgress {
			var err error
			currentHeader, err = parseFrameHeader(buf)
			if err != nil {
				ic.connErr <- err
				return
			}
			frameInProgress = true
		}

		// check size is reasonable
		if currentHeader.Size > math.MaxInt32 { // make max size configurable
			ic.connErr <- errorNew("payload too large")
			return
		}

		bodySize := int64(currentHeader.Size - frameHeaderSize)

		// the full frame has been received
		if int64(buf.len()) < bodySize {
			continue
		}
		frameInProgress = false

		// check if body is empty (keepalive)
		if bodySize == 0 {
			continue
		}

		// parse the frame
		b, ok := buf.next(bodySize)
		if !ok {
			ic.connErr <- io.EOF
			return
		}

		parsedBody, err := parseFrameBody(&buffer{b: b})
		if err != nil {
			ic.connErr <- err
			return
		}

		// send to mux
		select {
		case <-ic.done:
			return
		case ic.rxFrame <- frame{channel: currentHeader.Channel, body: parsedBody}:
		}
	}
}

func (ic *IncomingConn) connWriter() {
	defer close(ic.txDone)

	// disable write timeout
	if ic.connectTimeout != 0 {
		ic.connectTimeout = 0
		_ = ic.net.SetWriteDeadline(time.Time{})
	}

	var err error
	for {
		if err != nil {
			ic.connErr <- err
			return
		}

		select {
		// frame write request
		case fr := <-ic.txFrame:
			err = ic.writeFrame(fr)
			if err == nil && fr.done != nil {
				close(fr.done)
			}

		case err := <-ic.txStop:
			// send close
			_ = ic.writeFrame(frame{
				type_: frameTypeAMQP,
				body:  &performClose{err},
			})
			return
		}
	}
}

// writeFrame writes a frame to the network, may only be used
// by connWriter after initial negotiation.
func (ic *IncomingConn) writeFrame(fr frame) error {
	debugFrame(ic, "TX", &fr)
	if ic.connectTimeout != 0 {
		_ = ic.net.SetWriteDeadline(time.Now().Add(ic.connectTimeout))
	}

	// writeFrame into txBuf
	ic.txBuf.reset()
	err := writeFrame(&ic.txBuf, fr)
	if err != nil {
		return err
	}

	// validate the frame isn't exceeding peer's max frame size
	requiredFrameSize := ic.txBuf.len()
	if uint64(requiredFrameSize) > uint64(ic.peerMaxFrameSize) {
		return errorErrorf("%T frame size %d larger than peer's max frame size", fr, requiredFrameSize, ic.peerMaxFrameSize)
	}

	// write to network
	_, err = ic.net.Write(ic.txBuf.bytes())
	return err
}

// writeProtoHeader writes an AMQP protocol header to the
// network
func (ic *IncomingConn) writeProtoHeader(pID protoID) error {
	if ic.connectTimeout != 0 {
		_ = ic.net.SetWriteDeadline(time.Now().Add(ic.connectTimeout))
	}
	_, err := ic.net.Write([]byte{'A', 'M', 'Q', 'P', byte(pID), 1, 0, 0})
	return err
}

// readProtoHeader reads a protocol header packet from c.rxProto.
func (ic *IncomingConn) readProtoHeader() (protoHeader, error) {
	var deadline <-chan time.Time
	if ic.connectTimeout != 0 {
		deadline = time.After(ic.connectTimeout)
	}
	var p protoHeader
	select {
	case p = <-ic.rxProto:
		return p, nil
	case err := <-ic.connErr:
		return p, err
	case fr := <-ic.rxFrame:
		return p, errorErrorf("unexpected frame %#v", fr)
	case <-deadline:
		return p, ErrTimeout
	}
}

func (ic *IncomingConn) sendOpen() error {
	open := &performOpen{
		ContainerID:         uuid.New().String(),
		MaxFrameSize:        ic.maxFrameSize,
		Properties:          make(map[Symbol]interface{}, 0),
		OfferedCapabilities: make(Symbols, 0),
		ChannelMax:          1,
	}

	open.Properties["product"] = "access-control-server"

	if ic.saslOutcome != nil && ic.saslOutcome.Code == codeSASLOK {
		var foundCapability = false
		if len(ic.peerDesiredCapabilities) > 0 {
			for _, capability := range ic.peerDesiredCapabilities {
				if capability == addressAuthzCapability {
					foundCapability = true
					break
				}
			}
		}

		open.Properties[authenticatedIdentityProperty] = buildAuthenticatedIdentity(ic.saslAuthenticatedIdentity)
		open.Properties[groupsProperty] = buildGroups()

		if foundCapability {
			open.OfferedCapabilities = append(open.OfferedCapabilities, addressAuthzCapability)
		}
	}
	return ic.writeFrame(frame{type_: frameTypeAMQP, body: open, channel: 0})
}

func (ic *IncomingConn) recvOpen() (*performOpen, error) {
	fr, err := ic.readFrame()
	if err != nil {
		return nil, err
	}
	o, ok := fr.body.(*performOpen)
	if !ok {
		return nil, errorErrorf("expected OPEN frame, received %T", fr.body)
	}

	if o.MaxFrameSize > 0 {
		ic.peerMaxFrameSize = o.MaxFrameSize

	}
	ic.peerDesiredCapabilities = o.DesiredCapabilities
	return o, nil
}

func (ic *IncomingConn) readFrame() (frame, error) {
	var deadline <-chan time.Time
	if ic.connectTimeout != 0 {
		deadline = time.After(ic.connectTimeout)
	}

	var fr frame
	select {
	case fr = <-ic.rxFrame:
		debugFrame(ic, "RX", &fr)
		return fr, nil
	case err := <-ic.connErr:
		return fr, err
	case p := <-ic.rxProto:
		return fr, errorErrorf("unexpected protocol header %#v", p)
	case <-deadline:
		return fr, ErrTimeout
	}
}

func (ic *IncomingConn) Close() error {
	<-ic.done
	<-ic.txDone
	<-ic.rxDone
	if ic.err == ErrConnClosed {
		return nil
	}
	return ic.err
}

// close should only be called by conn.mux.
func (ic *IncomingConn) close() {
	// wait for writing to stop, allows it to send the final close frame
	close(ic.txStop)
	<-ic.txDone

	// Set ic.err first, before closing ic.done
	err := ic.net.Close()
	switch {
	// conn.err already set
	case ic.err != nil:

	// conn.err not set and ic.net.Close() returned a non-nil error
	case err != nil:
		ic.err = err

	// no errors
	default:
		ic.err = ErrConnClosed
	}
	close(ic.done) // notify goroutines and blocked functions to exit

	// check rxDone after closing net, otherwise may block
	// for up to ic.idleTimeout
	<-ic.rxDone
}

func WithConnTimeout(connTimeout time.Duration) IncomingConnOption {
	return func(ic *IncomingConn) error {
		ic.connectTimeout = connTimeout
		return nil
	}
}
