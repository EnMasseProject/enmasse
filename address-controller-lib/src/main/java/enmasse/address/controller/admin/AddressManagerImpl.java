/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.address.controller.admin;

import enmasse.address.controller.generator.DestinationClusterGenerator;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.Flavor;
import enmasse.address.controller.openshift.DestinationCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * The {@link AddressManagerImpl} maintains the number of destinations to be consistent with the number of destinations in config.
 */
public class AddressManagerImpl implements AddressManager {
    private static final Logger log = LoggerFactory.getLogger(AddressManagerImpl.class.getName());

    private final OpenShiftHelper helper;
    private final DestinationClusterGenerator generator;
    private final FlavorRepository flavorRepository;

    public AddressManagerImpl(OpenShiftHelper openshiftHelper, DestinationClusterGenerator generator, FlavorRepository flavorRepository) {
        this.helper = openshiftHelper;
        this.generator = generator;
        this.flavorRepository = flavorRepository;
    }

    @Override
    public synchronized void destinationsUpdated(Set<Destination> destinations) {
        Set<Destination> newDestinations = groupSharedDestinations(destinations);
        List<DestinationCluster> clusterList = helper.listClusters(flavorRepository);
        log.info("Brokers got updated to " + destinations.size() + " destinations across " + newDestinations.size() + " brokers. We have " + clusterList.size() + " brokers: " + clusterList.stream().map(DestinationCluster::getDestination).collect(Collectors.toList()));
        createBrokers(clusterList, newDestinations);
        deleteBrokers(clusterList, newDestinations);
    }

    private static Destination mergeDestinations(Destination destA, Destination destB) {
        return new Destination.Builder(destA).addresses(destB.addresses()).build();
    }

    private String getDestinationIdentifier(Destination dest) {
        Flavor flavor = dest.flavor()
                .map(f ->flavorRepository.getFlavor(f, TimeUnit.SECONDS.toMillis(60)))
                .orElse(new Flavor.Builder("direct", "direct").build());
        if (flavor.isShared()) {
            return flavor.name();
        } else {
            return dest.addresses().iterator().next();
        }
    }

    private Set<Destination> groupSharedDestinations(Set<Destination> newDestinations) {
        /*
         * 1. Group by flavor name
         * 2. Merge addresses on destinations having the same flavor
         * 3. Collect a new set
         */
        return newDestinations.stream()
                .collect(Collectors.groupingBy(
                        this::getDestinationIdentifier,
                        Collectors.reducing(AddressManagerImpl::mergeDestinations))).values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private static boolean brokerExists(Collection<DestinationCluster> clusterList, Destination destination) {
        return clusterList.stream()
                .anyMatch(cluster -> cluster.getDestination().equals(destination));
    }

    private void createBrokers(Collection<DestinationCluster> clusterList, Collection<Destination> newDestinations) {
        for (Destination dest : newDestinations) {
            if (!brokerExists(clusterList, dest)) {
                DestinationCluster cluster = generator.generateCluster(dest);
                if (cluster.isShared()) {
                    cluster.createReplace();
                } else {
                    cluster.create();
                }
            }
        }
    }


    private void deleteBrokers(Collection<DestinationCluster> clusterList, Collection<Destination> newDestinations) {
        clusterList.stream()
                .filter(cluster -> !cluster.isShared())
                .filter(cluster -> newDestinations.stream()
                        .noneMatch(destination -> cluster.getDestination().equals(destination)))
                .forEach(DestinationCluster::delete);

    }

    public synchronized Set<Destination> listDestinations() {
        return helper.listClusters(flavorRepository).stream().map(DestinationCluster::getDestination).collect(Collectors.toSet());
    }
}
