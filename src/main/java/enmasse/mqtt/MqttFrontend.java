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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * Vert.x based MQTT Frontend for EnMasse
 */
@Component
public class MqttFrontend extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttFrontend.class);

    private String bindAddress;
    private int listenPort;
    private String connectAddress;
    private int connectPort;

    private MqttServer server;

    private List<AmqpBridge> bridges;

    /**
     * Set the IP address the MQTT Frontend will bind to
     *
     * @param bindAddress   the IP address
     * @return  current MQTT Frontend instance
     */
    @Value(value = "${enmasse.mqtt.bindaddress:0.0.0.0}")
    public MqttFrontend setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
        return this;
    }

    /**
     * Set the port the MQTT Frontend will listen on for MQTT connections.
     *
     * @param listePort the port to listen on
     * @return  current MQTT Frontend instance
     */
    @Value(value = "${enmasse.mqtt.listenport:1883}")
    public MqttFrontend setListenPort(int listePort) {
        this.listenPort = listePort;
        return this;
    }

    /**
     * Set the address for connecting to the AMQP services
     *
     * @param connectAddress    address for AMQP connections
     * @return  current MQTT Frontend instance
     */
    @Value(value = "${enmasse.mqtt.connectaddress:0.0.0.0}")
    public MqttFrontend setConnectAddress(String connectAddress) {
        this.connectAddress = connectAddress;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP services
     *
     * @param connectPort   port for AMQP connections
     * @return  current MQTT Frontend instance
     */
    @Value(value = "${enmasse.mqtt.connectport:5672}")
    public MqttFrontend setConnectPort(int connectPort) {
        this.connectPort = connectPort;
        return this;
    }

    /**
     * Start the MQTT server component
     *
     * @param startFuture
     */
    private void bindMqttServer(Future<Void> startFuture) {

        MqttServerOptions options = new MqttServerOptions();
        options.setHost(this.bindAddress).setPort(this.listenPort);

        this.server = MqttServer.create(this.vertx, options);

        this.server
                .endpointHandler(this::handleMqttEndpointConnection)
                .listen(done -> {

                    if (done.succeeded()) {

                        this.bridges = new ArrayList<>();

                        LOG.info("MQTT frontend running on {}:{}", this.bindAddress, this.server.actualPort());
                        startFuture.complete();
                    } else {
                        LOG.error("Error while starting up MQTT frontend", done.cause());
                        startFuture.fail(done.cause());
                    }

                });
    }

    /**
     * Handler for a connection request (CONNECT) received by a remote MQTT client
     *
     * @param mqttEndpoint  MQTT local endpoint
     */
    private void handleMqttEndpointConnection(MqttEndpoint mqttEndpoint) {

        LOG.info("Connection from {}", mqttEndpoint.clientIdentifier());

        AmqpBridge bridge = new AmqpBridge(this.vertx, mqttEndpoint);

        bridge.open(this.connectAddress, this.connectPort, done -> {

            if (done.succeeded()) {
                this.bridges.add(done.result());
            } else {
                LOG.info("Error opening the AMQP bridge ...", done.cause());
            }
        });
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        LOG.info("Starting MQTT frontend verticle...");
        this.bindMqttServer(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        LOG.info("Stopping MQTT frontend verticle ...");

        Future<Void> shutdownTracker = Future.future();
        shutdownTracker.setHandler(done -> {
           if (done.succeeded()) {
               LOG.info("MQTT frontend has been shut down successfully");
               stopFuture.complete();
           } else {
               LOG.info("Error while shutting down MQTT frontend", done.cause());
               stopFuture.fail(done.cause());
           }
        });

        if (this.server != null) {

            this.bridges.stream().forEach(bridge -> {
                bridge.close();
            });

            this.server.close(shutdownTracker.completer());
        } else {
            shutdownTracker.complete();
        }
    }
}
