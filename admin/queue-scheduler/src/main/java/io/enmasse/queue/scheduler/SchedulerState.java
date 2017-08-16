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

package io.enmasse.queue.scheduler;

import io.enmasse.address.model.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contains the mapping from queue to broker and ensures there is only one modifying the state at a time.
 */
public class SchedulerState {
    private static final Logger log = LoggerFactory.getLogger(SchedulerState.class.getName());
    private final Map<String, Map<String, Broker>> brokerGroupMap = new LinkedHashMap<>();
    private final Map<String, Set<Address>> addressMap = new LinkedHashMap<>();


    public synchronized void addressesChanged(Map<String, Set<Address>> updatedMap) {
        Set<String> removedGroups = new HashSet<>(addressMap.keySet());
        removedGroups.removeAll(updatedMap.keySet());
        removedGroups.forEach(addressMap::remove);

        updatedMap.forEach(this::groupUpdated);
    }

    private synchronized void groupUpdated(String groupId, Set<Address> addresses) {
        Set<Address> existing = addressMap.getOrDefault(groupId, Collections.emptySet());

        Set<Address> removed = new HashSet<>(existing);
        removed.removeAll(addresses);
        if (!removed.isEmpty()) {
            deleteAddresses(groupId, removed);
        }

        Set<Address> added = new HashSet<>(addresses);
        added.removeAll(existing);
        if (!added.isEmpty()) {
            addAddresses(groupId, addresses, added);
        }

        addressMap.put(groupId, addresses);
        log.info("Updated addresses for " + groupId + ": " + addresses);
    }


    public synchronized void brokerAdded(String groupId, String brokerId, Broker broker) {
        if (!brokerGroupMap.containsKey(groupId)) {
            brokerGroupMap.put(groupId, new LinkedHashMap<>());
        }

        if (brokerGroupMap.get(groupId).containsKey(brokerId)) {
            throw new IllegalArgumentException("Broker with id " + brokerId + " already exists in group " + groupId);
        }
        brokerGroupMap.get(groupId).put(brokerId, broker);

        Set<Address> addresses = addressMap.getOrDefault(groupId, Collections.emptySet());
        log.info("Broker " + brokerId + " in group " + groupId + " was added, distributing addresses: " + addresses);
        if (addresses.size() == 1) {
            broker.deployQueue(addresses.iterator().next().getAddress());
        } else {
            distributeAddressesByNumQueues(groupId, addresses);
        }
    }

    public synchronized void brokerRemoved(String groupId, String brokerId) {
        Map<String, Broker> brokerMap = brokerGroupMap.get(groupId);
        if (brokerMap != null && brokerMap.containsKey(brokerId)) {
            brokerMap.remove(brokerId);
            if (brokerMap.isEmpty()) {
                brokerGroupMap.remove(groupId);
            }
            Set<Address> addresses = addressMap.getOrDefault(groupId, Collections.emptySet());
            // If colocated queues, ensure missing queues are recreated on other brokers.
            if (addresses.size() > 1) {
                distributeAddressesByNumQueues(groupId, addresses);
            }
            log.info("Broker " + brokerId + " in group " + groupId + " was removed");
        } else {
            log.info("Broker was already removed, ignoring");
        }
    }

    private void addAddresses(String groupId, Set<Address> addresses, Set<Address> added) {

        // TODO: Fetch this information from somewhere, but assume > 1 address means shared flavor
        if (addresses.size() > 1) {
            distributeAddressesByNumQueues(groupId, added);
        } else {
            distributeAddressesAll(groupId, added);
        }
    }

    private void distributeAddressesByNumQueues(String groupId, Set<Address> addresses) {
        Map<String, Broker> brokerMap = brokerGroupMap.get(groupId);
        if (brokerMap == null) {
            return;
        }


        Map<String, Address> addressesToDeploy = addresses.stream().collect(Collectors.toMap(Address::getAddress, Function.identity()));

        // Remove addresses that are already distributed. This is to avoid changes in broker list to affect where queues are scheduler
        for (Broker broker : brokerMap.values()) {
            addressesToDeploy.keySet().removeAll(broker.getQueueNames());
        }

        PriorityQueue<Broker> brokerByNumQueues = new PriorityQueue<>(brokerMap.size(), (a, b) -> {
            if (a.getNumQueues() < b.getNumQueues()) {
                return -1;
            } else if (a.getNumQueues() > b.getNumQueues()) {
                return 1;
            } else {
                return 0;
            }
        });

        brokerByNumQueues.addAll(brokerMap.values());

        for (Address address : addressesToDeploy.values()) {
            Broker broker = brokerByNumQueues.poll();
            broker.deployQueue(address.getAddress());
            brokerByNumQueues.offer(broker);
        }
    }

    private void distributeAddressesAll(String groupId, Set<Address> addresses) {
        for (Address address : addresses) {
            for (Broker  broker : brokerGroupMap.getOrDefault(groupId, Collections.emptyMap()).values()) {
                broker.deployQueue(address.getAddress());
            }
        }
    }

    private void deleteAddresses(String groupId, Set<Address> removed) {
        for (Broker broker : brokerGroupMap.getOrDefault(groupId, Collections.emptyMap()).values()) {
            for (Address address : removed) {
                broker.deleteQueue(address.getAddress());
            }
        }
    }
}
