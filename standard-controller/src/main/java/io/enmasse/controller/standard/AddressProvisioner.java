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

            if (sumMap(resourceUsage) + needed > limits.get(resourceRequest.getResourceName())) {
                address.getStatus().setPhase(Status.Phase.Pending);
                address.getStatus().appendMessage("Quota exceeded");
                return null;
            }
        }

        log.info("address: {}, usage: {}, needed: {}, aggregate: {}", address.getAddress(), usageMap, neededMap, limits);
        if (sumTotal(usageMap) + sumMap(neededMap) > limits.get("aggregate")) {
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

    public void provisionResources(Map<String, Map<String, Double>> usageMap, Map<Address, Map<String, Double>> neededMap) {
        Map<String, Map<String, Double>> newUsageMap = copyUsageMap(usageMap);

        for (Map.Entry<Address, Map<String, Double>> entry : neededMap.entrySet()) {
            Address address = entry.getKey();
            // Update usage map if provisioning was successful
            if (provisionResources(newUsageMap, entry.getValue(), address)) {
                addToUsage(newUsageMap, address);
            }
        }

    }

    private boolean provisionResources(Map<String, Map<String, Double>> usageMap, Map<String, Double> neededMap, Address address) {

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

                log.info("Require ceil(usage({}) + needed({})) = {} routers", sumUsage, needed, required);
                success = provisionRouter(required);
            } else if ("broker".equals(resourceRequest.getResourceName()) && resourceRequest.getAmount() < 1) {
                double sumUsage = sumMap(resourceUsage);
                double shardedUsage = resourceUsage.getOrDefault("all", 0.0);
                double needed = neededMap.get(resourceRequest.getResourceName());
                int required = (int) Math.ceil(sumUsage
                        - shardedUsage
                        + needed);

                log.info("Require ceil(usage({}) - sharded({}) + needed({})) = {} pooled brokers", sumUsage, shardedUsage, needed, required);
                ResourceDefinition resourceDefinition = addressResolver.getResourceDefinition(addressPlan, resourceRequest.getResourceName());
                success = provisionBroker(resourceRequest.getResourceName(), resourceDefinition, required, null);
                if (success) {
                    success = scheduleAddress(resourceRequest.getResourceName(), address, resourceUsage, resourceRequest.getAmount());
                }
            } else if ("broker".equals(resourceRequest.getResourceName())) {
                double sumUsage = resourceUsage.getOrDefault("all", 0.0);
                double needed = neededMap.get(resourceRequest.getResourceName());
                int required = (int) Math.ceil(
                        sumUsage
                        + needed);

                log.info("Require ceil(usage({}) + needed({})) = {} sharded brokers", sumUsage, needed, required);
                ResourceDefinition resourceDefinition = addressResolver.getResourceDefinition(addressPlan, resourceRequest.getResourceName());
                success = provisionBroker(address.getName(), resourceDefinition, required, address);
            }

            if (success) {
                numOk++;
            }
        }

        if (numOk < addressPlan.getRequiredResources().size()) {
            log.warn("Error provisioning resources for {}", address);
            return false;
        } else {
            address.getStatus().setPhase(Status.Phase.Configuring);
            return true;
        }
    }


    private boolean scheduleAddress(String clusterId, Address address, Map<String, Double> usageMap, double credit) {

        List<BrokerInfo> brokers = new ArrayList<>();

        address.getAnnotations().put(AnnotationKeys.CLUSTER_ID, clusterId);

        // Add all brokers we know about
        for (String host : kubernetes.listBrokers(clusterId)) {
            brokers.add(new BrokerInfo(host, usageMap.getOrDefault(host, 0.0)));
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

    private boolean provisionRouter(int numReplicas) {
        try {
            // TODO: 'qdrouterd' should ideally be passed through env?
            kubernetes.scaleDeployment("qdrouterd", numReplicas);
            return true;
        } catch (Exception e) {
            log.warn("Error scaling router deployment", e);
            return false;
        }
    }

    private boolean provisionBroker(String clusterId, ResourceDefinition resourceDefinition, int numReplicas, Address address) {
        try {
            List<AddressCluster> clusters = kubernetes.listClusters();
            for (AddressCluster cluster : clusters) {
                if (cluster.getClusterId().equals(clusterId)) {
                    log.info("Scaling broker set with id {} and {} replicas", cluster.getClusterId(), numReplicas);
                    kubernetes.scaleStatefulSet(cluster.getClusterId(), numReplicas);
                    return true;
                }
            }

            // Needs to be created
            AddressCluster cluster = clusterGenerator.generateCluster(clusterId, resourceDefinition, numReplicas, address);
            if (!cluster.getResources().getItems().isEmpty()) {
                kubernetes.create(cluster.getResources());
                eventLogger.log(BrokerCreated, "Created broker " + cluster.getClusterId() + " with " + numReplicas + " replicas", Normal, Broker, cluster.getClusterId());
            }
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
