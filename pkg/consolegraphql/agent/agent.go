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
	"log"
	"net"
	"sync"
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"

	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"pack.ag/amqp"
)

// The agent delegate is the facade to the address space's agent component.  It allows events broadcast by the
// agent to be received.  It also allows commands to be sent to the agent.

type Delegate interface {
	Collect(handler EventHandler) error
	CommandDelegate(bearerToken string, impersonateUser string) (CommandDelegate, error)
	Shutdown()
}

type CommandDelegate interface {
	PurgeAddress(address string) error
	CloseConnection(address v1.ObjectMeta) error
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
	connectTimeout              time.Duration
	maxFrameSize                uint32
}

func NewAmqpAgentDelegate(bearerToken, host string, port int32, tlsConfig *tls.Config, addressSpaceNamespace, addressSpace, infraUuid string, expirePeriod, connectTimeout time.Duration, maxFrameSize uint32) Delegate {
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
		connectTimeout:              connectTimeout,
		maxFrameSize:                maxFrameSize,
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
		amqp.ConnSASLXOAUTH2("", aad.bearerToken, amqpOverrideSaslFrameSize),
		amqp.ConnServerHostname(aad.host),
		amqp.ConnProperty("product", "console-server"),
		amqp.ConnConnectTimeout(aad.connectTimeout),
		amqp.ConnMaxFrameSize(aad.maxFrameSize),
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
		amqp.LinkCredit(500),
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

func (aad *amqpAgentDelegate) CommandDelegate(bearerToken string, impersonateUser string) (CommandDelegate, error) {
	aad.commandDelegatesMux.Lock()
	defer aad.commandDelegatesMux.Unlock()

	key := getShaSum(bearerToken)

	now := time.Now()
	if pair, present := aad.commandDelegates[key]; !present {
		delegate := aad.newAgentDelegate(bearerToken, impersonateUser)
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
	aac           *amqpAgentDelegate
	commandClient amqpcommand.Client
	lastUsed      time.Time
}

func (aad *amqpAgentDelegate) newAgentDelegate(token string, impersonateUser string) CommandDelegate {
	commandClient := amqpcommand.NewCommandClient(buildAmqpAddress(aad.host, aad.port),
		agentCommandAddress,
		agentCommandResponseAddress,
		amqp.ConnTLSConfig(aad.tlsConfig),
		amqp.ConnSASLXOAUTH2(impersonateUser, token, amqpOverrideSaslFrameSize),
		amqp.ConnServerHostname(aad.host),
		amqp.ConnProperty("product", "command-delegate; console-server"),
		amqp.ConnConnectTimeout(aad.connectTimeout),
		amqp.ConnMaxFrameSize(aad.maxFrameSize))

	a := &amqpAgentCommandDelegate{
		aac:           aad,
		commandClient: commandClient,
		lastUsed:      time.Now(),
	}

	a.commandClient.Start()
	return a
}

func (ad *amqpAgentCommandDelegate) PurgeAddress(address string) error {
	request := &amqp.Message{
		Properties: &amqp.MessageProperties{
			Subject: "purge_address",
		},
		Value: map[interface{}]interface{}{
			"address": address,
		},
	}

	response, err := ad.commandClient.Request(request)
	if err != nil {
		return fmt.Errorf("failed to purge address %s : %s", address, err)
	}

	if outcome, present := response.ApplicationProperties["outcome"]; present {
		if oc, ok := outcome.(bool); ok && oc {
			return nil
		} else {
			if e, present := response.ApplicationProperties["error"]; present && e != nil {
				return fmt.Errorf("failed to purge address %s : %s", address, e)
			}
		}
	}
	return fmt.Errorf("failed to purge address %s : command %+v failed for unknown reason", address, request)
}

func (ad *amqpAgentCommandDelegate) CloseConnection(connection v1.ObjectMeta) error {
	request := &amqp.Message{
		Properties: &amqp.MessageProperties{
			Subject: "close_connection",
		},
		Value: map[interface{}]interface{}{
			"connectionUid": string(connection.UID),
		},
	}

	response, err := ad.commandClient.Request(request)
	if err != nil {
		return fmt.Errorf("failed to close connection %s : %s", connection.UID, err)
	}

	if outcome, present := response.ApplicationProperties["outcome"]; present {
		if oc, ok := outcome.(bool); ok && oc {
			return nil
		} else {
			if e, present := response.ApplicationProperties["error"]; present && e != nil {
				return fmt.Errorf("failed to close connection %s : %s", connection.UID, e)
			}
		}
	}
	return fmt.Errorf("failed to close connection %s : command %+v failed for unknown reason", connection.UID, request)
}

func (ad *amqpAgentCommandDelegate) LastUsed() time.Time {
	return ad.lastUsed
}

func (ad *amqpAgentCommandDelegate) Shutdown() {
	ad.commandClient.Stop()
}

func buildAmqpAddress(host string, port int32) string {
	addr := fmt.Sprintf("amqps://%s:%d", host, port)
	return addr
}
