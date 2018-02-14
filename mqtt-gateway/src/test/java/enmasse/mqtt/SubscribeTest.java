/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to subscribe
 */
@RunWith(VertxUnitRunner.class)
public class SubscribeTest extends MockMqttGatewayTestBase {

    private static final String MQTT_TOPIC = "mytopic";
    private static final String MQTT_MESSAGE = "Hello MQTT on EnMasse";
    private static final String SUBSCRIBER_ID = "my_subscriber_id";
    private static final String PUBLISHER_ID = "my_publisher_id";

    @Before
    public void before(TestContext context) {
        super.setup(context, false);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void subscribe(TestContext context) {

        try {

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), SUBSCRIBER_ID, persistence);
            client.connect();

            String[] topics = new String[]{ MQTT_TOPIC };
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
            MqttClient subscriber = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), SUBSCRIBER_ID, subscriberPersistence);
            subscriber.connect();

            subscriber.subscribe(MQTT_TOPIC, 1, (topic, message) -> {

                LOG.info("topic: {} message: {}", topic, message);
                async.complete();
            });

            MemoryPersistence publisherPersistence = new MemoryPersistence();
            MqttClient publisher = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), PUBLISHER_ID, publisherPersistence);
            publisher.connect();

            publisher.publish(MQTT_TOPIC, MQTT_MESSAGE.getBytes(), 1, false);

            async.await();

            context.assertTrue(true);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }

    @Test
    public void subscribeAndReceiveRetained(TestContext context) {

        Async async = context.async();

        try {

            MemoryPersistence publisherPersistence = new MemoryPersistence();

            MqttClient publisher = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), PUBLISHER_ID, publisherPersistence);
            publisher.connect();

            publisher.publish(MQTT_TOPIC, MQTT_MESSAGE.getBytes(), 0, true);

            publisher.disconnect();

            MemoryPersistence subscriberPersistence = new MemoryPersistence();
            MqttClient subscriber = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), SUBSCRIBER_ID, subscriberPersistence);
            subscriber.connect();

            subscriber.subscribe(MQTT_TOPIC, 1, (topic, message) -> {

                LOG.info("topic: {} message: {}", topic, message);
                async.complete();
            });

            async.await();

            context.assertTrue(true);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }

    @Test
    public void subscribeRecoveringSession(TestContext context) {

        Async async = context.async();

        try {

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);

            MemoryPersistence subscriberPersistence = new MemoryPersistence();
            MqttClient subscriber = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), SUBSCRIBER_ID, subscriberPersistence);
            subscriber.connect(options);

            subscriber.subscribe(MQTT_TOPIC, 0);
            subscriber.disconnect();

            // re-connect without subscribing, should receive published message
            subscriber.connect(options);
            subscriber.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable throwable) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {

                    LOG.info("topic: {} message: {}", topic, new String(mqttMessage.getPayload()));
                    async.complete();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

                }
            });

            MemoryPersistence publisherPersistence = new MemoryPersistence();

            MqttClient publisher = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), PUBLISHER_ID, publisherPersistence);
            publisher.connect();

            publisher.publish(MQTT_TOPIC, MQTT_MESSAGE.getBytes(), 0, true);

            publisher.disconnect();

            async.await();

            context.assertTrue(true);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }
}
