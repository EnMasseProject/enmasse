/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

/**
 * Test related to SSL/TLS support
 */
public class SslTest extends MockMqttGatewayTestBase {

    private static final String MQTT_TOPIC = "mytopic";
    private static final String MQTT_MESSAGE = "Hello MQTT on EnMasse";
    private static final String SUBSCRIBER_ID = "my_subscriber_id";
    private static final String PUBLISHER_ID = "my_publisher_id";

    private Async async;

    @Before
    public void before(TestContext context) {

        URL trustStore =  this.getClass().getResource("/tls/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStore", trustStore.getPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "wibble");

        super.setup(context, true);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void mqttPublishQoS0toMqtt(TestContext context) {

        this.mqttReceiver(context, MQTT_TOPIC, 0);
        this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 0);

        context.assertTrue(true);
    }

    @Test
    public void mqttPublishQoS1toMqtt(TestContext context) {

        this.mqttReceiver(context, MQTT_TOPIC, 1);
        this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 1);

        context.assertTrue(true);
    }

    @Test
    public void mqttPublishQoS2toMqtt(TestContext context) {

        this.mqttReceiver(context, MQTT_TOPIC, 2);
        this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 2);

        context.assertTrue(true);
    }

    private void mqttReceiver(TestContext context, String topic, int qos) {

        try {

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(String.format("ssl://%s:%d", MQTT_BIND_ADDRESS, MQTT_TLS_LISTEN_PORT), SUBSCRIBER_ID, persistence);
            client.connect();

            client.subscribe(topic, qos, (t, m) -> {

                LOG.info("topic: {}, message: {}", t, m);
                this.async.complete();
            });

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }

    private void mqttPublish(TestContext context, String topic, String message, int qos) {

        this.async = context.async();

        try {

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(String.format("ssl://%s:%d", MQTT_BIND_ADDRESS, MQTT_TLS_LISTEN_PORT), PUBLISHER_ID, persistence);
            client.connect();

            client.publish(topic, message.getBytes(), qos, false);

            this.async.await();

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }
}
