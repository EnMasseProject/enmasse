/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard.mqtt;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.bases.StandardTestBase;
import io.enmasse.systemtest.mqtt.MqttClient;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests related to publish messages via MQTT
 */
public class PublishTest extends StandardTestBase {

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

        Destination dest = Destination.topic("mytopic", getDefaultPlan(AddressType.TOPIC));
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
}
