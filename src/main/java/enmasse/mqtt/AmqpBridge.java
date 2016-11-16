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
import enmasse.mqtt.endpoints.AmqpSubscriptionServiceEndpoint;
import enmasse.mqtt.endpoints.AmqpWillServiceEndpoint;
import enmasse.mqtt.messages.AmqpQos;
import enmasse.mqtt.messages.AmqpSessionMessage;
import enmasse.mqtt.messages.AmqpWillClearMessage;
import enmasse.mqtt.messages.AmqpWillMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttWill;
import io.vertx.proton.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AMQP bridging class from/to the MQTT endpoint to/from the AMQP related endpoints
 */
public class AmqpBridge {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpBridge.class);

    private Vertx vertx;

    private ProtonClient client;

    // local endpoint for handling remote connected MQTT client
    private MqttEndpoint mqttEndpoint;

    // endpoint for handling communication with Will Service (WS)
    private AmqpWillServiceEndpoint wsEndpoint;
    // endpoint for handling communication with Subscription Service (SS)
    private AmqpSubscriptionServiceEndpoint ssEndpoint;
    // endpoint for publishing message on topic (via AMQP)
    private AmqpPublishEndpoint pubEndpoint;

    /**
     * Constructor
     *
     * @param vertx Vert.x instance
     * @param mqttEndpoint  MQTT local endpoint
     */
    public AmqpBridge(Vertx vertx, MqttEndpoint mqttEndpoint) {
        this.vertx = vertx;
        this.mqttEndpoint = mqttEndpoint;
    }

    /**
     * Connect to the AMQP service provider
     *
     * @param address   AMQP service provider address
     * @param port      AMQP service provider port
     */
    public void connect(String address, int port) {

        this.client = ProtonClient.create(this.vertx);

        this.client.connect(address, port, done -> {

            if (done.succeeded()) {

                ProtonConnection connection = done.result();
                connection.open();

                // TODO: setup AMQP endpoints
                ProtonSender wsSender = connection.createSender(AmqpWillServiceEndpoint.WILL_SERVICE_ENDPOINT);
                this.wsEndpoint = new AmqpWillServiceEndpoint(wsSender);

                ProtonSender ssSender = connection.createSender(AmqpSubscriptionServiceEndpoint.SUBSCRIPTION_SERVICE_ENDPOINT);
                ProtonReceiver ssReceiver = connection.createReceiver(String.format(AmqpSubscriptionServiceEndpoint.CLIENT_ENDPOINT_TEMPLATE, this.mqttEndpoint.clientIdentifier()));
                this.ssEndpoint = new AmqpSubscriptionServiceEndpoint(ssSender, ssReceiver);

                this.setupMqttEndpointHandlers();

                this.wsEndpoint.open();
                this.ssEndpoint.open();

                // TODO: sending AMQP_WILL
                MqttWill will = this.mqttEndpoint.will();

                AmqpWillMessage amqpWillMessage =
                        new AmqpWillMessage(will.isWillRetain(),
                                will.willTopic(),
                                AmqpQos.toAmqpQoS(will.willQos()),
                                Buffer.buffer(will.willMessage()));

                // TODO: sending AMQP_SESSION
                AmqpSessionMessage amqpSessionMessage =
                        new AmqpSessionMessage(this.mqttEndpoint.isCleanSession(),
                                this.mqttEndpoint.clientIdentifier());

                // setup a Future for completed connection steps with all services
                // with AMQP_WILL and AMQP_SESSION/AMQP_SESSION_PRESENT handled
                Future<Void> connectionFuture = Future.future();
                connectionFuture.setHandler(ar -> {

                    if (ar.succeeded()) {

                        this.mqttEndpoint.writeConnack(MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
                        LOG.info("Connection accepted");
                    } else {

                        this.mqttEndpoint.writeConnack(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, false);
                        LOG.info("Connection NOT accepted");
                    }

                    LOG.info("CONNACK sent");
                });

                // step 1 : send AMQP_WILL to Will Service
                Future<ProtonDelivery> willFuture = Future.future();
                this.wsEndpoint.sendWill(amqpWillMessage, willFuture.completer());

                willFuture.compose(v -> {

                    // handling AMQP_SESSION_PRESENT reply from Subscription Service
                    this.ssEndpoint.sessionHandler(amqpSessionPresentMessage -> {

                        LOG.info("session present {}", amqpSessionPresentMessage.isSessionPresent());
                        connectionFuture.complete();
                    });

                    // step 2 : send AMQP_SESSION to Subscription Service
                    Future<ProtonDelivery> cleanSessionFuture = Future.future();
                    this.ssEndpoint.sendCleanSession(amqpSessionMessage, cleanSessionFuture.completer());
                    return cleanSessionFuture;

                }).compose(v -> {
                    // nothing here !??
                }, connectionFuture);

                vertx.setTimer(5000, timer -> {
                   if (!connectionFuture.isComplete()) {
                       connectionFuture.fail("timeout");
                   }
                });

            } else {

                LOG.info("Error connecting to AMQP services ...", done.cause());
                // no connection with the AMQP side
                this.mqttEndpoint.writeConnack(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE, false);
            }

        });

    }

    /**
     * Setup handlers for MQTT endpoint
     */
    private void setupMqttEndpointHandlers() {

        this.mqttEndpoint
                .publishHandler(null)
                .subscribeHandler(null)
                .unsubscribeHandler(null)
                .disconnectHandler(v -> {

                    AmqpWillClearMessage amqpWillClearMessage = new AmqpWillClearMessage();
                    this.wsEndpoint.clearWill(amqpWillClearMessage, ar -> {

                        this.wsEndpoint.close();
                    });

                })
                .closeHandler(v -> {

                    this.wsEndpoint.close();
                });
    }
}
