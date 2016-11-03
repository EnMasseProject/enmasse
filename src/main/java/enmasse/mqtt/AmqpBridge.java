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

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;

/**
 * AMQP bridging class from/to the MQTT endpoint to/from the AMQP related endpoints
 */
public class AmqpBridge {

    private Vertx vertx;

    private ProtonClient client;

    // local endpoint for handling remote connected MQTT client
    private MqttEndpoint mqttEndpoint;

    // endpoint for handling communication with Will Service (WS)
    private WillServiceEndpoint wsEndpoint;
    // endpoint for handling communication with Subscription Service (SS)
    private SubscriptionServiceEndpoint ssEndpoint;
    // endpoint for publishing message on topic (via AMQP)
    private PublishEndpoint pubEndpoint;

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

                this.setupMqttEndpointHandlers();

            } else {

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
