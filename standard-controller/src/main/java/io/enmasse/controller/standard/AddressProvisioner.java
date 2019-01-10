/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import static io.enmasse.controller.standard.ControllerKind.Broker;
import static io.enmasse.controller.standard.ControllerReason.BrokerCreateFailed;
import static io.enmasse.controller.standard.ControllerReason.BrokerCreated;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.EventLogger;

public class AddressProvisioner {
    private static final Logger log = LoggerFactory.getLogger(AddressProvisioner.class);
    private final AddressSpaceResolver addressSpaceResolver;
    private final AddressResolver addressResolver;
    private final AddressSpacePlan addressSpacePlan;
    private final BrokerSetGenerator clusterGenerator;
    private final Kubernetes kubernetes;
    private final EventLogger eventLogger;
    private final String infraUuid;
    private final String pooledClusterIdPrefix;
    private final BrokerIdGenerator brokerIdGenerator;

    public AddressProvisioner(AddressSpaceResolver addressSpaceResolver, AddressResolver addressResolver, AddressSpacePlan addressSpacePlan, BrokerSetGenerator clusterGenerator, Kubernetes kubernetes, EventLogger eventLogger, String infraUuid, BrokerIdGenerator brokerIdGenerator) {
        this.addressSpaceResolver = addressSpaceResolver;
        this.addressResolver = addressResolver;
        this.addressSpacePlan = addressSpacePlan;
        this.clusterGenerator = clusterGenerator;
        this.kubernetes = kubernetes;
        this.eventLogger = eventLogger;
        this.infraUuid = infraUuid;
        this.pooledPattern = Pattern.compile("^broker-" + infraUuid + "-.*");
        this.pooledClusterIdPrefix = "broker-" + infraUuid + "-";
        this.brokerIdGenerator = brokerIdGenerator;
    }

    /**
     * Computes the resource usage for a set of addresses
     */
    public Map<String, Map<String, UsageInfo>> checkUsage(Set<Address> addressSet) {
        Map<String, Map<String, UsageInfo>> usageMap = new HashMap<>();

        for (Address address : addressSet) {
            addToUsage(usageMap, address);
        }
        return usageMap;
    }

    private void addToUsage(Map<String, Map<String, UsageInfo>> usageMap, Address address) {
        AddressType addressType = addressResolver.getType(address);
        AddressPlan addressPlan = addressResolver.getPlan(addressType, address);

        for (ResourceRequest resourceRequest : addressPlan.getRequiredResources()) {
            if ("subscription".equals(address.getType())) {
                if (address.getStatus().getBrokerStatuses().isEmpty() && address.getAnnotation(AnnotationKeys.BROKER_ID) == null) {
                    log.warn("Unexpected pooled address without cluster id: " + address.getAddress());
                    return;
                }
                String instanceId = address.getAnnotation(AnnotationKeys.BROKER_ID);
                if (!address.getStatus().getBrokerStatuses().isEmpty()) {
                    for (BrokerStatus status : address.getStatus().getBrokerStatuses()) {
                        if (BrokerState.Active.equals(status.getState())) {
                            addToUsage(usageMap, status.getContainerId(), "subscription", resourceRequest.getCredit());
                        }
                    }
                } else {
                    addToUsage(usageMap, instanceId, "subscription", resourceRequest.getCredit());
                }
            } else if (resourceRequest.getName().equals("router")) {
                addToUsage(usageMap, "all", "router", resourceRequest.getCredit());

            } else if (resourceRequest.getName().equals("broker") && resourceRequest.getCredit() < 1) {
                if (address.getStatus().getBrokerStatuses().isEmpty() && address.getAnnotation(AnnotationKeys.CLUSTER_ID) == null) {
                    log.warn("Unexpected pooled address without cluster id: " + address.getAddress());
                    return;
                }
                if (!address.getStatus().getBrokerStatuses().isEmpty()) {
                    for (BrokerStatus status : address.getStatus().getBrokerStatuses()) {
                        if (BrokerState.Active.equals(status.getState())) {
                            addToUsage(usageMap, status.getClusterId(), "broker", resourceRequest.getCredit());
                        }
                    }
                } else {
                    addToUsage(usageMap, address.getAnnotation(AnnotationKeys.CLUSTER_ID), "broker", resourceRequest.getCredit());
                }

            } else if (resourceRequest.getName().equals("broker")) {
                if (address.getStatus().getBrokerStatuses().isEmpty()) {
                    addToUsage(usageMap, getShardedClusterId(address), "broker", resourceRequest.getCredit());
                } else {
                    for (BrokerStatus status : address.getStatus().getBrokerStatuses()) {
                        if (BrokerState.Active.equals(status.getState())) {
                            addToUsage(usageMap, status.getClusterId(), "broker", resourceRequest.getCredit());
                        }
                    }
                }
            }

        }
    }

    private static void addToUsage(Map<String, Map<String, UsageInfo>> usageMap, String id, String resourceName, double credit) {
        Map<String, UsageInfo> resourceUsage = usageMap.computeIfAbsent(resourceName, k -> new HashMap<>());
        UsageInfo info = resourceUsage.computeIfAbsent(id, i -> new UsageInfo());
        info.addUsed(credit);
    }

    public Map<String, Map<String, UsageInfo>> checkQuota(Map<String, Map<String, UsageInfo>> usageMap, Set<Address> pending, Set<Address> all) {
        Map<String, Map<String, UsageInfo>> newUsageMap = usageMap;
        Map<String, Double> limits = computeLimits();

        Set<Address> pendingSubscriptionsWithConfiguredTopics = filterSubscriptionsWithConfiguredTopics(pending, all);
        Set<Address> pendingSubscriptionsWithPendingTopics = filterSubscriptionsWithPendingTopics(pending, all);
        Set<Address> pendingNonSubscriptions = filterByNotType(pending, "subscription");

        newUsageMap = addQuotaForAddress(pendingSubscriptionsWithConfiguredTopics , all, newUsageMap, limits);

        while(!pendingSubscriptionsWithPendingTopics.isEmpty()) {
            Address address = pendingSubscriptionsWithPendingTopics.iterator().next();
            Address topic = findAddress(address.getTopic().get(), all);
            newUsageMap = addQuotaForAddress(new HashSet<Address>(Arrays.asList(topic)), all, newUsageMap, limits);
            pendingNonSubscriptions.remove(topic);

            Set<Address> subscriptionsWithNewlyConfiguredTopics = filterSubscriptionsWithConfiguredTopics(pendingSubscriptionsWithPendingTopics, all);
            newUsageMap = addQuotaForAddress(subscriptionsWithNewlyConfiguredTopics, all, newUsageMap, limits);
            pendingSubscriptionsWithPendingTopics.removeAll(subscriptionsWithNewlyConfiguredTopics);
        }
        newUsageMap = addQuotaForAddress(pendingNonSubscriptions, all, newUsageMap, limits);

        return newUsageMap;
    }

    private Map<String, Map<String, UsageInfo>> addQuotaForAddress(Set<Address> pending, Set<Address> all,
            Map<String, Map<String, UsageInfo>> newUsageMap, Map<String, Double> limits) {
        for (Address address : pending) {
            if (!Status.Phase.Configuring.equals(address.getStatus().getPhase())) {
                Map<String, Map<String, UsageInfo>> neededMap = checkQuotaForAddress(limits, newUsageMap, address, all);
                if (neededMap != null) {
                    newUsageMap = neededMap;
                    address.getStatus().setPhase(Status.Phase.Configuring);
                    address.putAnnotation(AnnotationKeys.APPLIED_PLAN, address.getPlan());
                }
            }
        }
        return newUsageMap;
    }

    private Set<Address> filterSubscriptionsWithConfiguredTopics(Set<Address> addressSet, Set<Address> all) {
        return addressSet.stream()
                .filter(address -> "subscription".equals(address.getType()) && Arrays.asList(Status.Phase.Configuring, Status.Phase.Active).contains(findAddress(address.getTopic().get(), all).getStatus().getPhase()))
                .collect(Collectors.toSet());
    }
    private Set<Address> filterSubscriptionsWithPendingTopics(Set<Address> addressSet, Set<Address> all) {
        return addressSet.stream()
                .filter(address -> "subscription".equals(address.getType()) && Arrays.asList(Status.Phase.Pending).contains(findAddress(address.getTopic().get(), all).getStatus().getPhase()))
                .collect(Collectors.toSet());
    }
    private Set<Address> filterByNotType(Set<Address> addressSet, String type) {
        return addressSet.stream()
                .filter(address -> !type.equals(address.getType()))
                .collect(Collectors.toSet());
    }

    private static Map<String, Map<String, UsageInfo>> copyUsageMap(Map<String, Map<String, UsageInfo>> usageMap) {
        Map<String, Map<String, UsageInfo>> newUsageMap = new HashMap<>();
        for (Map.Entry<String, Map<String, UsageInfo>> entry : usageMap.entrySet()) {
            newUsageMap.put(entry.getKey(), new HashMap<>());
            for (Map.Entry<String, UsageInfo> innerEntry : entry.getValue().entrySet()) {
                newUsageMap.get(entry.getKey()).put(innerEntry.getKey(), new UsageInfo(innerEntry.getValue()));
            }
        }
        return newUsageMap;
    }

    private Address findAddress(String name, Set<Address> addressSet) {
        for (Address address : addressSet) {
            if (name.equals(address.getAddress())) {
                return address;
            }
        }
        return null;
    }

    private boolean scheduleSubscription(Address subscription, Address topic, Map<String, UsageInfo> brokerUsage, Map<String, UsageInfo> subscriptionUsage, ResourceRequest requested) {
        String cluster = topic.getAnnotation(AnnotationKeys.CLUSTER_ID);
        String broker = topic.getAnnotation(AnnotationKeys.BROKER_ID);

        AddressPlan topicPlan = addressResolver.getPlan(topic);
        boolean isPooled = false;

        for (ResourceRequest resourceRequest : topicPlan.getRequiredResources()) {
            if ("broker".equals(resourceRequest.getName()) && resourceRequest.getCredit() < 1) {
                isPooled = true;
                break;
            }
        }

        if (isPooled && !topic.getStatus().getBrokerStatuses().isEmpty()) {
            broker = topic.getStatus().getBrokerStatuses().get(0).getContainerId();
        }

        subscription.putAnnotation(AnnotationKeys.CLUSTER_ID, cluster);

        if (isPooled) {
            UsageInfo usageInfo = subscriptionUsage.computeIfAbsent(broker, k -> new UsageInfo());
            if (usageInfo.getUsed()+requested.getCredit() <= 1) {
                usageInfo.addUsed(requested.getCredit());

                // TODO: Remove after releasing 0.26.0
                subscription.putAnnotation(AnnotationKeys.BROKER_ID, broker);

                BrokerStatus brokerStatus = new BrokerStatus(cluster, broker);
                brokerStatus.setState(BrokerState.Active);
                subscription.getStatus().setBrokerStatuses(Collections.singletonList(brokerStatus));
            } else {
                log.info("no quota available on broker {} for {} on topic {}", cluster, subscription.getAddress(), topic.getAddress());
            }
        } else {
            List<BrokerInfo> shardedBrokers = new ArrayList<>();
            for (String host : brokerUsage.keySet()) {
                if (host.equals(cluster)) {
                    UsageInfo brokerUsageInfo = brokerUsage.get(host);
                    int replicas = brokerUsageInfo.getNeeded();
                    for (String container : subscriptionUsage.keySet()) {
                        shardedBrokers.add(new BrokerInfo(container, subscriptionUsage.get(container).getUsed()));
                    }
                    shardedBrokers.sort(Comparator.comparingDouble(BrokerInfo::getCredit));
                    if (shardedBrokers.size() < replicas) {
                        shardedBrokers.add(0, new BrokerInfo(cluster+"-"+shardedBrokers.size(), 0));
                    }
                }
            }
            for (BrokerInfo brokerInfo : shardedBrokers) {
                UsageInfo usageInfo = subscriptionUsage.computeIfAbsent(brokerInfo.getBrokerId(), k -> new UsageInfo());
                if (brokerInfo.getCredit() + requested.getCredit() < 1) {
                    // TODO: Remove after releasing 0.26.0
                    subscription.putAnnotation(AnnotationKeys.BROKER_ID, brokerInfo.getBrokerId());
                    BrokerStatus brokerStatus = new BrokerStatus(cluster, brokerInfo.getBrokerId());
                    brokerStatus.setState(BrokerState.Active);
                    subscription.getStatus().setBrokerStatuses(Collections.singletonList(brokerStatus));
                    usageInfo.addUsed(requested.getCredit());
                    break;
                }
            }
        }
        return !subscription.getStatus().getBrokerStatuses().isEmpty();
    }

    private Map<String, Map<String, UsageInfo>> checkQuotaForAddress(Map<String, Double> limits, Map<String, Map<String, UsageInfo>> usage, Address address, Set<Address> addressSet) {
        AddressType addressType = addressResolver.getType(address);
        AddressPlan addressPlan = addressResolver.getPlan(addressType, address.getPlan());

        Map<String, Map<String, UsageInfo>> needed = copyUsageMap(usage);

        for (ResourceRequest resourceRequest : addressPlan.getRequiredResources()) {
            String resourceName = resourceRequest.getName();
            Map<String, UsageInfo> resourceUsage = needed.computeIfAbsent(resourceName, k -> new HashMap<>());
            if ("router".equals(resourceName)) {
                UsageInfo info = resourceUsage.computeIfAbsent("all", k -> new UsageInfo());
                info.addUsed(resourceRequest.getCredit());
            } else if ("broker".equals(resourceName)) {
                if ("subscription".equals(address.getType())) {
                    Map<String, UsageInfo> subscriptionUsage = needed.computeIfAbsent("subscription", k -> new HashMap<>());
                    if (address.getTopic().isPresent()) {
                        Address topic = findAddress(address.getTopic().get(), addressSet);
                        if (!scheduleSubscription(address, topic, resourceUsage, subscriptionUsage, resourceRequest)) {
                            log.warn("Unable to find broker for scheduling subscription: {}", address);
                            return null;
                        }
                    } else {
                        log.warn("No topic specified for subscription {}", address.getAddress());
                    }
                } else if (resourceRequest.getCredit() < 1) {
                    boolean scheduled = scheduleAddress(resourceUsage, address, resourceRequest.getCredit());
                    if (!scheduled) {
                        allocateBroker(resourceUsage, pooledClusterIdPrefix);
                        if (!scheduleAddress(resourceUsage, address, resourceRequest.getCredit())) {
                            log.warn("Unable to find broker for scheduling {}", address);
                            return null;
                        }
                    }
                } else {
                    if (hasPlansChanged(address)) {
                        address.removeAnnotation(AnnotationKeys.CLUSTER_ID);
                        address.removeAnnotation(AnnotationKeys.BROKER_ID);
                    }
                    String clusterId = getShardedClusterId(address);
                    UsageInfo info = resourceUsage.get(clusterId);
                    if (info != null) {
                        throw new IllegalArgumentException("Found unexpected conflicting usage for address " + address.getName());
                    }
                    info = new UsageInfo();
                    info.addUsed(resourceRequest.getCredit());
                    resourceUsage.put(clusterId, info);

                    address.putAnnotation(AnnotationKeys.CLUSTER_ID, clusterId);
	                List<BrokerInfo> brokers = new ArrayList<>();
                    for (String host : resourceUsage.keySet()) {
                        if (host.startsWith(address.getAddress())) {
                            brokers.add(new BrokerInfo(host, resourceUsage.get(host).getUsed()));
                        }
                    }

                    BrokerStatus brokerStatus = new BrokerStatus(clusterId, address.getAddress());
                    brokerStatus.setState(BrokerState.Active);

                    updateBrokerStatus(address, Collections.singletonList(brokerStatus));

                    brokers.sort(Comparator.comparingDouble(BrokerInfo::getCredit));

                    for (BrokerInfo brokerInfo : brokers) {
                        if (brokerInfo.getCredit() + resourceRequest.getCredit() < 1) {
                            UsageInfo used = resourceUsage.get(brokerInfo.getBrokerId());
                            used.addUsed(resourceRequest.getCredit());
                            break;
                        } else {
                            log.warn("not enough credit on {} for {} ",brokerInfo.getBrokerId(), address.getAddress() );
                        }
                    }
                }
            } else {
                log.warn("should not be called with: {}", resourceName);
            }

            double resourceNeeded = sumNeeded(resourceUsage);
            if (resourceNeeded > limits.get(resourceName)) {
                log.info("address {} for {} needed {} > limit {}", address.getAddress(), resourceName, resourceNeeded, limits.get(resourceRequest.getName()));
                address.getStatus().setPhase(Status.Phase.Pending);
                address.getStatus().appendMessage("Quota exceeded");
                return null;
            }
        }

        log.debug("address: {}, usage {}, needed: {}, aggregate: {}", address.getAddress(), usage, needed, limits);

        double totalNeeded = sumTotalNeeded(needed);
        if (totalNeeded > limits.get("aggregate")) {
            log.info("address {} usage {}, total needed {} > limit {}", address.getAddress(), usage, totalNeeded, limits.get("aggregate"));
            address.getStatus().setPhase(Status.Phase.Pending);
            address.getStatus().appendMessage("Quota exceeded");
            return null;
        }
        return needed;
    }

    static boolean hasPlansChanged(Address address) {
        return !address.getPlan().equals(address.getAnnotation(AnnotationKeys.APPLIED_PLAN));
    }

    static int sumTotalNeeded(Map<String, Map<String, UsageInfo>> usageMap) {
        int totalNeeded = 0;
        for (String resource : usageMap.keySet()) {
            Map<String, UsageInfo> usage = usageMap.get(resource);
            for (UsageInfo value : usage.values()) {
                if (!"subscription".equals(resource)) {
                    totalNeeded += value.getNeeded();
                }
            }
        }
        return totalNeeded;
    }

    static int sumNeeded(Map<String, UsageInfo> resourceUsage) {
        int needed = 0;
        for (UsageInfo value : resourceUsage.values()) {
            needed += value.getNeeded();
        }
        return needed;
    }

    static int sumNeededMatching(Map<String, UsageInfo> resourceUsage, Pattern pattern) {
        int needed = 0;
        for (Map.Entry<String, UsageInfo> entry : resourceUsage.entrySet()) {
            if (pattern.matcher(entry.getKey()).matches()) {
                needed += entry.getValue().getNeeded();
            }
        }
        return needed;
    }

    private static void updateBrokerStatus(Address address, List<BrokerStatus> brokerStatuses) {
        List<BrokerStatus> toAdd = new ArrayList<>(brokerStatuses);
        for (BrokerStatus brokerStatus : address.getStatus().getBrokerStatuses()) {
            boolean found = false;
            for (BrokerStatus newBrokerStatus : brokerStatuses) {
                if (newBrokerStatus.getContainerId().equals(brokerStatus.getContainerId()) && newBrokerStatus.getClusterId().equals(brokerStatus.getClusterId())) {
                    brokerStatus.setState(newBrokerStatus.getState());
                    toAdd.remove(newBrokerStatus);
                    found = true;
                    break;
                }
            }
            if (!found) {
                brokerStatus.setState(BrokerState.Draining);
            }
        }

        address.getStatus().addAllBrokerStatuses(toAdd);
    }

    public String getShardedClusterId(Address address) {
        final String clusterId = address.getAnnotation(AnnotationKeys.CLUSTER_ID);

        if ( clusterId != null ) {
            return KubeUtil.sanitizeName(clusterId);
        }

        CRC32 crc32 = new CRC32();
        crc32.update(address.getNamespace().getBytes());
        crc32.update(address.getAddressSpace().getBytes());
        crc32.update(address.getAddress().getBytes());
        return "broker-sharded-" + Long.toHexString(crc32.getValue()) + "-" + infraUuid;
    }

    private Map<String, Double> computeLimits() {
        Map<String, Double> limits = new HashMap<>();
        for (ResourceAllowance allowance : addressSpacePlan.getResources()) {
            limits.put(allowance.getName(), allowance.getMax());
        }
        return limits;
    }

    public void provisionResources(RouterCluster router, List<BrokerCluster> existingClusters, Map<String, Map<String, UsageInfo>> neededMap, Set<Address> addressSet) {

        Map<String, Address> addressByClusterId = new HashMap<>();
        for (Address address : addressSet) {
            if (!"subscription".equals(address.getType())) {
                addressByClusterId.putIfAbsent(address.getAnnotation(AnnotationKeys.CLUSTER_ID), address);
            }
        }

        for (Map.Entry<String, Map<String, UsageInfo>> entry : neededMap.entrySet()) {
            String resourceName = entry.getKey();
            if ("router".equals(resourceName)) {
                int totalNeeded = sumNeeded(entry.getValue());
                router.setNewReplicas(Math.max(totalNeeded, router.getInfraConfig() != null ? router.getInfraConfig().getSpec().getRouter().getMinReplicas() : 0));
            } else if ("broker".equals(resourceName)) {
                // Provision pooled broker
                int needPooled = sumNeededMatching(entry.getValue(), pooledPattern);
                if (needPooled > 0) {
                    for (String id : entry.getValue().keySet()) {
                        if (pooledPattern.matcher(id).matches()) {
                            provisionBroker(existingClusters, id, 1, null, null);
                        }
                    }
                }

                // Collect all sharded brokers
                Map<String, Integer> shardedBrokers = new HashMap<>();
                for (Map.Entry<String, UsageInfo> usageEntry : entry.getValue().entrySet()) {
                    if (addressByClusterId.containsKey(usageEntry.getKey())) {
                        shardedBrokers.put(usageEntry.getKey(), usageEntry.getValue().getNeeded());
                    }
                }

                for (Map.Entry<String, Integer> brokerIdEntry : shardedBrokers.entrySet()) {
                    Address address = addressByClusterId.get(brokerIdEntry.getKey());
                    if ("subscription".equals(address.getType())) {
                        break;
                    }
                    AddressType addressType = addressResolver.getType(address);
                    AddressPlan addressPlan = addressResolver.getPlan(addressType, address);
                    provisionBroker(existingClusters, brokerIdEntry.getKey(), brokerIdEntry.getValue(), address, addressPlan);
                }
            }
        }

        if (router.hasChanged()) {
            log.info("Scaling router to {} replicas", router.getNewReplicas());
            kubernetes.scaleStatefulSet(router.getName(), router.getNewReplicas());
        }

        for (BrokerCluster cluster : existingClusters) {
            if (cluster.hasChanged()) {
                log.info("Scaling broker cluster {} to {} replicas", cluster.getClusterId(), cluster.getNewReplicas());
                kubernetes.scaleStatefulSet(cluster.getClusterId(), cluster.getNewReplicas());
            }
        }


    }

    private final Pattern pooledPattern;
    private boolean scheduleAddress(Map<String, UsageInfo> usageMap, Address address, double credit) {

        List<BrokerInfo> brokers = new ArrayList<>();
        for (String host : usageMap.keySet()) {
            if (pooledPattern.matcher(host).matches()) {
                brokers.add(new BrokerInfo(host, usageMap.get(host).getUsed()));
            }
        }

        brokers.sort(Comparator.comparingDouble(BrokerInfo::getCredit));

        for (BrokerInfo brokerInfo : brokers) {
            if (brokerInfo.getCredit() + credit < 1) {
                // TODO: Remove after releasing 0.26.0
                address.putAnnotation(AnnotationKeys.BROKER_ID, brokerInfo.getBrokerId() + "-0");

                address.putAnnotation(AnnotationKeys.CLUSTER_ID, brokerInfo.getBrokerId());
                BrokerStatus brokerStatus = new BrokerStatus(brokerInfo.getBrokerId(), brokerInfo.getBrokerId() + "-0");
                brokerStatus.setState(BrokerState.Active);
                updateBrokerStatus(address, Collections.singletonList(brokerStatus));
                UsageInfo used = usageMap.get(brokerInfo.getBrokerId());
                used.addUsed(credit);
                return true;
            }
        }
        return false;
    }

    private String allocateBroker(Map<String, UsageInfo> resourceNeeded, String clusterIdPrefix) {
        String randomId = brokerIdGenerator.generateBrokerId();
        String clusterId = clusterIdPrefix + randomId;
        resourceNeeded.put(clusterId, new UsageInfo());
        return clusterId;
    }

    private void provisionBroker(List<BrokerCluster> clusterList, String clusterId, int numReplicas, Address address, AddressPlan addressPlan) {
        try {
            for (BrokerCluster cluster : clusterList) {
                if (cluster.getClusterId().equals(clusterId)) {
                    cluster.setNewReplicas(numReplicas);
                    return;
                }
            }

            // Needs to be created
            StandardInfraConfig desiredConfig = (StandardInfraConfig) addressSpaceResolver.getInfraConfig("standard", addressSpacePlan.getMetadata().getName());
            BrokerCluster cluster = clusterGenerator.generateCluster(clusterId, numReplicas, address, addressPlan, desiredConfig);
            if (!cluster.getResources().getItems().isEmpty()) {
                kubernetes.create(cluster.getResources());
                eventLogger.log(BrokerCreated, "Created broker " + cluster.getClusterId() + " with " + numReplicas + " replicas", Normal, Broker, cluster.getClusterId());
            }
            clusterList.add(cluster);
        } catch (Exception e) {
            log.warn("Error creating broker", e);
            eventLogger.log(BrokerCreateFailed, "Error creating broker: " + e.getMessage(), Warning, Broker, clusterId);
            address.getStatus().setPhase(Status.Phase.Failed);
            address.getStatus().appendMessage("Error creating broker: " + e.getMessage());
        }
    }

    private static class BrokerInfo {
        private final String brokerId;
        private final double credit;

        public String getBrokerId() {
            return brokerId;
        }

        public double getCredit() {
            return credit;
        }

        private BrokerInfo(String brokerId, double credit) {
            this.brokerId = brokerId;
            this.credit = credit;
        }

        @Override
        public String toString() {
            return "{brokerId=" + brokerId + ",credit=" + credit + "}";
        }
    }

}
