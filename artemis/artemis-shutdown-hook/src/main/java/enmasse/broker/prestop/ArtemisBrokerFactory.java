/*
 * Copyright 2017 Red Hat Inc.
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
