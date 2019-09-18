/*
 * Copyright 2016-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.broker.cli;

import io.enmasse.amqp.ProtonRequestClient;
import io.enmasse.amqp.SyncRequestClient;
import io.vertx.core.Vertx;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.proton.ProtonClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class ClientFactory {
    private final Vertx vertx;
    private final ProtonClientOptions protonClientOptions;

    private static final Logger log = LoggerFactory.getLogger(Cli.class.getName());

    public ClientFactory(Vertx vertx) {

        this.vertx = vertx;


        String path = System.getenv("HOME") + System.getenv("AMQ_NAME") + "/etc";

        JksOptions clientJksOptions = new JksOptions();
        clientJksOptions
            .setPath(path + "/enmasse-keystore.jks")
            .setPassword("enmasse");

        PfxOptions clientPfxOptions = new PfxOptions()
            .setPath(path + "/enmasse-truststore.jks")
            .setPassword("enmasse");

        this.protonClientOptions =  new ProtonClientOptions()
            .setSsl(true)
            .setHostnameVerificationAlgorithm("")
            .setKeyStoreOptions(clientJksOptions)
            .setPfxTrustOptions(clientPfxOptions);

    }

    public SyncRequestClient connectBrokerManagementClient(String host, int port) throws Exception {
        ProtonRequestClient client = null;
        try {
            client = new ProtonRequestClient(vertx, System.getenv("HOSTNAME"));
            CompletableFuture<Void> promise = new CompletableFuture<>();
            client.connect(host, port, protonClientOptions, "activemq.management", promise);

            promise.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error creating/connecting to client: ", e);
            if (client != null) {
                client.close();
            }
        }
        log.info("Client created: " + client.getRemoteContainer());
        return client;
    }
}