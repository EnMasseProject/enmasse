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
import enmasse.discovery.DiscoveryListener;
import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TopicMigrator implements DiscoveryListener {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Vertx vertx;
    private volatile Set<Host> destinationBrokers = Collections.emptySet();
    private final Host localHost;
    private final BrokerManager localBroker;

    private final String containerId = "scaledown";
    private final String linkId = "scaledown";

    public TopicMigrator(Host localHost, Vertx vertx) throws Exception {
        this.localHost = localHost;
        this.localBroker = new BrokerManager(localHost.coreEndpoint());
        this.vertx = vertx;
    }

    public void migrate(String address) throws Exception {
        // Step 0: Cutoff new subscriptions

        // Step 1: Retrieve subscription identities
        Set<Subscription> subs = listSubscriptions(localBroker, address);
        System.out.println("Current subs: " + subs);

        List<MigrateMessageHandler> migrateHandlers = subs.stream()
                .map(s -> new MigrateMessageHandler())
                .collect(Collectors.toList());

        // Step 2: Create local subscription
        createSubscriptions(address, migrateHandlers);

        Thread.sleep(10000);
        System.out.println("Closing subscriptions");

        // Step 3: Close all subscriptions except our own with a amqp redirect
        closeSubscriptions(address, subs);

        // Step 4: Wait until subscription identities fetched in Step 1 appears on other brokers
        List<Endpoint> endpoints = watchForSubscriptions(address, subs);

        // Step 5: Send messages on broker where subscription identity has appeared
        migrateMessages(address, endpoints, migrateHandlers);
    }

    private void migrateMessages(String address, List<Endpoint> endpoints, List<MigrateMessageHandler> handlers) {
        if (endpoints.size() != handlers.size()) {
            throw new IllegalStateException("#endpoints and #handler must be the same");
        }
        for (int i = 0; i < endpoints.size(); i++) {
            migrateMessages(address, endpoints.get(i), handlers.get(i));
        }
    }


    private void migrateMessages(String address, Endpoint toEndpoint, MigrateMessageHandler handler) {
        ProtonClient protonClient = ProtonClient.create(vertx);
        protonClient.connect(toEndpoint.hostname(), toEndpoint.port(), toConnection -> {
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

    private List<Endpoint> watchForSubscriptions(String address, Set<Subscription> subs) throws Exception {

        List<BrokerManager> brokers = destinationBrokers.stream()
                .filter(host -> !host.equals(localHost))
                .map(host -> new BrokerManager(host.coreEndpoint()))
                .collect(Collectors.toList());


        Map<Subscription, Endpoint> endpointMap = new HashMap<>();
        System.out.println("Waiting for subscriptions");
        while (true) {
            for (BrokerManager broker : brokers) {
                Set<Subscription> brokerSubs = listSubscriptions(broker, address);
                System.out.println("Remote subs for " + broker.getEndpoint().hostname() + ": " + brokerSubs);
                for (Subscription sub : brokerSubs) {
                    if (subs.contains(sub)) {
                        endpointMap.put(sub, broker.getEndpoint());
                    }
                }
            }
            System.out.println("Found: " + endpointMap.keySet() + ", waiting for: " + subs);
            if (endpointMap.keySet().equals(subs)) {
                break;
            }
            Thread.sleep(1000);
        }
        return new ArrayList<>(endpointMap.values());
    }

    private void closeSubscriptions(String address, Set<Subscription> subs) throws Exception {
        localBroker.closeSubscriptions(address, subs);
    }

    private void createSubscriptions(String address, List<MigrateMessageHandler> handlers) throws Exception {
        for (MigrateMessageHandler handler : handlers) {
            createSubscription(address, handler);
        }
    }

    private void createSubscription(String address, MigrateMessageHandler handler) {
        ProtonClient protonClient = ProtonClient.create(vertx);
        Endpoint localEndpoint = localHost.amqpEndpoint();
        protonClient.connect(localEndpoint.hostname(), localEndpoint.port(), connection -> {
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

    @Override
    public void hostsChanged(Set<Host> set) {
        destinationBrokers = set;
    }
}
