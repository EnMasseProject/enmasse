/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to unsubscribe
 */
@RunWith(VertxUnitRunner.class)
public class UnsubscribeTest extends MockMqttGatewayTestBase {

    private static final String MQTT_TOPIC = "mytopic";
    private static final String CLIENT_ID = "my_client_id";

    @Before
    public void before(TestContext context) {
        super.setup(context, false);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void unsubscribe(TestContext context) {

        try {

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), CLIENT_ID, persistence);
            client.connect();

            String[] topics = new String[]{ MQTT_TOPIC };
            int[] qos = new int[]{ 1 };
            // after calling subscribe, the qos is replaced with granted QoS that should be the same
            client.subscribe(topics, qos);

            client.unsubscribe(MQTT_TOPIC);

            context.assertTrue(true);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }

    }
}
