/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    public Map<String, Map<String, Double>> checkUsage(Set<Address> addressSet) {
        Map<String, Map<String, Double>> usageMap = new HashMap<>();

        for (Address address : addressSet) {
            addToUsage(usageMap, address);
        }
        return usageMap;
    }

    private void addToUsage(Map<String, Map<String, Double>> usageMap, Address address) {
        AddressType addressType = addressResolver.getType(address);
        AddressPlan addressPlan = addressResolver.getPlan(addressType, address);

        for (ResourceRequest resourceRequest : addressPlan.getRequiredResources()) {
            String brokerId = getBrokerId(address).orElse("all");
            if (resourceRequest.getResourceName().equals("router")) {
                brokerId = "all";
            }
            Map<String, Double> resourceUsage = usageMap.computeIfAbsent(resourceRequest.getResourceName(), k -> new HashMap<>());
            double current = resourceUsage.getOrDefault(brokerId, 0.0);
            resourceUsage.put(brokerId, current + resourceRequest.getAmount());
        }
    }

    public Map<Address, Map<String, Double>> checkQuota(Map<String, Map<String, Double>> usageMap, Set<Address> addressSet) {
        Map<Address, Map<String, Double>> neededPerAddressMap = new HashMap<>();
        Map<String, Double> limits = computeLimits();
        Map<String, Map<String, Double>> newUsageMap = copyUsageMap(usageMap);
        for (Address address : addressSet) {
            Map<String, Double> neededMap = checkQuotaForAddress(limits, newUsageMap, address);
            if (neededMap != null) {
                addToUsage(newUsageMap, address);
                neededPerAddressMap.put(address, neededMap);
            }
        }
        return neededPerAddressMap;
    }

    private static Map<String, Map<String, Double>> copyUsageMap(Map<String, Map<String, Double>> usageMap) {
        Map<String, Map<String, Double>> newUsageMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : usageMap.entrySet()) {
            newUsageMap.put(entry.getKey(), new HashMap<>());
            for (Map.Entry<String, Double> innerEntry : entry.getValue().entrySet()) {
                newUsageMap.get(entry.getKey()).put(innerEntry.getKey(), innerEntry.getValue());
            }
        }
        return newUsageMap;
    }

    private Map<String, Double> checkQuotaForAddress(Map<String, Double> limits, Map<String, Map<String, Double>> usageMap, Address address) {
        Map<String, Double> neededMap = new HashMap<>();
        AddressType addressType = addressResolver.getType(address);
        AddressPlan addressPlan = addressResolver.getPlan(addressType, address);

        for (ResourceRequest resourceRequest : addressPlan.getRequiredResources()) {
            Map<String, Double> resourceUsage = usageMap.getOrDefault(resourceRequest.getResourceName(), new HashMap<>());
            double needed = 0.0;
            if ("router".equals(resourceRequest.getResourceName())) {
                needed = resourceRequest.getAmount();
            } else if ("broker".equals(resourceRequest.getResourceName()) && resourceRequest.getAmount() < 1) {
                double free = 1 - minUsed(resourceUsage);
                if (resourceRequest.getAmount() < free) {
                    needed = resourceRequest.getAmount();
                } else {
                    needed = 1.0;
                }
            } else {
                needed = resourceRequest.getAmount();
            }

            neededMap.put(resourceRequest.getResourceName(), needed);

            double sumResource = sumMap(resourceUsage);
            if (sumResource + needed > limits.get(resourceRequest.getResourceName())) {
                log.info("usage {} + needed {} > limit {}", sumResource, needed, limits.get(resourceRequest.getResourceName()));
                address.getStatus().setPhase(Status.Phase.Pending);
                address.getStatus().appendMessage("Quota exceeded");
                return null;
            }
        }

        log.debug("address: {}, usage: {}, needed: {}, aggregate: {}", address.getAddress(), usageMap, neededMap, limits);

        double totalUsage = sumTotal(usageMap);
        double totalNeeded = sumMap(neededMap);
        if (totalUsage + totalNeeded > limits.get("aggregate")) {
            log.info("total usage {} + total needed {} > limit {}", totalUsage, totalNeeded, limits.get("aggregate"));
            address.getStatus().setPhase(Status.Phase.Pending);
            address.getStatus().appendMessage("Quota exceeded");
            return null;
        }
        return neededMap;
    }

    private double sumTotal(Map<String, Map<String, Double>> usageMap) {
        double totalUsage = 0.0;
        for (Map<String, Double> usage : usageMap.values()) {
            for (double value : usage.values()) {
                totalUsage += value;
            }
        }
        return totalUsage;
    }

    private double sumMap(Map<String, Double> resourceUsage) {
        double used = 0.0;
        for (double value : resourceUsage.values()) {
            used += value;
        }
        return used;
    }

    private double minUsed(Map<String, Double> resourceUsage) {
        double minUsed = Double.MAX_VALUE;
        for (double value : resourceUsage.values()) {
            minUsed = Math.min(minUsed, value);
        }
        return minUsed;
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

    public void provisionResources(RouterCluster router, List<BrokerCluster> existingClusters, Map<String, Map<String, Double>> usageMap, Map<Address, Map<String, Double>> neededMap) {
        Map<String, Map<String, Double>> newUsageMap = copyUsageMap(usageMap);
        Map<String, List<BrokerInfo>> brokerInfoMap = new HashMap<>();

        for (Map.Entry<Address, Map<String, Double>> entry : neededMap.entrySet()) {
            Address address = entry.getKey();
            // Update usage map if provisioning was successful
            if (provisionResources(router, existingClusters, brokerInfoMap, newUsageMap, entry.getValue(), address)) {
                addToUsage(newUsageMap, address);
            }
        }

        if (router.hasChanged()) {
            kubernetes.scaleDeployment(router.getName(), router.getNewReplicas());
        }
        for (BrokerCluster cluster : existingClusters) {
            if (cluster.hasChanged()) {
                kubernetes.scaleStatefulSet(cluster.getClusterId(), cluster.getNewReplicas());
            }
        }

    }

    private boolean provisionResources(RouterCluster router, List<BrokerCluster> clusterList, Map<String, List<BrokerInfo>> brokerInfoMap, Map<String, Map<String, Double>> usageMap, Map<String, Double> neededMap, Address address) {

        AddressType addressType = addressResolver.getType(address);
        AddressPlan addressPlan = addressResolver.getPlan(addressType, address);

        int numOk = 0;
        for (ResourceRequest resourceRequest : addressPlan.getRequiredResources()) {
            Map<String, Double> resourceUsage = usageMap.getOrDefault(resourceRequest.getResourceName(), new HashMap<>());
            boolean success = false;
            if ("router".equals(resourceRequest.getResourceName())) {
                double sumUsage = sumMap(resourceUsage);
                double needed = neededMap.get(resourceRequest.getResourceName());
                int required = (int) Math.ceil(
                        sumUsage
                        + needed);

                log.info("Address {} require ceil(usage({}) + needed({})) = {} routers", address.getAddress(), sumUsage, needed, required);
                router.setNewReplicas(required);
                success = true;
            } else if ("broker".equals(resourceRequest.getResourceName()) && resourceRequest.getAmount() < 1) {
                double sumUsage = sumMap(resourceUsage);
                double shardedUsage = resourceUsage.getOrDefault("all", 0.0);
                double needed = neededMap.get(resourceRequest.getResourceName());
                int required = (int) Math.ceil(sumUsage
                        - shardedUsage
                        + needed);

                log.info("Address {} require ceil(usage({}) - sharded({}) + needed({})) = {} pooled brokers", address.getAddress(), sumUsage, shardedUsage, needed, required);
                ResourceDefinition resourceDefinition = addressResolver.getResourceDefinition(addressPlan, resourceRequest.getResourceName());
                success = provisionBroker(clusterList, resourceRequest.getResourceName(), resourceDefinition, required, null);
                if (success) {
                    success = scheduleAddress(brokerInfoMap, resourceRequest.getResourceName(), address, resourceUsage, resourceRequest.getAmount());
                }
            } else if ("broker".equals(resourceRequest.getResourceName())) {
                double sumUsage = resourceUsage.getOrDefault("all", 0.0);
                double needed = neededMap.get(resourceRequest.getResourceName());
                int required = (int) Math.ceil(needed);

                log.info("Address {} require ceil(usage({}) + needed({})) = {} sharded brokers", address.getAddress(), sumUsage, needed, required);
                ResourceDefinition resourceDefinition = addressResolver.getResourceDefinition(addressPlan, resourceRequest.getResourceName());
                success = provisionBroker(clusterList, address.getName(), resourceDefinition, required, address);
            }

            if (success) {
                numOk++;
            }
        }

        if (numOk < addressPlan.getRequiredResources().size()) {
            log.warn("Error provisioning resources for {}", address);
            return false;
        } else {
            log.info("Setting phase of {} to Configuring", address.getAddress());
            address.getStatus().setPhase(Status.Phase.Configuring);
            return true;
        }
    }

    private boolean scheduleAddress(Map<String, List<BrokerInfo>> brokerUsageMap, String clusterId, Address address, Map<String, Double> usageMap, double credit) {


        address.getAnnotations().put(AnnotationKeys.CLUSTER_ID, clusterId);

        List<BrokerInfo> brokers = brokerUsageMap.get(clusterId);
        if (brokers == null) {
            // Add all brokers we know about
            brokers = new ArrayList<>();
            for (String host : kubernetes.listBrokers(clusterId)) {
                brokers.add(new BrokerInfo(host));
            }
            brokerUsageMap.put(clusterId, brokers);
        }

        // Update from usage map and sort
        for (BrokerInfo info : brokers) {
            info.setCredit(usageMap.getOrDefault(info.brokerId, 0.0));
        }
        brokers.sort(Comparator.comparingDouble(BrokerInfo::getCredit));

        for (BrokerInfo brokerInfo : brokers) {
            if (brokerInfo.getCredit() + credit < 1) {
                address.getAnnotations().put(AnnotationKeys.BROKER_ID, brokerInfo.getBrokerId());
                return true;
            }
        }
        log.warn("Unable to find broker for scheduling {}", address);
        return false;
    }

    private boolean provisionRouter(Deployment router, int numReplicas) {
        try {
            router.getSpec().setReplicas(numReplicas);
            return true;
        } catch (Exception e) {
            log.warn("Error scaling router deployment", e);
            return false;
        }
    }

    private boolean provisionBroker(List<BrokerCluster> clusterList, String clusterId, ResourceDefinition resourceDefinition, int numReplicas, Address address) {
        try {
            for (BrokerCluster cluster : clusterList) {
                if (cluster.getClusterId().equals(clusterId)) {
                    cluster.setNewReplicas(numReplicas);
                    return true;
                }
            }

            // Needs to be created
            BrokerCluster cluster = clusterGenerator.generateCluster(clusterId, resourceDefinition, numReplicas, address);
            if (!cluster.getResources().getItems().isEmpty()) {
                kubernetes.create(cluster.getResources());
                eventLogger.log(BrokerCreated, "Created broker " + cluster.getClusterId() + " with " + numReplicas + " replicas", Normal, Broker, cluster.getClusterId());
            }
            clusterList.add(cluster);
            return true;
        } catch (Exception e) {
            log.warn("Error creating broker", e);
            eventLogger.log(BrokerCreateFailed, "Error creating broker: " + e.getMessage(), Warning, Broker, clusterId);
            address.getStatus().setPhase(Status.Phase.Failed);
            address.getStatus().appendMessage("Error creating broker: " + e.getMessage());
        }
        return false;
    }

    private static class BrokerInfo {
        private final String brokerId;
        private double credit;

        public String getBrokerId() {
            return brokerId;
        }

        public double getCredit() {
            return credit;
        }

        public void setCredit(double credit) {
            this.credit = credit;
        }

        private BrokerInfo(String brokerId) {
            this.brokerId = brokerId;
        }

    }
}
