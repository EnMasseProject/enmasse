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

package enmasse.mqtt.mocks;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Simple MQTT publisher client
 */
public class MqttPublisherClient {

    private static final Logger LOG = LoggerFactory.getLogger(MqttPublisherClient.class);

    public static void main(String[] args) {

        MqttClient client = null;

        try {

            MemoryPersistence persistence = new MemoryPersistence();

            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(60);

            client = new MqttClient(String.format("tcp://%s:%d", "localhost", 1883), "67890", persistence);
            client.connect(options);

            client.publish("mytopic", "Hello".getBytes(), 0, false);

        } catch (MqttException e) {

            e.printStackTrace();
        }

        try {

            System.in.read();
            client.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
