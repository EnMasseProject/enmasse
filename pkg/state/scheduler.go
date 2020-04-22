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
