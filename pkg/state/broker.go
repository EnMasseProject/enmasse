/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package state

func NewBrokerState(host string, port int32) *BrokerState {
	return &BrokerState{
		Host: host,
		Port: port,
	}
}

func (b *BrokerState) Shutdown() {
}
