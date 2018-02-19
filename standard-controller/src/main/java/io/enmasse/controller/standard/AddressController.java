/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.address.model.v1.SchemaProvider;
import io.enmasse.amqp.SyncRequestClient;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.*;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Pod;
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

import static io.enmasse.address.model.Status.Phase.Active;
import static io.enmasse.address.model.Status.Phase.Pending;
import static io.enmasse.address.model.Status.Phase.Terminating;
import static io.enmasse.controller.standard.ControllerKind.AddressSpace;
import static io.enmasse.controller.standard.ControllerReason.*;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

/**
 * Controller for a single standard address space
 */
public class AddressController extends AbstractVerticle implements Watcher<Address> {
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    private final String addressSpaceName;
    private final AddressApi addressApi;
    private final Kubernetes kubernetes;
    private final BrokerSetGenerator clusterGenerator;
    private Watch watch;
    private final String certDir;
    private final EventLogger eventLogger;
    private final SchemaProvider schemaProvider;

    public AddressController(String addressSpaceName, AddressApi addressApi, Kubernetes kubernetes, BrokerSetGenerator clusterGenerator, String certDir, EventLogger eventLogger, SchemaProvider schemaProvider) {
        this.addressSpaceName = addressSpaceName;
        this.addressApi = addressApi;
        this.kubernetes = kubernetes;
        this.clusterGenerator = clusterGenerator;
        this.certDir = certDir;
        this.eventLogger = eventLogger;
        this.schemaProvider = schemaProvider;
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
                watch = result.result();
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    @Override
    public synchronized void resourcesUpdated(Set<Address> addressSet) throws Exception {
        log.info("Check addresses in address space controller: " + addressSet.stream().map(Address::getAddress).collect(Collectors.toList()));

        Schema schema = schemaProvider.getSchema();
        AddressSpaceType addressSpaceType = schema.findAddressSpaceType("standard").orElseThrow(() -> new RuntimeException("Unable to start standard-controller: standard address space not found in schema!"));
        AddressResolver addressResolver = new AddressResolver(schema, addressSpaceType);
        AddressSpacePlan addressSpacePlan = addressSpaceType.getPlans().get(0);

        AddressProvisioner provisioner = new AddressProvisioner(addressResolver, addressSpacePlan, clusterGenerator, kubernetes, eventLogger);

        Map<String, Map<String, Double>> usageMap = provisioner.checkUsage(filterByNotPhases(addressSet, Arrays.asList(Pending)));
        Map<Address, Map<String, Double>> neededMap = provisioner.checkQuota(usageMap, filterByPhases(addressSet, Arrays.asList(Pending)));

        provisioner.provisionResources(usageMap, neededMap);

        checkStatuses(filterByPhases(addressSet, Arrays.asList(Status.Phase.Configuring, Status.Phase.Active)), addressResolver);
        for (Address address : filterByPhases(addressSet, Arrays.asList(Status.Phase.Configuring, Status.Phase.Active))) {
            if (address.getStatus().isReady()) {
                address.getStatus().setPhase(Active);
            }
        }

        deprovisionUnused(filterByNotPhases(addressSet, Arrays.asList(Terminating)));
        for (Address address : addressSet) {
            addressApi.replaceAddress(address);
        }
        garbageCollectTerminating(filterByPhases(addressSet, Arrays.asList(Status.Phase.Terminating)), addressResolver);

    }

    private void deprovisionUnused(Set<Address> addressSet) {
        List<AddressCluster> clusters = kubernetes.listClusters();
        log.info("Deprovisioning unused addresses in {} clusters", clusters.size());
        for (AddressCluster cluster : clusters) {
            int numFound = 0;
            log.info("Checking cluster with id {}", cluster.getClusterId());
            for (Address address : addressSet) {
                log.info("Checking cluster {} against address {}", cluster.getClusterId(), address);
                String brokerId = address.getAnnotations().get(AnnotationKeys.BROKER_ID);
                String clusterId = address.getAnnotations().get(AnnotationKeys.CLUSTER_ID);
                String name = address.getName();
                if (brokerId == null && name.equals(cluster.getClusterId())) {
                    numFound++;
                } else if (cluster.getClusterId().equals(clusterId)) {
                    numFound++;
                }
            }

            if (numFound == 0) {
                try {
                    kubernetes.delete(cluster.getResources());
                    eventLogger.log(ControllerReason.BrokerDeleted, "Deleted broker " + cluster.getClusterId(), EventLogger.Type.Normal, ControllerKind.Address, cluster.getClusterId());
                } catch (Exception e) {
                    log.warn("Error deleting cluster {}", cluster.getClusterId(), e);
                    eventLogger.log(ControllerReason.BrokerDeleteFailed, "Error deleting broker cluster " + cluster.getClusterId(), EventLogger.Type.Warning, ControllerKind.Address, cluster.getClusterId());
                }
            }
        }
    }

    private Set<Address> filterByPhases(Set<Address> addressSet, List<Status.Phase> phases) {
        return addressSet.stream()
                .filter(address -> phases.contains(address.getStatus().getPhase()))
                .collect(Collectors.toSet());
    }

    private Set<Address> filterByNotPhases(Set<Address> addressSet, List<Status.Phase> phases) {
        return addressSet.stream()
                .filter(address -> !phases.contains(address.getStatus().getPhase()))
                .collect(Collectors.toSet());
    }

    private void garbageCollectTerminating(Set<Address> addresses, AddressResolver addressResolver) throws Exception {
        Map<Address, Integer> okMap = checkStatuses(addresses, addressResolver);
        for (Map.Entry<Address, Integer> entry : okMap.entrySet()) {
            if (entry.getValue() == 0) {
                log.info("Garbage collecting {}", entry.getKey());
                addressApi.deleteAddress(entry.getKey());
            }
        }
    }

    private Map<Address, Integer> checkStatuses(Set<Address> addresses, AddressResolver addressResolver) throws Exception {
        Map<Address, Integer> numOk = new HashMap<>();
        for (Address address : addresses) {
            address.getStatus().setReady(true).clearMessages();
        }
        // TODO: Instead of going to the routers directly, list routers, and perform a request against the
        // router agent to do the check
        for (Pod router : kubernetes.listRouters()) {
            if (router.getStatus().getPodIP() != null && !"".equals(router.getStatus().getPodIP())) {
                checkRouterStatus(router, addresses, numOk, addressResolver);
            }
        }

        for (Address address : addresses) {
            AddressType addressType = addressResolver.getType(address);
            AddressPlan addressPlan = addressResolver.getPlan(addressType, address);
            numOk.put(address, checkClusterStatus(address, addressPlan) + numOk.getOrDefault(address, 0));
        }

        return numOk;
    }

    private int checkClusterStatus(Address address, AddressPlan addressPlan) {
        int numOk = 0;
        String clusterId = isPooled(addressPlan) ? "broker" : address.getName();
        String addressType = address.getType();
        // TODO: Get rid of references to queue and topic
        if ((addressType.equals("queue") || addressType.equals("topic")) && !kubernetes.isDestinationClusterReady(clusterId)) {
            address.getStatus().setReady(false).appendMessage("Cluster " + clusterId + " is unavailable");
        } else {
            numOk++;
        }
        return numOk;
    }

    private boolean isPooled(AddressPlan plan) {
        for (ResourceRequest request : plan.getRequiredResources()) {
            if ("broker".equals(request.getResourceName()) && request.getAmount() < 1.0) {
                return true;
            }
        }
        return false;
    }


    private void checkRouterStatus(Pod router, Set<Address> addressList, Map<Address, Integer> okMap, AddressResolver addressResolver) throws Exception {

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
                AddressType addressType = addressResolver.getType(address);
                AddressPlan addressPlan = addressResolver.getPlan(addressType, address);
                // TODO: Move these checks to agent
                if (!address.getType().equals("topic")) {
                    boolean found = addresses.contains(address.getAddress());
                    if (!found) {
                        address.getStatus().setReady(false).appendMessage("Address " + address.getAddress() + " not found on " + router.getMetadata().getName());
                    } else {
                        okMap.put(address, checkClusterStatus(address, addressPlan) + okMap.getOrDefault(address, 0));
                    }
                    if (address.getType().equals("queue")) {
                        found = autoLinks.contains(address.getAddress());
                        if (!found) {
                            address.getStatus().setReady(false).appendMessage("Address " + address.getAddress() + " is missing autoLinks on " + router.getMetadata().getName());
                        } else {
                            okMap.put(address, checkClusterStatus(address, addressPlan) + okMap.getOrDefault(address, 0));
                        }
                    }
                } else {
                    boolean found = linkRoutes.contains(address.getAddress());
                    if (!found) {
                        address.getStatus().setReady(false).appendMessage("Address " + address.getAddress() + " is missing linkRoutes on " + router.getMetadata().getName());
                    } else {
                        okMap.put(address, checkClusterStatus(address, addressPlan) + okMap.getOrDefault(address, 0));
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
