/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.broker;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.amqp.AmqpClient;

import java.util.List;

public abstract class BrokerManagement {

    String managementAddress;
    String resourceProperty;
    String operationProperty;

    public abstract List<String> getQueueNames(AmqpClient queueClient, Address replyQueue, String topic) throws Exception;

    public abstract int getSubscriberCount(AmqpClient queueClient, Address replyQueue, String queue) throws Exception;

}
