/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package scheduler

import (
	v1 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1"
	"github.com/enmasseproject/enmasse/pkg/state/broker"
)

type Scheduler interface {
	// Schedule an address to a broker. The implication is that all addresses will be configured on the same broker.
	ScheduleTenant(tenant *v1.MessagingTenant, brokers []*broker.BrokerState) error
	// Schedule an address to a broker.
	ScheduleAddress(address *v1.MessagingAddress, brokers []*broker.BrokerState) error
}
