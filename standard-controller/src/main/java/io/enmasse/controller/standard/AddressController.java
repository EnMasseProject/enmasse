/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.*;
import io.enmasse.metrics.api.*;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
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
public class AddressController implements Watcher<Address> {
    private static final Logger log = LoggerFactory.getLogger(AddressController.class);
    private final StandardControllerOptions options;
    private final AddressApi addressApi;
    private final Kubernetes kubernetes;
    private final BrokerSetGenerator clusterGenerator;
    private Watch watch;
    private final EventLogger eventLogger;
    private final SchemaProvider schemaProvider;
    private final Vertx vertx;
    private final Metrics metrics;
    private final BrokerIdGenerator brokerIdGenerator;
    private final BrokerClientFactory brokerClientFactory;

    public AddressController(StandardControllerOptions options, AddressApi addressApi, Kubernetes kubernetes, BrokerSetGenerator clusterGenerator, EventLogger eventLogger, SchemaProvider schemaProvider, Vertx vertx, Metrics metrics, BrokerIdGenerator brokerIdGenerator, BrokerClientFactory brokerClientFactory) {
        this.options = options;
        this.addressApi = addressApi;
        this.kubernetes = kubernetes;
        this.clusterGenerator = clusterGenerator;
        this.eventLogger = eventLogger;
        this.schemaProvider = schemaProvider;
        this.vertx = vertx;
        this.metrics = metrics;
        this.brokerIdGenerator = brokerIdGenerator;
        this.brokerClientFactory = brokerClientFactory;
    }

    public void start() throws Exception {
        ResourceChecker<Address> checker = new ResourceChecker<Address>(this, options.getRecheckInterval());
        checker.start();
        watch = addressApi.watchAddresses(checker, options.getResyncInterval());
    }

    public void stop() throws Exception {
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

        final Map<String, ProvisionState> previousStatus = addressList.stream()
                .collect(Collectors.toMap(Address::getName,
                                          a -> new ProvisionState(a.getStatus(),
                                                                  a.getAnnotation(AnnotationKeys.BROKER_ID),
                                                                  a.getAnnotation(AnnotationKeys.CLUSTER_ID),
                                                                  a.getAnnotation(AnnotationKeys.APPLIED_PLAN))));

        AddressSpacePlan addressSpacePlan = addressSpaceType.findAddressSpacePlan(options.getAddressSpacePlanName()).orElseThrow(() -> new RuntimeException("Unable to handle updates: address space plan " + options.getAddressSpacePlanName() + " not found!"));

        long resolvedPlan = System.nanoTime();

        AddressProvisioner provisioner = new AddressProvisioner(addressSpaceResolver, addressResolver, addressSpacePlan, clusterGenerator, kubernetes, eventLogger, options.getInfraUuid(), brokerIdGenerator);

        List<Status.Phase> readyPhases = Arrays.asList(Configuring, Active);
        Map<String, Address> validAddresses = new HashMap<>();
        for (Address address : addressList) {
            address.getStatus().clearMessages();
            if (readyPhases.contains(address.getStatus().getPhase())) {
                address.getStatus().setReady(true);
            }

            Address existing = validAddresses.get(address.getAddress());
            if (existing != null) {
                if (!address.getStatus().getPhase().equals(Pending) && existing.getStatus().getPhase().equals(Pending)) {
                    // If existing address is pending, and we are not pending, we take priority
                    String errorMessage = String.format("Address '%s' already exists with resource name '%s'", address.getAddress(), address.getName());
                    existing.getStatus().setPhase(Pending);
                    existing.getStatus().appendMessage(errorMessage);
                    validAddresses.put(address.getAddress(), address);
                } else {
                    // Existing address has already been accepted, or we are both pending, existing takes priority.
                    String errorMessage = String.format("Address '%s' already exists with resource name '%s'", address.getAddress(), existing.getName());
                    address.getStatus().setPhase(Pending);
                    address.getStatus().appendMessage(errorMessage);
                }
            } else {
                validAddresses.put(address.getAddress(), address);
            }
        }

        Set<Address> addressSet = new LinkedHashSet<>(validAddresses.values());
        Map<Status.Phase, Long> countByPhase = countPhases(addressSet);
        log.info("Total: {}, Active: {}, Configuring: {}, Pending: {}, Terminating: {}, Failed: {}", addressSet.size(), countByPhase.get(Active), countByPhase.get(Configuring), countByPhase.get(Pending), countByPhase.get(Terminating), countByPhase.get(Failed));

        Map<String, Map<String, UsageInfo>> usageMap = provisioner.checkUsage(filterByNotPhases(addressSet, EnumSet.of(Pending)));

        log.info("Usage: {}", usageMap);

        long calculatedUsage = System.nanoTime();
        Set<Address> pendingAddresses = filterBy(addressSet, address -> address.getStatus().getPhase().equals(Pending) ||
                    AddressProvisioner.hasPlansChanged(address));

        Map<String, Map<String, UsageInfo>> neededMap = provisioner.checkQuota(usageMap, pendingAddresses, addressSet);

        log.info("Needed: {}", neededMap);

        long checkedQuota = System.nanoTime();

        List<BrokerCluster> clusterList = kubernetes.listClusters();
        RouterCluster routerCluster = kubernetes.getRouterCluster();
        long listClusters = System.nanoTime();

        provisioner.provisionResources(routerCluster, clusterList, neededMap, pendingAddresses);

        long provisionResources = System.nanoTime();

        Set<Address> liveAddresses = filterByPhases(addressSet, EnumSet.of(Configuring, Active));
        checkStatuses(liveAddresses, addressResolver);
        long checkStatuses = System.nanoTime();
        for (Address address : liveAddresses) {
            if (address.getStatus().isReady()) {
                address.getStatus().setPhase(Active);
            }
        }

        checkAndMoveMigratingBrokersToDraining(addressSet, clusterList);
        checkAndRemoveDrainingBrokers(addressSet);

        deprovisionUnused(clusterList, filterByNotPhases(addressSet, EnumSet.of(Terminating)));
        long deprovisionUnused = System.nanoTime();

        StandardInfraConfig desiredConfig = (StandardInfraConfig) addressSpaceResolver.getInfraConfig("standard", addressSpacePlan.getMetadata().getName());
        upgradeClusters(desiredConfig, addressResolver, clusterList, filterByNotPhases(addressSet, EnumSet.of(Terminating)));

        long upgradeClusters = System.nanoTime();

        int staleCount = 0;
        for (Address address : addressList) {
            ProvisionState previous = previousStatus.get(address.getName());
            ProvisionState current = new ProvisionState(address.getStatus(),
                    address.getAnnotation(AnnotationKeys.BROKER_ID),
                    address.getAnnotation(AnnotationKeys.CLUSTER_ID),
                    address.getAnnotation(AnnotationKeys.APPLIED_PLAN));
            if (!current.equals(previous)) {
                try {
                    addressApi.replaceAddress(address);
                } catch (KubernetesClientException e) {
                    if (e.getStatus().getCode() == 409) {
                        // The address record is stale.  The address controller will be notified again by the watcher,
                        // so safe ignore the stale record.
                        log.debug("Address {} has stale resource version {}", address.getName(), address.getResourceVersion());
                        staleCount++;
                    } else {
                        throw e;
                    }
                }
            }
        }

        if (staleCount > 0) {
            log.info("{} address(es) were stale.", staleCount);
        }

        long replaceAddresses = System.nanoTime();
        garbageCollectTerminating(filterByPhases(addressSet, EnumSet.of(Terminating)), addressResolver);
        long gcTerminating = System.nanoTime();

        log.info("total: {} ns, resolvedPlan: {} ns, calculatedUsage: {} ns, checkedQuota: {} ns, listClusters: {} ns, provisionResources: {} ns, checkStatuses: {} ns, deprovisionUnused: {} ns, upgradeClusters: {} ns, replaceAddresses: {} ns, gcTerminating: {} ns", gcTerminating - start, resolvedPlan - start, calculatedUsage - resolvedPlan,  checkedQuota  - calculatedUsage, listClusters - checkedQuota, provisionResources - listClusters, checkStatuses - provisionResources, deprovisionUnused - checkStatuses, upgradeClusters - deprovisionUnused, replaceAddresses - upgradeClusters, gcTerminating - replaceAddresses);

        int ready = 0;
        for (Address address : addressList) {
            ready += address.getStatus().isReady() ? 1 : 0;
        }
        int notReady = addressList.size() - ready;

        long now = System.currentTimeMillis();

        String componentName = "standard-controller-" + options.getInfraUuid();
        metrics.reportMetric(new Metric("version", new MetricValue(0, now, new MetricLabel("name", componentName), new MetricLabel("version", options.getVersion()))));
        metrics.reportMetric(new Metric("health", new MetricValue(0, now, new MetricLabel("status", "ok"), new MetricLabel("summary", componentName + " is healthy"))));

        MetricLabel [] metricLabels = new MetricLabel[]{new MetricLabel("addressspace", options.getAddressSpace()), new MetricLabel("namespace", options.getAddressSpaceNamespace())};
        metrics.reportMetric(new Metric("addresses_ready_total", "Total number of addresses in ready state", MetricType.gauge, new MetricValue(ready, now, metricLabels)));
        metrics.reportMetric(new Metric("addresses_not_ready_total", "Total number of address in a not ready state", MetricType.gauge, new MetricValue(notReady, now, metricLabels)));
        metrics.reportMetric(new Metric("addresses_total", "Total number of addresses", MetricType.gauge, new MetricValue(addressList.size(), now, metricLabels)));

        metrics.reportMetric(new Metric("addresses_pending_total", "Total number of addresses in Pending state", MetricType.gauge, new MetricValue(countByPhase.get(Pending), now, metricLabels)));
        metrics.reportMetric(new Metric("addresses_failed_total", "Total number of addresses in Failed state", MetricType.gauge, new MetricValue(countByPhase.get(Failed), now, metricLabels)));
        metrics.reportMetric(new Metric("addresses_terminating_total", "Total number of addresses in Terminating state", MetricType.gauge, new MetricValue(countByPhase.get(Terminating), now, metricLabels)));
        metrics.reportMetric(new Metric("addresses_configuring_total", "Total number of addresses in Configuring state", MetricType.gauge, new MetricValue(countByPhase.get(Configuring), now, metricLabels)));
        metrics.reportMetric(new Metric("addresses_active_total", "Total number of addresses in Active state", MetricType.gauge, new MetricValue(countByPhase.get(Active), now, metricLabels)));

        long totalTime = gcTerminating - start;
        metrics.reportMetric(new Metric("standard_controller_loop_duration_seconds", "Time spent in controller loop", MetricType.gauge, new MetricValue((double) totalTime / 1_000_000_000.0, now, metricLabels)));
    }

    private void upgradeClusters(StandardInfraConfig desiredConfig, AddressResolver addressResolver, List<BrokerCluster> clusterList, Set<Address> addresses) throws Exception {
        for (BrokerCluster cluster : clusterList) {
            StandardInfraConfig currentConfig = cluster.getInfraConfig();
            if (!desiredConfig.equals(currentConfig)) {
                if (options.getVersion().equals(desiredConfig.getSpec().getVersion())) {
                    if (!desiredConfig.getUpdatePersistentVolumeClaim() && currentConfig != null && !currentConfig.getSpec().getBroker().getResources().getStorage().equals(desiredConfig.getSpec().getBroker().getResources().getStorage())) {
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
                    if (!cluster.getClusterId().startsWith("broker-sharded")) {
                        upgradedCluster = clusterGenerator.generateCluster(cluster.getClusterId(), 1, null, null, desiredConfig);
                    } else {
                        Address address = addresses.stream()
                                .filter(a -> cluster.getClusterId().equals(a.getAnnotation(AnnotationKeys.CLUSTER_ID)))
                                .findFirst()
                                .orElse(null);
                        if (address != null) {
                            AddressPlan plan = addressResolver.getPlan(address);
                            int brokerNeeded = 0;
                            for (ResourceRequest resourceRequest : plan.getRequiredResources()) {
                                if (resourceRequest.getName().equals("broker")) {
                                    brokerNeeded = (int) resourceRequest.getCredit();
                                    break;
                                }
                            }
                            upgradedCluster = clusterGenerator.generateCluster(cluster.getClusterId(), brokerNeeded, address, plan, desiredConfig);
                        }
                    }
                    log.info("Upgrading broker {}", cluster.getClusterId());
                    cluster.updateResources(upgradedCluster, desiredConfig);
                    kubernetes.apply(cluster.getResources(), desiredConfig.getUpdatePersistentVolumeClaim());
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
                String clusterId = address.getAnnotation(AnnotationKeys.CLUSTER_ID);
                if (cluster.getClusterId().equals(clusterId)) {
                    numFound++;
                }
                for (BrokerStatus brokerStatus : address.getStatus().getBrokerStatuses()) {
                    if (brokerStatus.getClusterId().equals(cluster.getClusterId())) {
                        numFound++;
                    }
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

    private Set<Address> filterBy(Set<Address> addressSet, Predicate<Address> predicate) {
        return addressSet.stream()
                .filter(predicate::test)
                .collect(Collectors.toSet());
    }

    private Set<Address> filterByPhases(Set<Address> addressSet, Set<Status.Phase> phases) {
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

    private Set<Address> filterByNotPhases(Set<Address> addressSet, Set<Status.Phase> phases) {
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

    private void checkAndMoveMigratingBrokersToDraining(Set<Address> addresses, List<BrokerCluster> brokerList) throws Exception {
        for (Address address : addresses) {
            int numActive = 0;
            int numReadyActive = 0;
            for (BrokerStatus brokerStatus : address.getStatus().getBrokerStatuses()) {
                if (BrokerState.Active.equals(brokerStatus.getState())) {
                    numActive++;
                    for (BrokerCluster cluster : brokerList) {
                        if (brokerStatus.getClusterId().equals(cluster.getClusterId()) && cluster.getReadyReplicas() > 0) {
                            numReadyActive++;
                            break;
                        }
                    }
                }
            }

            if (numActive == numReadyActive) {
                for (BrokerStatus brokerStatus : address.getStatus().getBrokerStatuses()) {
                    if (BrokerState.Migrating.equals(brokerStatus.getState())) {
                        brokerStatus.setState(BrokerState.Draining);
                    }
                }
            }
        }
    }

    private void checkAndRemoveDrainingBrokers(Set<Address> addresses) throws Exception {
        BrokerStatusCollector brokerStatusCollector = new BrokerStatusCollector(kubernetes, brokerClientFactory);
        for (Address address : addresses) {
            List<BrokerStatus> brokerStatuses = new ArrayList<>();
            for (BrokerStatus brokerStatus : address.getStatus().getBrokerStatuses()) {
                if (BrokerState.Draining.equals(brokerStatus.getState())) {
                    try {
                        long messageCount = brokerStatusCollector.getQueueMessageCount(address.getAddress(), brokerStatus.getClusterId());
                        if (messageCount > 0) {
                            brokerStatuses.add(brokerStatus);
                        }
                    } catch (Exception e) {
                        log.warn("Error checking status of broker {}:{} in state Draining. Keeping.", brokerStatus.getClusterId(), brokerStatus.getContainerId(), e);
                        brokerStatuses.add(brokerStatus);
                    }
                } else {
                    brokerStatuses.add(brokerStatus);
                }
            }
            address.getStatus().setBrokerStatuses(brokerStatuses);
        }
    }

    private Map<Address, Integer> checkStatuses(Set<Address> addresses, AddressResolver addressResolver) throws Exception {
        Map<Address, Integer> numOk = new HashMap<>();
        if (addresses.isEmpty()) {
            return numOk;
        }
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

    private class ProvisionState {
        private final Status status;
        private final String brokerId;
        private final String clusterId;
        private final String plan;

        public ProvisionState(Status status, String brokerId, String clusterId, String plan) {
            this.status = new Status(status);
            this.brokerId = brokerId;
            this.clusterId = clusterId;
            this.plan = plan;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProvisionState that = (ProvisionState) o;
            return Objects.equals(status, that.status) &&
                    Objects.equals(brokerId, that.brokerId) &&
                    Objects.equals(clusterId, that.clusterId) &&
                    Objects.equals(plan, that.plan);
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, brokerId, clusterId, plan);
        }

        @Override
        public String toString() {
            return "ProvisionState{" +
                    "status=" + status +
                    ", brokerId='" + brokerId + '\'' +
                    ", clusterId='" + clusterId + '\'' +
                    ", plan='" + plan + '\'' +
                    '}';
        }
    }
}
