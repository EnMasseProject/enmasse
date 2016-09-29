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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonMessageHandler;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class TopicMigrator {
    private final BrokerManager brokerManager;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Endpoint local;
    private final Vertx vertx;
    private final String containerId = "scaledown";
    private final String linkId = "scaledown";

    public TopicMigrator(BrokerManager brokerManager, Endpoint local, Vertx vertx) {
        this.brokerManager = brokerManager;
        this.local = local;
        this.vertx = vertx;
    }

    public void migrateTo(Endpoint toEndpoint, String address) throws Exception {
        // Step 1: Retrieve subscription identities
        Set<Subscription> subs = listSubscriptions(brokerManager, address);
        System.out.println("Current subs: " + subs);

        MigrateMessageHandler handler = new MigrateMessageHandler();
        // Step 2: Create local subscription
        createSubscription(address, handler);

        Thread.sleep(10000);
        System.out.println("Closing subscriptions");

        // Step 3: Close all subscriptions except our own with a amqp redirect
        closeSubscriptions(address, subs);

        // Step 4: Wait until subscription identities fetch in Step 1 appears on other broker
        watchForSubscriptions(toEndpoint, address, subs);

        // Step 5: Send messages on broker where subscription identity has appeared
        migrateMessages(toEndpoint, address, handler);
    }

    private void migrateMessages(Endpoint toEndpoint, String address, MigrateMessageHandler handler) {
        ProtonClient protonClient = ProtonClient.create(vertx);
        protonClient.connect(toEndpoint.hostName(), toEndpoint.port(), toConnection -> {
            if (toConnection.succeeded()) {
                ProtonConnection toConn = toConnection.result();
                toConn.setContainer(containerId);
                toConn.closeHandler(result -> {
                    System.out.println("Migrator connection closed");
                });
                toConn.openHandler(toResult -> {
                    Target target = new Target();
                    target.setAddress(address);
                    target.setCapabilities(Symbol.getSymbol("topic"));
                    ProtonSender sender = toConn.createSender(address, new ProtonLinkOptions().setLinkName(linkId));
                    sender.setTarget(target);
                    sender.closeHandler(res -> {
                        System.out.println("Sender connection closed");
                    });
                    sender.openHandler(toRes -> {
                        if (toRes.succeeded()) {
                            System.out.println("Opened sender, marking ready!");
                            handler.setSender(sender);
                            handler.setReady(true);
                        }
                    });
                    sender.open();
                });
            }
        });
    }

    private void watchForSubscriptions(Endpoint toEndpoint, String address, Set<Subscription> subs) throws Exception {
        BrokerManager toMgr = new BrokerManager(toEndpoint);
        Set<Subscription> foundSubs = Collections.emptySet();
        System.out.println("Waiting for " + subs);
        while (!subs.equals(foundSubs)) {
            foundSubs = listSubscriptions(toMgr, address);
            System.out.println("Remote subs: " + foundSubs);
            Thread.sleep(1000);
        }
        System.out.println("DONE!");
    }

    private void closeSubscriptions(String address, Set<Subscription> subs) throws Exception {
        brokerManager.closeSubscriptions(address, subs);
    }

    private void createSubscription(String address, MigrateMessageHandler handler) throws Exception {
        ProtonClient protonClient = ProtonClient.create(vertx);
        protonClient.connect(local.hostName(), local.port(), connection -> {

            if (connection.succeeded()) {
                ProtonConnection conn = connection.result();
                conn.setContainer(containerId);
                conn.closeHandler(result -> {
                    System.out.println("Migrator sub connection closed");
                });
                conn.openHandler(result -> {
                    Source source = new Source();
                    source.setAddress(address);
                    source.setCapabilities(Symbol.getSymbol("topic"));
                    source.setDurable(TerminusDurability.UNSETTLED_STATE);
                    ProtonReceiver localReceiver = conn.createReceiver(address, new ProtonLinkOptions().setLinkName(linkId));
                    localReceiver.setSource(source);
                    localReceiver.setPrefetch(0);
                    localReceiver.setAutoAccept(false);
                    localReceiver.closeHandler(res -> {
                        System.out.println("Migrator sub receiver closed");
                    });
                    localReceiver.openHandler(res -> {
                        if (res.succeeded()) {
                            System.out.println("Opened localReceiver");
                            handler.setReceiver(localReceiver);
                            Handler<Long> checkReady = new Handler<Long>() {
                                @Override
                                public void handle(Long event) {
                                    try {
                                        if (handler.isReady()) {
                                            System.out.println("Ready, starting flow!");
                                            localReceiver.flow(1);
                                        } else {
                                            System.out.println("Not ready yet, waiting");
                                            vertx.setTimer(1000, this);
                                        }
                                    } catch (Exception e) {
                                        vertx.setTimer(1000, this);
                                    }
                                }
                            };
                            vertx.setTimer(1000, checkReady);
                        } else {
                            System.out.println("Failed opening received: " + res.cause().getMessage());
                        }
                    });
                    localReceiver.handler(handler.messageHandler());
                    localReceiver.open();
                });
                conn.open();
            } else {
                System.out.println("Connection failed: " + connection.cause().getMessage());
            }
        });
    }

    private Set<Subscription> listSubscriptions(BrokerManager mgr, String address) throws Exception {
        JsonNode nodes = null;
        while (nodes == null) {
            try {
                nodes = mapper.readTree(mgr.listAllSubscriptions(address));
            } catch (Exception e) {
                System.out.println("Error listing: " + e.getMessage());
            }
        }

        Set<Subscription> subs = new LinkedHashSet<>();
        Iterator<JsonNode> it = nodes.iterator();
        while (it.hasNext()) {
            JsonNode entry = it.next();
            String clientId = entry.get("clientID").asText();
            String name = entry.get("name").asText();
            boolean durable = entry.get("durable").asBoolean();
            subs.add(new Subscription(clientId, name, durable));
        }
        return subs;
    }

    public static class MigrateMessageHandler {
        private final AtomicReference<ProtonReceiver> protonReceiver = new AtomicReference<>();
        private final AtomicReference<ProtonSender> protonSender = new AtomicReference<>();
        private volatile boolean ready = false;

        public ProtonMessageHandler messageHandler() {
            return (sourceDelivery, message) -> protonSender.get().send(message, protonDelivery -> {
                System.out.println("Forwarding message to subscriber");
                sourceDelivery.disposition(protonDelivery.getRemoteState(), protonDelivery.remotelySettled());
                protonReceiver.get().flow(protonSender.get().getCredit() - protonReceiver.get().getCredit());
            });
        }

        public boolean isReady() {
            return ready;
        }

        public void setReady(boolean ready) {
            this.ready = ready;
        }

        public void setReceiver(ProtonReceiver protonReceiver) {
            this.protonReceiver.set(protonReceiver);
        }

        public void setSender(ProtonSender protonSender) {
            this.protonSender.set(protonSender);
        }
    }
}
