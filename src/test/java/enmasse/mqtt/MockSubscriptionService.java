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

import enmasse.mqtt.messages.AmqpQos;
import enmasse.mqtt.messages.AmqpSessionMessage;
import enmasse.mqtt.messages.AmqpSessionPresentMessage;
import enmasse.mqtt.messages.AmqpSubackMessage;
import enmasse.mqtt.messages.AmqpSubscribeMessage;
import enmasse.mqtt.messages.AmqpUnsubackMessage;
import enmasse.mqtt.messages.AmqpUnsubscribeMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock for the Subscription Service
 */
public class MockSubscriptionService extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MockSubscriptionService.class);

    private static final String SUBSCRIPTION_SERVICE_ENDPOINT = "$mqtt.subscriptionservice";
    private static final String CONTAINER_ID = "subscription-service";

    private String connectAddress;
    private int connectPort;

    private ProtonClient client;
    private ProtonConnection connection;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        this.client = ProtonClient.create(this.vertx);

        this.client.connect(this.connectAddress, this.connectPort, done -> {

            if (done.succeeded()) {

                LOG.info("Subscription Service started successfully ...");

                this.connection = done.result();
                this.connection.setContainer(CONTAINER_ID);

                this.connection
                        .sessionOpenHandler(session -> session.open())
                        .open();

                ProtonReceiver receiver = this.connection.createReceiver(SUBSCRIPTION_SERVICE_ENDPOINT);

                receiver
                        .setTarget(receiver.getRemoteTarget())
                        .handler(this::messageHandler)
                        .open();

                startFuture.complete();

            } else {

                LOG.info("Error starting the Subscription Service ...", done.cause());

                startFuture.fail(done.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        this.connection.close();
        LOG.info("Subscription Service has been shut down successfully");
        stopFuture.complete();
    }

    private void messageHandler(ProtonDelivery delivery, Message message) {

        LOG.info("Received {}", message);

        switch (message.getSubject()) {

            case AmqpSessionMessage.AMQP_SUBJECT:

                {
                    // get AMQP_SESSION message and sends disposition for settlement
                    AmqpSessionMessage amqpSessionMessage = AmqpSessionMessage.from(message);
                    delivery.disposition(Accepted.getInstance(), true);

                    // send AMQP_SESSION_PRESENT to the unique client address
                    ProtonSender sender = this.connection.createSender(message.getReplyTo());

                    AmqpSessionPresentMessage amqpSessionPresentMessage =
                            new AmqpSessionPresentMessage(amqpSessionMessage.clientId().equals("12345"));

                    sender.open();

                    sender.send(amqpSessionPresentMessage.toAmqp(), d -> {

                        sender.close();
                    });
                }

                break;

            case AmqpSubscribeMessage.AMQP_SUBJECT:

                {
                    // get AMQP_SUBSCRIBE message and sends disposition for settlement
                    AmqpSubscribeMessage amqpSubscribeMessage = AmqpSubscribeMessage.from(message);
                    delivery.disposition(Accepted.getInstance(), true);

                    // the request object is exchanged through the map using messageId in the event bus message
                    this.vertx.sharedData().getLocalMap(MockBroker.EB_SUBSCRIBE)
                            .put(amqpSubscribeMessage.messageId(), new AmqpSubscribeData(amqpSubscribeMessage.messageId(), amqpSubscribeMessage));

                    // send SUBSCRIBE request to the broker
                    this.vertx.eventBus().send(MockBroker.EB_SUBSCRIBE, amqpSubscribeMessage.messageId(), done -> {

                        if (done.succeeded()) {

                            // event bus message body contains granted QoS levels (JSON encoded)
                            List<AmqpQos> grantedQoSLevels = new ArrayList<>();
                            JsonArray jsonArray = (JsonArray) done.result().body();
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JsonObject object = jsonArray.getJsonObject(i);
                                grantedQoSLevels.add(AmqpQos.from(object));
                            }

                            // send AMQP_SUBACK to the unique client address
                            ProtonSender sender = this.connection.createSender(message.getReplyTo());

                            AmqpSubackMessage amqpSubackMessage =
                                    new AmqpSubackMessage(amqpSubscribeMessage.messageId(), grantedQoSLevels);

                            sender.open();

                            sender.send(amqpSubackMessage.toAmqp(), d -> {

                                // after sending AMQP_SUBACK, start to send retained AMQP_PUBLISH messages

                                // reply to the mock broker, for allowing it to start sending retained messages
                                done.result().reply(null);

                                sender.close();
                            });

                        }

                    });

                }

                break;

            case AmqpUnsubscribeMessage.AMQP_SUBJECT:

                {
                    // get AMQP_UNSUBSCRIBE mesage and sends disposition for settlment
                    AmqpUnsubscribeMessage amqpUnsubscribeMessage = AmqpUnsubscribeMessage.from(message);
                    delivery.disposition(Accepted.getInstance(), true);

                    // the request object is exchanged through the map using messageId in the event bus message
                    this.vertx.sharedData().getLocalMap(MockBroker.EB_UNSUBSCRIBE)
                            .put(amqpUnsubscribeMessage.messageId(), new AmqpUnsubscribeData(amqpUnsubscribeMessage.messageId(), amqpUnsubscribeMessage));

                    // send UNSUBSCRIBE request to the broker
                    this.vertx.eventBus().send(MockBroker.EB_UNSUBSCRIBE, amqpUnsubscribeMessage.messageId(), done -> {

                        if (done.succeeded()) {

                            // send AMQP_UNSUBACK to the unique client address
                            ProtonSender sender = this.connection.createSender(message.getReplyTo());

                            AmqpUnsubackMessage amqpUnsubackMessage =
                                    new AmqpUnsubackMessage(amqpUnsubscribeMessage.messageId());

                            sender.open();

                            sender.send(amqpUnsubackMessage.toAmqp(), d -> {

                                sender.close();
                            });
                        }

                    });

                }

                break;
        }

    }

    /**
     * Set the address for connecting to the AMQP services
     *
     * @param connectAddress    address for AMQP connections
     * @return  current Mock Subscription Service instance
     */
    public MockSubscriptionService setConnectAddress(String connectAddress) {
        this.connectAddress = connectAddress;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP services
     *
     * @param connectPort   port for AMQP connections
     * @return  current Mock Subscription Service instance
     */
    public MockSubscriptionService setConnectPort(int connectPort) {
        this.connectPort = connectPort;
        return this;
    }
}
