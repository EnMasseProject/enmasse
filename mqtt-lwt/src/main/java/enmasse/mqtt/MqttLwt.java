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
import enmasse.mqtt.storage.impl.InMemoryLwtStorage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Vert.x based MQTT Last Will and Testament service for EnMasse
 */
public class MqttLwt extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MqttLwt.class);

    private static final String CONTAINER_ID = "lwt-service";
    private final MqttLwtOptions options;
    private final LwtStorage lwtStorage;


    private ProtonClient client;

    private AmqpLwtEndpoint lwtEndpoint;
    private AmqpPublishEndpoint publishEndpoint;

    public MqttLwt(MqttLwtOptions options) {

        this.options = options;
        this.lwtStorage = new InMemoryLwtStorage();
    }

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
        this.client.connect(options, this.options.getMessagingServiceHost(), this.options.getRouteContainerPort(), done -> {

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

            this.client.connect(options, this.options.getMessagingServiceHost(), this.options.getMessagingServiceNormalPort(), done -> {

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

        String certDir = this.options.getCertDir();
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

                    AmqpPublishMessage amqpPublishMessage =
                            new AmqpPublishMessage(amqpWillMessage.qos(), false, amqpWillMessage.isRetain(), amqpWillMessage.topic(), amqpWillMessage.payload());

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
}
