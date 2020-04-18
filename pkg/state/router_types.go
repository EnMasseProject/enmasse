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
	addresses     []*RouterAddress
	autoLinks     []*RouterAutoLink
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
	Name         string `json:"name"`
	Prefix       string `json:"prefix"`
	Waypoint     bool   `json:"waypoint,omitempty"`
	Distribution string `json:"distribution,omitempty"`
}

type RouterAutoLink struct {
	Name            string `json:"name"`
	Address         string `json:"address"`
	Direction       string `json:"direction"`
	Connection      string `json:"connection,omitempty"`
	ExternalAddress string `json:"externalAddress,omitempty"`
}

type RouterLinkRoute struct {
	Prefix      string `json:"string"`
	ContainerId string `json:"containerId"`
}
