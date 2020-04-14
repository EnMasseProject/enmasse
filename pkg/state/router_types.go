/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"github.com/enmasseproject/enmasse/pkg/amqpcommand"
)

type RouterState struct {
	host          string
	port          int32
	initialized   bool
	commandClient amqpcommand.Client
	connectors    []*RouterConnector
}

type RouterConnector struct {
	Host               string `json:"host"`
	Port               string `json:"port"`
	Role               string `json:"role,omitempty"`
	SslProfile         string `json:"sslProfile,omitempty"`
	SaslMechanisms     string `json:"saslMechanisms,omitempty"`
	SaslUsername       string `json:"saslUsername,omitempty"`
	SaslPassword       string `json:"saslPassword,omitempty"`
	LinkCapacity       int    `json:"linkCapacity,omitempty"`
	IdleTimeoutSeconds int    `json:"idleTimeoutSeconds,omitempty"`
	VerifyHostname     bool   `json:"verifyHostname,omitempty"`
	PolicyVhost        string `json:"policyVhost,omitempty"`
}

type RouterListener struct {
	Port int `json:"port"`
}

type RouterVhost struct {
	Host string `json:"host"`
}

type RouterAddress struct {
	Prefix   string `json:"prefix"`
	Waypoint bool   `json:"waypoint,omitempty"`
}

type RouterAutoLink struct {
	Address     string `json:"address"`
	ContainerId string `json:"containerId"`
}

type RouterLinkRoute struct {
	Prefix      string `json:"string"`
	ContainerId string `json:"containerId"`
}
