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
import io.vertx.core.Vertx;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class TopicMigrator implements DiscoveryListener {
    private final Vertx vertx;
    private volatile Set<Host> destinationBrokers = Collections.emptySet();
    private final Host localHost;
    private final BrokerManager localBroker;
    private final Endpoint messagingEndpoint;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public TopicMigrator(Vertx vertx, Host localHost, Endpoint messagingEndpoint) throws Exception {
        this.vertx = vertx;
        this.localHost = localHost;
        this.localBroker = new BrokerManager(localHost.coreEndpoint());
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
        localBroker.destroySubscriptions(subscriptions);

        // Step 5: Activate queues
        activateQueues(queueMap);

        // Step 6: Shutdown
        vertx.close();
        localBroker.shutdownBroker();
    }

    private void activateQueues(Map<QueueInfo, Host> queueMap) throws Exception {
        for (Map.Entry<QueueInfo, Host> entry : queueMap.entrySet()) {
            try (BrokerManager mgr = new BrokerManager(entry.getValue().coreEndpoint())) {
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
            try (BrokerManager manager = new BrokerManager(dest.coreEndpoint())) {
                manager.createQueue(queue.getAddress(), queue.getQueueName());
                manager.pauseQueue(queue.getQueueName());
                if (subscriptionInfo.getDivertInfo().isPresent()) {
                    DivertInfo divert = subscriptionInfo.getDivertInfo().get();
                    manager.createDivert(divert.getName(), divert.getRoutingName(), divert.getAddress(), divert.getForwardingAddress());
                    manager.createConnectorService(messagingEndpoint, divert.getForwardingAddress());
                }
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

    private Set<SubscriptionInfo> listSubscriptions(BrokerManager brokerManager) throws Exception {
        Set<QueueInfo> queues = listQueuesForMigration(brokerManager);
        Set<DivertInfo> diverts = listDivertsForMigration(brokerManager);

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

    private Set<QueueInfo> listQueuesForMigration(BrokerManager mgr) throws Exception {
        Set<QueueInfo> queues = new LinkedHashSet<>();
        for (String queueName : mgr.listQueues()) {
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

    private Set<DivertInfo> listDivertsForMigration(BrokerManager mgr) throws Exception {
        Set<DivertInfo> diverts = new LinkedHashSet<>();
        for (String divertName : mgr.listDiverts()) {
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


    @Override
    public void hostsChanged(Set<Host> set) {
        destinationBrokers = set;
    }
}
