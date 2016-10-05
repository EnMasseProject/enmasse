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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TopicMigrator implements DiscoveryListener {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Vertx vertx = Vertx.vertx();
    private volatile Set<Host> destinationBrokers = Collections.emptySet();
    private final Host localHost;
    private final BrokerManager localBroker;

    public TopicMigrator(Host localHost) throws Exception {
        this.localHost = localHost;
        this.localBroker = new BrokerManager(localHost.coreEndpoint());
    }

    public void migrate(String address) throws Exception {
        // Step 0: Cutoff new subscriptions

        // Step 1: Retrieve subscription identities
        System.out.println("Listing subscriptions");
        Set<String> queues = listQueuesForAddress(localBroker, address);

        List<MigrateMessageHandler> migrateHandlers = queues.stream()
                .map(s -> new MigrateMessageHandler(s))
                .collect(Collectors.toList());

        // Step 2: Create local subscription
        System.out.println("Creating local subscriptions");
        createSubscriptions(address, migrateHandlers);

        // Step 3: Close all subscriptions except our own with a amqp redirect
        System.out.println("Closing other subscriptions");
        localBroker.destroyQueues(queues);

        // Step 4: Wait until subscription identities fetched in Step 1 appears on other brokers
        System.out.println("Waiting for subscriptions");
        List<Endpoint> endpoints = watchForQueues(address, queues);

        // Step 5: Send messages on broker where subscription identity has appeared
        System.out.println("Migrating messages");
        migrateMessages(address, endpoints, migrateHandlers);

        waitUntilEmpty(migrateHandlers.stream().map(MigrateMessageHandler::getQueueName).collect(Collectors.toSet()));
        vertx.close();
        localBroker.shutdownBroker();
    }

    private void waitUntilEmpty(Set<String> queues) throws InterruptedException {
        for (String queue : queues) {
            localBroker.waitUntilEmpty(queue);
        }
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
                toConn.setContainer(handler.getId());
                toConn.closeHandler(result -> {
                    System.out.println("Migrator connection closed");
                });
                toConn.openHandler(toResult -> {
                    Target target = new Target();
                    target.setAddress(address);
                    target.setCapabilities(Symbol.getSymbol("topic"));
                    ProtonSender sender = toConn.createSender(address, new ProtonLinkOptions().setLinkName(handler.getId()));
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

    private List<Endpoint> watchForQueues(String address, Set<String> queues) throws Exception {
        Map<String, Endpoint> endpointMap = new HashMap<>();
        System.out.println("Waiting for subscriptions");
        while (true) {
            List<BrokerManager> brokers = destinationBrokers.stream()
                .filter(host -> !host.equals(localHost))
                .map(host -> new BrokerManager(host.coreEndpoint()))
                .collect(Collectors.toList());

            for (BrokerManager broker : brokers) {
                Set<String> brokerQueues = listQueuesForAddress(broker, address);
                System.out.println("Remote queues for " + broker.getEndpoint().hostname() + ": " + brokerQueues);
                for (String queue : brokerQueues) {
                    if (queues.contains(queue)) {
                        endpointMap.put(queue, broker.getEndpoint());
                    }
                }
            }
            System.out.println("Found: " + endpointMap.keySet() + ", waiting for: " + queues);
            if (endpointMap.keySet().equals(queues)) {
                break;
            }
            Thread.sleep(1000);
        }
        return new ArrayList<>(endpointMap.values());
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
                conn.setContainer(handler.getId());
                conn.closeHandler(result -> {
                    System.out.println("Migrator sub connection closed");
                });
                conn.openHandler(result -> {
                    Source source = new Source();
                    source.setAddress(address);
                    source.setCapabilities(Symbol.getSymbol("topic"));
                    source.setDurable(TerminusDurability.UNSETTLED_STATE);
                    ProtonReceiver localReceiver = conn.createReceiver(address, new ProtonLinkOptions().setLinkName(handler.getId()));
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


    private Set<String> listQueuesForAddress(BrokerManager mgr, String address) throws Exception {
        Set<String> queues = new LinkedHashSet<>();
        for (String queue : mgr.listQueues()) {
            try {
                String queueAddress = mgr.getQueueAddress(queue);
                if (address.equals(queueAddress)) {
                    queues.add(queue);
                }
            } catch (Exception e) {
                System.out.println("Error getting queue address, ignoring: " + e.getMessage());
            }
        }
        return queues;
    }

    @Override
    public void hostsChanged(Set<Host> set) {
        destinationBrokers = set;
    }
}
