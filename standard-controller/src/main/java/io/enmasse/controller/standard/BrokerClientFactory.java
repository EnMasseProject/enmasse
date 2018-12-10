/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.amqp.SyncRequestClient;

public interface BrokerClientFactory {
    SyncRequestClient connectBrokerManagementClient(String host, int port) throws Exception;
}
