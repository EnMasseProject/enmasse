/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"
)

type BrokerState struct {
	Host          Host
	Port          int32
	initialized   bool
	nextResync    time.Time
	commandClient amqpcommand.Client
	queues        map[string]bool
}

type QueueConfiguration struct {
	Name               string      `json:"name"`
	Address            string      `json:"address"`
	RoutingType        RoutingType `json:"routing-type"`
	MaxConsumers       int         `json:"max-consumers"`
	Durable            bool        `json:"durable"`
	AutoCreateAddress  bool        `json:"auto-create-address"`
	PurgeOnNoConsumers bool        `json:"purge-on-no-consumers"`
}

type RoutingType string

const (
	RoutingTypeAnycast   RoutingType = "ANYCAST"
	RoutingTypeMulticast RoutingType = "MULTICAST"
)
