package enmasse.storage.controller.admin;

import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The {@link ClusterManager} maintains the number of broker replication controllers to be consistent with the number of destinations in config that require store_and_forward.
 *
 * @author lulf
 */
public class ClusterManager {
    private static final Logger log = Logger.getLogger(ClusterManager.class.getName());

    private final OpenshiftClient openshiftClient;
    private final StorageGenerator generator;

    public ClusterManager(OpenshiftClient openshiftClient, StorageGenerator generator) {
        this.openshiftClient = openshiftClient;
        this.generator = generator;
    }

    public void destinationsUpdated(Collection<Destination> newDestinations) {
        List<StorageCluster> clusterList = openshiftClient.listClusters();
        Collection<Destination> destinations = newDestinations.stream()
                .filter(Destination::storeAndForward)
                .collect(Collectors.toList());
        log.log(Level.INFO, "Brokers got updated to " + destinations.size() + " destinations, we have " + clusterList.size() + " destinations: " + clusterList.stream().map(StorageCluster::getName).toString());
        createBrokers(clusterList, destinations);
        deleteBrokers(clusterList, destinations);
        updateBrokers(clusterList, destinations);
    }

    private static boolean brokerExists(Collection<StorageCluster> clusterList, Destination destination) {
        return clusterList.stream()
                .filter(cluster -> destination.address().equals(cluster.getAddress()))
                .findAny()
                .isPresent();
    }

    private void createBrokers(Collection<StorageCluster> clusterList, Collection<Destination> newDestinations) {
        newDestinations.stream()
                .filter(destination -> !brokerExists(clusterList, destination))
                .map(destination -> generator.generateStorage(destination))
                .forEach(StorageCluster::create);
    }



    private void deleteBrokers(Collection<StorageCluster> clusterList, Collection<Destination> newDestinations) {
        clusterList.stream()
                .filter(cluster -> !newDestinations.stream()
                        .filter(destination -> destination.address().equals(cluster.getAddress()))
                        .findAny()
                        .isPresent())
                .forEach(StorageCluster::delete);

    }

    private static class ClusterDestinationPair {
        final StorageCluster cluster;
        final Destination destination;
        public ClusterDestinationPair(StorageCluster cluster, Destination destination) {
            this.cluster = cluster;
            this.destination = destination;
        }
    }

    private void updateBrokers(Collection<StorageCluster> clusterList, Collection<Destination> newDestinations) {
        newDestinations.stream()
                .map(destination ->
                    findCluster(clusterList, destination)
                        .map(cluster -> new ClusterDestinationPair(cluster, destination)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(clusterDest -> {
                    StorageCluster newCluster = generator.generateStorage(clusterDest.destination);
                    clusterDest.cluster.update(newCluster);
                });
    }

    private Optional<StorageCluster> findCluster(Collection<StorageCluster> clusterList, Destination destination) {
        return clusterList.stream()
                .filter(cluster -> cluster.getAddress().equals(destination.address()))
                .findFirst();
    }
}
