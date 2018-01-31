/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a cluster of resources for a given destination.
 */
public class AddressCluster {
    private static final Logger log = LoggerFactory.getLogger(AddressCluster.class.getName());

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressCluster that = (AddressCluster) o;

        if (!clusterId.equals(that.clusterId)) return false;
        return resources.equals(that.resources);
    }

    @Override
    public int hashCode() {
        int result = clusterId.hashCode();
        result = 31 * result + resources.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return clusterId;
    }

    private final String clusterId;
    private final KubernetesList resources;

    public AddressCluster(String clusterId, KubernetesList resources) {
        this.clusterId = clusterId;
        this.resources = resources;
    }

    public KubernetesList getResources() {
        return resources;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void scheduleAddresses(Kubernetes kubernetes, Set<Address> addresses) {
        Map<String, BrokerInfo> brokers = new HashMap<>();

        // Add all brokers we know about
        for (EndpointAddress endpoint : kubernetes.listBrokers(clusterId)) {
            brokers.put(endpoint.getHostname(), new BrokerInfo(endpoint.getHostname(), new HashSet<>()));
        }

        // Add all addresses referring to these brokers
        for (Address address : addresses) {
            String brokerId = address.getAnnotations().get(AnnotationKeys.BROKER_ID);
            if (brokerId != null) {
                BrokerInfo brokerInfo = brokers.get(brokerId);
                if (brokerInfo != null) {
                    brokerInfo.addAddress(address.getAddress());
                } else {
                    // Don't use unknown brokers when scheduling new addresses
                }
            }
        }

        // Distribute addresses across brokers with the fewest addresses
        PriorityQueue<BrokerInfo> brokerByNumAddresses = new PriorityQueue<>(brokers.size(), (a, b) -> {
            if (a.getNumAddresses() < b.getNumAddresses()) {
                return -1;
            } else if (a.getNumAddresses() > b.getNumAddresses()) {
                return 1;
            } else {
                return 0;
            }
        });
        brokerByNumAddresses.addAll(brokers.values());

        for (Address address : addresses) {
            String brokerId = address.getAnnotations().get(AnnotationKeys.BROKER_ID);
            if (brokerId == null) {
                BrokerInfo brokerInfo = brokerByNumAddresses.poll();
                address.getAnnotations().put(AnnotationKeys.BROKER_ID, brokerInfo.brokerId);
                brokerInfo.addAddress(address.getAddress());
                brokerByNumAddresses.offer(brokerInfo);
            }
        }
    }

    private static class BrokerInfo {
        private final String brokerId;
        private final Set<String> addresses;

        public String getBrokerId() {
            return brokerId;
        }

        public int getNumAddresses() {
            return addresses.size();
        }

        public void addAddress(String address) {
            addresses.add(address);
        }

        private BrokerInfo(String brokerId, Set<String> addresses) {
            this.brokerId = brokerId;
            this.addresses = new HashSet<>(addresses);
        }

    }
}
