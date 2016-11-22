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

package enmasse.mqtt;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to subscribe
 */
@RunWith(VertxUnitRunner.class)
public class SubscribeTest extends MockMqttFrontendTestBase {

    @Test
    public void subscribe(TestContext context) {

        try {

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), "12345", persistence);
            client.connect();

            String[] topics = new String[]{ "my_topic" };
            int[] qos = new int[]{ 1 };
            // after calling subscribe, the qos is replaced with granted QoS that should be the same
            client.subscribe(topics, qos);

            context.assertTrue(qos[0] == 1);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }

    }

    @Test
    public void subscribeAndWaitPublished(TestContext context) {

        Async async = context.async();

        try {

            MemoryPersistence subscriberPersistence = new MemoryPersistence();
            MqttClient subscriber = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), "12345", subscriberPersistence);
            subscriber.connect();

            subscriber.subscribe("my_topic", 1, (topic, message) -> {

                System.out.println("topic: " + topic + " message: " + message);
                async.complete();
            });

            MemoryPersistence publisherPersistence = new MemoryPersistence();
            MqttClient publisher = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), "67890", publisherPersistence);
            publisher.connect();

            this.vertx.runOnContext(v -> {

                try {

                    publisher.publish("my_topic", "Hello".getBytes(), 1, false);

                } catch (MqttException e) {

                }

            });

            async.await();

            context.assertTrue(true);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }

    }
}
