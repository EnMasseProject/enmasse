package enmasse.controller.address;

import enmasse.config.LabelKeys;
import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.common.DestinationClusterGenerator;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.model.Destination;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for a single address space
 */
public class AddressSpaceController extends AbstractVerticle implements Watcher<ConfigMap> {
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    private final DestinationApi destinationApi;
    private final Kubernetes kubernetes;
    private final OpenShiftClient client;
    private final DestinationClusterGenerator clusterGenerator;
    private Watch addressWatch;

    public AddressSpaceController(DestinationApi destinationApi, Kubernetes kubernetes, OpenShiftClient client, DestinationClusterGenerator clusterGenerator) {
        this.destinationApi = destinationApi;
        this.kubernetes = kubernetes;
        this.client = client;
        this.clusterGenerator = clusterGenerator;
    }

    @Override
    public void start() {
        Map<String, String> labelMap = new HashMap<>();
        labelMap.put(LabelKeys.TYPE, "address-config");
        log.info("Starting address space controller verticle for " + kubernetes.getInstanceId());
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                promise.complete(startWatch(labelMap, kubernetes.getInstanceId().getNamespace()));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                addressWatch = result.result();
            } else {
                log.error("Error starting watch", result.cause());
            }
        });
    }

    private Watch startWatch(Map<String, String> labelMap, String namespace) {
        ConfigMapList list = client.configMaps().inNamespace(namespace).withLabels(labelMap).list();
        for (ConfigMap item : list.getItems()) {
            eventReceived(Action.ADDED, item);
        }
        return client.configMaps().withLabels(labelMap).withResourceVersion(list.getMetadata().getResourceVersion()).watch(this);
    }

    @Override
    public void stop() {
        if (addressWatch != null) {
            addressWatch.close();
        }
    }

    @Override
    public void eventReceived(Action action, ConfigMap configMap) {
        log.info("Got action " + action + " for config " + configMap.getMetadata().getName());
        switch (action) {
            case ADDED:
                updateDestinations();
                break;
            case DELETED:
                updateDestinations();
                break;
        }
    }

    private void updateDestinations() {

        List<DestinationCluster> clusterList = kubernetes.listClusters();
        Set<Destination> newDestinations = destinationApi.listDestinations();
        Map<String, Set<Destination>> destinationByGroup = newDestinations.stream().collect(Collectors.groupingBy(Destination::group, Collectors.toSet()));
        validateDestinationGroups(destinationByGroup);

        Set<Destination> currentDestinations = clusterList.stream().flatMap(cluster -> cluster.getDestinations().stream()).collect(Collectors.toSet());

        log.info("Destinations updated from " + currentDestinations + " to " + newDestinations);
        createBrokers(clusterList, destinationByGroup);
        deleteBrokers(clusterList, destinationByGroup);
    }

    /*
     * Ensure that a destination groups meet the criteria of all destinations sharing the same properties, until we can
     * support a mix.
     */
    private static void validateDestinationGroups(Map<String, Set<Destination>> destinationByGroup) {
        for (Map.Entry<String, Set<Destination>> entry : destinationByGroup.entrySet()) {
            Iterator<Destination> it = entry.getValue().iterator();
            Destination first = it.next();
            while (it.hasNext()) {
                Destination current = it.next();
                if (current.storeAndForward() != first.storeAndForward() ||
                    current.multicast() != first.multicast() ||
                    !current.flavor().equals(first.flavor()) ||
                    !current.group().equals(first.group())) {

                    throw new IllegalArgumentException("All destinations in a destination group must share the same properties. Found: " + current + " and " + first);
                }
            }
        }
    }

    private void createBrokers(Collection<DestinationCluster> clusterList, Map<String, Set<Destination>> newDestinationGroups) {
        newDestinationGroups.entrySet().stream()
                .map(group -> clusterGenerator.generateCluster(group.getValue()))
                .filter(cluster -> !clusterList.contains(cluster))
                .forEach(cluster -> kubernetes.create(cluster.getResources()));
    }

    private void deleteBrokers(Collection<DestinationCluster> clusterList, Map<String, Set<Destination>> newDestinationGroups) {
        clusterList.stream()
                .filter(cluster -> newDestinationGroups.entrySet().stream()
                        .noneMatch(destinationGroup -> cluster.getClusterId().equals(destinationGroup.getKey())))
                .forEach(cluster -> kubernetes.delete(cluster.getResources()));
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        if (cause != null) {
            log.info("Received onClose for instance config watcher", cause);
            stop();
            start();
        } else {
            log.info("Watch for instance configs force closed, stopping");
            addressWatch = null;
            stop();
        }
    }
}
