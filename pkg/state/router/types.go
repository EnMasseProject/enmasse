/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package router

import (
	"time"

	"github.com/enmasseproject/enmasse/pkg/amqpcommand"
	. "github.com/enmasseproject/enmasse/pkg/state/common"
)

type RouterState struct {
	host           Host
	port           int32
	initialized    bool
	nextResync     time.Time
	reconnectCount int64
	commandClient  amqpcommand.Client
	entities       map[RouterEntityType]map[string]RouterEntity
}

type RouterEntityType string

const (
	RouterAddressEntity           RouterEntityType = "org.apache.qpid.dispatch.router.config.address"
	RouterListenerEntity          RouterEntityType = "org.apache.qpid.dispatch.listener"
	RouterConnectorEntity         RouterEntityType = "org.apache.qpid.dispatch.connector"
	RouterAutoLinkEntity          RouterEntityType = "org.apache.qpid.dispatch.router.config.autoLink"
	RouterLinkRouteEntity         RouterEntityType = "org.apache.qpid.dispatch.router.config.linkRoute"
	RouterSslProfileEntity        RouterEntityType = "org.apache.qpid.dispatch.sslProfile"
	RouterAuthServicePluginEntity RouterEntityType = "org.apache.qpid.dispatch.authServicePlugin"
)

type RouterEntity interface {
	Type() RouterEntityType
	GetName() string
	Equals(RouterEntity) bool
	Order() int
}

type RouterEntityGroup struct {
	Entities []RouterEntity
}

type NamedEntity struct {
	EntityType RouterEntityType
	Name       string
}

type RouterConnector struct {
	Name               string `json:"name"`
	Host               string `json:"host"`
	Port               string `json:"port"`
	Role               string `json:"role,omitempty"`
	SslProfile         string `json:"sslProfile,omitempty"`
	SaslMechanisms     string `json:"saslMechanisms,omitempty"`
	SaslUsername       string `json:"saslUsername,omitempty"`
	SaslPassword       string `json:"saslPassword,omitempty"`
	LinkCapacity       int    `json:"linkCapacity,omitempty"`
	IdleTimeoutSeconds int    `json:"idleTimeoutSeconds,omitempty"`
	VerifyHostname     bool   `json:"verifyHostname"`
	PolicyVhost        string `json:"policyVhost,omitempty"`
	ConnectionStatus   string `json:"connectionStatus,omitempty"`
	ConnectionMsg      string `json:"connectionMsg,omitempty"`
}

type RouterListener struct {
	Name                           string `json:"name"`
	Host                           string `json:"host"`
	Port                           string `json:"port"`
	Role                           string `json:"role,omitempty"`
	SslProfile                     string `json:"sslProfile,omitempty"`
	SaslMechanisms                 string `json:"saslMechanisms,omitempty"`
	LinkCapacity                   int    `json:"linkCapacity,omitempty"`
	IdleTimeoutSeconds             int    `json:"idleTimeoutSeconds,omitempty"`
	InitialHandshakeTimeoutSeconds int    `json:"initialHandshakeTimeoutSeconds,omitempty"`
	PolicyVhost                    string `json:"policyVhost,omitempty"`
	AuthenticatePeer               bool   `json:"authenticatePeer"`
	MultiTenant                    bool   `json:"multiTenant"`
	RequireSsl                     bool   `json:"requireSsl"`
	Http                           bool   `json:"http"`
	Metrics                        bool   `json:"metrics"`
	Healthz                        bool   `json:"healthz"`
	Websockets                     bool   `json:"websockets"`
	HttpRootDir                    string `json:"httpRootDir,omitempty"`
	SaslPlugin                     string `json:"saslPlugin,omitempty"`
}

type RouterVhost struct {
	Host string `json:"host"`
}

type RouterAddress struct {
	Name         string `json:"name"`
	Prefix       string `json:"prefix"`
	Waypoint     bool   `json:"waypoint"`
	Distribution string `json:"distribution"`
}

type RouterAutoLink struct {
	Name            string `json:"name"`
	Address         string `json:"address"`
	Direction       string `json:"direction"`
	Connection      string `json:"connection,omitempty"`
	ExternalAddress string `json:"externalAddress,omitempty"`
}

type RouterLinkRoute struct {
	Name       string `json:"name"`
	Prefix     string `json:"prefix"`
	Direction  string `json:"direction"`
	Connection string `json:"connection,omitempty"`
}

type RouterSslProfile struct {
	Name           string `json:"name"`
	Ciphers        string `json:"ciphers,omitempty"`
	Protocols      string `json:"protocols,omitempty"`
	CaCertFile     string `json:"caCertFile,omitempty"`
	CertFile       string `json:"certFile,omitempty"`
	PrivateKeyFile string `json:"privateKeyFile,omitempty"`
}

type RouterAuthServicePlugin struct {
	Host       string `json:"host"`
	Port       string `json:"port"`
	Realm      string `json:"realm"`
	SslProfile string `json:"sslProfile"`
}
