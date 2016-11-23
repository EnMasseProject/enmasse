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
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to connection
 */
@RunWith(VertxUnitRunner.class)
public class PublishTest extends MockMqttFrontendTestBase {

    @Test
    public void publish(TestContext context) {

        Async async = context.async();

        try {

            // AMQP client connects for receiving the published message
            ProtonClient amqpClient = ProtonClient.create(this.vertx);

            amqpClient.connect(AMQP_CLIENTS_LISTENER_ADDRESS, AMQP_CLIENTS_LISTENER_PORT, done -> {

                if (done.succeeded()) {

                    ProtonConnection connection = done.result();
                    connection.open();

                    ProtonReceiver receiver = connection.createReceiver("my_topic");
                    receiver.handler((delivery, message) -> {

                        LOG.info("Message received {}", message);
                        async.complete();

                    }).open();
                }
            });

            MemoryPersistence persistence = new MemoryPersistence();

            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), "12345", persistence);
            client.connect();

            client.publish("my_topic", "my_payload".getBytes(), 1, false);

            client.disconnect();

            async.await();

            context.assertTrue(true);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }
}
