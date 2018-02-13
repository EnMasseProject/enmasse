/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.amqp.AmqpClient;

import java.util.List;

public abstract class BrokerManagement {

    protected String managementAddress;
    protected String resourceProperty;
    protected String operationProperty;

    public abstract List<String> getQueueNames(AmqpClient queueClient, Destination replyQueue, String topic) throws Exception;

    public abstract int getSubscriberCount(AmqpClient queueClient, Destination replyQueue, String queue) throws Exception;

}
