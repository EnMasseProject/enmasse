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

package enmasse.broker.prestop;

import enmasse.discovery.Endpoint;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;

/**
 * TODO: Description
 */
public class TestSubscriber {
    private final Vertx vertx;

    public TestSubscriber(Vertx vertx) {
        this.vertx = vertx;
    }

    public void subscribe(Endpoint endpoint, String address, Endpoint failover) {
        String containerId = "helloclient";
        ProtonClient client = ProtonClient.create(vertx);
        client.connect(endpoint.hostname(), endpoint.port(), connection -> {
            if (connection.succeeded()) {
                ProtonConnection conn = connection.result();
                conn.setContainer(containerId);
                conn.closeHandler(res -> {
                    System.out.println("CLIENT cONN cLOSED");
                });
                conn.openHandler(result -> {
                    System.out.println("Connected: " + result.result().getRemoteContainer());
                    Source source = new Source();
                    source.setAddress(address);
                    source.setCapabilities(Symbol.getSymbol("topic"));
                    source.setDurable(TerminusDurability.UNSETTLED_STATE);
                    ProtonReceiver receiver = conn.createReceiver(address, new ProtonLinkOptions().setLinkName(containerId));
                    receiver.setSource(source);
                    receiver.openHandler(res -> {
                        if (res.succeeded()) {
                            System.out.println("Opened receiver");
                        } else {
                            System.out.println("Failed opening received: " + res.cause().getMessage());
                        }
                    });
                    receiver.closeHandler(res -> {
                        System.out.println("CLIENT CLOSED");
                        conn.close();
                        if (failover != null) {
                            vertx.setTimer(5000, timerId -> {
                                System.out.println("Connecting to failover");
                                subscribe(failover, address, null);
                            });
                        }
                    });
                    receiver.handler((delivery, message) -> System.out.println("GOT MESSAGE"));
                    receiver.open();
                });
                conn.open();
            } else {
                System.out.println("Connection failed: " + connection.cause().getMessage());
            }
        });
    }
}
