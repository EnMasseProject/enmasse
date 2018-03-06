/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
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
        if (addressSpaceType.getPlans().isEmpty()) {
            log.info("No address space plan available");
            return;
        }
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
        for (AddressCluster cluster : clusters) {
            int numFound = 0;
            for (Address address : addressSet) {
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
                    eventLogger.log(ControllerReason.BrokerDeleteFailed, "Error deleting broker cluster " + cluster.getClusterId() + ": " + e.getMessage(), EventLogger.Type.Warning, ControllerKind.Address, cluster.getClusterId());
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
        RouterStatusCollector routerStatusCollector = new RouterStatusCollector(vertx, certDir);
        List<RouterStatus> routerStatusList = new ArrayList<>();
        for (Pod router : kubernetes.listRouters()) {
            if (router.getStatus().getPodIP() != null && !"".equals(router.getStatus().getPodIP())) {
                try {
                    RouterStatus routerStatus = routerStatusCollector.collect(router);
                    if (routerStatus != null) {
                        routerStatusList.add(routerStatus);
                    }
                } catch (Exception e) {
                    log.info("Error requesting router status from {}. Ignoring", router.getMetadata().getName(), e);
                    eventLogger.log(RouterCheckFailed, e.getMessage(), Warning, AddressSpace, addressSpaceName);
                }
            }
        }

        Map<String, Integer> clusterOk = new HashMap<>();
        for (Address address : addresses) {
            AddressType addressType = addressResolver.getType(address);
            AddressPlan addressPlan = addressResolver.getPlan(addressType, address);

            int ok = 0;
            switch (addressType.getName()) {
                case "queue":
                    ok += checkBrokerStatus(address, clusterOk, addressPlan);
                    for (RouterStatus routerStatus : routerStatusList) {
                        ok += routerStatus.checkAddress(address);
                        ok += routerStatus.checkAutoLinks(address);
                    }
                    ok += RouterStatus.checkActiveAutoLink(address, routerStatusList);
                    break;
                case "topic":
                    ok += checkBrokerStatus(address, clusterOk, addressPlan);
                    for (RouterStatus routerStatus : routerStatusList) {
                        ok += routerStatus.checkLinkRoutes(address);
                    }
                    if (isPooled(addressPlan)) {
                        ok += RouterStatus.checkActiveLinkRoute(address, routerStatusList);
                    } else {
                        ok += RouterStatus.checkConnection(address, routerStatusList);
                    }
                    break;
                case "anycast":
                case "multicast":
                    for (RouterStatus routerStatus : routerStatusList) {
                        ok += routerStatus.checkAddress(address);
                    }
                    break;
            }
            numOk.put(address, ok);
        }

        return numOk;
    }

    private int checkBrokerStatus(Address address, Map<String, Integer> clusterOk, AddressPlan addressPlan) {
        String clusterId = isPooled(addressPlan) ? "broker" : address.getName();
        if (!clusterOk.containsKey(clusterId)) {
            if (!kubernetes.isDestinationClusterReady(clusterId)) {
                address.getStatus().setReady(false).appendMessage("Cluster " + clusterId + " is unavailable");
                clusterOk.put(clusterId, 0);
            } else {
                clusterOk.put(clusterId, 1);
            }
        }
        return clusterOk.get(clusterId);
    }

    private boolean isPooled(AddressPlan plan) {
        for (ResourceRequest request : plan.getRequiredResources()) {
            if ("broker".equals(request.getResourceName()) && request.getAmount() < 1.0) {
                return true;
            }
        }
        return false;
    }


}
