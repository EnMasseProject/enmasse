/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import enmasse.mqtt.messages.AmqpWillMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.LinkError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test related to handling "will" information
 */
@RunWith(VertxUnitRunner.class)
public class WillTest extends MqttLwtTestBase {

    private static final String MQTT_WILL_TOPIC = "will";
    private static final String MQTT_WILL_MESSAGE = "Will on EnMasse";
    private static final String CLIENT_ID = "client_id";

    private static final int DETACH_TIMEOUT = 1000;

    private Async async;

    @Before
    public void before(TestContext context) {
        // these tests are based on deploying MQTT LWT service on start
        super.setup(context, true);
    }

    @After
    public void after(TestContext context) {
        super.tearDown(context);
    }

    @Test
    public void bruteDisconnection(TestContext context) {

        this.amqpReceiver(context, MQTT_WILL_TOPIC, 0, true);
        this.willClient(context, MQTT_WILL_TOPIC, MQTT_WILL_MESSAGE, 0, true);
    }

    @Test
    public void cleanDisconnection(TestContext context) {

        this.amqpReceiver(context, MQTT_WILL_TOPIC, 0, false);
        this.willClient(context, MQTT_WILL_TOPIC, MQTT_WILL_MESSAGE, 0, false);
    }

    private void amqpReceiver(TestContext context, String topic, int qos, boolean isDetachForced) {

        // AMQP client connects for receiving the published message
        ProtonClient client = ProtonClient.create(this.vertx);

        client.connect(MESSAGING_SERVICE_HOST, dispatchRouterJ.getNormalPort(), done -> {

            if (done.succeeded()) {

                ProtonConnection connection = done.result();
                connection.open();

                ProtonLinkOptions options = new ProtonLinkOptions();
                options.setLinkName(CLIENT_ID);

                ProtonReceiver receiver = connection.createReceiver(topic, options);

                AtomicBoolean received = new AtomicBoolean();

                receiver
                        .setQoS((qos == 0) ? ProtonQoS.AT_MOST_ONCE : ProtonQoS.AT_LEAST_ONCE)
                        .handler((d, m) -> {

                            LOG.info("topic: {}, message: {}", topic, ((Data)m.getBody()).getValue());
                            d.disposition(Accepted.getInstance(), true);
                            received.compareAndSet(false, true);

                            //this.async.complete();
                            //context.assertTrue(true);

                        }).open();

                this.vertx.setTimer(DETACH_TIMEOUT * 2, t -> {

                    // brute disconnection
                    if (isDetachForced) {

                        // receiver should have received the will message
                        context.assertTrue(received.get());

                    // clean disconnection
                    } else {

                        // receiver should not have received the will message
                        context.assertTrue(!received.get());
                    }
                    this.async.complete();

                });

            } else {

                context.assertTrue(false);
                done.cause().printStackTrace();
            }
        });
    }

    private void willClient(TestContext context, String topic, String message, int qos, boolean isDetachForced) {

        this.async = context.async();

        // AMQP client connects for publishing message
        ProtonClient client = ProtonClient.create(this.vertx);

        client.connect(MESSAGING_SERVICE_HOST, dispatchRouterJ.getNormalPort(), done -> {

            if (done.succeeded()) {

                ProtonConnection connection = done.result();
                connection.open();

                ProtonSender sender = connection.createSender(LWT_SERVICE_ENDPOINT);

                sender.open();

                AmqpWillMessage amqpWillMessage = new AmqpWillMessage(false, topic, MqttQoS.valueOf(qos), Buffer.buffer(message));
                sender.send(amqpWillMessage.toAmqp());

                // disconnect after some time
                this.vertx.setTimer(DETACH_TIMEOUT, t -> {

                    if (isDetachForced) {
                        ErrorCondition errorCondition =
                                new ErrorCondition(LinkError.DETACH_FORCED, "Link detached due to a brute client disconnection");
                        sender.setCondition(errorCondition);
                    }
                    sender.close();
                });

            } else {

                context.assertTrue(false);
                done.cause().printStackTrace();
            }

        });

        this.async.await();
    }
}
