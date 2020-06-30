/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package scheduler

import (
	"fmt"

	"github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
	"github.com/enmasseproject/enmasse/pkg/state/broker"
)

/*
 * Very dumb scheduler that doesn't look at broker capacity.
 */
type dummyScheduler struct {
}

var _ Scheduler = &dummyScheduler{}

func NewDummyScheduler() Scheduler {
	return &dummyScheduler{}
}

func (s *dummyScheduler) ScheduleTenant(tenant *v1beta2.MessagingTenant, brokers []*broker.BrokerState) error {
	if len(brokers) > 0 {
		broker := brokers[0]
		tenant.Status.Broker = &v1beta2.MessagingAddressBroker{
			State: v1beta2.MessagingAddressBrokerScheduled,
			Host:  broker.Host().Hostname,
		}
	} else {
		return fmt.Errorf("no brokers available")
	}
	return nil
}

func (s *dummyScheduler) ScheduleAddress(address *v1beta2.MessagingAddress, brokers []*broker.BrokerState) error {
	if len(brokers) > 0 {
		broker := brokers[0]
		address.Status.Brokers = append(address.Status.Brokers, v1beta2.MessagingAddressBroker{
			State: v1beta2.MessagingAddressBrokerScheduled,
			Host:  broker.Host().Hostname,
		})
	} else {
		return fmt.Errorf("no brokers available")
	}
	return nil
}
