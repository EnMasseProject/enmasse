package enmasse.controller.address;

import enmasse.amqp.SyncRequestClient;
import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.common.*;
import enmasse.controller.model.Destination;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Controller for a single address space
 */
public class AddressSpaceController extends AbstractVerticle implements Watcher<Destination> {
    private static final Logger log = LoggerFactory.getLogger(AddressSpaceController.class);
    private final DestinationApi destinationApi;
    private final Kubernetes kubernetes;
    private final DestinationClusterGenerator clusterGenerator;
    private Watch watch;

    public AddressSpaceController(DestinationApi destinationApi, Kubernetes kubernetes, OpenShiftClient client, DestinationClusterGenerator clusterGenerator) {
        this.destinationApi = destinationApi;
        this.kubernetes = kubernetes;
        this.clusterGenerator = clusterGenerator;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                promise.complete(destinationApi.watchDestinations(this));
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                this.watch = result.result();
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        vertx.executeBlocking(promise -> {
            try {
                if (watch != null) {
                    watch.close();
                }
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }, result -> {
            if (result.succeeded()) {
                stopFuture.complete();
            } else {
                stopFuture.fail(result.cause());
            }
        });
    }

    @Override
    public synchronized void resourcesUpdated(Set<Destination> newDestinations) throws Exception {
        log.debug("Check destinations in address space controller: " + newDestinations);

        Map<String, Set<Destination>> destinationByGroup = newDestinations.stream().collect(Collectors.groupingBy(Destination::group, Collectors.toSet()));
        validateDestinationGroups(destinationByGroup);

        List<DestinationCluster> clusterList = kubernetes.listClusters();
        log.debug("Current set of clusters: " + clusterList);
        deleteBrokers(clusterList, destinationByGroup);
        createBrokers(clusterList, destinationByGroup);


        checkStatuses(newDestinations);
        newDestinations.forEach(destinationApi::replaceDestination);
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

    private void createBrokers(List<DestinationCluster> clusterList, Map<String, Set<Destination>> newDestinationGroups) {
        newDestinationGroups.entrySet().stream()
                .filter(group -> !brokerExists(clusterList, group.getKey()))
                .map(group -> clusterGenerator.generateCluster(group.getValue()))
                .forEach(cluster -> {
                    if (!cluster.getResources().getItems().isEmpty()) {
                        log.info("Creating cluster {}", cluster.getClusterId());
                        kubernetes.create(cluster.getResources());
                    }
                });
    }

    private boolean brokerExists(List<DestinationCluster> clusterList, String clusterId) {
        for (DestinationCluster existing : clusterList) {
            if (existing.getClusterId().equals(clusterId)) {
                return true;
            }
        }
        return false;
    }

    private void deleteBrokers(Collection<DestinationCluster> clusterList, Map<String, Set<Destination>> newDestinationGroups) {
        clusterList.stream()
                .filter(cluster -> newDestinationGroups.entrySet().stream()
                        .noneMatch(destinationGroup -> cluster.getClusterId().equals(destinationGroup.getKey())))
                .forEach(cluster -> {

                    log.info("Deleting cluster {}", cluster.getClusterId());
                    kubernetes.delete(cluster.getResources());
                });
    }

    private void checkStatuses(Set<Destination> destinations) throws Exception {
        for (Destination destination : destinations) {
            destination.status().setReady(true).clearMessages();
        }
        // TODO: Instead of going to the routers directly, list routers, and perform a request against the
        // router agent to do the check
        for (Pod router : kubernetes.listRouters()) {
            if (router.getStatus().getPodIP() != null && !"".equals(router.getStatus().getPodIP())) {
                checkRouterStatus(router, destinations);
            }
        }

        for (Destination destination : destinations) {
            checkClusterStatus(destination);
        }
    }

    private void checkClusterStatus(Destination destination) {
        if (destination.storeAndForward() && !kubernetes.isDestinationClusterReady(destination.group())) {
            destination.status().setReady(false).appendMessage("Cluster is unavailable");
        }
    }


    private void checkRouterStatus(Pod router, Set<Destination> destinations) throws Exception {

        List<String> addresses = checkRouter(router.getStatus().getPodIP(), "org.apache.qpid.dispatch.router.config.address", "prefix");
        List<String> autoLinks = checkRouter(router.getStatus().getPodIP(), "org.apache.qpid.dispatch.router.config.autoLink", "addr");
        List<String> linkRoutes = checkRouter(router.getStatus().getPodIP(), "org.apache.qpid.dispatch.router.config.linkRoute", "prefix");

        for (Destination destination : destinations) {
            if (!(destination.storeAndForward() && destination.multicast())) {
                boolean found = addresses.contains(destination.address());
                if (!found) {
                    destination.status().setReady(false).appendMessage("Address " + destination.address() + " not found on " + router.getMetadata().getName());
                }
                if (destination.storeAndForward()) {
                    found = autoLinks.contains(destination.address());
                    if (!found) {
                        destination.status().setReady(false).appendMessage("Address " + destination.address() + " is missing autoLinks on " + router.getMetadata().getName());
                    }
                }
            } else {
                boolean found = linkRoutes.contains(destination.address());
                if (!found) {
                    destination.status().setReady(false).appendMessage("Address " + destination.address() + " is missing linkRoutes on " + router.getMetadata().getName());
                }
            }
        }
    }

    private List<String> checkRouter(String hostname, String entityType, String attributeName) {
        SyncRequestClient client = new SyncRequestClient(hostname, 5672, vertx);
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("operation", "QUERY");
        properties.put("entityType", entityType);
        Map body = new LinkedHashMap<>();

        body.put("attributeNames", Arrays.asList(attributeName));

        Message message = Proton.message();
        message.setAddress("$management");
        message.setApplicationProperties(new ApplicationProperties(properties));
        message.setBody(new AmqpValue(body));

        try {
            Message response = client.request(message, 10, TimeUnit.SECONDS);
            AmqpValue value = (AmqpValue) response.getBody();
            Map values = (Map) value.getValue();
            List<List<String>> results = (List<List<String>>) values.get("results");
            return results.stream().map(l -> l.get(0)).collect(Collectors.toList());
        } catch (Exception e) {
            log.info("Error requesting router status. Ignoring", e);
            return Collections.emptyList();
        }
    }
}
