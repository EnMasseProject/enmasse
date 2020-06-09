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
	Host           Host
	Port           int32
	initialized    bool
	nextResync     time.Time
	commandClient  amqpcommand.Client
	entities       map[BrokerEntityType]map[string]BrokerEntity
	reconnectCount int64
}

type BrokerEntityType string

const (
	BrokerQueueEntity          BrokerEntityType = "queue"
	BrokerAddressEntity        BrokerEntityType = "address"
	BrokerDivertEntity         BrokerEntityType = "divert"
	BrokerAddressSettingEntity BrokerEntityType = "address-setting"
)

type BrokerEntity interface {
	Type() BrokerEntityType
	GetName() string
	Order() int
	Equals(_ BrokerEntity) bool
	Create(_ amqpcommand.Client) error
	Delete(_ amqpcommand.Client) error
}

type BrokerQueue struct {
	Name               string      `json:"name"`
	Address            string      `json:"address"`
	RoutingType        RoutingType `json:"routing-type"`
	MaxConsumers       int         `json:"max-consumers"`
	Durable            bool        `json:"durable"`
	AutoCreateAddress  bool        `json:"auto-create-address"`
	PurgeOnNoConsumers bool        `json:"purge-on-no-consumers"`
}

type BrokerAddress struct {
	Name        string      `json:"name"`
	RoutingType RoutingType `json:"routing-type"`
}

type RoutingType string

const (
	RoutingTypeAnycast   RoutingType = "ANYCAST"
	RoutingTypeMulticast RoutingType = "MULTICAST"
)
