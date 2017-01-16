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

    private Set<Destination> groupSharedDestinations(Set<Destination> newDestinations) {
        Set<Destination> destByFlavor = new HashSet<>();
        //log.info("Grouping destinations : " + newDestinations);
        for (Destination newDest : newDestinations) {
            Flavor flavor = newDest.flavor()
                    .map(f ->flavorRepository.getFlavor(f, TimeUnit.SECONDS.toMillis(60)))
                    .orElse(new Flavor.Builder("direct", "direct").build());
            if (flavor.isShared()) {
                if (destByFlavor.isEmpty()) {
                    destByFlavor.add(newDest);
                } else {
                    Iterator<Destination> destIt = destByFlavor.iterator();
                    // TODO: Rework this loop by introducing a Destination builder class
                    while (destIt.hasNext()) {
                        Destination dest = destIt.next();
                        //log.info("Comparing " + dest + " against " + newDest);
                        if (dest.flavor().get().equals(newDest.flavor().get())) {
                            Set<String> mergedAddresses = new HashSet<>(dest.addresses());
                            mergedAddresses.addAll(newDest.addresses());
                            Destination merged = new Destination(mergedAddresses, dest.storeAndForward(), dest.multicast(), dest.flavor());
                            destIt.remove();
                            destByFlavor.add(merged);
                            break;
                            //log.info("Merged into new dest: " + merged);
                        }
                    }
                }
                //log.info("Found shared flavor, collected multiple addresses into destinations: " + destByFlavor);
            } else {
                //log.info("Flavor is not shared, deploying as normal");
                destByFlavor.add(newDest);
            }
        }
        return destByFlavor;
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
