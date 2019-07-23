/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE;

import enmasse.mqtt.endpoints.AmqpPublishData;
import enmasse.mqtt.endpoints.AmqpPublishEndpoint;
import enmasse.mqtt.endpoints.AmqpPublisher;
import enmasse.mqtt.endpoints.AmqpReceiver;
import enmasse.mqtt.endpoints.AmqpReceiverEndpoint;
import enmasse.mqtt.endpoints.AmqpSubscriptionServiceEndpoint;
import enmasse.mqtt.endpoints.AmqpLwtServiceEndpoint;
import enmasse.mqtt.messages.AmqpCloseMessage;
import enmasse.mqtt.messages.AmqpListMessage;
import enmasse.mqtt.messages.AmqpPublishMessage;
import enmasse.mqtt.messages.AmqpPubrelMessage;
import enmasse.mqtt.messages.AmqpSubscribeMessage;
import enmasse.mqtt.messages.AmqpSubscriptionsMessage;
import enmasse.mqtt.messages.AmqpTopicSubscription;
import enmasse.mqtt.messages.AmqpUnsubscribeMessage;
import enmasse.mqtt.messages.AmqpWillMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttWill;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import io.vertx.mqtt.messages.MqttUnsubscribeMessage;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * AMQP bridging class from/to the MQTT endpoint to/from the AMQP related endpoints
 */
public class AmqpBridge {

    private static final int AMQP_SERVICES_CONNECTION_TIMEOUT = 60_000; // in ms

    private static final Logger LOG = LoggerFactory.getLogger(AmqpBridge.class);

    // Completed once a bridge is opened, or fails to open
    private final Future<Void> openedFuture = Future.future();

    // Future that completes when the bridge closes.
    private final Future<Void> closedFuture = Future.future();

    // local endpoint for handling remote connected MQTT client
    private final MqttEndpoint mqttEndpoint;

    private final SocketAddress remoteAddress;

    private final Vertx vertx;

    private final AtomicBoolean closed = new AtomicBoolean();

    private ProtonClient client;
    private ProtonConnection connection;


    // endpoint for handling communication with Last Will and Testament Service (LWTS)
    private AmqpLwtServiceEndpoint lwtEndpoint;
    // endpoint for handling communication with Subscription Service (SS)
    private AmqpSubscriptionServiceEndpoint ssEndpoint;
    // endpoint for handling incoming messages on the unique client address
    private AmqpReceiverEndpoint rcvEndpoint;
    // endpoint for publishing message on topic (via AMQP)
    private AmqpPublishEndpoint pubEndpoint;

    // callback called when the MQTT client closes connection
    private Handler<AmqpBridge> mqttEndpointCloseHandler;

    // topic subscriptions with granted QoS levels
    private Map<String, MqttQoS> grantedQoSLevels;

    private boolean detachForced = true;

    /**
     * Constructor
     *
     * @param vertx Vert.x instance
     * @param mqttEndpoint  MQTT local endpoint
     */
    public AmqpBridge(Vertx vertx, MqttEndpoint mqttEndpoint) {
        this.vertx = vertx;
        this.mqttEndpoint = mqttEndpoint;
        this.remoteAddress = mqttEndpoint.remoteAddress();
    }

    /**
     * Open the bridge and connect to the AMQP service provider
     *
     * @param address   AMQP service provider address
     * @param port      AMQP service provider port
     * @param openHandler   handler called when the open is completed (with success or not)
     */
    public void open(String address, int port, Handler<AsyncResult<AmqpBridge>> openHandler) {

        this.client = ProtonClient.create(this.vertx);

        // TODO: check correlation between MQTT and AMQP keep alive
        ProtonClientOptions clientOptions = new ProtonClientOptions();
        clientOptions.setHeartbeat(this.mqttEndpoint.keepAliveTimeSeconds() * 1000);

        String userName = (this.mqttEndpoint.auth() != null) ? this.mqttEndpoint.auth().getUsername() : null;
        String password = (this.mqttEndpoint.auth() != null) ? this.mqttEndpoint.auth().getPassword() : null;
        // NOTE : if username/password are null then Vert.x Proton just provides SASL ANONYMOUS as supported mechanism
        //        otherwise it provides PLAIN with username/password provided here
        this.client.connect(clientOptions, address, port, userName, password, done -> {

            String clientIdentifier = this.mqttEndpoint.clientIdentifier();
            if (done.succeeded()) {

                this.connection = done.result();
                this.connection
                        .closeHandler(remoteClose -> handleRemoteConnectionClose(this.connection, remoteClose))
                        .disconnectHandler(this::handleRemoteDisconnect)
                        .open();

                // setup MQTT endpoint handlers and AMQP endpoints
                this.setupMqttEndpoint();
                this.setupAmqpEndpoits();

                // setup a Future for completed connection steps with all services
                // with AMQP_WILL and AMQP_LIST/AMQP_SUBSCRIPTIONS or AMQP_CLOSE handled
                Future<AmqpSubscriptionsMessage> connectionFuture = Future.future();
                connectionFuture.setHandler(ar -> {

                    try {
                        if (ar.succeeded()) {

                            this.rcvEndpoint.publishHandler(this::publishHandler);
                            this.rcvEndpoint.pubrelHandler(this::pubrelHandler);

                            AmqpSubscriptionsMessage amqpSubscriptionsMessage = ar.result();

                            if (amqpSubscriptionsMessage != null) {
                                this.mqttEndpoint.accept(!amqpSubscriptionsMessage.topicSubscriptions().isEmpty());
                                // added topic subscriptions of a previous session in the local collection
                                this.grantedQoSLevels = amqpSubscriptionsMessage.topicSubscriptions()
                                        .stream()
                                        .collect(Collectors.toMap(amqpTopicSubscription -> amqpTopicSubscription.topic(),
                                                amqpTopicSubscription -> amqpTopicSubscription.qos()));

                            } else {
                                this.mqttEndpoint.accept(false);
                                this.grantedQoSLevels = new HashMap<>();
                            }
                            LOG.info("CONNACK [{}] to MQTT client {} at {}", CONNECTION_ACCEPTED.ordinal(),
                                     clientIdentifier, this.remoteAddress);

                            // open unique client publish address receiver
                            this.rcvEndpoint.openPublish();

                            openHandler.handle(Future.succeededFuture(AmqpBridge.this));

                        } else {

                            this.mqttEndpoint.reject(CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                            LOG.error("CONNACK [{}] to MQTT client {} at {}", CONNECTION_REFUSED_SERVER_UNAVAILABLE.ordinal(),
                                      clientIdentifier, this.remoteAddress);

                            openHandler.handle(Future.failedFuture(ar.cause()));
                        }
                    }
                    finally
                    {
                        openedFuture.complete();
                    }
                });

                // step 1 : send AMQP_WILL to Last Will and Testament Service
                Future<ProtonDelivery> willFuture = Future.future();
                // if remote MQTT has specified the will
                if (this.mqttEndpoint.will().isWillFlag()) {

                    // sending AMQP_WILL
                    MqttWill will = this.mqttEndpoint.will();

                    AmqpWillMessage amqpWillMessage =
                            new AmqpWillMessage(will.isWillRetain(),
                                    will.getWillTopic(),
                                    MqttQoS.valueOf(will.getWillQos()),
                                    Buffer.buffer(will.getWillMessageBytes()));

                    // specified link name for the Last Will and Testament Service as MQTT clientid
                    ProtonLinkOptions linkOptions = new ProtonLinkOptions();
                    linkOptions.setLinkName(clientIdentifier);

                    // setup and open AMQP endpoints to Last Will and Testament Service
                    ProtonSender wsSender = this.connection.createSender(AmqpLwtServiceEndpoint.LWT_SERVICE_ENDPOINT, linkOptions);
                    this.lwtEndpoint = new AmqpLwtServiceEndpoint(wsSender);

                    this.lwtEndpoint.open();
                    this.lwtEndpoint.sendWill(amqpWillMessage, willFuture);

                } else {

                    // otherwise just complete the Future
                    willFuture.complete();
                }

                willFuture.compose(v -> {

                    // handling AMQP_SUBSCRIPTIONS reply from Subscription Service
                    this.rcvEndpoint.subscriptionsHandler(amqpSubscriptionsMessage -> {

                        LOG.info("Session present: {}", !amqpSubscriptionsMessage.topicSubscriptions().isEmpty());
                        LOG.info(amqpSubscriptionsMessage.toString());

                        connectionFuture.complete(amqpSubscriptionsMessage);
                    });

                    // step 2 : send AMQP_CLOSE or AMQP_LIST (based on "clean session" flag) to Subscription Service
                    Future<ProtonDelivery> sessionFuture = Future.future();

                    if (this.mqttEndpoint.isCleanSession()) {

                        // sending AMQP_CLOSE
                        AmqpCloseMessage amqpCloseMessage =
                                new AmqpCloseMessage(clientIdentifier);

                        this.ssEndpoint.sendClose(amqpCloseMessage, closeAsyncResult -> {

                            // in case of AMQP_CLOSE, the connection completes on its disposition
                            // no other AMQP message will be delivered by Subscription Service (i.e. AMQP_SUBSCRIPTIONS)
                            if (closeAsyncResult.succeeded()) {
                                connectionFuture.complete();
                            } else {
                                connectionFuture.fail(closeAsyncResult.cause());
                            }
                        });

                    } else {

                        // sending AMQP_LIST
                        AmqpListMessage amqpListMessage =
                                new AmqpListMessage(clientIdentifier);

                        this.ssEndpoint.sendList(amqpListMessage, sessionFuture);
                    }

                    return sessionFuture;

                }).compose(v -> {
                    // nothing here !??
                }, connectionFuture);

                // timeout for the overall connection process
                vertx.setTimer(AMQP_SERVICES_CONNECTION_TIMEOUT, timer -> {
                   if (!connectionFuture.isComplete()) {
                       connectionFuture.fail("Timeout on connecting to AMQP services");
                   }
                });

            } else {

                LOG.error("Error connecting to AMQP services ...", done.cause());
                final MqttConnectReturnCode code;
                if (done.cause() instanceof SecurityException) {
                    // error on the SASL mechanism side
                    code = CONNECTION_REFUSED_NOT_AUTHORIZED;
                } else {
                    code = CONNECTION_REFUSED_SERVER_UNAVAILABLE;

                }
                this.mqttEndpoint.reject(code);

                openHandler.handle(Future.failedFuture(done.cause()));

                LOG.info("CONNACK [{}] to MQTT client {} at {}", code.ordinal(),
                         clientIdentifier, this.remoteAddress);
            }

        });

    }

    /**
     * Close the bridge with all related attached links and connection to AMQP services
     */
    public Future<Void> close() {
        if (this.closed.compareAndSet(false, true)) {
            openedFuture.setHandler(unused -> {
                LOG.info("Closing session for MQTT client : {} at {}", this.mqttEndpoint.clientIdentifier(), this.remoteAddress);
                Future<Void> cleanSessionFuture = Future.future();
                cleanSessionFuture.setHandler(unused1 -> {
                    AsyncResult<Void> result = Future.failedFuture((Throwable) null);
                    try {
                        if (this.lwtEndpoint != null) {
                            this.lwtEndpoint.close(this.detachForced);
                        }
                        if (this.ssEndpoint != null) {
                            this.ssEndpoint.close();
                        }
                        if (this.rcvEndpoint != null) {
                            this.rcvEndpoint.close();
                        }
                        if (this.pubEndpoint != null) {
                            this.pubEndpoint.close();
                        }
                        if (this.connection != null) {
                            this.connection.close();
                        }
                        if (this.grantedQoSLevels != null) {
                            this.grantedQoSLevels.clear();
                        }

                        try {
                            this.mqttEndpoint.close();
                        } catch (IllegalStateException e) {
                        }
                        result = Future.succeededFuture();
                    } catch (Throwable e) {
                        result = Future.failedFuture(e);
                    } finally {
                        this.handleMqttEndpointClose();
                        this.closedFuture.handle(result);
                    }
                });

                if (this.mqttEndpoint.isCleanSession()) {
                    AmqpCloseMessage value = new AmqpCloseMessage(this.mqttEndpoint.clientIdentifier());
                    this.ssEndpoint.sendClose(value, event -> {
                        if (event.failed()) {
                            LOG.warn("Failed to close session for MQTT client : {}",
                                     this.mqttEndpoint.clientIdentifier(),
                                     event.cause());
                            cleanSessionFuture.fail(event.cause());
                        } else {
                            LOG.trace("Closed session for MQTT client : {} at {}", this.mqttEndpoint.clientIdentifier(), this.remoteAddress);
                            cleanSessionFuture.complete();
                        }
                    });
                } else {
                    LOG.trace("Closed session for MQTT client : {} at {}", this.mqttEndpoint.clientIdentifier(), this.remoteAddress);
                    cleanSessionFuture.complete();
                }
            });
        }
        return this.closedFuture;
    }

    /**
     * Handler for incoming MQTT PUBLISH message
     *
     * @param publish   PUBLISH message
     */
    private void publishHandler(MqttPublishMessage publish) {

        final int mqttPacketId = publish.messageId();
        LOG.info("PUBLISH [{}] from MQTT client {}", mqttPacketId, this.mqttEndpoint.clientIdentifier());

        // TODO: simple way, without considering wildcards

        // check if a publisher already exists for the requested topic
        if (!this.pubEndpoint.isPublisher(publish.topicName())) {

            // create two sender for publishing QoS 0/1 and QoS 2 messages
            ProtonSender senderQoS01 = this.connection.createSender(publish.topicName());
            ProtonSender senderQoS2 = this.connection.createSender(publish.topicName());

            this.pubEndpoint.addPublisher(publish.topicName(), new AmqpPublisher(senderQoS01, senderQoS2));
        }

        // sending AMQP_PUBLISH
        AmqpPublishMessage amqpPublishMessage =
                new AmqpPublishMessage(null,
                                       publish.qosLevel(),
                                       publish.isDup(),
                                       publish.isRetain(),
                                       publish.topicName(),
                                       publish.payload());

        pubEndpoint.publish(amqpPublishMessage, mqttPacketId, done -> {

            if (done.succeeded()) {

                ProtonDelivery delivery = done.result();
                if (delivery != null) {

                    if (publish.qosLevel() == MqttQoS.AT_LEAST_ONCE) {
                        this.mqttEndpoint.publishAcknowledge(mqttPacketId);
                        LOG.info("PUBACK [{}] to MQTT client {}", mqttPacketId, this.mqttEndpoint.clientIdentifier());
                    } else {

                        this.mqttEndpoint.publishReceived(mqttPacketId);
                        LOG.info("PUBREC [{}] to MQTT client {}", mqttPacketId, this.mqttEndpoint.clientIdentifier());
                    }

                }
            }

        });
    }

    /**
     * Handler for incoming AMQP_PUBLISH message
     *
     * @param amqpPublishData   object with the AMQP_PUBLISH message
     */
    private void publishHandler(AmqpPublishData amqpPublishData) {

        AmqpPublishMessage publish = amqpPublishData.amqpPublishMessage();

        // defensive ... check that current bridge has information about subscriptions and related granted QoS
        // see https://github.com/EnMasseProject/subserv/issues/8

        // try to get subscribed topic (that could have wildcards) that matches the publish topic
        String topic = (this.grantedQoSLevels.size() == 0) ? null :
                TopicMatcher.match(this.grantedQoSLevels.keySet().stream().collect(Collectors.toList()), publish.topic());

        if (topic != null) {

            // MQTT 3.1.1 spec :  The QoS of Payload Messages sent in response to a Subscription MUST be
            // the minimum of the QoS of the originally published message and the maximum QoS granted by the Server
            MqttQoS qos = (publish.qos().value() < this.grantedQoSLevels.get(topic).value()) ?
                    publish.qos() :
                    this.grantedQoSLevels.get(topic);

            this.mqttEndpoint.publish(publish.topic(), publish.payload(), qos, publish.isDup(), publish.isRetain());
            // the the message identifier assigned to the published message
            amqpPublishData.setMessageId(this.mqttEndpoint.lastMessageId());

            LOG.info("PUBLISH [{}] to MQTT client {}", this.mqttEndpoint.lastMessageId(), this.mqttEndpoint.clientIdentifier());

            // for QoS 0, message settled immediately
            if (qos == MqttQoS.AT_MOST_ONCE) {
                this.rcvEndpoint.settle(amqpPublishData.messageId());
            }

        } else {

            LOG.error("Published message : MQTT client {} is not subscribed to {} !!", this.mqttEndpoint.clientIdentifier(), publish.topic());
        }
    }

    /**
     * Handler for incoming AMQP_PUBREL message
     *
     * @param pubrel    AMQP_PUBREL message
     */
    private void pubrelHandler(AmqpPubrelMessage pubrel) {

        this.mqttEndpoint.publishRelease((int) pubrel.messageId());

        LOG.info("PUBREL [{}] to MQTT client {}", pubrel.messageId(), this.mqttEndpoint.clientIdentifier());
    }

    /**
     * Handler for incoming MQTT SUBSCRIBE message
     *
     * @param subscribe SUBSCRIBE message
     */
    private void subscribeHandler(MqttSubscribeMessage subscribe) {

        final int messageId = subscribe.messageId();
        LOG.info("SUBSCRIBE [{}] from MQTT client {}", messageId, this.mqttEndpoint.clientIdentifier());

        // sending AMQP_SUBSCRIBE

        List<AmqpTopicSubscription> topicSubscriptions =
                subscribe.topicSubscriptions().stream().map(topicSubscription -> {
                    return new AmqpTopicSubscription(topicSubscription.topicName(), topicSubscription.qualityOfService());
                }).collect(Collectors.toList());

        AmqpSubscribeMessage amqpSubscribeMessage =
                new AmqpSubscribeMessage(this.mqttEndpoint.clientIdentifier(),
                                         topicSubscriptions);

        this.ssEndpoint.sendSubscribe(amqpSubscribeMessage, done -> {

            if (done.succeeded()) {

                ProtonDelivery delivery = done.result();

                List<MqttQoS> grantedQoSLevels = null;
                if (delivery.getRemoteState() == Accepted.getInstance()) {

                    // QoS levels requested are granted
                    grantedQoSLevels = amqpSubscribeMessage.topicSubscriptions().stream().map(topicSubscription -> {
                        return topicSubscription.qos();
                    }).collect(Collectors.toList());

                    // add accepted topic subscriptions to the local collection
                    amqpSubscribeMessage.topicSubscriptions().stream().forEach(amqpTopicSubscription -> {
                        this.grantedQoSLevels.put(amqpTopicSubscription.topic(), amqpTopicSubscription.qos());
                    });

                } else {

                    // failure for all QoS levels requested
                    grantedQoSLevels = new ArrayList<>(Collections.nCopies(amqpSubscribeMessage.topicSubscriptions().size(), MqttQoS.FAILURE));
                }

                this.mqttEndpoint.subscribeAcknowledge(messageId, grantedQoSLevels);

                LOG.info("SUBACK [{}] to MQTT client {}", messageId, this.mqttEndpoint.clientIdentifier());
            }
        });
    }

    /**
     * Handler for incoming MQTT UNSUBSCRIBE message
     *
     * @param unsubscribe   UNSUBSCRIBE message
     */
    private void unsubscribeHandler(MqttUnsubscribeMessage unsubscribe) {

        final int messageId = unsubscribe.messageId();
        LOG.info("UNSUBSCRIBE [{}] from MQTT client {}", messageId, this.mqttEndpoint.clientIdentifier());

        // sending AMQP_UNSUBSCRIBE

        AmqpUnsubscribeMessage amqpUnsubscribeMessage =
                new AmqpUnsubscribeMessage(this.mqttEndpoint.clientIdentifier(), unsubscribe.topics());

        this.ssEndpoint.sendUnsubscribe(amqpUnsubscribeMessage, done -> {

            if (done.succeeded()) {

                this.mqttEndpoint.unsubscribeAcknowledge(messageId);

                // removing topics from local collection
                unsubscribe.topics().stream().forEach(topic -> {

                    this.grantedQoSLevels.remove(topic);
                });

                LOG.info("UNSUBACK [{}] to MQTT client {}", messageId, this.mqttEndpoint.clientIdentifier());
            }
        });
    }

    /**
     * Handler for incoming MQTT DISCONNECT message
     *
     * @param v
     */
    private void disconnectHandler(Void v) {

        LOG.info("DISCONNECT from MQTT client {}", this.mqttEndpoint.clientIdentifier());
        this.detachForced = false;
    }

    /**
     * Handler for connection closed by remote MQTT client
     *
     * @param v
     */
    private void closeHandler(Void v) {
        LOG.info("Close from MQTT client {} at {}", this.mqttEndpoint.clientIdentifier(), this.remoteAddress);
        close();
    }

    /**
     * Handler for incoming MQTT PUBACK message
     *
     * @param messageId message identifier
     */
    private void pubackHandler(int messageId) {

        LOG.info("PUBACK [{}] from MQTT client {}", messageId, this.mqttEndpoint.clientIdentifier());

        // a PUBLISH message with QoS 1 was sent to remote MQTT client (not settled yet at source)
        // now PUBACK is received so it's time to settle
        this.rcvEndpoint.settle(messageId);
    }

    /**
     * Handler for incoming MQTT PUBREL message
     *
     * @param messageId message identifier
     */
    private void pubrelHandler(int messageId) {

        LOG.info("PUBREL [{}] from MQTT client {}", messageId, this.mqttEndpoint.clientIdentifier());

        // a PUBLISH message with QoS 2 was received from remote MQTT client, PUBREC was already sent
        // as reply, now that PUBREL is coming it's time to settle and reply with PUBCOMP
        this.pubEndpoint.settle(messageId);

        this.mqttEndpoint.publishComplete(messageId);

        LOG.info("PUBCOMP [{}] to MQTT client {}", messageId, this.mqttEndpoint.clientIdentifier());
    }

    /**
     * Handler for incoming MQTT PUBREC message
     *
     * @param messageId message identifier
     */
    private void pubrecHandler(int messageId) {

        LOG.info("PUBREC [{}] from MQTT client {}", messageId, this.mqttEndpoint.clientIdentifier());

        AmqpPubrelMessage amqpPubrelMessage = new AmqpPubrelMessage(messageId);

        this.pubEndpoint.publish(amqpPubrelMessage, done -> {

            if (done.succeeded()) {

                this.rcvEndpoint.settle(messageId);
            }
        });
    }

    /**
     * Handler for incoming MQTT PUBCOMP message
     *
     * @param messageId message identifier
     */
    private void pubcompHandler(int messageId) {

        LOG.info("PUBCOMP [{}] from MQTT client {}", messageId, this.mqttEndpoint.clientIdentifier());

        // a PUBLISH message with QoS 2 was sent to remote MQTT client (not settled yet at source)
        // then PUBREC was received. The corresponding PUBREL was sent (after PUBLISH settlement at source)
        // and now the PUBCOMP was received so it's time to settle
        this.rcvEndpoint.settle(messageId);
    }

    /**
     * Setup handlers for MQTT endpoint
     */
    private void setupMqttEndpoint() {

        this.mqttEndpoint
                .publishHandler(this::publishHandler)
                .publishAcknowledgeHandler(this::pubackHandler)
                .publishReleaseHandler(this::pubrelHandler)
                .publishReceivedHandler(this::pubrecHandler)
                .publishCompletionHandler(this::pubcompHandler)
                .subscribeHandler(this::subscribeHandler)
                .unsubscribeHandler(this::unsubscribeHandler)
                .disconnectHandler(this::disconnectHandler)
                .closeHandler(this::closeHandler);
    }

    /**
     * Setup all AMQP endpoints
     */
    private void setupAmqpEndpoits() {

        // NOTE : Last Will and Testament Service endpoint is opened only if MQTT client provides will information
        //        The receiver on the unique client publish address will be opened only after
        //        connection is established (and CONNACK sent to the MQTT client)

        // setup and open AMQP endpoint for receiving on unique client control/publish addresses
        ProtonReceiver receiverControl = this.connection.createReceiver(String.format(AmqpReceiverEndpoint.CLIENT_CONTROL_ENDPOINT_TEMPLATE, this.mqttEndpoint.clientIdentifier()));
        ProtonReceiver receiverPublish = this.connection.createReceiver(String.format(AmqpReceiverEndpoint.CLIENT_PUBLISH_ENDPOINT_TEMPLATE, this.mqttEndpoint.clientIdentifier()));
        this.rcvEndpoint = new AmqpReceiverEndpoint(new AmqpReceiver(receiverControl, receiverPublish));

        // setup and open AMQP endpoint to Subscription Service
        ProtonSender ssSender = this.connection.createSender(AmqpSubscriptionServiceEndpoint.SUBSCRIPTION_SERVICE_ENDPOINT);
        this.ssEndpoint = new AmqpSubscriptionServiceEndpoint(ssSender);

        // setup and open AMQP endpoint for publishing
        ProtonSender senderPubrel = this.connection.createSender(String.format(AmqpPublishEndpoint.AMQP_CLIENT_PUBREL_ENDPOINT_TEMPLATE, this.mqttEndpoint.clientIdentifier()));
        this.pubEndpoint = new AmqpPublishEndpoint(senderPubrel);

        this.rcvEndpoint.openControl();
        this.ssEndpoint.open();
        this.pubEndpoint.open();
    }

    /**
     * Set the session handler called when MQTT client closes connection
     *
     * @param handler   the handler
     * @return  the current AmqpBridge instance
     */
    public AmqpBridge mqttEndpointCloseHandler(Handler<AmqpBridge> handler) {

        this.mqttEndpointCloseHandler = handler;
        return this;
    }

    /**
     * Used for calling the close handler when MQTT client closes connection
     *
     */
    private void handleMqttEndpointClose() {

        if (this.mqttEndpointCloseHandler != null) {
            this.mqttEndpointCloseHandler.handle(this);
        }
    }

    /**
     * Handle connection closed with remote AMQP container
     *
     * @param connection    current ProtonConnection instance
     * @param result    result of remote connection closing
     */
    private void handleRemoteConnectionClose(ProtonConnection connection, AsyncResult<ProtonConnection> result) {

        // NOTE : the connection parameter is needed because Vert.x doesn't provide the ProtonConnection
        //        instance when the operation ends with errors (so exception). We need the instance for closing.
        if (result.succeeded()) {
            LOG.info("AMQP connection closed with {}", connection.getRemoteContainer());
        } else {
            LOG.info("AMQP connection closed with {} with error", connection.getRemoteContainer(), result.cause());
        }
        connection.close();

        try {
            this.mqttEndpoint.close();
        } catch (IllegalStateException e) {
            LOG.warn("MQTT endpoint for client {} already closed", this.mqttEndpoint.clientIdentifier());
        }
    }

    /**
     * Handler disconnection with remote AMQP container
     *
     * @param connection    current ProtonConnection instance
     */
    private void handleRemoteDisconnect(ProtonConnection connection) {

        LOG.info("AMQP disconnection with {}", connection.getRemoteContainer());
        connection.disconnect();

        try {
            this.mqttEndpoint.close();
        } catch (IllegalStateException e) {
            LOG.warn("MQTT endpoint for client {} already closed", this.mqttEndpoint.clientIdentifier());
        }
    }

    /**
     * AMQP bridge identifier
     *
     * @return
     */
    public String id() {
        // just the MQTT client identifier
        return this.mqttEndpoint.clientIdentifier();
    }

    public SocketAddress remoteAddress()
    {
        return remoteAddress;
    }
}
