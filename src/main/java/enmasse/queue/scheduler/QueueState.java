package enmasse.queue.scheduler;

import java.util.*;

/**
 * Contains the mapping from queue to broker and ensures there is only one modifying the state at a time.
 */
public class QueueState {
    private final Map<String, List<Broker>> brokerMap = new LinkedHashMap<>();
    private final Map<String, Set<String>> addressMap = new LinkedHashMap<>();

    public synchronized void groupUpdated(String groupId, Set<String> addresses) {
        Set<String> existing = addressMap.getOrDefault(groupId, Collections.emptySet());

        Set<String> removed = new HashSet<>(existing);
        removed.removeAll(addresses);
        if (!removed.isEmpty()) {
            deleteAddresses(groupId, removed);
        }

        Set<String> added = new HashSet<>(addresses);
        added.removeAll(existing);
        if (!added.isEmpty()) {
            addAddresses(groupId, addresses, added);
        }

        addressMap.put(groupId, addresses);
    }


    public synchronized void groupDeleted(String groupId) {
        // TODO: Close/free resources?
        addressMap.remove(groupId);
    }

    public synchronized void brokerAdded(String brokerId, Broker broker) {
        if (!brokerMap.containsKey(brokerId)) {
            brokerMap.put(brokerId, new ArrayList<>());
        }
        brokerMap.get(brokerId).add(broker);

        Set<String> addresses = addressMap.getOrDefault(brokerId, Collections.emptySet());
        if (addresses.size() == 1) {
            broker.deployQueue(addresses.iterator().next());
        } else {
            distributeAddressesByNumQueues(brokerId, addresses);
        }
    }

    public synchronized void brokerRemoved(String brokerId) {
        // TODO: Close resources etc?
        brokerMap.remove(brokerId);
    }

    private void addAddresses(String groupId, Set<String> addresses, Set<String> added) {

        // TODO: Fetch this information from somewhere, but assume > 1 address means shared flavor
        if (addresses.size() > 1) {
            distributeAddressesByNumQueues(groupId, added);
        } else {
            distributeAddressesAll(groupId, added);
        }
    }

    private void distributeAddressesByNumQueues(String groupId, Set<String> addresses) {
        List<Broker> brokerList = brokerMap.getOrDefault(groupId, Collections.emptyList());
        if (brokerList.isEmpty()) {
            return;
        }
        PriorityQueue<Broker> brokerByNumQueues = new PriorityQueue<>(brokerList.size(), (a, b) -> {
            if (a.numQueues() < b.numQueues()) {
                return -1;
            } else if (a.numQueues() > b.numQueues()) {
                return 1;
            } else {
                return 0;
            }
        });

        brokerByNumQueues.addAll(brokerList);

        for (String address : addresses) {
            Broker broker = brokerByNumQueues.poll();
            broker.deployQueue(address);
            brokerByNumQueues.offer(broker);
        }
    }

    private void distributeAddressesAll(String groupId, Set<String> addresses) {
        for (String address : addresses) {
            for (Broker  broker : brokerMap.getOrDefault(groupId, Collections.emptyList())) {
                broker.deployQueue(address);
            }
        }
    }

    private void deleteAddresses(String groupId, Set<String> removed) {
        for (Broker broker : brokerMap.getOrDefault(groupId, Collections.emptyList())) {
            for (String address : removed) {
                broker.deleteQueue(address);
            }
        }
    }
}
