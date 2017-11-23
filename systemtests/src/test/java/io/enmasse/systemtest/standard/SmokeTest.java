/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.StandardTestBase;
import io.enmasse.systemtest.TestBase;
import io.enmasse.systemtest.TestUtils;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.mqtt.MqttClient;
import io.enmasse.systemtest.standard.QueueTest;
import org.apache.qpid.proton.message.Message;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * This is a simple smoketest of EnMasse. If this passes, the chances of something being
 * very wrong is minimized. The test should not take to long too execute
 */
public class SmokeTest extends StandardTestBase {

    private Destination queue = Destination.queue("smokequeue");
    private Destination topic = Destination.topic("smoketopic");
    private Destination mqttTopic = Destination.topic("smokemqtt");
    private Destination anycast = Destination.anycast("smokeanycast");
    private Destination multicast = Destination.multicast("smokemulticast");

    @Before
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

        assertThat(client.sendMessages(topic.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertThat(recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(3).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(4).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(5).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    private void testMqtt() throws Exception {
        List<String> messages = Arrays.asList("foo", "bar", "baz");
        List<Integer> publisherQos = Arrays.asList(1, 1, 1);

        MqttClient client = mqttClientFactory.createClient();

        Future<List<String>> recvResult = client.recvMessages(mqttTopic.getAddress(), messages.size(), 1);
        Future<Integer> sendResult = client.sendMessages(mqttTopic.getAddress(), messages, publisherQos);

        assertThat(sendResult.get(1, TimeUnit.MINUTES), is(messages.size()));
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(messages.size()));
    }

    private void testAnycast() throws Exception {
        AmqpClient client = amqpClientFactory.createQueueClient();

        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        Future<List<Message>> recvResult = client.recvMessages(anycast.getAddress(), msgs.size());
        Future<Integer> sendResult = client.sendMessages(anycast.getAddress(), msgs);

        assertThat(sendResult.get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    private void testMulticast() throws Exception {
        AmqpClient client = amqpClientFactory.createBroadcastClient();
        List<String> msgs = Arrays.asList("foo");

        List<Future<List<Message>>> recvResults = Arrays.asList(
                client.recvMessages(multicast.getAddress(), msgs.size()),
                client.recvMessages(multicast.getAddress(), msgs.size()),
                client.recvMessages(multicast.getAddress(), msgs.size()));

        Thread.sleep(60_000);

        assertThat(client.sendMessages(multicast.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertTrue(recvResults.get(0).get(30, TimeUnit.SECONDS).size() >= msgs.size());
        assertTrue(recvResults.get(1).get(30, TimeUnit.SECONDS).size() >= msgs.size());
        assertTrue(recvResults.get(2).get(30, TimeUnit.SECONDS).size() >= msgs.size());
    }
}
