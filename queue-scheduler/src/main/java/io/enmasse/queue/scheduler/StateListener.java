/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.queue.scheduler;

import io.enmasse.address.model.Address;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public interface StateListener {
    void addressesChanged(Map<String, Set<Address>> updatedMap) throws TimeoutException;
    void brokerAdded(String groupId, String brokerId, Broker broker) throws TimeoutException;
    void brokerRemoved(String groupId, String brokerId) throws TimeoutException;
}
