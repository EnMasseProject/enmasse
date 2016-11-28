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

import enmasse.mqtt.messages.AmqpHelper;
import enmasse.mqtt.messages.AmqpPublishMessage;
import enmasse.mqtt.messages.AmqpQos;
import enmasse.mqtt.messages.AmqpSubscribeMessage;
import enmasse.mqtt.messages.AmqpTopicSubscription;
import enmasse.mqtt.messages.AmqpUnsubscribeMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock for a "broker like" component
 */
public class MockBroker extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MockBroker.class);

    private static final String CONTAINER_ID = "broker";

    // event bus names for communication between Subscription Service and broker
    public static final String EB_SUBSCRIBE = "subscribe";
    public static final String EB_UNSUBSCRIBE = "unsubscribe";

    // topic -> receiver
    private Map<String, ProtonReceiver> receivers;
    // client-id -> sender (to $mqtt.to.<client-id>
    private Map<String, ProtonSender> senders;
    // topic -> client-id lists (subscribers)
    private Map<String, List<String>> subscriptions;
    // topic -> retained message
    private Map<String, AmqpPublishMessage> retained;

    private String connectAddress;
    private int connectPort;

    private ProtonClient client;
    private ProtonConnection connection;

    private List<String> topics;

    /**
     * Constructor
     */
    public MockBroker() {

        this.receivers = new HashMap<>();
        this.senders = new HashMap<>();
        this.subscriptions = new HashMap<>();
        this.retained = new HashMap<>();
        this.topics = Arrays.asList("my_topic", "will");
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        this.client = ProtonClient.create(this.vertx);

        this.client.connect(this.connectAddress, this.connectPort, done -> {

            if (done.succeeded()) {

                LOG.info("Broker started successfully ...");

                this.connection = done.result();
                this.connection.setContainer(CONTAINER_ID);

                this.connection
                        .sessionOpenHandler(session -> session.open())
                        .open();

                // attach receivers of pre-configured topics
                for (String topic: this.topics) {

                    ProtonReceiver receiver = this.connection.createReceiver(topic);

                    receiver
                            .setQoS(ProtonQoS.AT_LEAST_ONCE)
                            .setTarget(receiver.getRemoteTarget())
                            .handler((delivery, message) -> {

                                this.messageHandler(receiver, delivery, message);
                            })
                            .open();

                    this.receivers.put(topic, receiver);
                }

                // consumer for SUBSCRIBE requests from the Subscription Service
                this.vertx.eventBus().consumer(EB_SUBSCRIBE, ebMessage -> {

                    // the request object is exchanged through the map using messageId in the event bus message
                    Object obj = this.vertx.sharedData().getLocalMap(EB_SUBSCRIBE).remove(ebMessage.body());

                    if (obj instanceof AmqpSubscribeData) {

                        AmqpSubscribeMessage amqpSubscribeMessage = ((AmqpSubscribeData) obj).subscribe();
                        List<AmqpQos> grantedQoSLevels = this.subscribe(amqpSubscribeMessage);

                        // build the reply message body with granted QoS levels (JSON encoded)
                        JsonArray jsonArray = new JsonArray();
                        for (AmqpQos amqpQos: grantedQoSLevels) {
                            jsonArray.add(amqpQos.toJson());
                        }

                        // reply to the SUBSCRIBE request; the Subscription Service can send SUBACK
                        ebMessage.reply(jsonArray, replyDone -> {

                            // after sending SUBACK, Subscription Service reply in order to have mock broker
                            // sending retained message for topic subscriptions
                            if (replyDone.succeeded()) {

                                if (!this.retained.isEmpty()) {

                                    ProtonSender sender = this.connection.createSender(AmqpHelper.getClientAddress(amqpSubscribeMessage.clientId()));

                                    sender.open();

                                    for (AmqpTopicSubscription amqpTopicSubscription: amqpSubscribeMessage.topicSubscriptions()) {
                                        if (this.retained.containsKey(amqpTopicSubscription.topic())) {

                                            AmqpPublishMessage amqpPublishMessage = this.retained.get(amqpTopicSubscription.topic());
                                            // TODO: with which QoS ?
                                            sender.send(amqpPublishMessage.toAmqp());
                                        }
                                    }

                                    sender.close();
                                }
                            }
                        });
                    }
                });

                // consumer for UNSUBSCRIBE requests from the Subscription Service
                this.vertx.eventBus().consumer(EB_UNSUBSCRIBE, ebMessage -> {

                    // the request object is exchanged through the map using messageId in the event bus message
                    Object obj = this.vertx.sharedData().getLocalMap(EB_UNSUBSCRIBE).remove(ebMessage.body());

                    if (obj instanceof  AmqpUnsubscribeData) {

                        AmqpUnsubscribeMessage amqpUnsubscribeMessage = ((AmqpUnsubscribeData) obj).unsubscribe();
                        this.unsubscribe(amqpUnsubscribeMessage);
                        ebMessage.reply(null);
                    }
                });

                startFuture.complete();

            } else {

                LOG.info("Error starting the broker ...", done.cause());

                startFuture.fail(done.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        this.connection.close();
        LOG.info("Broker has been shut down successfully");
        stopFuture.complete();
    }

    /**
     * Handle a subscription request
     *
     * @param amqpSubscribeMessage  AMQP_SUBSCRIBE message with subscribe request
     * @return  granted QoS levels
     */
    public List<AmqpQos> subscribe(AmqpSubscribeMessage amqpSubscribeMessage) {

        List<AmqpQos> grantedQoSLevels = new ArrayList<>();

        for (AmqpTopicSubscription amqpTopicSubscription: amqpSubscribeMessage.topicSubscriptions()) {

            // create a receiver for getting messages from the requested topic
            if (!this.receivers.containsKey(amqpTopicSubscription.topic())) {

                ProtonReceiver receiver = this.connection.createReceiver(amqpTopicSubscription.topic());

                receiver
                        .setQoS(amqpTopicSubscription.qos().toProtonQos())
                        .setTarget(receiver.getRemoteTarget())
                        .handler((delivery, message) -> {

                            this.messageHandler(receiver, delivery, message);
                        })
                        .open();

                this.receivers.put(amqpTopicSubscription.topic(), receiver);
            }

            // create a sender to the unique client address for forwarding
            // messages when received on requested topic
            if (!this.senders.containsKey(amqpSubscribeMessage.clientId())) {

                ProtonSender sender = this.connection.createSender(String.format(AmqpHelper.AMQP_CLIENT_ADDRESS_TEMPLATE, amqpSubscribeMessage.clientId()));

                sender
                        .setQoS(amqpTopicSubscription.qos().toProtonQos())
                        .open();

                this.senders.put(amqpSubscribeMessage.clientId(), sender);
            }

            // add the subscription to the requested topic by the client identifier
            if (!this.subscriptions.containsKey(amqpTopicSubscription.topic())) {

                this.subscriptions.put(amqpTopicSubscription.topic(), new ArrayList<>());
            }

            this.subscriptions.get(amqpTopicSubscription.topic()).add(amqpSubscribeMessage.clientId());

            // just as mock all requested QoS levels are granted
            grantedQoSLevels.add(amqpTopicSubscription.qos());
        }

        return grantedQoSLevels;
    }

    /**
     * Handle an unsubscription request
     *
     * @param amqpUnsubscribeMessage  AMQP_UNSUBSCRIBE message with unsubscribe request
     */
    public void unsubscribe(AmqpUnsubscribeMessage amqpUnsubscribeMessage) {

        for (String topic: amqpUnsubscribeMessage.topics()) {

            this.subscriptions.get(topic).remove(amqpUnsubscribeMessage.clientId());

            if (this.subscriptions.get(topic).size() == 0) {
                this.subscriptions.remove(topic);
            }
        }
    }

    private void messageHandler(ProtonReceiver receiver, ProtonDelivery delivery, Message message) {

        String topic = receiver.getSource().getAddress();

        // TODO: what when raw AMQP message hasn't "publish" as subject ??

        // check if it's retained
        AmqpPublishMessage amqpPublishMessage = AmqpPublishMessage.from(message);
        if (amqpPublishMessage.isRetain()) {
            this.retained.put(amqpPublishMessage.topic(), amqpPublishMessage);
        }

        List<String> subscribers = this.subscriptions.get(topic);

        if (subscribers != null) {

            for (String clientId: subscribers) {

                this.senders.get(clientId).send(message);

            }
        }

    }

    /**
     * Set the address for connecting to the AMQP services
     *
     * @param connectAddress    address for AMQP connections
     * @return  current Mock broker instance
     */
    public MockBroker setConnectAddress(String connectAddress) {
        this.connectAddress = connectAddress;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP services
     *
     * @param connectPort   port for AMQP connections
     * @return  current Mock broker instance
     */
    public MockBroker setConnectPort(int connectPort) {
        this.connectPort = connectPort;
        return this;
    }

}
