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
package io.enmasse.controller.brokered;

import enmasse.amqp.artemis.Artemis;
import enmasse.amqp.artemis.Broker;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.types.brokered.BrokeredType;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.Watcher;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.ProtonConnection;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controls the addresses of a brokered address space.
 */
public class AddressController extends AbstractVerticle implements Watcher<Address> {
    private volatile Broker broker;

    @Override
    public void start(Future<Void> startPromise) {
        ProtonClient client = ProtonClient.create(vertx);
        // TODO: Pass from environment
        client.connect(System.getenv("MESSAGING_SERVICE_HOST"), Integer.parseInt(System.getenv("MESSAGING_SERVICE_PORT_AMQPS")), connection -> {
            ProtonConnection conn = connection.result();
            conn.setContainer("address-controller");
            Artemis.create(vertx, conn).setHandler(result -> {
                if (result.succeeded()) {
                    startPromise.complete();
                } else {
                    startPromise.fail(result.cause());
                }
            });
            conn.open();
        });
    }

    @Override
    public void resourcesUpdated(Set<Address> addresses) throws Exception {

        /**
         * TODO:
         *
         * For each broker in the deployment:
         * * Delete queues and topics that should no longer exist
         * * Create queues and topics that should exist
         */

        if (broker != null) {
            Set<String> desiredQueues = addresses.stream()
                    .filter(address -> address.getType() == BrokeredType.QUEUE)
                    .map(Address::getAddress)
                    .collect(Collectors.toSet());


            for (String queue : desiredQueues) {
                broker.deployQueue(queue);
            }

            Set<String> toRemove = broker.getQueueNames();
            toRemove.removeAll(desiredQueues);

            for (String queue : toRemove) {
                broker.deleteQueue(queue);
            }
        }
    }
}
