/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.address.model.KubeUtil;
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
    public Map<String,Map<String, Double>> checkUsage(Set<Address> addressSet) {
        Map<String, Map<String, Double>> usageMap = new HashMap<>();

        for (Address address : addressSet) {
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
        return usageMap;
    }

    public Map<Address, Map<String, Double>> checkQuota(Map<String, Map<String, Double>> usageMap, Set<Address> addressSet) {
        Map<Address, Map<String, Double>> neededPerAddressMap = new HashMap<>();
        Map<String, Double> limits = computeLimits();
        for (Address address : addressSet) {
            Map<String, Double> neededMap = checkQuotaForAddress(limits, usageMap, address);
            if (neededMap == null) {
                continue;
            }
            neededPerAddressMap.put(address, neededMap);
        }
        return neededPerAddressMap;
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

        log.info("usage: {}, needed: {}, aggregate: {}", usageMap, neededMap, limits);
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
        log.info("Total usage: {}", totalUsage);
        return totalUsage;
    }

    private double sumMap(Map<String, Double> resourceUsage) {
        double used = 0.0;
        for (double value : resourceUsage.values()) {
            used += value;
        }
        log.info("Sum map: {}", used);
        return used;
    }

    private double minUsed(Map<String, Double> resourceUsage) {
        double minUsed = Double.MAX_VALUE;
        for (double value : resourceUsage.values()) {
            minUsed = Math.min(minUsed, value);
        }
        log.info("Min used: {}", minUsed);
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
        for (Map.Entry<Address, Map<String, Double>> entry : neededMap.entrySet()) {
            provisionResources(usageMap, entry.getValue(), entry.getKey());
        }

    }

    private void provisionResources(Map<String, Map<String, Double>> usageMap, Map<String, Double> neededMap, Address address) {

        AddressType addressType = addressResolver.getType(address);
        AddressPlan addressPlan = addressResolver.getPlan(addressType, address);

        int numOk = 0;
        for (ResourceRequest resourceRequest : addressPlan.getRequiredResources()) {
            Map<String, Double> resourceUsage = usageMap.getOrDefault(resourceRequest.getResourceName(), new HashMap<>());
            boolean success = false;
            if ("router".equals(resourceRequest.getResourceName())) {
                int required = (int) Math.ceil(
                        sumMap(resourceUsage)
                        + neededMap.get(resourceRequest.getResourceName()));

                log.info("Require {} routers", required);
                success = provisionRouter(required);
            } else if ("broker".equals(resourceRequest.getResourceName()) && resourceRequest.getAmount() < 1) {
                int required = (int) Math.ceil(sumMap(resourceUsage)
                        - resourceUsage.getOrDefault("all", 0.0)
                        + neededMap.get(resourceRequest.getResourceName()));

                log.info("Require {} pooled brokers", required);
                ResourceDefinition resourceDefinition = addressResolver.getResourceDefinition(addressPlan, resourceRequest.getResourceName());
                success = provisionBroker(resourceRequest.getResourceName(), resourceDefinition, required, null);
                if (success) {
                    success = scheduleAddress(resourceRequest.getResourceName(), address, resourceUsage, resourceRequest.getAmount());
                }
            } else if ("broker".equals(resourceRequest.getResourceName())) {
                int required = (int) Math.ceil(resourceUsage.getOrDefault("all", 0.0)
                        + neededMap.get(resourceRequest.getResourceName()));

                log.info("Require {} sharded brokers", required);
                ResourceDefinition resourceDefinition = addressResolver.getResourceDefinition(addressPlan, resourceRequest.getResourceName());
                success = provisionBroker(address.getName(), resourceDefinition, required, address);
            }

            if (success) {
                numOk++;
            }
        }

        if (numOk < addressPlan.getRequiredResources().size()) {
            log.warn("Error provisioning resources for {}", address);
        } else {
            address.getStatus().setPhase(Status.Phase.Configuring);
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
                log.info("Creating broker set with id {} and {} replicas", cluster.getClusterId(), numReplicas);
                kubernetes.create(cluster.getResources());
                eventLogger.log(BrokerCreated, "Created broker", Normal, Broker, cluster.getClusterId());
            }
            return true;
        } catch (Exception e) {
            log.error("Error creating broker", e);
            eventLogger.log(BrokerCreateFailed, "Error creating broker", Warning, Broker, clusterId);
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
