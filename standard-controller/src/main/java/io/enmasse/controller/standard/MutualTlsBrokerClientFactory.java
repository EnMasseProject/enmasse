/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.amqp.ProtonRequestClient;
import io.enmasse.amqp.SyncRequestClient;
import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class MutualTlsBrokerClientFactory implements BrokerClientFactory {
    private final Vertx vertx;
    private final ProtonClientOptions protonClientOptions;
    private final StandardControllerOptions options;

    public MutualTlsBrokerClientFactory(Vertx vertx, StandardControllerOptions options) {
        this.vertx = vertx;
        String certDir = options.getCertDir();
        this.protonClientOptions = new ProtonClientOptions()
                    .setSsl(true)
                    .setHostnameVerificationAlgorithm("")
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                            .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
        this.options = options;
    }

    @Override
    public SyncRequestClient connectBrokerManagementClient(String host, int port) throws Exception {
        ProtonRequestClient client = null;
        try {
            client = new ProtonRequestClient(vertx, "standard-controller");
            CompletableFuture<Void> promise = new CompletableFuture<>();
            client.connect(host, port, protonClientOptions, "activemq.management", promise);

            promise.get(options.getManagementConnectTimeout().getSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            if (client != null) {
                client.close();
            }
        }
        return client;
    }
}
