/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package agent

import (
	"context"
	"crypto/tls"
	"fmt"
	"log"
	"net"
	"pack.ag/amqp"
	"time"
)

type AgentEventHandler = func(event AgentConnectionEvent) error

type AgentCollector interface {
	Collect(addressSpaceNamespace string, addressSpace string, infraUuid string, host string, port int32, handler AgentEventHandler) error
	Shutdown()
}

type AmqpAgentCollector struct {
	bearerToken           string
	addressSpaceNamespace string
	addressSpace          string
	infraUuid             string
	handler               AgentEventHandler
	stopchan              chan struct{}
	stoppedchan           chan struct{}
}

func AmqpAgentCollectorCreator(bearerToken string) AgentCollector {
	return &AmqpAgentCollector{
		bearerToken: bearerToken,
	}
}

func (aac *AmqpAgentCollector) Collect(addressSpaceNamespace string, addressSpace string, infraUuid string, host string, port int32, handler AgentEventHandler) error {
	aac.addressSpaceNamespace = addressSpaceNamespace
	aac.addressSpace = addressSpace
	aac.infraUuid = infraUuid
	aac.handler = handler

	go func() {
		defer close(aac.stoppedchan)

		log.Printf("Agent Collector %s - starting", aac.infraUuid)

		for {
			err := aac.doCollect(host, port)

			if err == nil {
				break
			} else {
				backoff := 5 * time.Second
				if _, ok := err.(net.Error); ok {
					backoff = 30 * time.Second
				}
				log.Printf("Agent Collector %s - restarting - backoff %s(s), %v", infraUuid, backoff, err)
				if backoff > 0 {
					time.Sleep(backoff)
				}
			}
		}
		log.Printf("Agent Collector %s - stopped", infraUuid)
	}()

	return nil
}

func (aac *AmqpAgentCollector) Shutdown() {
	close(aac.stopchan)
	<-aac.stoppedchan
}

func (aac *AmqpAgentCollector) doCollect(host string, port int32) error {

	addr := fmt.Sprintf("amqps://%s:%d", host, port)
	log.Printf("Agent Collector %s - connecting %s", aac.infraUuid, addr)
	client, err := amqp.Dial(addr,
		amqp.ConnTLSConfig(&tls.Config{
			InsecureSkipVerify: true,
		}),
		amqp.ConnSASLPlain("unused", aac.bearerToken),
		amqp.ConnServerHostname(host),
	)
	if err != nil {
		return err
	}
	defer client.Close()
	log.Printf("Agent Collector %s - connected %s", aac.infraUuid, addr)

	// Open a session
	session, err := client.NewSession()
	if err != nil {
		return err
	}

	ctx := context.Background()

	// Create a receiver
	receiver, err := session.NewReceiver(
		amqp.LinkSourceAddress("/queue-name"),
	)
	if err != nil {
		return err
	}

	defer func() {
		ctx, cancel := context.WithTimeout(ctx, 1*time.Second)
		_ = receiver.Close(ctx)
		cancel()
	}()

	log.Printf("Agent Collector %s - commencing collecting", aac.infraUuid)
	restart := AgentConnectionEvent{
		AddressSpaceNamespace: aac.addressSpaceNamespace,
		AddressSpace:          aac.addressSpace,
		InfraUuid:             aac.infraUuid,
		Type:                  AgentConnectionEventTypeRestart,
	}
	err = aac.handler(restart)
	if err != nil {
		return err
	}

	for {
		select {
		case <-aac.stopchan:
			log.Printf("Agent Collector %s - Shutdown received", aac.infraUuid)
			return nil
		default:
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
						log.Printf("ignoring failure unmarsall connection message %s for infraUuid %s, %v", body, aac.infraUuid, err)
						break
					}

					if err == nil {
						add := AgentConnectionEvent{
							AddressSpaceNamespace: aac.addressSpaceNamespace,
							AddressSpace:          aac.addressSpace,
							InfraUuid:             aac.infraUuid,
							Type:                  AgentConnectionEventTypeAdd,
							Object:                agentcon,
						}
						err = aac.handler(add)
						if err != nil {
							return err
						}
					} else {
						log.Printf("ignoring failure to unmarshall connection message %s for infraUuid %s, %v", body, aac.infraUuid, err)
					}
				case "connection_deleted":
					body := msg.Value.(map[string]interface{})
					agentcon, err := FromAgentConnectionBody(body)
					if err != nil {
						log.Printf("ignoring failure unmarsall connection message %s for infraUuid %s %v", body, aac.infraUuid, err)
						break
					}
					if err == nil {
						delete := AgentConnectionEvent{
							AddressSpaceNamespace: aac.addressSpaceNamespace,
							AddressSpace:          aac.addressSpace,
							InfraUuid:             aac.infraUuid,
							Type:                  AgentConnectionEventTypeDelete,
							Object:                agentcon,
						}
						err = aac.handler(delete)
						if err != nil {
							return err
						}
					} else {
						log.Printf("ignoring failure to unmarshall connection delete message %s for infraUuid %s, %v", body, aac.infraUuid, err)
					}
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

func receiveWithTimeout(ctx context.Context, receiver *amqp.Receiver, timeout time.Duration) (*amqp.Message, error) {
	ctx, cancel := context.WithTimeout(ctx, timeout)
	defer cancel()

	msg, err := receiver.Receive(ctx)
	if err != nil && err != context.DeadlineExceeded {
		return nil, err
	}
	return msg, nil
}
