/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.*;
import io.fabric8.kubernetes.api.model.Pod;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static io.enmasse.address.model.Status.Phase.*;
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
    private final Duration recheckInterval;
    private final Duration resyncInterval;

    public AddressController(String addressSpaceName, AddressApi addressApi, Kubernetes kubernetes, BrokerSetGenerator clusterGenerator, String certDir, EventLogger eventLogger, SchemaProvider schemaProvider, Duration recheckInterval, Duration resyncInterval) {
        this.addressSpaceName = addressSpaceName;
        this.addressApi = addressApi;
        this.kubernetes = kubernetes;
        this.clusterGenerator = clusterGenerator;
        this.certDir = certDir;
        this.eventLogger = eventLogger;
        this.schemaProvider = schemaProvider;
        this.recheckInterval = recheckInterval;
        this.resyncInterval = resyncInterval;
    }

    @Override
    public void start(Future<Void> startPromise) {
        vertx.executeBlocking((Future<Watch> promise) -> {
            try {
                ResourceChecker<Address> checker = new ResourceChecker<Address>(this, recheckInterval);
                checker.start();
                promise.complete(addressApi.watchAddresses(checker, resyncInterval));
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
    public void onUpdate(Set<Address> addressSet) throws Exception {
        long start = System.nanoTime();
        Schema schema = schemaProvider.getSchema();
        if (schema == null) {
            log.info("No schema available");
            return;
        }
        AddressSpaceType addressSpaceType = schema.findAddressSpaceType("standard").orElseThrow(() -> new RuntimeException("Unable to start standard-controller: standard address space not found in schema!"));
        AddressResolver addressResolver = new AddressResolver(schema, addressSpaceType);
        if (addressSpaceType.getPlans().isEmpty()) {
            log.info("No address space plan available");
            return;
        }

        Map<String, Status> previousStatus = new HashMap<>();
        for (Address address : addressSet) {
            previousStatus.put(address.getAddress(), new Status(address.getStatus()));
        }

        AddressSpacePlan addressSpacePlan = addressSpaceType.getPlans().get(0);

        long resolvedPlan = System.nanoTime();
        AddressProvisioner provisioner = new AddressProvisioner(addressResolver, addressSpacePlan, clusterGenerator, kubernetes, eventLogger);

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
        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, pendingAddresses);

        log.info("Usage: {}, Needed: {}", usageMap, neededMap);

        long checkedQuota = System.nanoTime();

        List<BrokerCluster> clusterList = kubernetes.listClusters();
        RouterCluster routerCluster = kubernetes.getRouterCluster();
        long listClusters = System.nanoTime();

        provisioner.provisionResources(routerCluster, clusterList, neededMap, pendingAddresses);

        long provisionResources = System.nanoTime();

        Map<Address, List<StatusCheckResult>> statusChecks = checkStatuses(filterByPhases(addressSet, Arrays.asList(Status.Phase.Configuring, Status.Phase.Active, Status.Phase.Terminating)), addressResolver);
        long checkStatuses = System.nanoTime();
        for (Address address : filterByPhases(addressSet, Arrays.asList(Status.Phase.Configuring, Status.Phase.Active))) {
            if (address.getStatus().isReady()) {
                address.getStatus().setPhase(Active);
            }
        }

        deprovisionUnused(clusterList, filterByNotPhases(addressSet, Arrays.asList(Terminating)));
        long deprovisionUnused = System.nanoTime();

        for (Address address : addressSet) {
            if (statusChecks.containsKey(address)) {
                for (StatusCheckResult result : statusChecks.get(address)) {
                    if (!result.isSuccess()) {
                        address.getStatus().setReady(false).appendMessage(result.getMessage());
                    }
                }
            }
            if (!previousStatus.get(address.getAddress()).equals(address.getStatus())) {
                addressApi.replaceAddress(address);
            }
        }

        long replaceAddresses = System.nanoTime();
        garbageCollectTerminating(statusChecks, addressResolver);
        long gcTerminating = System.nanoTime();
        log.info("total: {} ns, resolvedPlan: {} ns, calculatedUsage: {} ns, checkedQuota: {} ns, listClusters: {} ns, provisionResources: {} ns, checkStatuses: {} ns, deprovisionUnused: {} ns, replaceAddresses: {} ns, gcTerminating: {} ns", gcTerminating - start, resolvedPlan - start, calculatedUsage - resolvedPlan,  checkedQuota  - calculatedUsage, listClusters - checkedQuota, provisionResources - listClusters, checkStatuses - provisionResources, deprovisionUnused - checkStatuses, replaceAddresses - deprovisionUnused, gcTerminating - replaceAddresses);

    }

    private void deprovisionUnused(List<BrokerCluster> clusters, Set<Address> addressSet) {
        for (BrokerCluster cluster : clusters) {
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

    private void garbageCollectTerminating(Map<Address, List<StatusCheckResult>> statusChecks, AddressResolver addressResolver) throws Exception {
        for (Map.Entry<Address, List<StatusCheckResult>> entry : statusChecks.entrySet()) {
            Address address = entry.getKey();
            if (address.getStatus().getPhase().equals(Terminating)) {
                List<StatusCheckResult> passingChecks = entry.getValue().stream()
                        .filter(StatusCheckResult::isSuccess)
                        .collect(Collectors.toList());

                AddressType addressType = addressResolver.getType(address);
                AddressPlan addressPlan = addressResolver.getPlan(addressType, address);

                if (canTerminate(statusChecks.keySet(), addressPlan, passingChecks, addressResolver)) {
                    log.info("Garbage collecting {}", entry.getKey());
                    addressApi.deleteAddress(entry.getKey());
                } else {
                    log.info("Not garbage collecting {} as it has {} passing status checks", entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private boolean canTerminate(Set<Address> addressSet, AddressPlan addressPlan, List<StatusCheckResult> passingChecks, AddressResolver addressResolver) {
        if (passingChecks.isEmpty()) {
            return true;
        } else if (isPooled(addressPlan)) {

            for (StatusCheckResult result : passingChecks) {
                if (!result.allowSuccessForPooled()) {
                    return false;
                }
            }
            return containsPooled(filterByNotPhases(addressSet, Arrays.asList(Terminating)), addressResolver);
        }
        return false;
    }

    private boolean containsPooled(Set<Address> addressSet, AddressResolver addressResolver) {
        for (Address address : addressSet) {
            AddressType type = addressResolver.getType(address);
            AddressPlan plan = addressResolver.getPlan(type, address);
            if (isPooled(plan)) {
                return true;
            }
        }
        return false;
    }

    private Map<Address, List<StatusCheckResult>> checkStatuses(Set<Address> addresses, AddressResolver addressResolver) throws Exception {
        Map<Address, List<StatusCheckResult>> results = new HashMap<>();
        if (addresses.isEmpty()) {
            return results;
        }
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


        Map<String, StatusCheckResult> clusterStatusCache = new HashMap<>();
        for (Address address : addresses) {
            AddressType addressType = addressResolver.getType(address);
            AddressPlan addressPlan = addressResolver.getPlan(addressType, address);
            List<StatusCheckResult> statusCheckResults = new ArrayList<>();
            switch (addressType.getName()) {
                case "queue":
                    statusCheckResults.add(checkBrokerStatus(address, clusterStatusCache, addressPlan));
                    for (RouterStatus routerStatus : routerStatusList) {
                        statusCheckResults.add(routerStatus.checkAddress(address));
                        statusCheckResults.addAll(routerStatus.checkAutoLinks(address));
                    }
                    statusCheckResults.add(RouterStatus.checkActiveAutoLink(address, routerStatusList));
                    break;
                case "topic":
                    statusCheckResults.add(checkBrokerStatus(address, clusterStatusCache, addressPlan));
                    for (RouterStatus routerStatus : routerStatusList) {
                        statusCheckResults.addAll(routerStatus.checkLinkRoutes(address));
                    }
                    if (isPooled(addressPlan)) {
                        statusCheckResults.add(RouterStatus.checkActiveLinkRoute(address, routerStatusList));
                    } else {
                        statusCheckResults.add(RouterStatus.checkConnection(address, routerStatusList));
                    }
                    break;
                case "anycast":
                case "multicast":
                    for (RouterStatus routerStatus : routerStatusList) {
                        statusCheckResults.add(routerStatus.checkAddress(address));
                    }
                    break;
            }

            results.put(address, statusCheckResults);
        }

        return results;
    }


    private StatusCheckResult checkBrokerStatus(Address address, Map<String, StatusCheckResult> statusCache, AddressPlan addressPlan) {
        String clusterId = isPooled(addressPlan) ? "broker" : address.getName();
        if (!statusCache.containsKey(clusterId)) {
            if (!kubernetes.isDestinationClusterReady(clusterId)) {
                statusCache.put(clusterId, new StatusCheckResult(false, "Cluster " + clusterId + " is unavailable"));
            } else {
                statusCache.put(clusterId, new StatusCheckResult(true, true, null));
            }
        }
        return statusCache.get(clusterId);
    }

    private static boolean isPooled(AddressPlan plan) {
        for (ResourceRequest request : plan.getRequiredResources()) {
            if ("broker".equals(request.getResourceName()) && request.getAmount() < 1.0) {
                return true;
            }
        }
        return false;
    }


}
