/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.*;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static io.enmasse.address.model.Status.Phase.*;
import static io.enmasse.controller.standard.ControllerKind.AddressSpace;
import static io.enmasse.controller.standard.ControllerKind.Broker;
import static io.enmasse.controller.standard.ControllerReason.BrokerUpgraded;
import static io.enmasse.controller.standard.ControllerReason.RouterCheckFailed;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

/**
 * Controller for a single standard address space
 */
public class AddressController extends AbstractVerticle implements Watcher<Address> {
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    private final StandardControllerOptions options;
    private final AddressApi addressApi;
    private final Kubernetes kubernetes;
    private final BrokerSetGenerator clusterGenerator;
    private Watch watch;
    private final EventLogger eventLogger;
    private final SchemaProvider schemaProvider;

    public AddressController(StandardControllerOptions options, AddressApi addressApi, Kubernetes kubernetes, BrokerSetGenerator clusterGenerator, EventLogger eventLogger, SchemaProvider schemaProvider) {
        this.options = options;
        this.addressApi = addressApi;
        this.kubernetes = kubernetes;
        this.clusterGenerator = clusterGenerator;
        this.eventLogger = eventLogger;
        this.schemaProvider = schemaProvider;
    }

    @Override
    public void start(Future<Void> startPromise) {
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                ResourceChecker<Address> checker = new ResourceChecker<Address>(this, options.getRecheckInterval());
                checker.start();
                promise.complete(addressApi.watchAddresses(checker, options.getResyncInterval()));
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
    public void stop(Future<Void> stopPromise) throws Exception {
        if (watch != null) {
            watch.close();
        }
    }

    @Override
    public void onUpdate(List<Address> addressList) throws Exception {
        long start = System.nanoTime();
        Schema schema = schemaProvider.getSchema();
        if (schema == null) {
            log.info("No schema available");
            return;
        }
        AddressSpaceType addressSpaceType = schema.findAddressSpaceType("standard").orElseThrow(() -> new RuntimeException("Unable to handle updates: standard address space not found in schema!"));
        AddressResolver addressResolver = new AddressResolver(addressSpaceType);
        if (addressSpaceType.getPlans().isEmpty()) {
            log.info("No address space plan available");
            return;
        }

        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schema);

        // Migrate plan names
        Set<Address> addressSet = new HashSet<>();
        if (options.getVersion().startsWith("0.24")) {
            for (Address address : addressList) {
                if (addressResolver.findPlan(address).isPresent()) {
                    addressSet.add(address);
                } else {
                    Address.Builder builder = new Address.Builder(address);
                    String planName = address.getPlan();
                    if ("pooled-queue".equals(planName)) {
                        builder.setPlan("standard-small-queue");
                    } else if ("pooled-topic".equals(planName)) {
                        builder.setPlan("standard-small-topic");
                    } else if ("standard-anycast".equals(planName)) {
                        builder.setPlan("standard-small-anycast");
                    } else if ("standard-multicast".equals(planName)) {
                        builder.setPlan("standard-small-multicast");
                    } else if ("standard-subscription".equals(planName)) {
                        builder.setPlan("standard-small-subscription");
                    } else if ("sharded-queue".equals(planName)) {
                        builder.setPlan("standard-large-queue");
                    } else if ("sharded-topic".equals(planName)) {
                        builder.setPlan("standard-large-topic");
                    }
                    addressSet.add(builder.build());
                }
            }
        } else {
            addressSet = new HashSet<>(addressList);
        }

        Map<String, Status> previousStatus = new HashMap<>();
        for (Address address : addressSet) {
            previousStatus.put(address.getAddress(), new Status(address.getStatus()));
        }

        AddressSpacePlan addressSpacePlan = addressSpaceType.findAddressSpacePlan(options.getAddressSpacePlanName()).orElseThrow(() -> new RuntimeException("Unable to handle updates: address space plan " + options.getAddressSpacePlanName() + " not found!"));

        long resolvedPlan = System.nanoTime();

        AddressProvisioner provisioner = new AddressProvisioner(addressSpaceResolver, addressResolver, addressSpacePlan, clusterGenerator, kubernetes, eventLogger, options.getInfraUuid());

        Map<Status.Phase, Long> countByPhase = countPhases(addressSet);
        log.info("Total: {}, Active: {}, Configuring: {}, Pending: {}, Terminating: {}, Failed: {}", addressSet.size(), countByPhase.get(Active), countByPhase.get(Configuring), countByPhase.get(Pending), countByPhase.get(Terminating), countByPhase.get(Failed));
        if (countByPhase.get(Configuring) < 5) {
            log.debug("Addresses in configuring: {}", filterByPhases(addressSet, Arrays.asList(Configuring)));
        }
        if (countByPhase.get(Pending) < 5) {
            log.debug("Addresses in pending : {}", filterByPhases(addressSet, Arrays.asList(Pending)));
        }

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(filterByNotPhases(addressSet, Arrays.asList(Pending)));

        long calculatedUsage = System.nanoTime();
        Set<Address> pendingAddresses = filterByPhases(addressSet, Arrays.asList(Pending));
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, pendingAddresses, addressSet);

        log.info("Usage: {}, Needed: {}", usageMap, neededMap);

        long checkedQuota = System.nanoTime();

        List<BrokerCluster> clusterList = kubernetes.listClusters();
        RouterCluster routerCluster = kubernetes.getRouterCluster();
        long listClusters = System.nanoTime();

        provisioner.provisionResources(routerCluster, clusterList, neededMap, pendingAddresses);

        long provisionResources = System.nanoTime();

        checkStatuses(filterByPhases(addressSet, Arrays.asList(Status.Phase.Configuring, Status.Phase.Active)), addressResolver);
        long checkStatuses = System.nanoTime();
        for (Address address : filterByPhases(addressSet, Arrays.asList(Status.Phase.Configuring, Status.Phase.Active))) {
            if (address.getStatus().isReady()) {
                address.getStatus().setPhase(Active);
            }
        }

        deprovisionUnused(clusterList, filterByNotPhases(addressSet, Arrays.asList(Terminating)));
        long deprovisionUnused = System.nanoTime();

        StandardInfraConfig desiredConfig = (StandardInfraConfig) addressSpaceResolver.getInfraConfig("standard", addressSpacePlan.getMetadata().getName());
        upgradeClusters(desiredConfig, addressResolver, clusterList, filterByNotPhases(addressSet, Arrays.asList(Terminating)));

        long upgradeClusters = System.nanoTime();

        for (Address address : addressSet) {
            if (!previousStatus.get(address.getAddress()).equals(address.getStatus())) {
                addressApi.replaceAddress(address);
            }
        }

        long replaceAddresses = System.nanoTime();
        garbageCollectTerminating(filterByPhases(addressSet, Arrays.asList(Status.Phase.Terminating)), addressResolver);
        long gcTerminating = System.nanoTime();
        log.info("total: {} ns, resolvedPlan: {} ns, calculatedUsage: {} ns, checkedQuota: {} ns, listClusters: {} ns, provisionResources: {} ns, checkStatuses: {} ns, deprovisionUnused: {} ns, upgradeClusters: {} ns, replaceAddresses: {} ns, gcTerminating: {} ns", gcTerminating - start, resolvedPlan - start, calculatedUsage - resolvedPlan,  checkedQuota  - calculatedUsage, listClusters - checkedQuota, provisionResources - listClusters, checkStatuses - provisionResources, deprovisionUnused - checkStatuses, upgradeClusters - deprovisionUnused, replaceAddresses - upgradeClusters, gcTerminating - replaceAddresses);

    }

    private void upgradeClusters(StandardInfraConfig desiredConfig, AddressResolver addressResolver, List<BrokerCluster> clusterList, Set<Address> addresses) throws Exception {
        for (BrokerCluster cluster : clusterList) {
            StandardInfraConfig currentConfig = cluster.getInfraConfig();
            if (!desiredConfig.equals(currentConfig)) {
                if (options.getVersion().equals(desiredConfig.getSpec().getVersion())) {
                    if (currentConfig != null && currentConfig.getSpec().getBroker().getResources().getStorage().equals(desiredConfig.getSpec().getBroker().getResources().getStorage())) {
                        desiredConfig = new StandardInfraConfigBuilder(desiredConfig)
                                .editSpec()
                                .editBroker()
                                .editResources()
                                .withStorage(currentConfig.getSpec().getBroker().getResources().getStorage())
                                .endResources()
                                .endBroker()
                                .endSpec()
                                .build();
                    }
                    BrokerCluster upgradedCluster = null;
                    if (cluster.getClusterId().startsWith("broker-pooled")) {
                        upgradedCluster = clusterGenerator.generateCluster(cluster.getClusterId(), cluster.getNewReplicas(), null, null, desiredConfig);
                    } else {
                        Address address = addresses.stream()
                                .filter(a -> cluster.getClusterId().equals(a.getAnnotation(AnnotationKeys.CLUSTER_ID)))
                                .findFirst()
                                .orElse(null);
                        if (address != null) {
                            AddressPlan plan = addressResolver.getPlan(address);
                            upgradedCluster = clusterGenerator.generateCluster(cluster.getClusterId(), cluster.getNewReplicas(), address, plan, desiredConfig);
                        }
                    }
                    log.info("Upgrading broker {}", cluster.getClusterId());
                    cluster.updateResources(upgradedCluster, desiredConfig);
                    kubernetes.apply(cluster.getResources());
                    eventLogger.log(BrokerUpgraded, "Upgraded broker", Normal, Broker, cluster.getClusterId());
                } else {
                    log.info("Version of desired config ({}) does not match controller version ({}), skipping upgrade", desiredConfig.getSpec().getVersion(), options.getVersion());
                }
            }
        }
    }

    private void deprovisionUnused(List<BrokerCluster> clusters, Set<Address> addressSet) {
        for (BrokerCluster cluster : clusters) {
            int numFound = 0;
            for (Address address : addressSet) {
                String clusterId = address.getAnnotations().get(AnnotationKeys.CLUSTER_ID);
                if (cluster.getClusterId().equals(clusterId)) {
                    numFound++;
                }
            }

            if (numFound == 0) {
                try {
                    kubernetes.delete(cluster.getResources());
                    eventLogger.log(ControllerReason.BrokerDeleted, "Deleted broker " + cluster.getClusterId(), Normal, ControllerKind.Address, cluster.getClusterId());
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

    private Map<Status.Phase,Long> countPhases(Set<Address> addressSet) {
        Map<Status.Phase, Long> countMap = new HashMap<>();
        for (Status.Phase phase : Status.Phase.values()) {
            countMap.put(phase, 0L);
        }
        for (Address address : addressSet) {
            countMap.put(address.getStatus().getPhase(), 1 + countMap.get(address.getStatus().getPhase()));
        }
        return countMap;
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
        if (addresses.isEmpty()) {
            return numOk;
        }
        for (Address address : addresses) {
            address.getStatus().setReady(true).clearMessages();
        }
        // TODO: Instead of going to the routers directly, list routers, and perform a request against the
        // router agent to do the check
        RouterStatusCollector routerStatusCollector = new RouterStatusCollector(vertx, options.getCertDir());
        List<RouterStatus> routerStatusList = new ArrayList<>();
        for (Pod router : kubernetes.listRouters()) {
            if (Readiness.isPodReady(router)) {
                try {
                    RouterStatus routerStatus = routerStatusCollector.collect(router);
                    if (routerStatus != null) {
                        routerStatusList.add(routerStatus);
                    }
                } catch (Exception e) {
                    log.info("Error requesting router status from {}. Ignoring", router.getMetadata().getName(), e);
                    eventLogger.log(RouterCheckFailed, e.getMessage(), Warning, AddressSpace, options.getAddressSpace());
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
                    ok += checkBrokerStatus(address, clusterOk);
                    for (RouterStatus routerStatus : routerStatusList) {
                        ok += routerStatus.checkAddress(address);
                        ok += routerStatus.checkAutoLinks(address);
                    }
                    ok += RouterStatus.checkActiveAutoLink(address, routerStatusList);
                    break;
                case "topic":
                    ok += checkBrokerStatus(address, clusterOk);
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

    private int checkBrokerStatus(Address address, Map<String, Integer> clusterOk) {
        String clusterId = address.getAnnotation(AnnotationKeys.CLUSTER_ID);
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
            if ("broker".equals(request.getName()) && request.getCredit() < 1.0) {
                return true;
            }
        }
        return false;
    }


}
