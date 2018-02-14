/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.mocks;

import enmasse.mqtt.AmqpSubscribeData;
import enmasse.mqtt.AmqpUnsubscribeData;
import enmasse.mqtt.messages.AmqpCloseMessage;
import enmasse.mqtt.messages.AmqpListMessage;
import enmasse.mqtt.messages.AmqpSubscribeMessage;
import enmasse.mqtt.messages.AmqpSubscriptionsMessage;
import enmasse.mqtt.messages.AmqpTopicSubscription;
import enmasse.mqtt.messages.AmqpUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
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

    private static final String SUBSCRIPTION_SERVICE_ENDPOINT = "$subctrl";
    private static final String CONTAINER_ID = "subscription-service";

    private String internalServiceHost;
    private int internalServicePort;

    private ProtonClient client;
    private ProtonConnection connection;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        this.client = ProtonClient.create(this.vertx);

        this.client.connect(this.internalServiceHost, this.internalServicePort, done -> {

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

            case AmqpListMessage.AMQP_SUBJECT:

                {
                    // get AMQP_LIST message and sends disposition for settlement
                    AmqpListMessage amqpListMessage = AmqpListMessage.from(message);
                    delivery.disposition(Accepted.getInstance(), true);

                    // send LIST request to the broker
                    this.vertx.eventBus().send(MockBroker.EB_LIST, amqpListMessage.clientId(), done -> {

                        List<AmqpTopicSubscription> subscriptions = new ArrayList<>();

                        // event bus message body contains subscriptions (JSON encoded)
                        JsonArray jsonArray = (JsonArray) done.result().body();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject jsonObject = jsonArray.getJsonObject(i);
                            subscriptions.add(
                                    new AmqpTopicSubscription(jsonObject.getString("topic"),
                                            MqttQoS.valueOf(jsonObject.getInteger("qos"))));
                        }

                        // send AMQP_SUBSCRIPTIONS to the unique client address
                        ProtonSender sender = this.connection.createSender(message.getReplyTo());

                        AmqpSubscriptionsMessage amqpSubscriptionsMessage =
                                new AmqpSubscriptionsMessage(subscriptions);

                        sender.open();

                        sender.send(amqpSubscriptionsMessage.toAmqp(), d -> {

                            sender.close();
                        });
                    });

                }

                break;

            case AmqpCloseMessage.AMQP_SUBJECT:

                {
                    // get AMQP_CLOSE message and sends disposition for settlement
                    AmqpCloseMessage amqpCloseMessage = AmqpCloseMessage.from(message);
                    delivery.disposition(Accepted.getInstance(), true);

                    // TODO: simulate closing a previous session with related subscriptions ? (communicate with MockBroker)
                }

                break;

            case AmqpSubscribeMessage.AMQP_SUBJECT:

                {
                    // get AMQP_SUBSCRIBE message and sends disposition for settlement
                    AmqpSubscribeMessage amqpSubscribeMessage = AmqpSubscribeMessage.from(message);

                    // the request object is exchanged through the map using messageId in the event bus message
                    this.vertx.sharedData().getLocalMap(MockBroker.EB_SUBSCRIBE)
                            .put(amqpSubscribeMessage.messageId(), new AmqpSubscribeData(amqpSubscribeMessage.messageId(), amqpSubscribeMessage));

                    // send SUBSCRIBE request to the broker
                    this.vertx.eventBus().send(MockBroker.EB_SUBSCRIBE, amqpSubscribeMessage.messageId(), done -> {

                        if (done.succeeded()) {

                            // event bus message body contains granted QoS levels (JSON encoded)
                            List<MqttQoS> grantedQoSLevels = new ArrayList<>();
                            JsonArray jsonArray = (JsonArray) done.result().body();
                            for (int i = 0; i < jsonArray.size(); i++) {
                                String qos = jsonArray.getString(i);
                                grantedQoSLevels.add(MqttQoS.valueOf(qos));
                            }

                            // TODO: removed AMQP_SUBACK, need for grantedQoSLevels here ?

                            delivery.disposition(Accepted.getInstance(), true);

                            // after disposition for AMQP_SUBSCRIBE, start to send retained AMQP_PUBLISH messages

                            // reply to the mock broker, for allowing it to start sending retained messages
                            done.result().reply(null);

                        } else {

                            ErrorCondition errorCondition = new ErrorCondition(Symbol.getSymbol("enmasse:subscribe-refused"), "SUBSCRIBE refused");
                            Rejected rejected = new Rejected();
                            rejected.setError(errorCondition);

                            delivery.disposition(rejected, true);
                        }

                    });

                }

                break;

            case AmqpUnsubscribeMessage.AMQP_SUBJECT:

                {
                    // get AMQP_UNSUBSCRIBE mesage and sends disposition for settlment
                    AmqpUnsubscribeMessage amqpUnsubscribeMessage = AmqpUnsubscribeMessage.from(message);

                    // the request object is exchanged through the map using messageId in the event bus message
                    this.vertx.sharedData().getLocalMap(MockBroker.EB_UNSUBSCRIBE)
                            .put(amqpUnsubscribeMessage.messageId(), new AmqpUnsubscribeData(amqpUnsubscribeMessage.messageId(), amqpUnsubscribeMessage));

                    // send UNSUBSCRIBE request to the broker
                    this.vertx.eventBus().send(MockBroker.EB_UNSUBSCRIBE, amqpUnsubscribeMessage.messageId(), done -> {

                        if (done.succeeded()) {

                            delivery.disposition(Accepted.getInstance(), true);
                        }

                    });

                }

                break;
        }

    }

    /**
     * Set the address for connecting to the AMQP services
     *
     * @param internalServiceHost    address for AMQP connections
     * @return  current Mock Subscription Service instance
     */
    public MockSubscriptionService setInternalServiceHost(String internalServiceHost) {
        this.internalServiceHost = internalServiceHost;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP services
     *
     * @param internalServicePort   port for AMQP connections
     * @return  current Mock Subscription Service instance
     */
    public MockSubscriptionService setInternalServicePort(int internalServicePort) {
        this.internalServicePort = internalServicePort;
        return this;
    }
}
