/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard.mqtt;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.bases.StandardTestBase;
import io.enmasse.systemtest.mqtt.MqttClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests related to publish messages via MQTT
 */
public class PublishTest extends StandardTestBase {

    @Override
    protected boolean skipDummyAddress() {
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

    private void publish(List<String> messages, List<Integer> publisherQos, int subscriberQos) throws Exception {

        Destination dest = Destination.topic("mytopic", "sharded-topic");
        setAddresses(dest);
        Thread.sleep(60_000);

        MqttClient client = mqttClientFactory.createClient();

        Future<List<String>> recvResult = client.recvMessages(dest.getAddress(), messages.size(), subscriberQos);
        Future<Integer> sendResult = client.sendMessages(dest.getAddress(), messages, publisherQos);

        assertThat("Wrong count of messages sent",
                sendResult.get(1, TimeUnit.MINUTES), is(messages.size()));
        assertThat("Wrong count of messages received",
                recvResult.get(1, TimeUnit.MINUTES).size(), is(messages.size()));
    }

    public static void simpleMQTTSendReceive(Destination dest, MqttClient client, int msgCount) throws InterruptedException, ExecutionException, TimeoutException {
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < msgCount; i++) {
            messages.add(String.format("mqtt-simple-send-receive-%s", i));
        }
        Future<List<String>> recvResult = client.recvMessages(dest.getAddress(), msgCount, 0);
        Future<Integer> sendResult = client.sendMessages(dest.getAddress(), messages, Collections.nCopies(msgCount, 0));

        assertThat("Incorrect count of messages sent",
                sendResult.get(1, TimeUnit.MINUTES), is(messages.size()));
        assertThat("Incorrect count of messages received",
                recvResult.get(1, TimeUnit.MINUTES).size(), is(msgCount));
    }
}
