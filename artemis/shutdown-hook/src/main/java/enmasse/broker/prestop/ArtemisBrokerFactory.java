/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import io.enmasse.amqp.Artemis;
import io.enmasse.amqp.ProtonRequestClient;
import io.enmasse.amqp.SyncRequestClient;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClientOptions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ArtemisBrokerFactory implements BrokerFactory {
    private final long timeoutInMillis;
    public ArtemisBrokerFactory(long timeoutInMillis) {
        this.timeoutInMillis = timeoutInMillis;
    }

    @Override
    public Artemis createClient(Vertx vertx, ProtonClientOptions protonClientOptions, Endpoint endpoint) throws InterruptedException, TimeoutException, ExecutionException {
        SyncRequestClient requestClient = new ProtonRequestClient(vertx, 10);
        CompletableFuture<Void> promise = new CompletableFuture<>();
        requestClient.connect(endpoint.hostname(), endpoint.port(), protonClientOptions, "activemq.management", promise);
        Artemis artemis = new Artemis(requestClient);
        promise.get(timeoutInMillis, TimeUnit.MILLISECONDS);
        return artemis;
    }
}
