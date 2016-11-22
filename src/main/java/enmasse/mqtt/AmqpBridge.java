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

import enmasse.mqtt.endpoints.AmqpPublishEndpoint;
import enmasse.mqtt.endpoints.AmqpReceiverEndpoint;
import enmasse.mqtt.endpoints.AmqpSubscriptionServiceEndpoint;
import enmasse.mqtt.endpoints.AmqpWillServiceEndpoint;
import enmasse.mqtt.messages.AmqpPublishMessage;
import enmasse.mqtt.messages.AmqpQos;
import enmasse.mqtt.messages.AmqpSessionMessage;
import enmasse.mqtt.messages.AmqpSubackMessage;
import enmasse.mqtt.messages.AmqpSubscribeMessage;
import enmasse.mqtt.messages.AmqpTopicSubscription;
import enmasse.mqtt.messages.AmqpUnsubackMessage;
import enmasse.mqtt.messages.AmqpUnsubscribeMessage;
import enmasse.mqtt.messages.AmqpWillClearMessage;
import enmasse.mqtt.messages.AmqpWillMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttWill;
import io.vertx.mqtt.messages.MqttPublishMessage;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import io.vertx.mqtt.messages.MqttUnsubscribeMessage;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AMQP bridging class from/to the MQTT endpoint to/from the AMQP related endpoints
 */
public class AmqpBridge {

    private static final int AMQP_SERVICES_CONNECTION_TIMEOUT = 5000; // in ms

    private static final Logger LOG = LoggerFactory.getLogger(AmqpBridge.class);

    private Vertx vertx;

    private ProtonClient client;
    private ProtonConnection connection;

    // local endpoint for handling remote connected MQTT client
    private MqttEndpoint mqttEndpoint;

    // endpoint for handling communication with Will Service (WS)
    private AmqpWillServiceEndpoint wsEndpoint;
    // endpoint for handling communication with Subscription Service (SS)
    private AmqpSubscriptionServiceEndpoint ssEndpoint;
    // endpoint for handling incoming messages on the unique client address
    private AmqpReceiverEndpoint rcvEndpoint;
    // endpoints for publishing message on topic (via AMQP)
    private Map<String, AmqpPublishEndpoint> pubEndpoints;

    /**
     * Constructor
     *
     * @param vertx Vert.x instance
     * @param mqttEndpoint  MQTT local endpoint
     */
    public AmqpBridge(Vertx vertx, MqttEndpoint mqttEndpoint) {
        this.vertx = vertx;
        this.mqttEndpoint = mqttEndpoint;
        this.pubEndpoints = new HashMap<>();
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

        this.client.connect(address, port, done -> {

            if (done.succeeded()) {

                this.connection = done.result();
                this.connection.open();

                // specified link name for the Will Service as MQTT clientid
                ProtonLinkOptions options = new ProtonLinkOptions();
                options.setLinkName(this.mqttEndpoint.clientIdentifier());

                // setup and open AMQP endpoint for receiving on unique client address
                ProtonReceiver rcvReceiver = this.connection.createReceiver(String.format(AmqpReceiverEndpoint.CLIENT_ENDPOINT_TEMPLATE, this.mqttEndpoint.clientIdentifier()));
                this.rcvEndpoint = new AmqpReceiverEndpoint(rcvReceiver);

                // setup and open AMQP endpoints to Will and Subscription services
                ProtonSender wsSender = this.connection.createSender(AmqpWillServiceEndpoint.WILL_SERVICE_ENDPOINT, options);
                this.wsEndpoint = new AmqpWillServiceEndpoint(wsSender);

                ProtonSender ssSender = this.connection.createSender(AmqpSubscriptionServiceEndpoint.SUBSCRIPTION_SERVICE_ENDPOINT);
                this.ssEndpoint = new AmqpSubscriptionServiceEndpoint(ssSender);

                this.setupMqttEndpointHandlers();

                this.rcvEndpoint.open();
                this.wsEndpoint.open();
                this.ssEndpoint.open();

                // setup a Future for completed connection steps with all services
                // with AMQP_WILL and AMQP_SESSION/AMQP_SESSION_PRESENT handled
                Future<Void> connectionFuture = Future.future();
                connectionFuture.setHandler(ar -> {

                    if (ar.succeeded()) {

                        this.mqttEndpoint.writeConnack(MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
                        LOG.info("Connection accepted");

                        openHandler.handle(Future.succeededFuture(AmqpBridge.this));

                    } else {

                        this.mqttEndpoint.writeConnack(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, false);
                        LOG.info("Connection NOT accepted");

                        openHandler.handle(Future.failedFuture(ar.cause()));
                    }

                    LOG.info("CONNACK sent");
                });

                // step 1 : send AMQP_WILL to Will Service
                Future<ProtonDelivery> willFuture = Future.future();
                // if remote MQTT has specified the will
                if (this.mqttEndpoint.will().isWillFlag()) {

                    // sending AMQP_WILL
                    MqttWill will = this.mqttEndpoint.will();

                    AmqpWillMessage amqpWillMessage =
                            new AmqpWillMessage(will.isWillRetain(),
                                    will.willTopic(),
                                    AmqpQos.toAmqpQoS(will.willQos()),
                                    Buffer.buffer(will.willMessage()));

                    this.wsEndpoint.sendWill(amqpWillMessage, willFuture.completer());
                } else {

                    // otherwise just complete the Future
                    willFuture.complete();
                }

                willFuture.compose(v -> {

                    // handling AMQP_SESSION_PRESENT reply from Subscription Service
                    this.rcvEndpoint.sessionHandler(amqpSessionPresentMessage -> {

                        LOG.info("session present {}", amqpSessionPresentMessage.isSessionPresent());

                        this.rcvEndpoint.subackHandler(this::subackHandler);
                        this.rcvEndpoint.unsubackHandler(this::unsubackHandler);

                        connectionFuture.complete();
                    });

                    // step 2 : send AMQP_SESSION to Subscription Service
                    Future<ProtonDelivery> cleanSessionFuture = Future.future();

                    // sending AMQP_SESSION
                    AmqpSessionMessage amqpSessionMessage =
                            new AmqpSessionMessage(this.mqttEndpoint.isCleanSession(),
                                    this.mqttEndpoint.clientIdentifier());

                    this.ssEndpoint.sendCleanSession(amqpSessionMessage, cleanSessionFuture.completer());
                    return cleanSessionFuture;

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

                LOG.info("Error connecting to AMQP services ...", done.cause());
                // no connection with the AMQP side
                this.mqttEndpoint.writeConnack(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, false);

                openHandler.handle(Future.failedFuture(done.cause()));

                LOG.info("CONNACK sent");
            }

        });

    }

    /**
     * Close the bridge with all related attached links and connection to AMQP services
     */
    public void close() {

        this.wsEndpoint.close();
        this.ssEndpoint.close();

        for (Map.Entry<String, AmqpPublishEndpoint> entry: this.pubEndpoints.entrySet()) {
            entry.getValue().close();
        }

        this.connection.close();
    }

    /**
     * Handler for incoming MQTT PUBLISH messages
     * @param publish   PUBLISH message
     */
    private void publishHandler(MqttPublishMessage publish) {

        // TODO:

        // TODO: simple way, without considering wildcards

        AmqpPublishEndpoint pubEndpoint = null;

        // check if publish endpoint already exists for the requested topic
        if (!this.pubEndpoints.containsKey(publish.topicName())) {

            ProtonSender sender = this.connection.createSender(publish.topicName());
            pubEndpoint = new AmqpPublishEndpoint(sender);
            this.pubEndpoints.put(publish.topicName(), pubEndpoint);
        }

        // TODO: sending AMQP_PUBLISH
        AmqpPublishMessage amqpPublishMessage =
                new AmqpPublishMessage(publish.messageId(),
                        AmqpQos.toAmqpQoS(publish.qosLevel().value()),
                        publish.isDup(),
                        publish.isRetain(),
                        publish.topicName(),
                        publish.payload());

        pubEndpoint.publish(amqpPublishMessage);
    }

    /**
     * Handler for incoming MQTT SUBSCRIBE messages
     * @param subscribe SUBSCRIBE message
     */
    private void subscribeHandler(MqttSubscribeMessage subscribe) {

        // TODO: sending AMQP_SUBSCRIBE

        List<AmqpTopicSubscription> topicSubscriptions =
                subscribe.topicSubscriptions().stream().map(topicSubscription -> {
                    return new AmqpTopicSubscription(topicSubscription.topicName(), AmqpQos.toAmqpQoS(topicSubscription.qualityOfService().value()));
                }).collect(Collectors.toList());

        AmqpSubscribeMessage amqpSubscribeMessage =
                new AmqpSubscribeMessage(this.mqttEndpoint.clientIdentifier(),
                        subscribe.messageId(),
                        topicSubscriptions);

        this.ssEndpoint.sendSubscribe(amqpSubscribeMessage);
    }

    /**
     * Handler for incoming MQTT UNSUBSCRIBE messages
     * @param unsubscribe   UNSUBSCRIBE message
     */
    private void unsubscribeHandler(MqttUnsubscribeMessage unsubscribe) {

        // TODO: sending AMQP_UNSUBSCRIBE

        AmqpUnsubscribeMessage amqpUnsubscribeMessage =
                new AmqpUnsubscribeMessage(this.mqttEndpoint.clientIdentifier(),
                        unsubscribe.messageId(),
                        unsubscribe.topics());

        this.ssEndpoint.sendUnsubscribe(amqpUnsubscribeMessage);
    }

    /**
     * Handler for incoming MQTT DISCONNECT message
     * @param v
     */
    private void disconnectHandler(Void v) {

        // sending AMQP_WILL_CLEAR
        AmqpWillClearMessage amqpWillClearMessage = new AmqpWillClearMessage();
        this.wsEndpoint.clearWill(amqpWillClearMessage, ar -> {

            this.wsEndpoint.close();
        });
    }

    /**
     * Handler for handling connection closed by remote MQTT client
     * @param v
     */
    private void closeHandler(Void v) {

        // TODO:
        this.wsEndpoint.close();
    }

    /**
     * Handler for handling AMQP_SUBACK message received by Subscription Service
     * @param suback    AMQP_SUBACK message
     */
    private void subackHandler(AmqpSubackMessage suback) {
        // TODO:

        List<Integer> grantedQoSLevels = suback.grantedQoSLevels().stream().map(qos -> { return qos.toMqttQos(); }).collect(Collectors.toList());
        this.mqttEndpoint.writeSuback((int)suback.messageId(), grantedQoSLevels);
    }

    /**
     * Handler for handling AMQP_UNSUBACK message received by Subscription Service
     * @param unsuback  AMQP_UNSUBACK message
     */
    private void unsubackHandler(AmqpUnsubackMessage unsuback) {
        // TODO:

        this.mqttEndpoint.writeUnsuback((int)unsuback.messageId());
    }

    /**
     * Setup handlers for MQTT endpoint
     */
    private void setupMqttEndpointHandlers() {

        this.mqttEndpoint
                .publishHandler(this::publishHandler)
                .subscribeHandler(this::subscribeHandler)
                .unsubscribeHandler(this::unsubscribeHandler)
                .disconnectHandler(this::disconnectHandler)
                .closeHandler(this::closeHandler);
    }
}
