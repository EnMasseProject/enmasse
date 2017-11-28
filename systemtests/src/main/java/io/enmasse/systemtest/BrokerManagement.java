package io.enmasse.systemtest;

import io.enmasse.systemtest.amqp.AmqpClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class BrokerManagement {

    protected String managementAddress;

    public abstract List<String> getQueueNames(AmqpClient queueClient, Destination replyQueue, Destination dest) throws Exception;

    public abstract int getSubscriberCount(AmqpClient queueClient, Destination replyQueue, String queue) throws Exception;

}
