/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package broker

import (
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"
	. "github.com/enmasseproject/enmasse/pkg/state/common"
)

type BrokerState struct {
	host           Host
	port           int32
	nextResync     time.Time
	initialized    bool
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

type BrokerDivert struct {
	Name                 string `json:"name"`
	RoutingName          string `json:"routingName"`
	Address              string `json:"address"`
	ForwardingAddress    string `json:"forwardingAddress"`
	Exclusive            bool   `json:"exclusive"`
	FilterString         string `json:"filterString,omitempty"`
	TransformerClassName string `json:"transformerClassName,omitempty"`
}

type BrokerAddressSetting struct {
	Name                     string             `json:"name"`
	DeadLetterAddress        string             `json:"DLA,omitempty"`
	ExpiryAddress            string             `json:"expiryAddress,omitempty"`
	ExpiryDelay              int64              `json:"expiryDelay,omitempty"`
	LastValueQueue           bool               `json:"lastValueQueue,omitempty"`
	DeliveryAttempts         int32              `json:"deliveryAttempts,omitempty"`
	MaxSizeBytes             int64              `json:"maxSizeBytes,omitempty"`
	PageSizeBytes            int32              `json:"pageSizeBytes,omitempty"`
	PageMaxCacheSize         int32              `json:"pageMaxCacheSize,omitempty"`
	RedeliveryDelay          int64              `json:"redeliveryDelay,omitempty"`
	RedeliveryMultiplier     float64            `json:"redeliveryMultiplier,omitempty"`
	MaxRedeliveryDelay       int64              `json:"maxRedeliveryDelay,omitempty"`
	RedistributionDelay      int64              `json:"redistributionDelay,omitempty"`
	SendToDLAOnNoRoute       bool               `json:"sendToDLAOnNoRoute,omitempty"`
	AddressFullMessagePolicy AddressFullPolicy  `json:"addressFullMessagePolicy,omitempty"`
	SlowConsumerThreshold    int64              `json:"slowConsumerThreshold,omitempty"`
	SlowConsumerCheckPeriod  int64              `json:"slowConsumerCheckPeriod,omitempty"`
	SlowConsumerPolicy       SlowConsumerPolicy `json:"slowConsumerPolicy,omitempty"`
	AutoCreateJmsQueues      bool               `json:"autoCreateJmsQueues,omitempty"`
	AutoDeleteJmsQueues      bool               `json:"autoDeleteJmsQueues,omitempty"`
	AutoCreateJmsTopics      bool               `json:"autoCreateJmsTopics,omitempty"`
	AutoDeleteJmsTopics      bool               `json:"autoDeleteJmsTopics,omitempty"`
	AutoCreateQueues         bool               `json:"autoCreateQueues,omitempty"`
	AutoDeleteQueues         bool               `json:"autoDeleteQueues,omitempty"`
	AutoCreateAddresses      bool               `json:"autoCreateAddresses,omitempty"`
	AutoDeleteAddresses      bool               `json:"autoDeleteAddresses,omitempty"`
}

type AddressFullPolicy string

const (
	AddressFullPolicyDrop  AddressFullPolicy = "DROP"
	AddressFullPolicyPage  AddressFullPolicy = "PAGE"
	AddressFullPolicyFail  AddressFullPolicy = "FAIL"
	AddressFullPolicyBlock AddressFullPolicy = "BLOCK"
)

type SlowConsumerPolicy string

const (
	SlowConsumerPolicyKill   SlowConsumerPolicy = "KILL"
	SlowConsumerPolicyNotify SlowConsumerPolicy = "NOTIFY"
)

type RoutingType string

const (
	RoutingTypeAnycast   RoutingType = "ANYCAST"
	RoutingTypeMulticast RoutingType = "MULTICAST"
	RoutingTypePass      RoutingType = "PASS"
	RoutingTypeStrip     RoutingType = "STRIP"
)
