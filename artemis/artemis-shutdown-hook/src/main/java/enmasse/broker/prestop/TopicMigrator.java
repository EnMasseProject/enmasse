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

import enmasse.discovery.DiscoveryListener;
import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import io.enmasse.amqp.Artemis;
import io.vertx.core.Vertx;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class TopicMigrator implements DiscoveryListener {
    private final Vertx vertx;
    private volatile Set<Host> destinationBrokers = Collections.emptySet();
    private final Host localHost;
    private final Artemis localBroker;
    private final Endpoint messagingEndpoint;
    private final BrokerFactory brokerFactory;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public TopicMigrator(Vertx vertx, Host localHost, Endpoint messagingEndpoint, BrokerFactory brokerFactory) throws Exception {
        this.vertx = vertx;
        this.localHost = localHost;
        this.brokerFactory = brokerFactory;
        this.localBroker = brokerFactory.createClient(vertx, localHost.amqpEndpoint());
        this.messagingEndpoint = messagingEndpoint;
    }

    public void migrate() throws Exception {
        // Step 0: Cutoff router link
        localBroker.destroyConnectorService("amqp-connector");

        // Step 1: Retrieve subscriptions and diverts
        Set<SubscriptionInfo> subscriptions = listSubscriptions(localBroker);

        System.out.println("Listed subscriptions: " + subscriptions);

        // Step 2: Create and pause queues on other brokers
        Map<QueueInfo, Host> queueMap = createSubscriptions(subscriptions);

        // Step 3: Migrate messages from local subscriptions to destinations
        System.out.println("Migrating messages for " + queueMap.keySet());
        migrateMessages(queueMap);

        // Step 4: Destroy local queues
        System.out.println("Destroying local queues");
        destroySubscriptions(localBroker, subscriptions);

        // Step 5: Activate queues
        activateQueues(queueMap);

        // Step 6: Shutdown
        vertx.close();
        localBroker.forceShutdown();
    }

    private void activateQueues(Map<QueueInfo, Host> queueMap) throws Exception {
        for (Map.Entry<QueueInfo, Host> entry : queueMap.entrySet()) {
            try (Artemis mgr = brokerFactory.createClient(vertx, entry.getValue().amqpEndpoint())) {
                mgr.resumeQueue(entry.getKey().getQueueName());
            }
        }
    }

    private Map<QueueInfo,Host> createSubscriptions(Set<SubscriptionInfo> subscriptions) throws Exception {
        Map<QueueInfo, Host> queueMap = new LinkedHashMap<>();
        Iterator<Host> destinations = Collections.emptyIterator();
        for (SubscriptionInfo subscriptionInfo : subscriptions) {
            while (!destinations.hasNext()) {
                destinations = otherHosts().iterator();
                if (destinations.hasNext()) {
                    break;
                }
                Thread.sleep(1000);
            }
            Host dest = destinations.next();
            QueueInfo queue = subscriptionInfo.getQueueInfo();
            try (Artemis manager = brokerFactory.createClient(vertx, dest.amqpEndpoint())) {
                manager.deployQueue(queue.getQueueName(), queue.getAddress());
                manager.pauseQueue(queue.getQueueName());
                if (subscriptionInfo.getDivertInfo().isPresent()) {
                    DivertInfo divert = subscriptionInfo.getDivertInfo().get();
                    manager.createDivert(divert.getName(), divert.getRoutingName(), divert.getAddress(), divert.getForwardingAddress());
                    createConnectorService(messagingEndpoint, divert.getForwardingAddress());
                }
            }
            queueMap.put(queue, dest);
        }
        return queueMap;
    }

    private void createConnectorService(Endpoint messagingEndpoint, String address) throws TimeoutException {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("host", messagingEndpoint.hostname());
        parameters.put("port", String.valueOf(messagingEndpoint.port()));
        parameters.put("containerId", address);
        parameters.put("groupId", address);
        parameters.put("clientAddress", address);
        parameters.put("sourceAddress", address);

        localBroker.createConnectorService(address, parameters);
    }

    private Set<Host> otherHosts() {
        return destinationBrokers.stream()
                .filter(host -> !host.equals(localHost))
                .collect(Collectors.toSet());
    }

    private void migrateMessages(Map<QueueInfo, Host> queueMap) throws Exception {
        List<Future<QueueMigrator>> results = queueMap.entrySet().stream()
                .map(e -> new QueueMigrator(vertx, e.getKey(), localHost, e.getValue(), localBroker))
                .map(sub -> service.submit(sub))
                .collect(Collectors.toList());

        for (Future<QueueMigrator> result : results) {
            try {
                result.get();
            } catch (Exception e) {
                System.out.println("Unable to migrate messages (ignoring): " + e.getMessage());
            }
        }
    }

    private Set<SubscriptionInfo> listSubscriptions(Artemis broker) throws Exception {
        Set<QueueInfo> queues = listQueuesForMigration(broker);
        Set<DivertInfo> diverts = listDivertsForMigration(broker);

        Set<SubscriptionInfo> subscriptions = new LinkedHashSet<>();
        Iterator<QueueInfo> it = queues.iterator();
        while (it.hasNext()) {
            QueueInfo queueInfo = it.next();
            for (DivertInfo divertInfo : diverts) {
                if (queueInfo.getAddress().equals(divertInfo.getForwardingAddress())) {
                    subscriptions.add(new SubscriptionInfo(queueInfo, Optional.of(divertInfo), Optional.of(new ConnectorInfo(divertInfo.getForwardingAddress()))));
                    it.remove();
                }
            }
        }

        subscriptions.addAll(queues.stream().map(q -> new SubscriptionInfo(q, Optional.empty(), Optional.empty())).collect(Collectors.toSet()));
        return subscriptions;
    }

    private Set<QueueInfo> listQueuesForMigration(Artemis mgr) throws Exception {
        Set<QueueInfo> queues = new LinkedHashSet<>();
        for (String queueName : mgr.getQueueNames()) {
            if (!queueName.startsWith("activemq.management") && !queueName.startsWith("topic-forwarder")) {
                String address = mgr.getQueueAddress(queueName);
                try {
                    queues.add(new QueueInfo(address, queueName));
                } catch (Exception e) {
                    System.out.println("Unable to get address of queue: " + e.getMessage());
                }
            }
        }
        return queues;
    }

    private Set<DivertInfo> listDivertsForMigration(Artemis mgr) throws Exception {
        Set<DivertInfo> diverts = new LinkedHashSet<>();
        for (String divertName : mgr.getDivertNames()) {
            if (!divertName.equals("qualified-topic-divert")) {
                try {
                    String routingName = mgr.getDivertRoutingName(divertName);
                    String address = mgr.getDivertAddress(divertName);
                    String forwardingAddress = mgr.getDivertForwardingAddress(divertName);
                    diverts.add(new DivertInfo(divertName, routingName, address, forwardingAddress));
                } catch (Exception e) {
                    System.out.println("Unable to retrieve attributes for divert " + divertName + ", skipping." + e.getMessage());
                }
            }
        }
        return diverts;
    }

    private void destroySubscriptions(Artemis broker, Set<SubscriptionInfo> subscriptions) throws Exception {
        for (SubscriptionInfo subscription : subscriptions) {
            QueueInfo queueInfo = subscription.getQueueInfo();
            broker.destroyQueue(queueInfo.getQueueName());
            if (subscription.getDivertInfo().isPresent()) {
                broker.destroyDivert(subscription.getDivertInfo().get().getName());
            }
            if (subscription.getConnectorInfo().isPresent()) {
                broker.destroyConnectorService(subscription.getConnectorInfo().get().getName());
            }
        }
    }

    @Override
    public void hostsChanged(Set<Host> set) {
        destinationBrokers = set;
    }
}
