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
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests related to publish
 */
@RunWith(VertxUnitRunner.class)
public class PublishTest extends MockMqttGatewayTestBase {

    private static final String MQTT_TOPIC = "mytopic";
    private static final String MQTT_MESSAGE = "Hello MQTT on EnMasse";
    private static final String SUBSCRIBER_ID = "my_subscriber_id";
    private static final String PUBLISHER_ID = "my_publisher_id";

    private int receivedQos;

    @Before
    public void before(TestContext context) {
        super.setup(context, false);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void mqttPublishQoStoMqtt(TestContext context) throws InterruptedException {

        for (int receiverQos = 0; receiverQos <= 2; receiverQos++) {

            for (int publisherQos = 0; publisherQos <= 2; publisherQos++) {

                // This is to make sure we get a clean state for each iteration
                super.tearDown(context);
                super.setup(context, false);

                // MQTT 3.1.1 spec :  The QoS of Payload Messages sent in response to a Subscription MUST be
                // the minimum of the QoS of the originally published message and the maximum QoS granted by the Server
                int qos = (receiverQos < publisherQos) ? receiverQos : publisherQos;

                Async messageReceived = context.async();
                this.mqttReceiver(context, MQTT_TOPIC, receiverQos, messageReceived);
                this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, publisherQos);

                messageReceived.await();
                context.assertTrue(qos == this.receivedQos);

            }
        }
    }

    @Test
    public void mqttPublishQoS0toMqtt(TestContext context) {

        Async messageReceived = context.async();
        this.mqttReceiver(context, MQTT_TOPIC, 0, messageReceived);
        this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 0);

        messageReceived.await();
        context.assertTrue(true);
    }

    @Test
    public void mqttPublishQoS1toMqtt(TestContext context) {

        Async messageReceived = context.async();
        this.mqttReceiver(context, MQTT_TOPIC, 1, messageReceived);
        this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 1);

        messageReceived.await();
        context.assertTrue(true);
    }

    @Test
    public void mqttPublishQoS2toMqtt(TestContext context) {

        Async messageReceived = context.async();
        this.mqttReceiver(context, MQTT_TOPIC, 2, messageReceived);
        this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 2);

        messageReceived.await();
        context.assertTrue(true);
    }

    @Test
    public void mqttPublishQoS0toAmqp(TestContext context) {

        Async messageReceived = context.async();
        this.amqpReceiver(context, MQTT_TOPIC, 0, messageReceived);
        this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 0);

        messageReceived.await();
        context.assertTrue(true);
    }

    @Test
    public void mqttPublishQoS1toAmqp(TestContext context) {

        Async messageReceived = context.async();
        this.amqpReceiver(context, MQTT_TOPIC, 1, messageReceived);
        this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 1);

        messageReceived.await();
        context.assertTrue(true);
    }

    @Test
    public void mqttPublishQoS2toAmqp(TestContext context) {

        Async messageReceived = context.async();
        this.amqpReceiver(context, MQTT_TOPIC, 2, messageReceived);
        this.mqttPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 2);
        messageReceived.await();

        context.assertTrue(true);
    }

    @Test
    public void amqpPublishQoS0toMqtt(TestContext context) {

        Async messageReceived = context.async();
        this.mqttReceiver(context, MQTT_TOPIC, 0, messageReceived);
        this.amqpPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 0);
        messageReceived.await();

        // without "durable" header and/or x-opt-mqtt-qos annotation, message always republished with qos = 0
        context.assertTrue(this.receivedQos == 0);
    }

    @Test
    public void amqpPublishQoS1toMqtt(TestContext context) {

        Async messageReceived = context.async();
        this.mqttReceiver(context, MQTT_TOPIC, 1, messageReceived);
        this.amqpPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 1);

        messageReceived.await();
        // using "durable" header, the qos is 1 as default
        context.assertTrue(this.receivedQos == 1);
    }

    @Test
    public void amqpPublishQoS2toMqtt(TestContext context) {

        Async messageReceived = context.async();
        this.mqttReceiver(context, MQTT_TOPIC, 2, messageReceived);
        this.amqpPublish(context, MQTT_TOPIC, MQTT_MESSAGE, 2);

        messageReceived.await();
        // using "durable" header, the qos is 1 as default
        context.assertTrue(this.receivedQos == 1);
    }

    private void mqttReceiver(TestContext context, String topic, int qos, Async messageReceived) {

        Async connected = context.async();

        try {

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), SUBSCRIBER_ID, persistence);
            client.connect();

            client.subscribe(topic, qos, (t, m) -> {

                LOG.info("topic: {}, message: {}", t, m);
                this.receivedQos = m.getQos();
                messageReceived.complete();
            });
            connected.complete();

        } catch (Exception e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
        connected.await();
    }

    private void amqpReceiver(TestContext context, String topic, int qos, Async receivedMessage) {

        // AMQP client connects for receiving the published message
        ProtonClient client = ProtonClient.create(this.vertx);

        Async connected = context.async();

        client.connect(MESSAGING_SERVICE_HOST, router.getNormalPort(), done -> {

            if (done.succeeded()) {

                ProtonConnection connection = done.result();
                connection.open();

                ProtonLinkOptions options = new ProtonLinkOptions();
                options.setLinkName(SUBSCRIBER_ID);

                ProtonReceiver receiver = connection.createReceiver(topic, options);

                receiver.setQoS((qos == 0) ? ProtonQoS.AT_MOST_ONCE : ProtonQoS.AT_LEAST_ONCE)
                        .openHandler(result -> {
                            if (result.succeeded()) {
                                connected.complete();
                            }
                        })
                        .handler((d, m) -> {

                            LOG.info("topic: {}, message: {}", topic, ((Data)m.getBody()).getValue());
                            d.disposition(Accepted.getInstance(), true);
                            receivedMessage.complete();

                        }).open();

            } else {

                context.assertTrue(false);
                done.cause().printStackTrace();
            }
        });

        connected.await();
    }

    private void amqpPublish(TestContext context, String topic, String payload, int qos) {

        // AMQP client connects for publishing message
        ProtonClient client = ProtonClient.create(this.vertx);

        client.connect(MESSAGING_SERVICE_HOST, router.getNormalPort(), done -> {

            if (done.succeeded()) {

                ProtonConnection connection = done.result();
                connection.open();

                ProtonSender sender = connection.createSender(topic);

                sender
                        .setQoS((qos == 0) ? ProtonQoS.AT_MOST_ONCE : ProtonQoS.AT_LEAST_ONCE)
                        .open();

                Message message = ProtonHelper.message();
                message.setBody(new Data(new Binary(payload.getBytes())));
                message.setAddress(topic);

                if (qos > 0) {
                    message.setDurable(true); // it will be AT_LEAST_ONCE as default
                }

                sender.send(message);

            } else {

                context.assertTrue(false);
                done.cause().printStackTrace();
            }

        });
    }

    private void mqttPublish(TestContext context, String topic, String message, int qos) {

        try {

            MemoryPersistence persistence = new MemoryPersistence();
            MqttClient client = new MqttClient(String.format("tcp://%s:%d", MQTT_BIND_ADDRESS, MQTT_LISTEN_PORT), PUBLISHER_ID, persistence);
            client.connect();

            client.publish(topic, message.getBytes(), qos, false);

        } catch (MqttException e) {

            context.assertTrue(false);
            e.printStackTrace();
        }
    }

}
