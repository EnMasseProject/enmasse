/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.amqp.AmqpClient;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


public class ArtemisManagement extends BrokerManagement {
    private static Logger log = CustomLogger.getLogger();

    public ArtemisManagement() {
        managementAddress = "activemq.management";
        resourceProperty = "_AMQ_ResourceName";
        operationProperty = "_AMQ_OperationName";
    }

    @Override
    public List<String> getQueueNames(AmqpClient queueClient, Destination replyQueue, String topic) throws Exception {
        Message requestMessage = Message.Factory.create();
        Map<String, Object> appProperties = new HashMap<>();
        appProperties.put(resourceProperty, "address." + topic);
        appProperties.put(operationProperty, "getQueueNames");
        requestMessage.setAddress(managementAddress);
        requestMessage.setApplicationProperties(new ApplicationProperties(appProperties));
        requestMessage.setReplyTo(replyQueue.getAddress());
        requestMessage.setBody(new AmqpValue("[]"));

        Future<Integer> sent = queueClient.sendMessages(managementAddress, requestMessage);
        assertThat(String.format("Sender failed, expected %d messages", 1), sent.get(30, TimeUnit.SECONDS), is(1));
        log.info("request sent");

        Future<List<Message>> received = queueClient.recvMessages(replyQueue.getAddress(), 1);
        assertThat(String.format("Receiver failed, expected %d messages", 1),
                received.get(30, TimeUnit.SECONDS).size(), is(1));


        AmqpValue val = (AmqpValue) received.get().get(0).getBody();
        log.info("answer received: " + val.toString());
        String queues = val.getValue().toString();
        queues = queues.replaceAll("\\[|]|\"", "");


        return Arrays.asList(queues.split(","));
    }

    @Override
    public int getSubscriberCount(AmqpClient queueClient, Destination replyQueue, String queue) throws Exception {
        Message requestMessage = Message.Factory.create();
        Map<String, Object> appProperties = new HashMap<>();
        appProperties.put(resourceProperty, "queue." + queue);
        appProperties.put(operationProperty, "getConsumerCount");
        requestMessage.setAddress(managementAddress);
        requestMessage.setApplicationProperties(new ApplicationProperties(appProperties));
        requestMessage.setReplyTo(replyQueue.getAddress());
        requestMessage.setBody(new AmqpValue("[]"));

        Future<Integer> sent = queueClient.sendMessages(managementAddress, requestMessage);
        assertThat(String.format("Sender failed, expected %d messages", 1),
                sent.get(30, TimeUnit.SECONDS), is(1));
        log.info("request sent");

        Future<List<Message>> received = queueClient.recvMessages(replyQueue.getAddress(), 1);
        assertThat(String.format("Receiver failed, expected %d messages", 1),
                received.get(30, TimeUnit.SECONDS).size(), is(1));


        AmqpValue val = (AmqpValue) received.get().get(0).getBody();
        log.info("answer received: " + val.toString());
        String count = val.getValue().toString().replaceAll("\\[|]|\"", "");

        return Integer.valueOf(count);
    }
}
