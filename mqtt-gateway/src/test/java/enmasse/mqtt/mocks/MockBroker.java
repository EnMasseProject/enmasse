/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt.mocks;

import enmasse.mqtt.AmqpSubscribeData;
import enmasse.mqtt.AmqpUnsubscribeData;
import enmasse.mqtt.AmqpWillData;
import enmasse.mqtt.messages.AmqpHelper;
import enmasse.mqtt.messages.AmqpPublishMessage;
import enmasse.mqtt.messages.AmqpPubrelMessage;
import enmasse.mqtt.messages.AmqpSubscribeMessage;
import enmasse.mqtt.messages.AmqpTopicSubscription;
import enmasse.mqtt.messages.AmqpUnsubscribeMessage;
import enmasse.mqtt.messages.AmqpWillMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonQoS;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
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
    public static final String EB_WILL = "will";
    public static final String EB_LIST = "list";

    // header field and related action available for interacting with the event bus "will"
    public static final String EB_WILL_ACTION_HEADER = "will-action";
    public static final String EB_WILL_ACTION_ADD = "will-add";
    public static final String EB_WILL_ACTION_CLEAR = "will-clear";
    public static final String EB_WILL_ACTION_DELIVERY = "will-delivery";

    // topic -> receiver
    private Map<String, ProtonReceiver> receivers;
    // client-id -> sender (to $mqtt.to.<client-id>.publish)
    private Map<String, ProtonSender> senders;
    // topic -> client-id lists (subscribers)
    private Map<String, List<String>> subscriptions;
    // client-id -> subscription
    private Map<String, List<AmqpTopicSubscription>> sessions;
    // topic -> retained message
    private Map<String, AmqpPublishMessage> retained;
    // receiver link name -> will message
    private Map<String, AmqpWillMessage> wills;
    // client-id -> receiver (to $mqtt.<client-id>.pubrel)
    private Map<String, ProtonReceiver> receiversPubrel;

    private String internalServiceHost;
    private int internalServicePort;

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
        this.sessions = new HashMap<>();
        this.retained = new HashMap<>();
        this.topics = Arrays.asList("mytopic", "will");
        this.wills = new HashMap<>();
        this.receiversPubrel = new HashMap<>();
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        this.client = ProtonClient.create(this.vertx);

        this.client.connect(this.internalServiceHost, this.internalServicePort, done -> {

            if (done.succeeded()) {

                LOG.info("Broker started successfully ...");

                this.connection = done.result();
                this.connection.setContainer(CONTAINER_ID);

                // the mock broker works with link routing (like for EnMasse) so
                // no more receiver attached from broker to router but handling
                // incoming attaching requests for receivers and senders
                this.connection
                        .sessionOpenHandler(session -> session.open())
                        .receiverOpenHandler(this::receiverHandler)
                        .senderOpenHandler(this::senderHandler)
                        .open();

                // consumer for SUBSCRIBE requests from the Subscription Service
                this.vertx.eventBus().consumer(EB_SUBSCRIBE, ebMessage -> {

                    // the request object is exchanged through the map using messageId in the event bus message
                    Object obj = this.vertx.sharedData().getLocalMap(EB_SUBSCRIBE).remove(ebMessage.body());

                    if (obj instanceof AmqpSubscribeData) {

                        AmqpSubscribeMessage amqpSubscribeMessage = ((AmqpSubscribeData) obj).subscribe();
                        List<MqttQoS> grantedQoSLevels = this.subscribe(amqpSubscribeMessage);

                        // build the reply message body with granted QoS levels (JSON encoded)
                        JsonArray jsonArray = new JsonArray();
                        for (MqttQoS qos: grantedQoSLevels) {
                            jsonArray.add(qos);
                        }

                        // reply to the SUBSCRIBE request; the Subscription Service can send SUBACK
                        ebMessage.reply(jsonArray, replyDone -> {

                            // after sending SUBACK, Subscription Service reply in order to have mock broker
                            // sending retained message for topic subscriptions
                            if (replyDone.succeeded()) {

                                if (!this.retained.isEmpty()) {

                                    for (AmqpTopicSubscription amqpTopicSubscription: amqpSubscribeMessage.topicSubscriptions()) {

                                        if (this.retained.containsKey(amqpTopicSubscription.topic())) {

                                            AmqpPublishMessage amqpPublishMessage = this.retained.get(amqpTopicSubscription.topic());
                                            // QoS already set at AT_LEAST_ONCE as requested by the receiver side
                                            this.senders.get(amqpSubscribeMessage.clientId()).send(amqpPublishMessage.toAmqp());
                                        }
                                    }

                                }
                            }
                        });
                    }
                });

                // consumer for UNSUBSCRIBE requests from the Subscription Service
                this.vertx.eventBus().consumer(EB_UNSUBSCRIBE, ebMessage -> {

                    // the request object is exchanged through the map using messageId in the event bus message
                    Object obj = this.vertx.sharedData().getLocalMap(EB_UNSUBSCRIBE).remove(ebMessage.body());

                    if (obj instanceof AmqpUnsubscribeData) {

                        AmqpUnsubscribeMessage amqpUnsubscribeMessage = ((AmqpUnsubscribeData) obj).unsubscribe();
                        this.unsubscribe(amqpUnsubscribeMessage);
                        ebMessage.reply(null);
                    }
                });

                // consumer for LIST requests from the Subscription Service
                this.vertx.eventBus().consumer(EB_LIST, ebMessage -> {

                    String clientId = (String) ebMessage.body();

                    // a session not exist yet, create an empty one
                    if (!this.sessions.containsKey(clientId)) {
                        this.sessions.put(clientId, new ArrayList<>());
                    }

                    JsonArray jsonArray = new JsonArray();
                    for (AmqpTopicSubscription amqpTopicSubscription: this.sessions.get(clientId)) {
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.put("topic", amqpTopicSubscription.topic());
                        jsonObject.put("qos", amqpTopicSubscription.qos().value());
                        jsonArray.add(jsonObject);
                    }

                    ebMessage.reply(jsonArray);
                });

                // consumer for will requests from Last Will and Testament Service
                this.vertx.eventBus().consumer(EB_WILL, ebMessage -> {

                    String willAction = ebMessage.headers().get(EB_WILL_ACTION_HEADER);

                    switch (willAction) {

                        case EB_WILL_ACTION_ADD:

                            // get the AMQP_WILL using the client link name from the message body as key
                            Object obj = this.vertx.sharedData().getLocalMap(EB_WILL).remove(ebMessage.body());

                            if (obj instanceof AmqpWillData) {

                                AmqpWillMessage amqpWillMessage = ((AmqpWillData) obj).will();
                                this.wills.put((String) ebMessage.body(), amqpWillMessage);

                                ebMessage.reply(null);
                            }
                            break;

                        case EB_WILL_ACTION_CLEAR:

                            // clear the will using the client link name as key
                            if (this.wills.containsKey(ebMessage.body())) {
                                this.wills.remove(ebMessage.body());

                                ebMessage.reply(false);
                            }
                            break;

                        case EB_WILL_ACTION_DELIVERY:

                            // the requested client link name has a will to publish
                            if (this.wills.containsKey(ebMessage.body())) {

                                AmqpWillMessage amqpWillMessage = this.wills.remove(ebMessage.body());

                                // no matter about the messageId, this will message will be published to the
                                // MQTT clients using the auto-generated message identifier from MQTT server
                                AmqpPublishMessage amqpPublishMessage =
                                        new AmqpPublishMessage(null, amqpWillMessage.qos(), false, amqpWillMessage.isRetain(), amqpWillMessage.topic(), amqpWillMessage.payload());

                                ProtonSender sender = this.connection.createSender(amqpPublishMessage.topic());

                                sender.setQoS(ProtonQoS.AT_LEAST_ONCE)
                                        .open();

                                sender.send(amqpPublishMessage.toAmqp(), delivery -> {

                                    // true ... will published
                                    ebMessage.reply(true);
                                    sender.close();
                                });

                            } else {

                                // false ... will not published (but request handled)
                                ebMessage.reply(false);
                            }
                            break;
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
    public List<MqttQoS> subscribe(AmqpSubscribeMessage amqpSubscribeMessage) {

        List<MqttQoS> grantedQoSLevels = new ArrayList<>();

        for (AmqpTopicSubscription amqpTopicSubscription: amqpSubscribeMessage.topicSubscriptions()) {

            // create a sender to the unique client publish address for forwarding
            // messages when received on requested topic
            if (!this.senders.containsKey(amqpSubscribeMessage.clientId())) {

                ProtonSender sender = this.connection.createSender(String.format(AmqpHelper.AMQP_CLIENT_PUBLISH_ADDRESS_TEMPLATE, amqpSubscribeMessage.clientId()));

                // QoS AT_LEAST_ONCE as requested by the receiver side
                sender.setQoS(ProtonQoS.AT_LEAST_ONCE)
                        .open();

                this.senders.put(amqpSubscribeMessage.clientId(), sender);
            }

            // create a receiver for the PUBREL client address for receiving
            // such messages (handling QoS 2)
            if (!this.receiversPubrel.containsKey(amqpSubscribeMessage.clientId())) {

                ProtonReceiver receiver = this.connection.createReceiver(String.format(AmqpHelper.AMQP_CLIENT_PUBREL_ADDRESS_TEMPLATE, amqpSubscribeMessage.clientId()));

                receiver
                        .setQoS(ProtonQoS.AT_LEAST_ONCE)
                        .setTarget(receiver.getRemoteTarget())
                        .handler((delivery, message) -> {

                            this.messageHandler(receiver, delivery, message);
                        })
                        .open();

                this.receiversPubrel.put(amqpSubscribeMessage.clientId(), receiver);
            }

            // add the subscription to the requested topic by the client identifier
            if (!this.subscriptions.containsKey(amqpTopicSubscription.topic())) {

                this.subscriptions.put(amqpTopicSubscription.topic(), new ArrayList<>());
            }

            this.subscriptions.get(amqpTopicSubscription.topic()).add(amqpSubscribeMessage.clientId());

            // if there is a session for the client, add the new subscription
            if (this.sessions.containsKey(amqpSubscribeMessage.clientId())) {
                this.sessions.get(amqpSubscribeMessage.clientId()).add(amqpTopicSubscription);
            }

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

    /**
     * Handler for publishers which want to send messages to the broker
     *
     * @param receiver  corresponding receiver for remote publisher
     */
    private void receiverHandler(ProtonReceiver receiver) {

        receiver.setTarget(receiver.getRemoteTarget())
                .setQoS(ProtonQoS.AT_LEAST_ONCE)
                .handler((delivery, message) -> {

                    this.messageHandler(receiver, delivery, message);
                })
                .closeHandler(ar -> {

                    if (ar.succeeded()) {
                        receiver.close();
                    }
                })
                .open();

    }

    /**
     * Handler for receivers which want to receive messages from the broker
     * // NOTE : this is needed when a native AMQP receiver attaches for a topic
     *
     * @param sender    corresponding sender for remote receiver
     */
    private void senderHandler(ProtonSender sender) {

        // put the native AMQP sender (for the native AMQP receiver) for forwarding
        // messages when received on requested topic
        if (!this.senders.containsKey(sender.getName())) {

            // QoS AT_LEAST_ONCE as requested by the receiver side
            sender.setSource(sender.getRemoteSource())
                    .setQoS(ProtonQoS.AT_LEAST_ONCE)
                    .closeHandler(ar -> {

                        if (ar.succeeded()) {

                            String topic = sender.getSource().getAddress();

                            this.subscriptions.get(topic).remove(sender.getName());

                            if (this.subscriptions.get(topic).size() == 0) {
                                this.subscriptions.remove(topic);
                            }
                        }
                    })
                    .open();

            this.senders.put(sender.getName(), sender);
        }

        // add the subscription to the requested topic by the link name
        if (!this.subscriptions.containsKey(sender.getSource().getAddress())) {

            this.subscriptions.put(sender.getSource().getAddress(), new ArrayList<>());
        }

        this.subscriptions.get(sender.getSource().getAddress()).add(sender.getName());
    }

    private void messageHandler(ProtonReceiver receiver, ProtonDelivery delivery, Message message) {

        // messages without subject are just AMQP_PUBLISH messages
        if (message.getSubject() == null) {

            //String topic = receiver.getSource().getAddress();
            String topic = receiver.getTarget().getAddress();

            // check if it's retained
            AmqpPublishMessage amqpPublishMessage = AmqpPublishMessage.from(message);
            if (amqpPublishMessage.isRetain()) {
                this.retained.put(amqpPublishMessage.topic(), amqpPublishMessage);
            }

            delivery.disposition(Accepted.getInstance(), true);

            List<String> subscribers = this.subscriptions.get(topic);

            if (subscribers != null) {

                for (String clientId : subscribers) {

                    // QoS already set at AT_LEAST_ONCE as requested by the receiver side
                    this.senders.get(clientId).send(message);

                }
            }

        // AMQP_PUBREL messages
        } else if (message.getSubject().equals(AmqpPubrelMessage.AMQP_SUBJECT)) {

            AmqpPubrelMessage amqpPubrelMessage = AmqpPubrelMessage.from(message);
            delivery.disposition(Accepted.getInstance(), true);

            String address = receiver.getSource().getAddress();

            String clientId = AmqpHelper.getClientIdFromPubrelAddress(address);

            // QoS already set at AT_LEAST_ONCE as requested by the receiver side
            this.senders.get(clientId).send(message);
        }
    }

    /**
     * Set the address for connecting to the AMQP services
     *
     * @param internalServiceHost    address for AMQP connections
     * @return  current Mock broker instance
     */
    public MockBroker setInternalServiceHost(String internalServiceHost) {
        this.internalServiceHost = internalServiceHost;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP services
     *
     * @param internalServicePort   port for AMQP connections
     * @return  current Mock broker instance
     */
    public MockBroker setInternalServicePort(int internalServicePort) {
        this.internalServicePort = internalServicePort;
        return this;
    }

}
