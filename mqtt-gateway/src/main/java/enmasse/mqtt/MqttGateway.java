/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;


/**
 * Vert.x based MQTT gateway for EnMasse
 */
public class MqttGateway extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttGateway.class);
    private final MqttGatewayOptions options;

    private MqttServer server;

    private final Map<String, AmqpBridge> bridges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Semaphore> clientIdSemaphores = new ConcurrentHashMap<>();

    public MqttGateway(MqttGatewayOptions options) {

        this.options = options;
    }


    /**
     * Start the MQTT server component
     *
     * @param startFuture
     */
    private void bindMqttServer(Future<Void> startFuture) {

        MqttServerOptions options = new MqttServerOptions();
        options.setMaxMessageSize(this.options.getMaxMessageSize());
        options.setHost(this.options.getBindAddress()).setPort(this.options.getListenPort());
        options.setAutoClientId(true);

        if (this.options.isSsl()) {

            PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                    .setKeyPath(this.options.getKeyFile())
                    .setCertPath(this.options.getCertFile());

            options.setKeyCertOptions(pemKeyCertOptions)
                    .setSsl(this.options.isSsl());

            LOG.info("SSL/TLS support enabled key {} cert {}", this.options.getKeyFile(), this.options.getCertFile());
        }

        this.server = MqttServer.create(this.vertx, options);

        this.server
                .endpointHandler(this::handleMqttEndpointConnection)
                .exceptionHandler(t -> {LOG.error("Error handling connection ", t);})
                .listen(done -> {

                    if (done.succeeded()) {

                        LOG.info("MQTT gateway running on {}:{}", this.options.getBindAddress(), this.server.actualPort());
                        LOG.info("AMQP messaging service on {}:{}", this.options.getMessagingServiceHost(), this.options.getMessagingServicePort());
                        startFuture.complete();
                    } else {
                        LOG.error("Error while starting up MQTT gateway", done.cause());
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

        final String clientIdentifier = mqttEndpoint.clientIdentifier();
        final SocketAddress remoteAddress = mqttEndpoint.remoteAddress();
        LOG.info("CONNECT from MQTT client {} at {}", clientIdentifier, remoteAddress);

        Semaphore clientIdSemaphore = clientIdSemaphores.computeIfAbsent(clientIdentifier,
                                                                         s -> new Semaphore(1));

        if (clientIdSemaphore.tryAcquire()) {
            AmqpBridge bridge = new AmqpBridge(this.vertx, mqttEndpoint);

            bridge.mqttEndpointCloseHandler(amqpBridge -> {

                try {
                    this.bridges.remove(amqpBridge.id());
                } finally {
                    clientIdSemaphores.remove(clientIdentifier, clientIdSemaphore);
                }
            }).open(this.options.getMessagingServiceHost(), this.options.getMessagingServicePort(), done -> {
                if (done.succeeded()) {
                    AmqpBridge newBridge = done.result();
                    this.bridges.put(newBridge.id(), newBridge);
                } else {
                    LOG.info("Error opening the AMQP bridge ...", done.cause());
                    clientIdSemaphores.remove(clientIdentifier, clientIdSemaphore);
                }
            });
        } else {
            AmqpBridge existingBridge = bridges.get(clientIdentifier);
            if (existingBridge == null) {
                // The semaphore is held but no bridge is present; another session must either be in the process of
                // forming a bridge or removing one that is closing.
                LOG.trace("No existing bridge found for {}", clientIdentifier);
                vertx.setTimer(100, unused -> {
                    handleMqttEndpointConnection(mqttEndpoint);
                });
            } else {
                // If the ClientId represents a Client already connected to the Server then the Server MUST
                // disconnect the existing Client [MQTT-3.1.4-2].
                LOG.info("MQTT client {} already in-use by {}", clientIdentifier, existingBridge.remoteAddress());
                existingBridge.close().compose(unused -> {
                    LOG.trace("MQTT Closing of existing client {} from {} completed (initiated by {})",
                              clientIdentifier, existingBridge.remoteAddress(), remoteAddress);
                    handleMqttEndpointConnection(mqttEndpoint);
                    return Future.succeededFuture();
                });
            }
        }
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        LOG.info("Starting MQTT gateway verticle...");
        this.bindMqttServer(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        LOG.info("Stopping MQTT gateway verticle ...");

        Future<Void> shutdownTracker = Future.future();
        shutdownTracker.setHandler(done -> {
           if (done.succeeded()) {
               LOG.info("MQTT gateway has been shut down successfully");
               stopFuture.complete();
           } else {
               LOG.info("Error while shutting down MQTT gateway", done.cause());
               stopFuture.fail(done.cause());
           }
        });

        if (this.server != null) {
            @SuppressWarnings("rawtypes")
            List<Future> closeFutures = this.bridges.entrySet()
                                                    .stream()
                                                    .map(entry -> entry.getValue().close())
                                                    .collect(Collectors.toList());

            CompositeFuture.all(closeFutures).setHandler(done -> {
                this.server.close(shutdownTracker);
            });
        } else {
            shutdownTracker.complete();
        }
    }
}
