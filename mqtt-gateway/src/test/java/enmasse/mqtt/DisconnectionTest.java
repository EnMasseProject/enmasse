/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Data;
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
 * Tests related to disconnection
 */
@RunWith(VertxUnitRunner.class)
public class DisconnectionTest extends MockMqttGatewayTestBase {

    private static final String MQTT_WILL_TOPIC = "will";
    private static final String MQTT_WILL_MESSAGE = "Will on EnMasse";
    private static final String CLIENT_ID = "client_id";
    private static final String WILL_CLIENT_ID = "ignore-disconnect";

    private Async async;

    @Before
    public void before(TestContext context) {
        super.setup(context, false);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void disconnection(TestContext context) {

        Async async = context.async();

        this.lwtService.willHandler(b -> {

            async.complete();
        });

        try {

            MemoryPersistence persistence = new MemoryPersistence();

            MqttConnectOptions options = new MqttConnectOptions();
            options.setWill(new MqttTopic(MQTT_WILL_TOPIC, null), MQTT_WILL_MESSAGE.getBytes(), 1, false);

            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), CLIENT_ID, persistence);
            client.connect(options);

            client.disconnect();

            async.await();

            context.assertTrue(true);

        } catch (Exception e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }

    @Test
    public void bruteDisconnectionWillQoS0ToMqtt(TestContext context) {

        this.mqttReceiver(context, MQTT_WILL_TOPIC, 0);
        this.willClient(context, MQTT_WILL_TOPIC, MQTT_WILL_MESSAGE, 0);
    }

    @Test
    public void bruteDisconnectionWillQoS1ToMqtt(TestContext context) {

        this.mqttReceiver(context, MQTT_WILL_TOPIC, 1);
        this.willClient(context, MQTT_WILL_TOPIC, MQTT_WILL_MESSAGE, 1);
    }

    @Test
    public void bruteDisconnectionWillQoS2ToMqtt(TestContext context) {

        this.mqttReceiver(context, MQTT_WILL_TOPIC, 2);
        this.willClient(context, MQTT_WILL_TOPIC, MQTT_WILL_MESSAGE, 2);
    }

    @Test
    public void bruteDisconnectionWillQoS0ToAmqp(TestContext context) {

        this.amqpReceiver(context, MQTT_WILL_TOPIC, 0);
        this.willClient(context, MQTT_WILL_TOPIC, MQTT_WILL_MESSAGE, 0);
    }

    @Test
    public void bruteDisconnectionWillQoS1ToAmqp(TestContext context) {

        this.amqpReceiver(context, MQTT_WILL_TOPIC, 1);
        this.willClient(context, MQTT_WILL_TOPIC, MQTT_WILL_MESSAGE, 1);
    }

    @Test
    public void bruteDisconnectionWillQoS2ToAmqp(TestContext context) {

        this.amqpReceiver(context, MQTT_WILL_TOPIC, 2);
        this.willClient(context, MQTT_WILL_TOPIC, MQTT_WILL_MESSAGE, 2);
    }

    private void amqpReceiver(TestContext context, String topic, int qos) {

        // AMQP client connects for receiving the published message
        ProtonClient client = ProtonClient.create(this.vertx);

        client.connect(MESSAGING_SERVICE_HOST, router.getNormalPort(), done -> {

            if (done.succeeded()) {

                ProtonConnection connection = done.result();
                connection.open();

                ProtonLinkOptions options = new ProtonLinkOptions();
                options.setLinkName(CLIENT_ID);

                ProtonReceiver receiver = connection.createReceiver(topic, options);

                receiver
                        .setQoS((qos == 0) ? ProtonQoS.AT_MOST_ONCE : ProtonQoS.AT_LEAST_ONCE)
                        .handler((d, m) -> {

                            LOG.info("topic: {}, message: {}", topic, ((Data)m.getBody()).getValue());
                            d.disposition(Accepted.getInstance(), true);
                            this.async.complete();

                        }).open();

            } else {

                context.assertTrue(false);
                done.cause().printStackTrace();
            }
        });
    }

    private void mqttReceiver(TestContext context, String topic, int qos) {

        try {

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), CLIENT_ID, persistence);
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

    private void willClient(TestContext context, String topic, String message, int qos) {

        this.async = context.async();

        try {

        MemoryPersistence persistence = new MemoryPersistence();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setWill(new MqttTopic(topic, null), message.getBytes(), qos, false);

        // workaround for testing "brute disconnection" ignoring the DISCONNECT
        // Eclipse Paho doesn't provide a way to close connection without sending DISCONNECT
        // The mock Last Will and Testament Service will not clear the will message for this "ignore-disconnect" client
        MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), WILL_CLIENT_ID, persistence);
        client.connect(options);

        client.disconnect();

        this.async.await();

        context.assertTrue(true);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }

}
