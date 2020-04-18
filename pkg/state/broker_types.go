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
	Host          string
	Port          int32
	initialized   bool
	nextResync    time.Time
	commandClient amqpcommand.Client
	queues        map[string]bool
}

type BrokerQueue struct {
	Name    string
	Address string
}
