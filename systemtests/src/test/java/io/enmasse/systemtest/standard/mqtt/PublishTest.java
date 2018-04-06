/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard.mqtt;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.bases.ITestBaseStandard;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.mqtt.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests related to publish messages via MQTT
 */
public class PublishTest extends TestBaseWithShared implements ITestBaseStandard {
    private static Logger log = CustomLogger.getLogger();

    public static void simpleMQTTSendReceive(Destination dest, MqttClient client, int msgCount) throws InterruptedException, ExecutionException, TimeoutException {
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < msgCount; i++) {
            messages.add(String.format("mqtt-simple-send-receive-%s", i));
        }
        Future<List<MqttMessage>> recvResult = client.recvMessages(dest.getAddress(), msgCount, 0);
        Future<Integer> sendResult = client.sendMessages(dest.getAddress(), messages, Collections.nCopies(msgCount, 0));

        assertThat("Incorrect count of messages sent",
                sendResult.get(1, TimeUnit.MINUTES), is(messages.size()));
        assertThat("Incorrect count of messages received",
                recvResult.get(1, TimeUnit.MINUTES).size(), is(msgCount));
    }

    @Override
    public boolean skipDummyAddress() {
        return true;
    }

    @Test
    public void testPublishQoS0() throws Exception {

        List<String> messages = Arrays.asList("foo", "bar", "baz");
        List<Integer> publisherQos = Arrays.asList(0, 0, 0);

        this.publish(messages, publisherQos, 0);
    }

    @Test
    public void testPublishQoS1() throws Exception {

        List<String> messages = Arrays.asList("foo", "bar", "baz");
        List<Integer> publisherQos = Arrays.asList(1, 1, 1);

        this.publish(messages, publisherQos, 1);
    }

    public void testPublishQoS2() throws Exception {

        List<String> messages = Arrays.asList("foo", "bar", "baz");
        List<Integer> publisherQos = Arrays.asList(2, 2, 2);

        this.publish(messages, publisherQos, 2);
    }

    @Test
    @Disabled("related issue: #?")
    public void testRetainedMessages() throws Exception {
        Destination topic = Destination.topic("retained-message-topic", "sharded-topic");
        setAddresses(topic);

        MqttMessage retainedMessage = new MqttMessage();
        retainedMessage.setQos(1);
        retainedMessage.setPayload("retained-message".getBytes());
        retainedMessage.setId(1);
        retainedMessage.setRetained(true);

        //send retained message to the topic
        MqttClient mqttClient = mqttClientFactory.createClient();
        Future<Integer> sendResultMqtt = mqttClient.sendMessages(topic.getAddress(), retainedMessage);
        assertThat("Incorrect count of messages sent",
                sendResultMqtt.get(1, TimeUnit.MINUTES), is(1));

        //each client which will subscribe to the topic should receive retained message!
        int clients = 5;
        for (int i = 0; i < clients; i++) {
            mqttClient = mqttClientFactory.createClient();
            Future<List<MqttMessage>> recvResult = mqttClient.recvMessages(topic.getAddress(), 1, 1);
            assertThat("Incorrect count of messages received",
                    recvResult.get(1, TimeUnit.MINUTES).size(), is(1));
            log.info("id:{}, retained: {}", recvResult.get().get(0).getId(), recvResult.get().get(0).isRetained());
            assertTrue("Retained message expected", recvResult.get().get(0).isRetained());
        }
    }

    private void publish(List<String> messages, List<Integer> publisherQos, int subscriberQos) throws Exception {

        Destination dest = Destination.topic("mytopic", "sharded-topic");
        setAddresses(dest);
        Thread.sleep(60_000);

        MqttClient client = mqttClientFactory.createClient();

        Future<List<MqttMessage>> recvResult = client.recvMessages(dest.getAddress(), messages.size(), subscriberQos);
        Future<Integer> sendResult = client.sendMessages(dest.getAddress(), messages, publisherQos);

        assertThat("Wrong count of messages sent",
                sendResult.get(1, TimeUnit.MINUTES), is(messages.size()));
        assertThat("Wrong count of messages received",
                recvResult.get(1, TimeUnit.MINUTES).size(), is(messages.size()));
    }
}
