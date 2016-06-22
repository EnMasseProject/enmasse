package enmasse.rc.admin;

import com.openshift.restclient.model.IContainer;
import com.openshift.restclient.model.IPort;
import com.openshift.restclient.model.IReplicationController;
import enmasse.rc.generator.ConfigGenerator;
import enmasse.rc.model.Destination;
import enmasse.rc.model.LabelKeys;
import enmasse.rc.openshift.OpenshiftClient;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * The {@link BrokerManager} maintains the number of broker replication controllers to be consistent with the number of destinations in config that require store_and_forward.
 *
 * @author lulf
 */
public class BrokerManager {
    private static final Logger log = Logger.getLogger(BrokerManager.class.getName());

    private final OpenshiftClient openshiftClient;
    private final ConfigGenerator generator;

    public BrokerManager(OpenshiftClient openshiftClient, ConfigGenerator generator) {
        this.openshiftClient = openshiftClient;
        this.generator = generator;
    }

    public void destinationsUpdated(Collection<Destination> newDestinations) {
        List<IReplicationController> currentBrokers = openshiftClient.listBrokers();
        Collection<Destination> destinations = newDestinations.stream()
                .filter(Destination::storeAndForward)
                .collect(Collectors.toList());
        log.log(Level.INFO, "Brokers got updated to " + destinations.size() + " destinations, we have " + currentBrokers.size() + " destinations: " + currentBrokers.stream().map(IReplicationController::getName).toString());
        createBrokers(currentBrokers, destinations);
        deleteBrokers(currentBrokers, destinations);
        updateBrokers(currentBrokers, destinations);
    }

    private void createBrokers(Collection<IReplicationController> currentBrokers, Collection<Destination> newDestinations) {
        newDestinations.stream()
                .filter(broker -> !currentBrokers.stream().filter(controller -> broker.address().equals(controller.getLabels().get(LabelKeys.ADDRESS))).findAny().isPresent())
                .map(generator::generateBroker)
                .forEach(openshiftClient::createBroker);
    }

    private void deleteBrokers(Collection<IReplicationController> currentBrokers, Collection<Destination> newDestinations) {
        currentBrokers.stream()
                .filter(controller -> !newDestinations.stream().filter(broker -> broker.address().equals(controller.getLabels().get(LabelKeys.ADDRESS))).findAny().isPresent())
                .map(controller -> {
                    controller.setReplicas(0);
                    openshiftClient.updateBroker(controller);
                    return controller;
                })
                .forEach(openshiftClient::deleteBroker);
    }

    private void updateBrokers(Collection<IReplicationController> currentBrokers, Collection<Destination> newDestinations) {
        newDestinations.stream()
                .filter(broker -> currentBrokers.stream().filter(controller -> broker.address().equals(controller.getLabels().get(LabelKeys.ADDRESS))).findAny().isPresent())
                .map(generator::generateBroker)
                .forEach(this::brokerModified);
    }

    private void brokerModified(IReplicationController controller) {
        IReplicationController oldController = openshiftClient.getBroker(controller.getName());
        if (!equivalent(controller, oldController)) {
            log.log(Level.INFO, "Modifying replication controller " + controller.getName());
            oldController.setContainers(controller.getContainers());
            oldController.setReplicas(controller.getReplicas());
            oldController.setReplicaSelector(controller.getReplicaSelector());

            for (Map.Entry<String, String> label : controller.getLabels().entrySet()) {
                oldController.addLabel(label.getKey(), label.getValue());
            }
            openshiftClient.updateBroker(oldController);
        }
    }

    private static boolean equivalent(IReplicationController a, IReplicationController b) {
        return equivalent(a.getContainers(), b.getContainers())
            && a.getLabels().equals(b.getLabels())
            && a.getReplicaSelector().equals(b.getReplicaSelector());
    }

    private static boolean equivalent(Collection<IContainer> a, Collection<IContainer> b) {
        return a.size() == b.size() && equivalent(index(a), index(b));
    }

    private static boolean equivalent(Map<String, IContainer> a, Map<String, IContainer> b) {
        if (a.size() != b.size()) return false;
        for (String name : a.keySet()) {
            if (!equivalent(a.get(name), b.get(name))) {
                return false;
            }
        }
        return true;
    }

    private static boolean equivalent(IContainer a, IContainer b) {
        if (a == null) return b == null;
        else if (b != null) return false;
        else return a.getImage().equals(b.getImage())
                 && a.getEnvVars().equals(b.getEnvVars())
                 && equivalent(a.getPorts(), b.getPorts())
                 && a.getVolumeMounts().equals(b.getVolumeMounts());
    }

    private static boolean equivalent(Set<IPort> a, Set<IPort> b) {
        if (a.size() != b.size()) return false;
        else return index(a).equals(index(b));
    }

    private static Map<String, IContainer> index(Collection<IContainer> in) {
        return in.stream().collect(Collectors.toMap(c -> c.getName(), c -> c));
    }

    private static Map<Integer, String> index(Set<IPort> in) {
        return in.stream().collect(Collectors.toMap(p -> p.getContainerPort(), p -> p.getName()));
    }
}
