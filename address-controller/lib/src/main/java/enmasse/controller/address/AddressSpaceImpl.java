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

package enmasse.controller.address;

import enmasse.controller.common.OpenShift;
import enmasse.controller.common.DestinationClusterGenerator;
import enmasse.controller.model.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The {@link AddressSpaceImpl} maintains the number of destinations to be consistent with the number of destinations in config.
 */
public class AddressSpaceImpl implements AddressSpace {
    private static final Logger log = LoggerFactory.getLogger(AddressSpaceImpl.class.getName());

    private final OpenShift openShift;
    private final DestinationClusterGenerator generator;

    public AddressSpaceImpl(OpenShift openShift, DestinationClusterGenerator generator) {
        this.openShift = openShift;
        this.generator = generator;
    }

    @Override
    public Set<Destination> addDestination(Destination destination) {
        List<DestinationCluster> clusterList = openShift.listClusters();
        Set<Destination> destinations = getClusterDestinations(clusterList);
        destinations.add(destination);
        setDestinations(destinations, clusterList);
        return destinations;
    }

    @Override
    public Set<Destination> addDestinations(Set<Destination> destinations) {
        List<DestinationCluster> clusterList = openShift.listClusters();
        Set<Destination> currentDestinations = getClusterDestinations(clusterList);
        currentDestinations.addAll(destinations);
        setDestinations(destinations, clusterList);
        return currentDestinations;
    }

    private Set<Destination> getClusterDestinations(List<DestinationCluster> clusterList) {
        return clusterList.stream().map(DestinationCluster::getDestinations).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public Set<Destination> deleteDestination(String address) {
        return deleteWithPredicate(destination -> destination.address().equals(address));
    }

    private Set<Destination> deleteWithPredicate(Predicate<Destination> predicate) {
        List<DestinationCluster> clusterList = openShift.listClusters();
        Set<Destination> destinations = getClusterDestinations(clusterList);
        destinations.removeIf(predicate::test);
        setDestinations(destinations, clusterList);
        return destinations;
    }

    @Override
    public Set<Destination> deleteDestinationWithUuid(String uuid) {
        return deleteWithPredicate(destination -> destination.uuid().filter(u -> u.equals(uuid)).isPresent());
    }

    @Override
    public Set<Destination> setDestinations(Set<Destination> destinations) {
        List<DestinationCluster> clusterList = openShift.listClusters();
        setDestinations(destinations, clusterList);
        return destinations;
    }

    /**
     * Set the destinations for this address space.
     */
    private void setDestinations(Set<Destination> newDestinations, List<DestinationCluster> clusterList) {
        Map<String, Set<Destination>> destinationByGroup = newDestinations.stream().collect(Collectors.groupingBy(Destination::group, Collectors.toSet()));
        validateDestinationGroups(destinationByGroup);


        log.info("Brokers got updated to " + destinationByGroup.size() + " groups. We have " + clusterList.size() + " groups: " + getClusterDestinations(clusterList));
        createBrokers(clusterList, destinationByGroup);
        updateBrokers(clusterList, destinationByGroup);
        deleteBrokers(clusterList, destinationByGroup);
    }


    /**
     * Return the destinations for this address space.
     */
    @Override
    public synchronized Set<Destination> getDestinations() {
        return getClusterDestinations(openShift.listClusters());
    }

    /*
     * Ensure that a destination groups meet the criteria of all destinations sharing the same properties, until we can
     * support a mix.
     */
    private static void validateDestinationGroups(Map<String, Set<Destination>> destinationByGroup) {
        for (Map.Entry<String, Set<Destination>> entry : destinationByGroup.entrySet()) {
            Iterator<Destination> it = entry.getValue().iterator();
            Destination first = it.next();
            while (it.hasNext()) {
                Destination current = it.next();
                if (current.storeAndForward() != first.storeAndForward() ||
                    current.multicast() != first.multicast() ||
                    !current.flavor().equals(first.flavor()) ||
                    !current.group().equals(first.group())) {

                    throw new IllegalArgumentException("All destinations in a destination group must share the same properties. Found: " + current + " and " + first);
                }
            }
        }
    }

    private static boolean brokerExists(Collection<DestinationCluster> clusterList, String groupId) {
        return clusterList.stream().anyMatch(cluster -> cluster.getClusterId().equals(groupId));
    }

    private void createBrokers(Collection<DestinationCluster> clusterList, Map<String, Set<Destination>> newDestinationGroups) {
        newDestinationGroups.entrySet().stream()
                .filter(group -> !brokerExists(clusterList, group.getKey()))
                .map(group -> {
                    return generator.generateCluster(group.getValue());
                })
                .forEach(DestinationCluster::create);
    }


    private void updateBrokers(Collection<DestinationCluster> clusterList, Map<String, Set<Destination>> newDestinationGroups) {
        clusterList.forEach(cluster -> newDestinationGroups.forEach((groupId, list) -> {
            if (cluster.getClusterId().equals(groupId)) {
                cluster.updateDestinations(list);
            }
        }));
    }

    private void deleteBrokers(Collection<DestinationCluster> clusterList, Map<String, Set<Destination>> newDestinationGroups) {
        clusterList.stream()
                .filter(cluster -> newDestinationGroups.entrySet().stream()
                        .noneMatch(destinationGroup -> cluster.getClusterId().equals(destinationGroup.getKey())))
                .forEach(DestinationCluster::delete);
    }
}
