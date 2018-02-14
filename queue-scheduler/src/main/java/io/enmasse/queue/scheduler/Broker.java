/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.queue.scheduler;

import java.util.Set;
import java.util.concurrent.TimeoutException;

/**
 * Generic interface towards brokers as expected by the queue-scheduler
 */
public interface Broker {
    Set<String> getQueueNames() throws TimeoutException;
    void createQueue(String address) throws TimeoutException;
    void deleteQueue(String address) throws TimeoutException;
}
