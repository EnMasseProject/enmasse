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
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
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

        this.client.connect(address, port, ar -> {

            if (ar.succeeded()) {

                ProtonConnection connection = ar.result();

                // TODO: setup AMQP endpoints
                ProtonSender wsSender = connection.createSender(AmqpWillServiceEndpoint.WILL_SERVICE_ENDPOINT);
                this.wsEndpoint = new AmqpWillServiceEndpoint(wsSender);

                ProtonSender ssSender = connection.createSender(AmqpSubscriptionServiceEndpoint.SUBSCRIPTION_SERVICE_ENDPOINT);
                ProtonReceiver ssReceiver = connection.createReceiver(String.format(AmqpSubscriptionServiceEndpoint.CLIENT_ENDPOINT_TEMPLATE, this.mqttEndpoint.clientIdentifier()));
                this.ssEndpoint = new AmqpSubscriptionServiceEndpoint(ssSender, ssReceiver);

                this.setupMqttEndpointHandlers();

                this.wsEndpoint.open();
                this.ssEndpoint.open();
            } else {

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
                .disconnectHandler(null);
    }
}
