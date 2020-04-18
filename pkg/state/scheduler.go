/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

import (
	"fmt"

	v1beta2 "github.com/enmasseproject/enmasse/pkg/apis/enmasse/v1beta2"
)

type Scheduler interface {
	ScheduleAddress(address *v1beta2.MessagingAddress, brokers []*BrokerState) error
}

/*
 * Very dumb scheduler that doesn't look at broker capacity.
 */
type DummyScheduler struct {
}

func (s *DummyScheduler) ScheduleAddress(address *v1beta2.MessagingAddress, brokers []*BrokerState) error {
	// Skip if already scheduled
	if len(address.Status.Brokers) > 0 {
		// TODO: Allow re-scheduling to balance load?
		return nil
	}

	if len(brokers) > 0 {
		broker := brokers[0]
		address.Status.Brokers = append(address.Status.Brokers, v1beta2.MessagingAddressBroker{
			State: v1beta2.MessagingAddressBrokerScheduled,
			Host:  broker.Host,
		})
	} else {
		return fmt.Errorf("No available broker")
	}
	return nil
}
