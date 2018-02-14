/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.queue.scheduler;

import io.enmasse.address.model.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contains the mapping from queue to broker and ensures there is only one modifying the state at a time.
 */
public class SchedulerState implements StateListener {
    private static final Logger log = LoggerFactory.getLogger(SchedulerState.class.getName());
    private final Map<String, Map<String, Broker>> brokerGroupMap = new LinkedHashMap<>();
    private final Map<String, Set<Address>> addressMap = new LinkedHashMap<>();
    private final StateListener chainedListener;

    public SchedulerState(StateListener chainedListener) {
        this.chainedListener = chainedListener;
    }

    public SchedulerState() {
        this(null);
    }

    public synchronized void addressesChanged(Map<String, Set<Address>> updatedMap) throws TimeoutException {
        Set<String> removedGroups = new HashSet<>(addressMap.keySet());
        removedGroups.removeAll(updatedMap.keySet());
        removedGroups.forEach(addressMap::remove);

        for (Map.Entry<String, Set<Address>> entry : updatedMap.entrySet()) {
            groupUpdated(entry.getKey(), entry.getValue());
        }
        if (chainedListener != null) {
            chainedListener.addressesChanged(updatedMap);
        }
    }

    private synchronized void groupUpdated(String groupId, Set<Address> addresses) throws TimeoutException {
        Set<Address> existing = addressMap.getOrDefault(groupId, Collections.emptySet());

        Set<Address> removed = new HashSet<>(existing);
        removed.removeAll(addresses);
        if (!removed.isEmpty()) {
            log.info("Removing addresses for {}: {}", groupId, removed);
            deleteAddresses(groupId, removed);
        }

        Set<Address> added = new HashSet<>(addresses);
        added.removeAll(existing);
        if (!added.isEmpty()) {
            log.info("Adding addresses for {}: {}", groupId, added);
            addAddresses(groupId, addresses, added);
        }

        addressMap.put(groupId, addresses);
        log.info("Updated addresses for {}", groupId);
    }


    public synchronized void brokerAdded(String groupId, String brokerId, Broker broker) throws TimeoutException {
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
            deployQueue(broker, addresses.iterator().next().getAddress());
        } else {
            distributeAddressesByNumQueues(groupId, addresses);
        }
        if (chainedListener != null) {
            chainedListener.brokerAdded(groupId, brokerId, broker);
        }
    }

    private static void deployQueue(Broker broker, String address) throws TimeoutException {
        broker.createQueue(address);
    }

    private static void deleteQueue(Broker broker, String address) throws TimeoutException {
        broker.deleteQueue(address);
    }

    public synchronized void brokerRemoved(String groupId, String brokerId) throws TimeoutException {
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
        if (chainedListener != null) {
            chainedListener.brokerRemoved(groupId, brokerId);
        }
    }

    private void addAddresses(String groupId, Set<Address> addresses, Set<Address> added) throws TimeoutException {

        // TODO: Fetch this information from somewhere, but assume > 1 address means shared flavor
        if (addresses.size() > 1) {
            distributeAddressesByNumQueues(groupId, added);
        } else {
            distributeAddressesAll(groupId, added);
        }
    }

    private void distributeAddressesByNumQueues(String groupId, Set<Address> addresses) throws TimeoutException {
        Map<String, Broker> brokerMap = brokerGroupMap.get(groupId);
        if (brokerMap == null) {
            return;
        }


        Map<String, Address> addressesToDeploy = addresses.stream().collect(Collectors.toMap(Address::getAddress, Function.identity()));

        // Remove addresses that are already distributed. This is to avoid changes in broker list to affect where queues are scheduler

        List<BrokerInfo> brokerInfos = new ArrayList<>();
        for (Map.Entry<String, Broker> entry : brokerMap.entrySet()) {
            BrokerInfo brokerInfo = new BrokerInfo(entry.getKey(), entry.getValue(), entry.getValue().getQueueNames());
            addressesToDeploy.keySet().removeAll(brokerInfo.queueNames);
            brokerInfos.add(brokerInfo);
        }

        PriorityQueue<BrokerInfo> brokerByNumQueues = new PriorityQueue<>(brokerInfos.size(), (a, b) -> {
            if (a.queueNames.size() < b.queueNames.size()) {
                return -1;
            } else if (a.queueNames.size() > b.queueNames.size()) {
                return 1;
            } else {
                return 0;
            }
        });

        brokerByNumQueues.addAll(brokerInfos);

        for (Address address : addressesToDeploy.values()) {
            BrokerInfo brokerInfo = brokerByNumQueues.poll();
            Broker broker = brokerInfo.broker;
            deployQueue(broker, address.getAddress());
            brokerInfo.queueNames.add(address.getAddress());
            brokerByNumQueues.offer(brokerInfo);
        }
    }

    private static class BrokerInfo {
        final String brokerId;
        final Broker broker;
        final Set<String> queueNames;

        private BrokerInfo(String brokerId, Broker broker, Set<String> queueNames) {
            this.brokerId = brokerId;
            this.broker = broker;
            this.queueNames = new HashSet<>(queueNames);
        }
    }

    private void distributeAddressesAll(String groupId, Set<Address> addresses) throws TimeoutException {
        for (Address address : addresses) {
            for (Broker  broker : brokerGroupMap.getOrDefault(groupId, Collections.emptyMap()).values()) {
                deployQueue(broker, address.getAddress());
            }
        }
    }

    private void deleteAddresses(String groupId, Set<Address> removed) throws TimeoutException {
        for (Broker broker : brokerGroupMap.getOrDefault(groupId, Collections.emptyMap()).values()) {
            for (Address address : removed) {
                deleteQueue(broker, address.getAddress());
            }
        }
    }
}
