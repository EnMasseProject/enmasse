/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.mocks;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Simple MQTT subscriber client
 */
public class MqttSubscriberClient {

    private static final Logger LOG = LoggerFactory.getLogger(MqttSubscriberClient.class);

    public static void main(String[] args) {

        MqttClient client = null;

        try {

            MemoryPersistence persistence = new MemoryPersistence();

            MqttConnectOptions options = new MqttConnectOptions();
            options.setConnectionTimeout(60);
            options.setCleanSession(true);

            client = new MqttClient(String.format("tcp://%s:%d", "localhost", 1883), "12345", persistence);

            client.connect(options);

            client.subscribe("mytopic", 0, (topic, mqttMessage) -> {

                LOG.info("topic: {} message: {}", topic, new String(mqttMessage.getPayload()));

            });

            client.disconnect();

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {

                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    LOG.info("topic {} message {}", s, new String(mqttMessage.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });
            client.connect(options);


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
