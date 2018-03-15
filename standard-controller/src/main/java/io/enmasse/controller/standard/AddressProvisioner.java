/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.EventLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

import static io.enmasse.controller.standard.ControllerKind.Broker;
import static io.enmasse.controller.standard.ControllerReason.BrokerCreateFailed;
import static io.enmasse.controller.standard.ControllerReason.BrokerCreated;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

public class AddressProvisioner {
    private static final Logger log = LoggerFactory.getLogger(AddressProvisioner.class);
    private final AddressResolver addressResolver;
    private final AddressSpacePlan addressSpacePlan;
    private final BrokerSetGenerator clusterGenerator;
    private final Kubernetes kubernetes;
    private final EventLogger eventLogger;

    public AddressProvisioner(AddressResolver addressResolver, AddressSpacePlan addressSpacePlan, BrokerSetGenerator clusterGenerator, Kubernetes kubernetes, EventLogger eventLogger) {
        this.addressResolver = addressResolver;
        this.addressSpacePlan = addressSpacePlan;
        this.clusterGenerator = clusterGenerator;
        this.kubernetes = kubernetes;
        this.eventLogger = eventLogger;
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
            String instanceId = null;
            if (resourceRequest.getResourceName().equals("router")) {
                instanceId = "all";
            } else if (resourceRequest.getResourceName().equals("broker") && resourceRequest.getAmount() < 1) {
                instanceId = getBrokerId(address).orElseThrow(() -> new IllegalArgumentException("Unexpected pooled address without broker id: " + address.getAddress()));
            } else if (resourceRequest.getResourceName().equals("broker")) {
                instanceId = address.getName();
            }
            Map<String, UsageInfo> resourceUsage = usageMap.computeIfAbsent(resourceRequest.getResourceName(), k -> new HashMap<>());
            UsageInfo info = resourceUsage.computeIfAbsent(instanceId, i -> new UsageInfo());
            info.addUsed(resourceRequest.getAmount());
        }
    }

    public Map<String, Map<String, UsageInfo>> checkQuota(Map<String, Map<String, UsageInfo>> usageMap, Set<Address> addressSet) {
        Map<String, Map<String, UsageInfo>> newUsageMap = usageMap;
        Map<String, Double> limits = computeLimits();
        for (Address address : addressSet) {
            Map<String, Map<String, UsageInfo>> neededMap = checkQuotaForAddress(limits, newUsageMap, address);
            if (neededMap != null) {
                newUsageMap = neededMap;
                address.getStatus().setPhase(Status.Phase.Configuring);
            }
        }
        return newUsageMap;
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

    private Map<String, Map<String, UsageInfo>> checkQuotaForAddress(Map<String, Double> limits, Map<String, Map<String, UsageInfo>> usage, Address address) {
        AddressType addressType = addressResolver.getType(address);
        AddressPlan addressPlan = addressResolver.getPlan(addressType, address);

        Map<String, Map<String, UsageInfo>> needed = copyUsageMap(usage);

        for (ResourceRequest resourceRequest : addressPlan.getRequiredResources()) {
            String resourceName = resourceRequest.getResourceName();
            Map<String, UsageInfo> resourceUsage = needed.computeIfAbsent(resourceName, k -> new HashMap<>());
            if ("router".equals(resourceName)) {
                UsageInfo info = resourceUsage.computeIfAbsent("all", k -> new UsageInfo());
                info.addUsed(resourceRequest.getAmount());
            } else if ("broker".equals(resourceName) && resourceRequest.getAmount() < 1) {
                boolean scheduled = scheduleAddress(resourceUsage, address, resourceRequest.getAmount());
                if (!scheduled) {
                    allocateBroker(resourceUsage);
                    if (!scheduleAddress(resourceUsage, address, resourceRequest.getAmount())) {
                        log.warn("Unable to find broker for scheduling {}", address);
                        return null;
                    }
                }
            } else if ("broker".equals(resourceName)) {
                UsageInfo info = resourceUsage.get(address.getName());
                if (info != null) {
                    throw new IllegalArgumentException("Found unexpected conflicting usage for address " + address.getName());
                }
                info = new UsageInfo();
                info.addUsed(resourceRequest.getAmount());
                resourceUsage.put(address.getName(), info);
            }

            double resourceNeeded = sumNeeded(resourceUsage);
            if (resourceNeeded > limits.get(resourceName)) {
                log.info("address {} for {} needed {} > limit {}", address.getAddress(), resourceName, resourceNeeded, limits.get(resourceRequest.getResourceName()));
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

    static int sumTotalNeeded(Map<String, Map<String, UsageInfo>> usageMap) {
        int totalNeeded = 0;
        for (Map<String, UsageInfo> usage : usageMap.values()) {
            for (UsageInfo value : usage.values()) {
                totalNeeded += value.getNeeded();
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

    public static Optional<String> getBrokerId(Address address) {
        if (address.getAnnotations() != null) {
            return Optional.ofNullable(address.getAnnotations().get(AnnotationKeys.BROKER_ID));
        }
        return Optional.empty();
    }

    private Map<String, Double> computeLimits() {
        Map<String, Double> limits = new HashMap<>();
        for (ResourceAllowance allowance : addressSpacePlan.getResources()) {
            limits.put(allowance.getResourceName(), allowance.getMax());
        }
        return limits;
    }

    public void provisionResources(RouterCluster router, List<BrokerCluster> existingClusters, Map<String, Map<String, UsageInfo>> neededMap, Set<Address> addressSet) {

        Map<String, Address> addressByClusterId = new HashMap<>();
        for (Address address : addressSet) {
            addressByClusterId.put(address.getName(), address);
        }

        for (Map.Entry<String, Map<String, UsageInfo>> entry : neededMap.entrySet()) {
            String resourceName = entry.getKey();
            if ("router".equals(resourceName)) {
                int totalNeeded = sumNeeded(entry.getValue());
                router.setNewReplicas(totalNeeded);
            } else if ("broker".equals(resourceName)) {
                // Provision pooled broker
                ResourceDefinition pooledDefinition = addressResolver.getResourceDefinition(resourceName);
                int needPooled = sumNeededMatching(entry.getValue(), pooledPattern);
                if (needPooled > 0) {
                    provisionBroker(existingClusters, "broker", pooledDefinition, needPooled, null);
                }

                // Collect all sharded brokers
                Map<String, Integer> sharedBrokers = new HashMap<>();
                for (Map.Entry<String, UsageInfo> usageEntry : entry.getValue().entrySet()) {
                    if (addressByClusterId.containsKey(usageEntry.getKey())) {
                        sharedBrokers.put(usageEntry.getKey(), usageEntry.getValue().getNeeded());
                    }
                }

                for (Map.Entry<String, Integer> clusterIdEntry : sharedBrokers.entrySet()) {
                    Address address = addressByClusterId.get(clusterIdEntry.getKey());
                    AddressType addressType = addressResolver.getType(address);
                    AddressPlan addressPlan = addressResolver.getPlan(addressType, address);
                    ResourceDefinition resourceDefinition = addressResolver.getResourceDefinition(addressPlan, resourceName);
                    provisionBroker(existingClusters, clusterIdEntry.getKey(), resourceDefinition, clusterIdEntry.getValue(), address);
                }
            }
        }

        if (router.hasChanged()) {
            log.info("Scaling router to {} replicas", router.getNewReplicas());
            kubernetes.scaleDeployment(router.getName(), router.getNewReplicas());
        }
        for (BrokerCluster cluster : existingClusters) {
            if (cluster.hasChanged()) {
                log.info("Scaling broker cluster {} to {} replicas", cluster.getClusterId(), cluster.getNewReplicas());
                kubernetes.scaleStatefulSet(cluster.getClusterId(), cluster.getNewReplicas());
            }
        }

    }

    private final Pattern pooledPattern = Pattern.compile("^broker-\\d+");
    private boolean scheduleAddress(Map<String, UsageInfo> usageMap, Address address, double credit) {

        address.getAnnotations().put(AnnotationKeys.CLUSTER_ID, "broker");

        List<BrokerInfo> brokers = new ArrayList<>();
        for (String host : usageMap.keySet()) {
            if (pooledPattern.matcher(host).matches()) {
                brokers.add(new BrokerInfo(host, usageMap.get(host).getUsed()));
            }
        }

        brokers.sort(Comparator.comparingDouble(BrokerInfo::getCredit));

        for (BrokerInfo brokerInfo : brokers) {
            if (brokerInfo.getCredit() + credit < 1) {
                address.getAnnotations().put(AnnotationKeys.BROKER_ID, brokerInfo.getBrokerId());
                UsageInfo used = usageMap.get(brokerInfo.getBrokerId());
                used.addUsed(credit);
                return true;
            }
        }
        return false;
    }

    private void allocateBroker(Map<String, UsageInfo> resourceNeeded) {
        int numPooled = 0;
        for (String id : resourceNeeded.keySet()) {
            if (pooledPattern.matcher(id).matches()) {
                numPooled++;
            }
        }

        resourceNeeded.put("broker-" + numPooled, new UsageInfo());
    }


    private void provisionBroker(List<BrokerCluster> clusterList, String clusterId, ResourceDefinition resourceDefinition, int numReplicas, Address address) {
        try {
            for (BrokerCluster cluster : clusterList) {
                if (cluster.getClusterId().equals(clusterId)) {
                    cluster.setNewReplicas(numReplicas);
                    return;
                }
            }

            // Needs to be created
            BrokerCluster cluster = clusterGenerator.generateCluster(clusterId, resourceDefinition, numReplicas, address);
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

    }
}
