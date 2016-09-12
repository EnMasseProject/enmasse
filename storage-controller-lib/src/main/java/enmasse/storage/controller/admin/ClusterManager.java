package enmasse.storage.controller.admin;

import com.fasterxml.jackson.databind.JsonNode;
import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.parser.AddressConfigParser;
import enmasse.storage.controller.openshift.OpenshiftClient;
import enmasse.storage.controller.openshift.StorageCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@link ClusterManager} maintains the number of broker replication controllers to be consistent with the number of destinations in config that require store_and_forward.
 */
public class ClusterManager {
    private static final Logger log = LoggerFactory.getLogger(ClusterManager.class.getName());

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
        log.info("Brokers got updated to " + destinations.size() + " destinations, we have " + clusterList.size() + " destinations: " + clusterList.stream().map(StorageCluster::getDestination).collect(Collectors.toList()));
        createBrokers(clusterList, destinations);
        deleteBrokers(clusterList, destinations);
    }

    private static boolean brokerExists(Collection<StorageCluster> clusterList, Destination destination) {
        return clusterList.stream()
                .filter(cluster -> destination.equals(cluster.getDestination()))
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
                        .filter(destination -> destination.equals(cluster.getDestination()))
                        .findAny()
                        .isPresent())
                .forEach(StorageCluster::delete);

    }

    public void configUpdated(JsonNode jsonConfig) throws IOException {
        destinationsUpdated(AddressConfigParser.parse(jsonConfig).destinations());
    }
}
