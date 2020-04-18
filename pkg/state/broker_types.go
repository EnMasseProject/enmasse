/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"github.com/enmasseproject/enmasse/pkg/amqpcommand"
)

type BrokerState struct {
	Host          string
	Port          int32
	initialized   bool
	commandClient amqpcommand.Client
	queues        map[string]bool
}
