/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.systemtest.mqtt;

import enmasse.systemtest.Destination;
import enmasse.systemtest.TestBase;
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
public class PublishTest extends TestBase {

    @Test
    public void testPublishQoS0() throws Exception {

        List<String> messages = Arrays.asList("foo", "bar", "baz");
        List<Integer> publisherQos = Arrays.asList(0, 0, 0);

        this.publish("mytopicQoS0", messages, publisherQos, 0);
    }

    @Test
    public void testPublishQoS1() throws Exception {

        List<String> messages = Arrays.asList("foo", "bar", "baz");
        List<Integer> publisherQos = Arrays.asList(1, 1, 1);

        this.publish("mytopicQoS1", messages, publisherQos, 1);
    }

    public void testPublishQoS2() throws Exception {

        List<String> messages = Arrays.asList("foo", "bar", "baz");
        List<Integer> publisherQos = Arrays.asList(2, 2, 2);

        this.publish("mytopicQoS2", messages, publisherQos, 2);
    }

    private void publish(String destName, List<String> messages, List<Integer> publisherQos, int subscriberQos) throws Exception {

        Destination dest = Destination.topic(destName);
        setAddresses(dest);
        Thread.sleep(60_000);

        MqttClient client = mqttClientFactory.createClient();

        Future<List<String>> recvResult = client.recvMessages(dest.getAddress(), messages.size(), subscriberQos);
        Future<Integer> sendResult = client.sendMessages(dest.getAddress(), messages, publisherQos);

        assertThat(sendResult.get(1, TimeUnit.MINUTES), is(messages.size()));
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(messages.size()));
    }
}
