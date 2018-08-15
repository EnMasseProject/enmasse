/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import com.google.common.cache.CacheBuilder;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Vert.x based MQTT gateway for EnMasse
 */
@Component
public class MqttGateway extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttGateway.class);

    // binding info for listening
    private String bindAddress;
    private int listenPort;
    // mqtt server options
    private int maxMessageSize;
    // connection info to the messaging service
    private String messagingServiceHost;
    private int messagingServicePort;

    // SSL/TLS support stuff
    private boolean ssl;
    private String certFile;
    private String keyFile;

    private MqttServer server;

    private final Map<String, AmqpBridge> bridges = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Semaphore> clientIdSemaphores = CacheBuilder.newBuilder()
                                                                                    .maximumSize(1000)
                                                                                    .expireAfterAccess(10, TimeUnit.MINUTES)
                                                                                    .<String, Semaphore>build().asMap();

    /**
     * Set the IP address the MQTT gateway will bind to
     *
     * @param bindAddress   the IP address
     * @return  current MQTT gateway instance
     */
    @Value(value = "${enmasse.mqtt.bindaddress:0.0.0.0}")
    public MqttGateway setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
        return this;
    }

    /**
     * Set the port the MQTT gateway will listen on for MQTT connections.
     *
     * @param listePort the port to listen on
     * @return  current MQTT gateway instance
     */
    @Value(value = "${enmasse.mqtt.listenport:1883}")
    public MqttGateway setListenPort(int listePort) {
        this.listenPort = listePort;
        return this;
    }

    /**
     * Set max message size for MQTT Gateway
     *
     * @param maxMessageSize   max message size for MQTT messages
     * @return  current MQTT gateway instance
     */
    @Value(value = "${enmasse.mqtt.maxmessagesize:131072}")
    public MqttGateway setMaxMessageSize(int maxMessageSize) {
        this.maxMessageSize = maxMessageSize;
        return this;
    }

    /**
     * Set the address for connecting to the AMQP services
     *
     * @param messagingServiceHost    address for AMQP connections
     * @return  current MQTT gateway instance
     */
    @Value(value = "${messaging.service.host:0.0.0.0}")
    public MqttGateway setMessagingServiceHost(String messagingServiceHost) {
        this.messagingServiceHost = messagingServiceHost;
        return this;
    }

    /**
     * Set the port for connecting to the AMQP services
     *
     * @param messagingServicePort   port for AMQP connections
     * @return  current MQTT gateway instance
     */
    @Value(value = "${messaging.service.port:5672}")
    public MqttGateway setMessagingServicePort(int messagingServicePort) {
        this.messagingServicePort = messagingServicePort;
        return this;
    }

    /**
     * Set the SSL/TLS support needed for the MQTT connections
     *
     * @param ssl   SSL/TLS is needed
     * @return  current MQTT gateway instance
     */
    @Value(value = "${enmasse.mqtt.ssl:false}")
    public MqttGateway setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    /**
     * Set the server certificate file path for SSL/TLS support
     *
     * @param certFile  server certificate file path
     * @return  current MQTT gateway instance
     */
    @Value(value = "${enmasse.mqtt.certfile:./src/test/resources/tls/server-cert.pem}")
    public MqttGateway setCertFile(String certFile) {
        this.certFile = certFile;
        return this;
    }

    /**
     * Set the server private key file path for SSL/TLS support
     *
     * @param keyFile   server private key file path
     * @return  current MQTT gateway instance
     */
    @Value(value = "${enmasse.mqtt.keyfile:./src/test/resources/tls/server-key.pem}")
    public MqttGateway setKeyFile(String keyFile) {
        this.keyFile = keyFile;
        return this;
    }

    /**
     * Start the MQTT server component
     *
     * @param startFuture
     */
    private void bindMqttServer(Future<Void> startFuture) {

        MqttServerOptions options = new MqttServerOptions();
        options.setMaxMessageSize(this.maxMessageSize);
        options.setHost(this.bindAddress).setPort(this.listenPort);
        options.setAutoClientId(true);

        if (this.ssl) {

            PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions()
                    .setKeyPath(this.keyFile)
                    .setCertPath(this.certFile);

            options.setKeyCertOptions(pemKeyCertOptions)
                    .setSsl(this.ssl);

            LOG.info("SSL/TLS support enabled key {} cert {}", this.keyFile, this.certFile);
        }

        this.server = MqttServer.create(this.vertx, options);

        this.server
                .endpointHandler(this::handleMqttEndpointConnection)
                .exceptionHandler(t -> {LOG.error("Error handling connection ", t);})
                .listen(done -> {

                    if (done.succeeded()) {

                        LOG.info("MQTT gateway running on {}:{}", this.bindAddress, this.server.actualPort());
                        LOG.info("AMQP messaging service on {}:{}", this.messagingServiceHost, this.messagingServicePort);
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
                    clientIdSemaphore.release();
                }
            }).open(this.messagingServiceHost, this.messagingServicePort, done -> {
                if (done.succeeded()) {
                    this.bridges.put(done.result().id(), done.result());
                } else {
                    LOG.info("Error opening the AMQP bridge ...", done.cause());
                }
            });
        } else {
            AmqpBridge existingBridge = bridges.get(clientIdentifier);
            if (existingBridge == null) {
                // The semaphore is held but no bridge is formed, another session must be in the process of forming.
                LOG.trace("No existing bridge found for {}", clientIdentifier);
                vertx.setTimer(100, unused -> {
                    handleMqttEndpointConnection(mqttEndpoint);
                });
            } else {
                // If the ClientId represents a Client already connected to the Server then the Server MUST
                // disconnect the existing Client [MQTT-3.1.4-2].
                LOG.info("MQTT client {} already in-use by {}", clientIdentifier, existingBridge.remoteAddress());
                existingBridge.close().compose(unused -> {
                    LOG.trace("MQTT Closing of existing client {} from {} completed (initated by {})",
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
            List<Future> closeFutures = this.bridges.entrySet()
                                                    .stream()
                                                    .map(entry -> entry.getValue().close())
                                                    .collect(Collectors.toList());

            CompositeFuture.all(closeFutures).setHandler(done -> {
                this.server.close(shutdownTracker.completer());
            });
        } else {
            shutdownTracker.complete();
        }
    }
}
