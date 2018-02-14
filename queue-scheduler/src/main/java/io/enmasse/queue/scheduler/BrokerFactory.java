/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.queue.scheduler;

import io.vertx.core.Future;
import io.vertx.proton.ProtonConnection;

/**
 * Factory for creating broker instances.
 */
public interface BrokerFactory {
    Future<Broker> createBroker(ProtonConnection connection);
}
