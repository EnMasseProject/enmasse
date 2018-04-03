/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.bases.StandardTestBase;
import io.enmasse.systemtest.TestUtils;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.mqtt.MqttClient;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a simple smoketest of EnMasse. If this passes, the chances of something being
 * very wrong is minimized. The test should not take to long too execute
 */
public class SmokeTest extends StandardTestBase {

    private Destination queue = Destination.queue("smokeQueue_1", "pooled-queue");
    private Destination topic = Destination.topic("smoketopic", "pooled-topic");
    private Destination mqttTopic = Destination.topic("smokeMqtt_1", "sharded-topic");
    private Destination anycast = Destination.anycast("smokeanycast");
    private Destination multicast = Destination.multicast("smokemulticast");

    @BeforeEach
    public void createAddresses() throws Exception {
        setAddresses(queue, topic, mqttTopic, anycast, multicast);
        Thread.sleep(60_000);
    }

    @Test
    public void smoketest() throws Exception {
        testAnycast();
        testQueue();
        testMulticast();
        testTopic();
        testMqtt();
    }

    private void testQueue() throws Exception {
        AmqpClient client = amqpClientFactory.createQueueClient();

        QueueTest.runQueueTest(client, queue);
    }

    private void testTopic() throws Exception {
        AmqpClient client = amqpClientFactory.createTopicClient();
        List<String> msgs = TestUtils.generateMessages(1000);

        List<Future<List<Message>>> recvResults = Arrays.asList(
                client.recvMessages(topic.getAddress(), msgs.size()),
                client.recvMessages(topic.getAddress(), msgs.size()),
                client.recvMessages(topic.getAddress(), msgs.size()),
                client.recvMessages(topic.getAddress(), msgs.size()),
                client.recvMessages(topic.getAddress(), msgs.size()),
                client.recvMessages(topic.getAddress(), msgs.size()));

        Thread.sleep(60_000);

        assertThat("Wrong count of messages sent",
                client.sendMessages(topic.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertThat("Wrong count of messages received: receiver0",
                recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat("Wrong count of messages received: receiver1",
                recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat("Wrong count of messages received: receiver2",
                recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat("Wrong count of messages received: receiver3",
                recvResults.get(3).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat("Wrong count of messages received: receiver4",
                recvResults.get(4).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat("Wrong count of messages received: receiver5",
                recvResults.get(5).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    private void testMqtt() throws Exception {
        List<String> messages = Arrays.asList("foo", "bar", "baz");
        List<Integer> publisherQos = Arrays.asList(1, 1, 1);

        MqttClient client = mqttClientFactory.createClient();

        Future<List<MqttMessage>> recvResult = client.recvMessages(mqttTopic.getAddress(), messages.size(), 1);
        Future<Integer> sendResult = client.sendMessages(mqttTopic.getAddress(), messages, publisherQos);

        assertThat("Wrong count of messages sent",
                sendResult.get(1, TimeUnit.MINUTES), is(messages.size()));
        assertThat("Wrong count of messages received",
                recvResult.get(1, TimeUnit.MINUTES).size(), is(messages.size()));
    }

    private void testAnycast() throws Exception {
        AmqpClient client = amqpClientFactory.createQueueClient();

        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        Future<List<Message>> recvResult = client.recvMessages(anycast.getAddress(), msgs.size());
        Future<Integer> sendResult = client.sendMessages(anycast.getAddress(), msgs);

        assertThat("Wrong count of messages sent", sendResult.get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat("Wrong count of messages received", recvResult.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    private void testMulticast() throws Exception {
        AmqpClient client = amqpClientFactory.createBroadcastClient();
        List<String> msgs = Arrays.asList("foo");

        List<Future<List<Message>>> recvResults = Arrays.asList(
                client.recvMessages(multicast.getAddress(), msgs.size()),
                client.recvMessages(multicast.getAddress(), msgs.size()),
                client.recvMessages(multicast.getAddress(), msgs.size()));

        Thread.sleep(60_000);

        assertThat("Wrong count of messages sent",
                client.sendMessages(multicast.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertTrue(recvResults.get(0).get(30, TimeUnit.SECONDS).size() >= msgs.size(),
                "Wrong count of messages received: receiver0");
        assertTrue(recvResults.get(1).get(30, TimeUnit.SECONDS).size() >= msgs.size(),
                "Wrong count of messages received: receiver1");
        assertTrue(recvResults.get(2).get(30, TimeUnit.SECONDS).size() >= msgs.size(),
                "Wrong count of messages received: receiver2");
    }
}
