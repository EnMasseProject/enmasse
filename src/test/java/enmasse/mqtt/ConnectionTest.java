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
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to connection
 */
@RunWith(VertxUnitRunner.class)
public class ConnectionTest extends MockMqttGatewayTestBase {

    private static final String MQTT_WILL_TOPIC = "will";
    private static final String MQTT_WILL_MESSAGE = "Will on EnMasse";
    private static final String CLIENT_ID = "my_client_id";
    private static final String CLIENT_ID_WITH_SESSION = "my_client_id_with_session";

    @Before
    public void before(TestContext context) {
        super.setup(context, false);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void connectionCleanSession(TestContext context) {

        this.connect(context, true, CLIENT_ID, true);
    }

    @Test
    public void connectionNotCleanSession(TestContext context) {

        this.connect(context, false, CLIENT_ID_WITH_SESSION, true);
    }

    @Test
    public void connectionNoWill(TestContext context) {

        this.connect(context, true, CLIENT_ID, false);
    }

    private void connect(TestContext context, boolean isCleanSession, String clientId, boolean isWill) {

        Async async = context.async();

        try {

            MemoryPersistence persistence = new MemoryPersistence();

            MqttConnectOptions options = new MqttConnectOptions();
            if (isWill) {
                options.setWill(new MqttTopic(MQTT_WILL_TOPIC, null), MQTT_WILL_MESSAGE.getBytes(), 1, false);
            }
            options.setCleanSession(isCleanSession);

            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), clientId, persistence);
            client.connect(options);

            context.assertTrue(true);

            async.complete();

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }
}
