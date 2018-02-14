/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
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
