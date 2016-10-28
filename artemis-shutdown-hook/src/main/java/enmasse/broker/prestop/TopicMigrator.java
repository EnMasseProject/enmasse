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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
        // Step 0: Cutoff router link
        localBroker.destroyConnectorService("amqp-connector");

        // Step 1: Retrieve subscriptions
        System.out.println("Listing subscriptions");
        Set<String> queues = listQueuesForAddress(localBroker, address);

        // Step 2: Create and pause queues on other brokers
        Map<String, Host> queueMap = createQueues(address, queues);

        // Step 3: Create local subscriptions
        System.out.println("Creating local subscriptions");
        List<MigrateMessageHandler> migrateHandlers = queueMap.entrySet().stream()
            .map(e -> new MigrateMessageHandler(e.getKey(), e.getValue().amqpEndpoint()))
            .collect(Collectors.toList());

        createSubscriptions(address, migrateHandlers);

        // Step 4: Destroy local queues
        System.out.println("Closing other subscriptions");
        localBroker.destroyQueues(queues);

        // Step 5: Send messages on broker where subscription identity has appeared
        System.out.println("Migrating messages");
        migrateMessages(address, migrateHandlers);
        waitUntilEmpty(migrateHandlers.stream().map(MigrateMessageHandler::getQueueName).collect(Collectors.toSet()));

        // Step 6: Activate queues
        activateQueues(queueMap);

        // Step 7: Shutdown
        vertx.close();
        localBroker.shutdownBroker();
    }

    private void activateQueues(Map<String, Host> queueMap) throws Exception {
        for (Map.Entry<String, Host> entry : queueMap.entrySet()) {
            try (BrokerManager mgr = new BrokerManager(entry.getValue().coreEndpoint())) {
                mgr.resumeQueue(entry.getKey());
            }
        }
    }

    private Map<String, Host> createQueues(String address, Set<String> queues) throws Exception {
        Map<String, Host> queueMap = new LinkedHashMap<>();
        Iterator<Host> destinations = Collections.emptyIterator();
        for (String queue : queues) {
            while (!destinations.hasNext()) {
                destinations = otherHosts().iterator();
                if (destinations.hasNext()) {
                    break;
                }
                Thread.sleep(1000);
            }
            Host dest = destinations.next();
            try (BrokerManager manager = new BrokerManager(dest.coreEndpoint())) {
                manager.createQueue(address, queue);
                manager.pauseQueue(queue);
            }
            queueMap.put(queue, dest);
        }
        return queueMap;
    }

    private Set<Host> otherHosts() {
        return destinationBrokers.stream()
                .filter(host -> !host.equals(localHost))
                .collect(Collectors.toSet());
    }

    private void waitUntilEmpty(Set<String> queues) throws InterruptedException {
        for (String queue : queues) {
            localBroker.waitUntilEmpty(queue);
        }
    }

    private void migrateMessages(String address, List<MigrateMessageHandler> handlers) {
        for (MigrateMessageHandler handler : handlers) {
            migrateMessages(address, handler);
        }
    }


    private void migrateMessages(String address, MigrateMessageHandler handler) {
        ProtonClient protonClient = ProtonClient.create(vertx);
        protonClient.connect(handler.toEndpoint().hostname(), handler.toEndpoint().port(), toConnection -> {
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
