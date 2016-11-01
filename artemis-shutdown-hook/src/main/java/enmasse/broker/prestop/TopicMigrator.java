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
import enmasse.discovery.Host;
import io.vertx.core.Vertx;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class TopicMigrator implements DiscoveryListener {
    private final Vertx vertx = Vertx.vertx();
    private volatile Set<Host> destinationBrokers = Collections.emptySet();
    private final Host localHost;
    private final BrokerManager localBroker;
    private final ExecutorService service = Executors.newSingleThreadExecutor();

    public TopicMigrator(Host localHost) throws Exception {
        this.localHost = localHost;
        this.localBroker = new BrokerManager(localHost.coreEndpoint());
    }

    public void migrate(String address) throws Exception {
        // Step 0: Cutoff router link
        localBroker.destroyConnectorService("amqp-connector");

        // Step 1: Retrieve subscriptions
        Set<String> queues = listQueuesForAddress(localBroker, address).stream()
                .filter(q -> SubscriptionMigrator.isValidSubscription(q))
                .collect(Collectors.toSet());
        System.out.println("Listed queues: " + queues);

        // Step 2: Create and pause queues on other brokers
        Map<String, Host> queueMap = createQueues(address, queues);

        // Step 3: Migrate messages from local subscriptions to destinations
        System.out.println("Migrating messages for " + queueMap.keySet());
        migrateMessages(address, queueMap);

        // Step 4: Destroy local queues
        System.out.println("Destroying local queues");
        localBroker.destroyQueues(queues);

        // Step 5: Activate queues
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

    private void migrateMessages(String address, Map<String, Host> queueMap) throws Exception {
        List<Future<SubscriptionMigrator>> results = queueMap.entrySet().stream()
                .map(e -> new SubscriptionMigrator(vertx, address, e.getKey(), localHost, e.getValue(), localBroker))
                .map(sub -> service.submit(sub))
                .collect(Collectors.toList());

        for (Future<SubscriptionMigrator> result : results) {
            try {
                result.get();
            } catch (Exception e) {
                System.out.println("Unable to migrate messages (ignoring): " + e.getMessage());
            }
        }
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
