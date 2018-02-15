/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.AddressResolver;
import io.enmasse.amqp.SyncRequestClient;
import io.enmasse.address.model.Address;
import io.enmasse.k8s.api.*;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.proton.ProtonClientOptions;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.enmasse.controller.standard.ControllerKind.AddressSpace;
import static io.enmasse.controller.standard.ControllerKind.Broker;
import static io.enmasse.controller.standard.ControllerReason.*;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

/**
 * Controller for a single standard address space
 */
public class AddressController extends AbstractVerticle implements Watcher<Address> {
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    private final String addressSpaceName;
    private final AddressApi addressApi;
    private final Kubernetes kubernetes;
    private final AddressClusterGenerator clusterGenerator;
    private Watch watch;
    private final String certDir;
    private final EventLogger eventLogger;
    private final AddressResolver addressResolver;

    public AddressController(String addressSpaceName, AddressApi addressApi, AddressResolver addressResolver, Kubernetes kubernetes, AddressClusterGenerator clusterGenerator, String certDir, EventLogger eventLogger) {
        this.addressSpaceName = addressSpaceName;
        this.addressApi = addressApi;
        this.addressResolver = addressResolver;
        this.kubernetes = kubernetes;
        this.clusterGenerator = clusterGenerator;
        this.certDir = certDir;
        this.eventLogger = eventLogger;
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
        return address.getPlan().startsWith("pooled");
    }

    @Override
    public synchronized void resourcesUpdated(Set<Address> newAddressSet) throws Exception {
        log.info("Check addresses in address space controller: " + newAddressSet.stream().map(Address::getAddress).collect(Collectors.toList()));

        Map<String, Set<Address>> addressByGroup = new LinkedHashMap<>();
        for (Address address : newAddressSet) {
            String key;
            if (isPooled(address)) {
                key = address.getPlan();
            } else {
                key = address.getName();
            }

            if (!addressByGroup.containsKey(key)) {
                addressByGroup.put(key, new LinkedHashSet<>());
            }
            addressByGroup.get(key).add(address);
        }

        try {
            validateAddressGroups(addressByGroup);

            List<AddressCluster> clusterList = kubernetes.listClusters();
            log.debug("Current set of clusters: " + clusterList);
            deleteBrokers(clusterList, addressByGroup);
            createBrokers(clusterList, addressByGroup);

            // Perform status check
            checkStatuses(newAddressSet);
            for (Address address : newAddressSet) {
                try {
                    addressApi.replaceAddress(address);
                } catch (KubernetesClientException ex) {
                    log.warn("Error syncing address {}", address, ex);
                    eventLogger.log(AddressSyncFailed, "Error syncing address: " + ex.getMessage(), Warning, ControllerKind.Address, address.getName());
                }
            }
        } catch (Exception ex) {
            log.warn("Errror synchronizing addresses", ex);
            eventLogger.log(AddressSyncFailed, ex.getMessage(), Warning, AddressSpace, addressSpaceName);
        }
    }

    /*
     * Ensure that a address groups meet the criteria of all address sharing the same properties, until we can
     * support a mix.
     */
    private void validateAddressGroups(Map<String, Set<Address>> addressByGroup) {
        for (Map.Entry<String, Set<Address>> entry : addressByGroup.entrySet()) {
            Iterator<Address> it = entry.getValue().iterator();
            Address first = it.next();
            while (it.hasNext()) {
                Address current = it.next();
                addressResolver.validate(current);
                if (!first.getAddressSpace().equals(current.getAddressSpace()) ||
                        !first.getType().equals(current.getType()) ||
                        !first.getPlan().equals(current.getPlan())) {

                    throw new IllegalArgumentException("All address in a shared group must share the same properties. Found: " + current + " and " + first);
                }
            }
        }
    }

    private void createBrokers(List<AddressCluster> clusterList, Map<String, Set<Address>> newAddressGroups) {
        for (Map.Entry<String, Set<Address>> group : newAddressGroups.entrySet()) {
            if (!brokerExists(clusterList, group.getKey())) {
                try {
                    AddressCluster cluster = clusterGenerator.generateCluster(group.getKey(), group.getValue());
                    if (!cluster.getResources().getItems().isEmpty()) {
                        log.info("Creating broker cluster with id {}", cluster.getClusterId());
                        kubernetes.create(cluster.getResources());
                        eventLogger.log(BrokerCreated, "Created broker", Normal, Broker, cluster.getClusterId());
                    }
                } catch (Exception e) {
                    log.error("Error creating broker", e);
                    eventLogger.log(BrokerCreateFailed, "Error creating broker", Warning, Broker, group.getKey());
                }
            }
        }
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

                    log.info("Deleting broker cluster with id {}", cluster.getClusterId());
                    try {
                        kubernetes.delete(cluster.getResources());
                        eventLogger.log(BrokerDeleted, "Deleted broker", Normal, Broker, cluster.getClusterId());
                    } catch (Exception e) {
                        log.error("Error deleting broker", e);
                        eventLogger.log(BrokerDeleteFailed, "Error deleting broker", Warning, Broker, cluster.getClusterId());
                    }
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
        String clusterName = isPooled(address) ? address.getPlan() : address.getName();
        String addressType = address.getType();
        // TODO: Get rid of references to queue and topic
        if ((addressType.equals("queue") || addressType.equals("topic")) && !kubernetes.isDestinationClusterReady(clusterName)) {
            address.getStatus().setReady(false).appendMessage("Cluster is unavailable");
        }
    }


    private void checkRouterStatus(Pod router, Set<Address> addressList) throws Exception {

        int port = 0;
        for (Container container : router.getSpec().getContainers()) {
            if (container.getName().equals("router")) {
                for (ContainerPort containerPort : container.getPorts()) {
                    if (containerPort.getName().equals("amqps-normal")) {
                        port = containerPort.getContainerPort();
                    }
                }
            }
        }

        if (port != 0) {
            log.debug("Checking router status of router " + router.getStatus().getPodIP());
            ProtonClientOptions clientOptions = new ProtonClientOptions()
                    .setSsl(true)
                    .addEnabledSaslMechanism("EXTERNAL")
                    .setHostnameVerificationAlgorithm("")
                    .setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(new File(certDir, "ca.crt").getAbsolutePath()))
                    .setPemKeyCertOptions(new PemKeyCertOptions()
                            .setCertPath(new File(certDir, "tls.crt").getAbsolutePath())
                            .setKeyPath(new File(certDir, "tls.key").getAbsolutePath()));
            SyncRequestClient client = new SyncRequestClient(router.getStatus().getPodIP(), port, vertx, clientOptions);
            List<String> addresses = checkRouter(client,"org.apache.qpid.dispatch.router.config.address", "prefix");
            List<String> autoLinks = checkRouter(client, "org.apache.qpid.dispatch.router.config.autoLink", "addr");
            List<String> linkRoutes = checkRouter(client, "org.apache.qpid.dispatch.router.config.linkRoute", "prefix");

            for (Address address : addressList) {
                // TODO: Move these checks to agent
                if (!address.getType().equals("topic")) {
                    boolean found = addresses.contains(address.getAddress());
                    if (!found) {
                        address.getStatus().setReady(false).appendMessage("Address " + address.getAddress() + " not found on " + router.getMetadata().getName());
                    }
                    if (address.getType().equals("queue")) {
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
        } else {
            log.info("Unable to find appropriate router port, skipping address check");
        }
    }

    private List<String> checkRouter(SyncRequestClient client, String entityType, String attributeName) {
        Map<String, Object> properties = new LinkedHashMap<>();
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
            eventLogger.log(RouterCheckFailed, e.getMessage(), Warning, AddressSpace, addressSpaceName);
            return Collections.emptyList();
        }
    }
}
