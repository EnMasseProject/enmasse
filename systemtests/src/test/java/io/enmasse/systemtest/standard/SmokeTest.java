/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.TestUtils;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.mqtt.MqttUtils;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.enmasse.systemtest.TestTag.nonPR;
import static io.enmasse.systemtest.TestTag.smoke;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a simple smoketest of EnMasse. If this passes, the chances of something being
 * very wrong is minimized. The test should not take to long too execute
 */
@Tag(nonPR)
@Tag(smoke)
class SmokeTest extends TestBaseWithShared implements ITestBaseStandard {

    private Destination queue = Destination.queue("smokeQueue_1", DestinationPlan.STANDARD_SMALL_QUEUE.plan());
    private Destination topic = Destination.topic("smoketopic", DestinationPlan.STANDARD_SMALL_TOPIC.plan());
    private Destination mqttTopic = Destination.topic("smokeMqtt_1", DestinationPlan.STANDARD_LARGE_TOPIC.plan());
    private Destination anycast = Destination.anycast("smokeanycast");
    private Destination multicast = Destination.multicast("smokemulticast");

    @BeforeEach
    void createAddresses() throws Exception {
        setAddresses(queue, topic, mqttTopic, anycast, multicast);
        Thread.sleep(60_000);
    }

    @Test
    void smoketest() throws Exception {
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

        assertAll("Every subscriber should receive all messages",
                () -> assertThat("Wrong count of messages received: receiver0",
                        recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver1",
                        recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver2",
                        recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver3",
                        recvResults.get(3).get(1, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver4",
                        recvResults.get(4).get(1, TimeUnit.MINUTES).size(), is(msgs.size())),
                () -> assertThat("Wrong count of messages received: receiver5",
                        recvResults.get(5).get(1, TimeUnit.MINUTES).size(), is(msgs.size()))
        );
    }

    private void testMqtt() throws Exception {
        List<MqttMessage> messages = Stream.generate(MqttMessage::new).limit(3).collect(Collectors.toList());
        messages.forEach(m -> m.setPayload(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8)));

        IMqttClient client = mqttClientFactory.create();
        client.connect();

        List<CompletableFuture<MqttMessage>> receiveFutures = MqttUtils.subscribeAndReceiveMessages(client, mqttTopic.getAddress(), messages.size(), 1);
        List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(client, mqttTopic.getAddress(), messages);

        int numberSent = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
        assertThat("Wrong count of messages sent", numberSent, is(messages.size()));

        int numberReceived = MqttUtils.awaitAndReturnCode(receiveFutures, 1, TimeUnit.MINUTES);
        assertThat("Wrong count of messages received", numberReceived, is(messages.size()));

        client.disconnect();
        client.close();
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
        List<String> msgs = Collections.singletonList("foo");

        List<Future<List<Message>>> recvResults = Arrays.asList(
                client.recvMessages(multicast.getAddress(), msgs.size()),
                client.recvMessages(multicast.getAddress(), msgs.size()),
                client.recvMessages(multicast.getAddress(), msgs.size()));

        Thread.sleep(60_000);

        assertThat("Wrong count of messages sent",
                client.sendMessages(multicast.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertAll("All receivers should receive all messages",
                () -> assertTrue(recvResults.get(0).get(30, TimeUnit.SECONDS).size() >= msgs.size(),
                        "Wrong count of messages received: receiver0"),
                () -> assertTrue(recvResults.get(1).get(30, TimeUnit.SECONDS).size() >= msgs.size(),
                        "Wrong count of messages received: receiver1"),
                () -> assertTrue(recvResults.get(2).get(30, TimeUnit.SECONDS).size() >= msgs.size(),
                        "Wrong count of messages received: receiver2")
        );
    }
}
