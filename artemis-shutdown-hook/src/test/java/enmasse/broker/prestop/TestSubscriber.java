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

import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;

/**
 * TODO: Description
 */
public class TestSubscriber {
    private final ProtonClient client;

    public TestSubscriber(ProtonClient client) {
        this.client = client;
    }

    public void subscribe(Endpoint endpoint, String address, Endpoint failover) {
        String containerId = "helloclient";
        client.connect(endpoint.hostName(), endpoint.port(), connection -> {
            if (connection.succeeded()) {
                System.out.println("Connected: ");
                ProtonConnection conn = connection.result();
                conn.setContainer(containerId);
                conn.closeHandler(res -> {
                    System.out.println("CLIENT cONN cLOSED");
                });
                conn.openHandler(result -> {
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
