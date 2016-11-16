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
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;

/**
 * Tests related to disconnection
 */
public class DisconnectionTest extends MockMqttFrontendTestBase {

    @Test
    public void bruteDisconnection(TestContext context) {

        Async async = context.async();


        this.willService.willHandler(v -> {

            async.complete();
        });

        try {

            MemoryPersistence persistence = new MemoryPersistence();

            MqttConnectOptions options = new MqttConnectOptions();
            options.setWill(new MqttTopic("will", null), "will".getBytes(), 1, false);

            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), "12345", persistence);
            client.connect(options);

            client.close();

        } catch (MqttException e) {

            e.printStackTrace();
        }

        async.await();

        context.assertTrue(true);
    }

    @Test
    public void disconnection(TestContext context) {

        Async async = context.async();

        this.willService.willHandler(b -> {

            async.complete();
        });

        try {

            MemoryPersistence persistence = new MemoryPersistence();

            MqttConnectOptions options = new MqttConnectOptions();
            options.setWill(new MqttTopic("will", null), "will".getBytes(), 1, false);

            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), "12345", persistence);
            client.connect(options);

            client.disconnect();

            async.await();

            context.assertTrue(true);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }
}
