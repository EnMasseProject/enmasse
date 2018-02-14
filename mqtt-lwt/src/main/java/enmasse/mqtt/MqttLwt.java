/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.mqtt;

import enmasse.mqtt.endpoints.AmqpLwtEndpoint;
import enmasse.mqtt.endpoints.AmqpPublishEndpoint;
import enmasse.mqtt.messages.AmqpPublishMessage;
import enmasse.mqtt.messages.AmqpWillMessage;
import enmasse.mqtt.storage.LwtStorage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * Vert.x based MQTT Last Will and Testament service for EnMasse
 */
@Component
public class MqttLwt extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttLwt.class);

    private static final String CONTAINER_ID = "lwt-service";

    private static final int MAX_MESSAGE_ID = 65535;

    private String certDir;

    // connection info to the messaging service
    private String host;
    private int normalPort;
    private int routeContainerPort;

    private ProtonClient client;

    private AmqpLwtEndpoint lwtEndpoint;
    private LwtStorage lwtStorage;
    private AmqpPublishEndpoint publishEndpoint;

    // counter for the message identifier
    private int messageIdCounter;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        LOG.info("Starting MQTT LWT service verticle...");
        this.connect(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {

        this.lwtEndpoint.close();
        this.publishEndpoint.close();
        this.lwtStorage.close();
        LOG.info("Stopping MQTT LWT service verticle...");
        stopFuture.complete();
    }

    /**
     * Connect to the AMQP messaging network
     *
     * @param startFuture
     */
    private void connect(Future<Void> startFuture) {

        this.client = ProtonClient.create(this.vertx);

        ProtonClientOptions options = this.createClientOptions();


        Future<ProtonConnection> lwtConnFuture = Future.future();

        // connecting to the messaging service internal (router network)
        this.client.connect(options, this.host, this.routeContainerPort, done -> {

            if (done.succeeded()) {

                ProtonConnection connection = done.result();
                connection.setContainer(CONTAINER_ID);

                // TODO
                this.lwtEndpoint = new AmqpLwtEndpoint(connection);
                this.lwtEndpoint
                        .willHandler(this::handleWill)
                        .disconnectionHandler(this::handleDisconnection);
                this.lwtEndpoint.open();

                connection.openHandler(o -> {
                    LOG.info("MQTT LWT service connected to the messaging service internal ...");
                    lwtConnFuture.complete();
                });

            } else {

                LOG.error("Error connecting MQTT LWT service to the messaging service internal ...", done.cause());

                lwtConnFuture.fail(done.cause());
            }

        });

        // compose the connection with messaging service
        lwtConnFuture.compose(v -> {

            Future<ProtonConnection> publishConnFuture = Future.future();

            this.client.connect(options, this.host, this.normalPort, done -> {

                if (done.succeeded()) {


                    ProtonConnection connection = done.result();
                    connection.setContainer(CONTAINER_ID);

                    // TODO
                    this.publishEndpoint = new AmqpPublishEndpoint(connection);
                    this.publishEndpoint.open();

                    connection.openHandler(o -> {
                        LOG.info("MQTT LWT service connected to the messaging service ...");
                        publishConnFuture.complete();
                    });

                } else {

                    LOG.error("Error connecting MQTT LWT service to the messaging service ...", done.cause());

                    publishConnFuture.fail(done.cause());
                }

            });

            return publishConnFuture;

        // compose the connection to the messaging service with connection to storage service
        }).compose(v -> {

            // connecting to the storage service
            this.lwtStorage.open(done -> {

                if (done.succeeded()) {

                    LOG.info("MQTT LWT service connected to the storage service ...");

                    // TODO

                    startFuture.complete();

                } else {

                    LOG.error("Error connecting MQTT LWT service to the storage service ...", done.cause());

                    startFuture.fail(done.cause());
                }

            });

        }, startFuture);
    }

    /**
     * Create an options instance for the ProtonClient
     *
     * @return  ProtonClient options instance
     */
    private ProtonClientOptions createClientOptions() {

        ProtonClientOptions options = new ProtonClientOptions();
        options.setConnectTimeout(5000);
        options.setReconnectAttempts(-1).setReconnectInterval(1000); // reconnect forever, every 1000 millisecs

        if (certDir != null) {
            options.setSsl(true)
                    .addEnabledSaslMechanism("EXTERNAL")
                    .setHostnameVerificationAlgorithm("")
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .addCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                            .addKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
        }
        return options;
    }

    private void handleWill(WillData willData) {

        // will message received, check for updating or adding
        this.lwtStorage.get(willData.clientId(), done -> {

            if (done.succeeded()) {
                this.lwtStorage.update(willData.clientId(), willData.amqpWillMessage(), ar -> {

                    LOG.info("Updated will for client {}", willData.clientId());
                });
            } else {
                this.lwtStorage.add(willData.clientId(), willData.amqpWillMessage(), ar -> {

                    LOG.info("Added will for client {}", willData.clientId());
                });
            }
        });
    }

    private void handleDisconnection(DisconnectionData disconnectionData) {

        // clean disconnection, just delete will message
        if (!disconnectionData.isError()) {

            this.lwtStorage.delete(disconnectionData.clientId(), done -> {

                LOG.info("Deleted will for client {}", disconnectionData.clientId());
            });
        } else {

            // brute disconnection, get will message and deliver it
            this.lwtStorage.get(disconnectionData.clientId(), ar -> {

                if (ar.succeeded()) {

                    AmqpWillMessage amqpWillMessage = ar.result();

                    // TODO : workaround ...
                    // check why with a message-id null or String the Artemis broker change "To" property
                    // so that the message isn't delivered by MQTT gateway
                    Object messageId = this.nextMessageId();
                    AmqpPublishMessage amqpPublishMessage =
                            new AmqpPublishMessage(messageId, amqpWillMessage.qos(), false, amqpWillMessage.isRetain(), amqpWillMessage.topic(), amqpWillMessage.payload());

                    this.publishEndpoint.publish(amqpPublishMessage, ar1 -> {

                        if (ar1.succeeded()) {

                            LOG.info("Published will message for client {}", disconnectionData.clientId());

                            this.lwtStorage.delete(disconnectionData.clientId(), ar2 -> {

                                LOG.info("Deleted will for client {}", disconnectionData.clientId());
                            });
                        }
                    });
                }
            });
        }
    }

    /**
     * Update and return the next message identifier
     *
     * @return message identifier
     */
    private int nextMessageId() {

        // if 0 or MAX_MESSAGE_ID, it becomes 1 (first valid messageId)
        this.messageIdCounter = ((this.messageIdCounter % MAX_MESSAGE_ID) != 0) ? this.messageIdCounter + 1 : 1;
        return this.messageIdCounter;
    }

    /**
     * Set the certificate directory where LWT certificates can be found.
     *
     * @param certDir path to certificate directory
     * @return current MQTT LWT instance
     */
    @Value(value = "${cert.dir}")
    public MqttLwt setCertDir(String certDir) {
        this.certDir = certDir;
        return this;
    }

    /**
     * Set the address for connecting to the AMQP internal network
     *
     * @param host hostname for AMQP connection
     * @return  current MQTT LWT instance
     */
    @Value(value = "${messaging.service.host:localhost}")
    public MqttLwt setHost(String host) {
        this.host = host;
        return this;
    }

    /**
     * Set the port for connecting to the internal AMQP network
     *
     * @param normalPort port for AMQP connections
     * @return  current MQTT LWT instance
     */
    @Value(value = "${messaging.service.normal.port:5672}")
    public MqttLwt setNormalPort(int normalPort) {
        this.normalPort = normalPort;
        return this;
    }

    /**
     * Set the port for connecting to the internal AMQP network where the port
     * has the route-container role.
     *
     * @param routeContainerPort port (with role route-container) for AMQP connections
     * @return  current MQTT LWT instance
     */
    @Value(value = "${messaging.service.route.container.port:55671}")
    public MqttLwt setRouteContainerPort(int routeContainerPort) {
        this.routeContainerPort = routeContainerPort;
        return this;
    }

    /**
     * Set the LWT Storage service implementation to use
     *
     * @param lwtStorage    LWT Storage service instance
     * @return  current MQTT LWT instance
     */
    @Autowired
    public MqttLwt setLwtStorage(LwtStorage lwtStorage) {
        this.lwtStorage = lwtStorage;
        return this;
    }
}
