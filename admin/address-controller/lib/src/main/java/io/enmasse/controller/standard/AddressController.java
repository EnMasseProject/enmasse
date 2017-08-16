package io.enmasse.controller.standard;

import enmasse.amqp.SyncRequestClient;
import io.enmasse.controller.common.*;
import io.enmasse.address.model.Address;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.Watch;
import io.enmasse.k8s.api.Watcher;
import io.fabric8.kubernetes.api.model.Pod;
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

import static io.enmasse.address.model.types.standard.StandardType.QUEUE;
import static io.enmasse.address.model.types.standard.StandardType.TOPIC;

/**
 * Controller for a single address space
 */
public class AddressController extends AbstractVerticle implements Watcher<Address> {
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    private final AddressApi addressApi;
    private final Kubernetes kubernetes;
    private final AddressClusterGenerator clusterGenerator;
    private Watch watch;

    public AddressController(AddressApi addressApi, Kubernetes kubernetes, AddressClusterGenerator clusterGenerator) {
        this.addressApi = addressApi;
        this.kubernetes = kubernetes;
        this.clusterGenerator = clusterGenerator;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                promise.complete(addressApi.watchAddresses(this));
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

    // TODO: Put this constant somewhere appropriate
    private static boolean isPooled(Address address) {
        return address.getPlan().getName().startsWith("pooled");
    }

    @Override
    public synchronized void resourcesUpdated(Set<Address> newAddressSet) throws Exception {
        log.info("Check address in address space controller: " + newAddressSet);

        Map<String, Set<Address>> addressByGroup = new LinkedHashMap<>();

        for (Address address : newAddressSet) {
            String key;
            if (isPooled(address)) {
                key = address.getPlan().getName();
            } else {
                key = address.getName();
            }

            if (!addressByGroup.containsKey(key)) {
                addressByGroup.put(key, new LinkedHashSet<>());
            }
            addressByGroup.get(key).add(address);
        }

        validateAddressGroups(addressByGroup);

        List<AddressCluster> clusterList = kubernetes.listClusters();
        log.info("Current set of clusters: " + clusterList);
        deleteBrokers(clusterList, addressByGroup);
        createBrokers(clusterList, addressByGroup);

        // Perform status check
        checkStatuses(newAddressSet);
        newAddressSet.forEach(addressApi::replaceAddress);
    }

    /*
     * Ensure that a address groups meet the criteria of all address sharing the same properties, until we can
     * support a mix.
     */
    private static void validateAddressGroups(Map<String, Set<Address>> addressByGroup) {
        for (Map.Entry<String, Set<Address>> entry : addressByGroup.entrySet()) {
            Iterator<Address> it = entry.getValue().iterator();
            Address first = it.next();
            while (it.hasNext()) {
                Address current = it.next();
                if (!first.getAddressSpace().equals(current.getAddressSpace()) ||
                        !first.getType().getName().equals(current.getType().getName()) ||
                        !first.getPlan().getName().equals(current.getPlan().getName())) {

                    throw new IllegalArgumentException("All address in a shared group must share the same properties. Found: " + current + " and " + first);
                }
            }
        }
    }

    private void createBrokers(List<AddressCluster> clusterList, Map<String, Set<Address>> newAddressGroups) {
        newAddressGroups.entrySet().stream()
                .filter(group -> !brokerExists(clusterList, group.getKey()))
                .map(group -> clusterGenerator.generateCluster(group.getKey(), group.getValue()))
                .forEach(cluster -> {
                    if (!cluster.getResources().getItems().isEmpty()) {
                        log.info("Creating cluster {}", cluster.getClusterId());
                        kubernetes.create(cluster.getResources());
                    }
                });
    }

    private boolean brokerExists(List<AddressCluster> clusterList, String clusterId) {
        for (AddressCluster existing : clusterList) {
            if (existing.getClusterId().equals(clusterId)) {
                return true;
            }
        }
        return false;
    }

    private void deleteBrokers(Collection<AddressCluster> clusterList, Map<String, Set<Address>> newAddressGroups) {
        clusterList.stream()
                .filter(cluster -> newAddressGroups.entrySet().stream()
                        .noneMatch(addressGroup -> cluster.getClusterId().equals(addressGroup.getKey())))
                .forEach(cluster -> {

                    log.info("Deleting cluster {}", cluster.getClusterId());
                    kubernetes.delete(cluster.getResources());
                });
    }

    private void checkStatuses(Set<Address> addresses) throws Exception {
        for (Address address : addresses) {
            address.getStatus().setReady(true).clearMessages();
        }
        // TODO: Instead of going to the routers directly, list routers, and perform a request against the
        // router agent to do the check
        for (Pod router : kubernetes.listRouters()) {
            if (router.getStatus().getPodIP() != null && !"".equals(router.getStatus().getPodIP())) {
                checkRouterStatus(router, addresses);
            }
        }

        for (Address address : addresses) {
            checkClusterStatus(address);
        }
    }

    private void checkClusterStatus(Address address) {
        String clusterName = isPooled(address) ? address.getPlan().getName() : address.getName();
        if (address.getType().equals(QUEUE) && !kubernetes.isDestinationClusterReady(clusterName)) {
            address.getStatus().setReady(false).appendMessage("Cluster is unavailable");
        }
    }


    private void checkRouterStatus(Pod router, Set<Address> addressList) throws Exception {

        List<String> addresses = checkRouter(router.getStatus().getPodIP(), "org.apache.qpid.dispatch.router.config.address", "prefix");
        List<String> autoLinks = checkRouter(router.getStatus().getPodIP(), "org.apache.qpid.dispatch.router.config.autoLink", "addr");
        List<String> linkRoutes = checkRouter(router.getStatus().getPodIP(), "org.apache.qpid.dispatch.router.config.linkRoute", "prefix");

        for (Address address : addressList) {
            if (!address.getType().equals(TOPIC)) {
                boolean found = addresses.contains(address.getAddress());
                if (!found) {
                    address.getStatus().setReady(false).appendMessage("Address " + address.getAddress() + " not found on " + router.getMetadata().getName());
                }
                if (address.getType().equals(QUEUE)) {
                    found = autoLinks.contains(address.getAddress());
                    if (!found) {
                        address.getStatus().setReady(false).appendMessage("Address " + address.getAddress() + " is missing autoLinks on " + router.getMetadata().getName());
                    }
                }
            } else {
                boolean found = linkRoutes.contains(address.getAddress());
                if (!found) {
                    address.getStatus().setReady(false).appendMessage("Address " + address.getAddress() + " is missing linkRoutes on " + router.getMetadata().getName());
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
