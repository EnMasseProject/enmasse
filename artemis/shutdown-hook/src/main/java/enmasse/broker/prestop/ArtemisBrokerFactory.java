/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import io.enmasse.amqp.Artemis;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClientOptions;

public class ArtemisBrokerFactory implements BrokerFactory {
    private final long timeoutInMillis;
    public ArtemisBrokerFactory(long timeoutInMillis) {
        this.timeoutInMillis = timeoutInMillis;
    }

    @Override
    public Artemis createClient(Vertx vertx, ProtonClientOptions protonClientOptions, Endpoint endpoint) throws InterruptedException {
        Future<Artemis> artemisFuture = Artemis.create(vertx, protonClientOptions, endpoint.hostname(), endpoint.port());
        long endTime = System.currentTimeMillis() + timeoutInMillis;
        while (System.currentTimeMillis() < endTime && !artemisFuture.isComplete()) {
            Thread.sleep(1000);
        }
        if (!artemisFuture.isComplete()) {
            throw new RuntimeException("Timed out connecting to Artemis");
        } else if (artemisFuture.failed()) {
            throw new RuntimeException("Failed connecting to Artemis", artemisFuture.cause());
        } else {
            return artemisFuture.result();
        }
    }
}
