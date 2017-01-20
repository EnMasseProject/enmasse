package enmasse.queue.scheduler;

import java.util.*;

/**
 * Contains the mapping from queue to broker and ensures there is only one modifying the state at a time.
 */
public class QueueState {
    private final Map<String, List<Broker>> brokerMap = new LinkedHashMap<>();
    private final Map<String, Set<String>> addressMap = new LinkedHashMap<>();

    public synchronized void deploymentUpdated(String deploymentId, Set<String> addresses) {
        Set<String> existing = addressMap.get(deploymentId);

        Set<String> removed = new HashSet<>(existing);
        removed.removeAll(addresses);
        if (!removed.isEmpty()) {
            deleteAddresses(deploymentId, removed);
        }

        Set<String> added = new HashSet<>(addresses);
        added.removeAll(existing);
        if (!added.isEmpty()) {
            addAddresses(deploymentId, addresses, added);
        }

        addressMap.put(deploymentId, addresses);
    }


    public synchronized void deploymentDeleted(String deploymentId) {
        // TODO: Close/free resources?
        addressMap.remove(deploymentId);
        brokerMap.remove(deploymentId);
    }

    public synchronized void brokerAdded(Broker broker) {
        String brokerId = broker.getId();
        if (!brokerMap.containsKey(brokerId)) {
            brokerMap.put(brokerId, new ArrayList<>());
        }
        brokerMap.get(brokerId).add(broker);

        Set<String> addresses = addressMap.get(brokerId);
        if (addresses != null) {
            if (addresses.size() > 1) {
                distributeAddressesByNumQueues(brokerId, addresses);
            } else {
                broker.deployQueue(addresses.iterator().next());
            }
        }
    }

    public synchronized void brokerRemoved(String brokerId) {
        // TODO: Close resources etc?
        brokerMap.remove(brokerId);
    }

    private void addAddresses(String deploymentId, Set<String> addresses, Set<String> added) {

        // TODO: Fetch this information from somewhere, but assume > 1 address means shared flavor
        if (addresses.size() > 1) {
            distributeAddressesByNumQueues(deploymentId, added);
        } else {
            distributeAddressesAll(deploymentId, added);
        }
    }

    private void distributeAddressesByNumQueues(String deploymentId, Set<String> addresses) {
        List<Broker> brokerList = brokerMap.get(deploymentId);
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

    private void distributeAddressesAll(String deploymentId, Set<String> addresses) {
        for (String address : addresses) {
            for (Broker  broker : brokerMap.get(deploymentId)) {
                broker.deployQueue(address);
            }
        }
    }

    private void deleteAddresses(String deploymentId, Set<String> removed) {
        for (Broker broker : brokerMap.get(deploymentId)) {
            for (String address : removed) {
                broker.deleteQueue(address);
            }
        }
    }
}
