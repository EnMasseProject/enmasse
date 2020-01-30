/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package agent

import (
	"context"
	"crypto/sha256"
	"crypto/tls"
	"encoding/base64"
	"fmt"
	"github.com/google/uuid"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"log"
	"net"
	"pack.ag/amqp"
	"sync"
	"time"
)

// The agent delegate is the facade to the address space's agent component.  It allows events broadcast by the
// agent to be received.  It also allows commands to be sent to the agent.

type Delegate interface {
	Collect(handler EventHandler) error
	CommandDelegate(bearerToken string) (CommandDelegate, error)
	Shutdown()
}

type CommandDelegate interface {
	PurgeAddress(address v1.ObjectMeta) error
	Shutdown()
}

const amqpOverrideSaslFrameSize = 4096
const agentDataAddress = "agent_data"
const agentCommandAddress = "agent_command"
const agentCommandResponseAddress = "agent_command_response"

type EventHandler = func(event AgentEvent) error

type DelegateCreator = func(bearerToken, host string, port int32) Delegate

type commandDelegatePair struct {
	delegate CommandDelegate
	lastUsed time.Time
}

type amqpAgentDelegate struct {
	bearerToken                 string
	host                        string
	port                        int32
	tlsConfig                   *tls.Config
	addressSpaceNamespace       string
	addressSpace                string
	infraUuid                   string
	handler                     EventHandler
	stopchan                    chan struct{}
	stoppedchan                 chan struct{}
	commandDelegates            map[string]commandDelegatePair
	commandDelegatesMux         sync.Mutex
	commandDelegateExpiryPeriod time.Duration
}

func NewAmqpAgentDelegate(bearerToken, host string, port int32, tlsConfig *tls.Config, addressSpaceNamespace, addressSpace, infraUuid string, expirePeriod time.Duration) Delegate {
	return &amqpAgentDelegate{
		bearerToken:                 bearerToken,
		host:                        host,
		port:                        port,
		tlsConfig:                   tlsConfig,
		addressSpaceNamespace:       addressSpaceNamespace,
		addressSpace:                addressSpace,
		infraUuid:                   infraUuid,
		stopchan:                    make(chan struct{}),
		stoppedchan:                 make(chan struct{}),
		commandDelegates:            make(map[string]commandDelegatePair),
		commandDelegateExpiryPeriod: expirePeriod,
	}
}

func (aad *amqpAgentDelegate) Collect(handler EventHandler) error {
	aad.handler = handler

	go func() {
		defer close(aad.stoppedchan)
		defer log.Printf("Agent Collector %s - stopped", aad.infraUuid)

		log.Printf("Agent Collector %s - starting", aad.infraUuid)
		for {
			select {
			case <-aad.stopchan:
				return
			default:
				err := aad.doCollect()

				if err == nil {
					return
				} else {
					backoff := computeBackoff(err)
					log.Printf("Agent Collector %s - restarting - backoff %s(s), %v", aad.infraUuid, backoff, err)
					if backoff > 0 {
						time.Sleep(backoff)
					}
				}
			}
		}
	}()

	return nil
}

func computeBackoff(err error) time.Duration {
	backoff := 5 * time.Second
	if _, ok := err.(net.Error); ok {
		backoff = 30 * time.Second
	}
	return backoff
}

func (aad *amqpAgentDelegate) Shutdown() {
	log.Printf("Agent Collector %s - Shutting down", aad.infraUuid)
	close(aad.stopchan)
	for key, commandDelegate := range aad.commandDelegates {
		commandDelegate.delegate.Shutdown()
		delete(aad.commandDelegates, key)
	}
	<-aad.stoppedchan
}

func (aad *amqpAgentDelegate) doCollect() error {

	addr := buildAmqpAddress(aad.host, aad.port)
	log.Printf("Agent Collector %s - connecting %s", aad.infraUuid, addr)

	client, err := amqp.Dial(addr,
		amqp.ConnTLSConfig(aad.tlsConfig),
		amqp.ConnSASLXOAUTH2("unused", aad.bearerToken, amqpOverrideSaslFrameSize),
		amqp.ConnServerHostname(aad.host),
		amqp.ConnProperty("product", "console-server"),
		amqp.ConnConnectTimeout(time.Second*10),
	)
	if err != nil {
		return err
	}
	defer func() {
		_ = client.Close()
	}()
	log.Printf("Agent Collector %s - connected %s", aad.infraUuid, addr)

	// Open a session
	session, err := client.NewSession()
	if err != nil {
		return err
	}

	ctx := context.Background()

	// Create a receiver
	receiver, err := session.NewReceiver(
		amqp.LinkSourceAddress(agentDataAddress),
	)
	if err != nil {
		return err
	}

	defer func() {
		ctx, cancel := context.WithTimeout(ctx, 1*time.Second)
		_ = receiver.Close(ctx)
		cancel()
	}()

	log.Printf("Agent Collector %s - commencing collecting", aad.infraUuid)
	restart := AgentEvent{
		AddressSpaceNamespace: aad.addressSpaceNamespace,
		AddressSpace:          aad.addressSpace,
		InfraUuid:             aad.infraUuid,
		Type:                  AgentEventTypeRestart,
	}
	err = aad.handler(restart)
	if err != nil {
		return err
	}

	next := time.Now().Add(aad.commandDelegateExpiryPeriod)
	for {

		select {
		case <-aad.stopchan:
			return nil
		default:
			now := time.Now()
			if next.Before(now) {
				go aad.expireCommandDelegates(now)
				next = now.Add(aad.commandDelegateExpiryPeriod)
			}

			// Receive next message
			msg, err := receiveWithTimeout(ctx, receiver, 5*time.Second)
			if err != nil {
				return err
			} else if msg != nil {
				switch msg.Properties.Subject {

				case "connection":
					body := msg.Value.(map[string]interface{})
					agentcon, err := FromAgentConnectionBody(body)
					if err != nil {
						log.Printf("ignoring failure unmarsall connection message %s for infraUuid %s, %v", body, aad.infraUuid, err)
						break
					} else {
						evt := AgentEvent{
							AddressSpaceNamespace: aad.addressSpaceNamespace,
							AddressSpace:          aad.addressSpace,
							InfraUuid:             aad.infraUuid,
							Type:                  AgentEventInsertOrUpdateType,
							Object:                agentcon,
						}
						err = aad.handler(evt)
						if err != nil {
							return err
						}
					}
				case "connection_deleted":
					body := msg.Value.(map[string]interface{})
					agentcon, err := FromAgentConnectionBody(body)
					if err != nil {
						log.Printf("ignoring failure unmarsall connection message %s for infraUuid %s %v", body, aad.infraUuid, err)
						break
					} else {
						evt := AgentEvent{
							AddressSpaceNamespace: aad.addressSpaceNamespace,
							AddressSpace:          aad.addressSpace,
							InfraUuid:             aad.infraUuid,
							Type:                  AgentEventTypeDelete,
							Object:                agentcon,
						}
						err = aad.handler(evt)
						if err != nil {
							return err
						}
					}
				case "address":
					body := msg.Value.(map[string]interface{})
					agentaddr, err := FromAgentAddressBody(body)
					if err != nil {
						log.Printf("ignoring failure unmarsall address message %s for infraUuid %s, %v", body, aad.infraUuid, err)
						break
					} else {
						evt := AgentEvent{
							AddressSpaceNamespace: aad.addressSpaceNamespace,
							AddressSpace:          aad.addressSpace,
							InfraUuid:             aad.infraUuid,
							Type:                  AgentEventInsertOrUpdateType,
							Object:                agentaddr,
						}
						err = aad.handler(evt)
						if err != nil {
							return err
						}

					}

				case "address_deleted":
				default:
					// Ignore messages with other subjects
				}

				// Accept message
				err = msg.Accept()
				if err != nil {
					return err
				}
			}
		}
	}
}

func (aad *amqpAgentDelegate) CommandDelegate(bearerToken string) (CommandDelegate, error) {
	aad.commandDelegatesMux.Lock()
	defer aad.commandDelegatesMux.Unlock()

	key := getShaSum(bearerToken)

	now := time.Now()
	if pair, present := aad.commandDelegates[key]; !present {
		delegate := aad.newAgentDelegate(bearerToken)
		aad.commandDelegates[key] = commandDelegatePair{
			delegate: delegate,
			lastUsed: now,
		}
		return delegate, nil
	} else {
		pair.lastUsed = now
		return pair.delegate, nil
	}
}

func (aad *amqpAgentDelegate) expireCommandDelegates(now time.Time) {
	findExpiredCommandDelegates := func() []CommandDelegate {
		aad.commandDelegatesMux.Lock()
		defer aad.commandDelegatesMux.Unlock()

		expired := make([]CommandDelegate, 0)

		horizon := now.Add(aad.commandDelegateExpiryPeriod)
		for key, commandDelegate := range aad.commandDelegates {
			if commandDelegate.lastUsed.Before(horizon) {
				expired = append(expired, commandDelegate.delegate)
				delete(aad.commandDelegates, key)
			}
		}

		return expired
	}

	expired := findExpiredCommandDelegates()
	for _, d := range expired {
		d.Shutdown()
	}
	if len(expired) > 0 {
		log.Printf("Shutdown %d expired command delegate(s)", len(expired))
	}
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

func getShaSum(token string) string {
	accessTokenSha := sha256.Sum256([]byte(token))
	return base64.StdEncoding.EncodeToString(accessTokenSha[:])
}

type agentCommandRequest struct {
	commandMessage *amqp.Message
	response       chan error
}

type amqpAgentCommandDelegate struct {
	bearerToken string
	aac         *amqpAgentDelegate
	request     chan *agentCommandRequest
	stopped     chan struct{}
	lastUsed    time.Time
}

func (aad *amqpAgentDelegate) newAgentDelegate(token string) CommandDelegate {
	a := &amqpAgentCommandDelegate{
		bearerToken: token,
		aac:         aad,
		request:     make(chan *agentCommandRequest),
		stopped:     make(chan struct{}),
		lastUsed:    time.Now(),
	}

	a.connectAndProcessCommandsForever()
	return a
}

func (ad *amqpAgentCommandDelegate) PurgeAddress(address v1.ObjectMeta) error {
	response := make(chan error)
	request := &agentCommandRequest{
		commandMessage: &amqp.Message{
			Properties: &amqp.MessageProperties{
				Subject: "purge_address",
			},
			Value: map[interface{}]interface{}{
				"address": address.Name,
			},
		},
		response: response,
	}

	ad.request <- request
	result := <-response

	if result != nil {
		return fmt.Errorf("failed to purge address %s : %s", address.Name, result)
	}
	return nil
}

func (ad *amqpAgentCommandDelegate) LastUsed() time.Time {
	return ad.lastUsed
}

func (ad *amqpAgentCommandDelegate) Shutdown() {
	close(ad.request)
	<-ad.stopped
}

func (ad *amqpAgentCommandDelegate) connectAndProcessCommandsForever() {

	go func() {
		defer close(ad.stopped)
		defer log.Printf("Agent Command Delegate %s - stopped", ad.aac.infraUuid)

		addr := buildAmqpAddress(ad.aac.host, ad.aac.port)

		for {

			err := ad.doProcess(addr)
			if err != nil {
				backoff := computeBackoff(err)
				log.Printf("Agent Command Delegate %s - restarting - backoff %s(s), %v", ad.aac.infraUuid, backoff, err)
				if backoff > 0 {
					time.Sleep(backoff)
				}
			} else {
				// Shutdown
				return
			}
		}
	}()

}

func (ad *amqpAgentCommandDelegate) doProcess(addr string) error {
	log.Printf("Agent Command Delegate %s - connecting %s", ad.aac.infraUuid, addr)

	client, err := amqp.Dial(addr,
		amqp.ConnTLSConfig(ad.aac.tlsConfig),
		amqp.ConnSASLXOAUTH2("unused", ad.bearerToken, amqpOverrideSaslFrameSize),
		amqp.ConnServerHostname(ad.aac.host),
		amqp.ConnProperty("product", "command-delegate; console-server"),
		amqp.ConnConnectTimeout(time.Second*10),
	)
	if err != nil {
		return err
	}
	defer func() {
		_ = client.Close()
	}()
	log.Printf("Agent Command Delegate %s - connected %s", ad.aac.infraUuid, addr)

	session, err := client.NewSession()
	if err != nil {
		return err
	}
	background := context.Background()
	defer func() { _ = session.Close(background) }()

	sender, err := session.NewSender(
		amqp.LinkTargetAddress(agentCommandAddress),
		amqp.LinkSenderSettle(amqp.ModeMixed),
	)
	if err != nil {
		return err
	}
	defer func() { _ = sender.Close(background) }()

	receiver, err := session.NewReceiver(
		amqp.LinkSourceAddress(agentCommandResponseAddress),
	)
	if err != nil {
		return err
	}
	replyTo := receiver.Address()

	defer func() { _ = receiver.Close(background) }()

	requests := make(map[string]*agentCommandRequest)
	for {
		select {
		case req := <-ad.request:
			if req == nil {
				log.Printf("Shutdown")
				return nil
			}

			msgId := uuid.New().String()
			req.commandMessage.Properties.MessageID = msgId
			req.commandMessage.Properties.ReplyTo = replyTo

			requests[msgId] = req

			err = sender.Send(background, req.commandMessage)
			if err != nil {
				log.Printf("failed to accept command %+v %s", req.commandMessage, err)
				req.response <- err
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

						if outcome, present := msg.ApplicationProperties["outcome"]; present {
							if oc, ok := outcome.(bool); ok && oc {
								req.response <- nil
								continue
							} else {
								if e, present := msg.ApplicationProperties["error"]; present && e != nil {
									req.response <- fmt.Errorf("%s", e)
									continue
								}
							}
						}
						req.response <- fmt.Errorf("command %+v failed for unknown reason", req)
						continue
					} else {
						log.Printf("Unable to find request with id %s (ignored)", key)
					}
				}
			}
		}
	}
}

func buildAmqpAddress(host string, port int32) string {
	addr := fmt.Sprintf("amqps://%s:%d", host, port)
	return addr
}
