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
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.openshift.DestinationCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@link AddressManagerImpl} maintains the number of destinations to be consistent with the number of destinations in config.
 */
public class AddressManagerImpl implements AddressManager {
    private static final Logger log = LoggerFactory.getLogger(AddressManagerImpl.class.getName());

    private final OpenShift openShift;
    private final DestinationClusterGenerator generator;

    public AddressManagerImpl(OpenShift openShift, DestinationClusterGenerator generator) {
        this.   openShift = openShift;
        this.generator = generator;
    }

    @Override
    public synchronized void destinationsUpdated(Set<DestinationGroup> newGroups) {
        newGroups.stream().forEach(AddressManagerImpl::validateDestinationGroup);

        List<DestinationCluster> clusterList = openShift.listClusters();
        log.info("Brokers got updated to " + newGroups.size() + " groups. We have " + clusterList.size() + " groups: " + clusterList.stream().map(DestinationCluster::getDestinationGroup).collect(Collectors.toList()));
        createBrokers(clusterList, newGroups);
        updateBrokers(clusterList, newGroups);
        deleteBrokers(clusterList, newGroups);
    }

    /*
     * Ensure that a destination groups meet the criteria of all destinations sharing the same properties, until we can
     * support a mix.
     */
    private static void validateDestinationGroup(DestinationGroup destinationGroup) {
        Iterator<Destination> it = destinationGroup.getDestinations().iterator();
        Destination first = it.next();
        while (it.hasNext()) {
            Destination current = it.next();
            if (current.storeAndForward() != first.storeAndForward() &&
                    current.multicast() != first.multicast() &&
                    current.flavor() != first.flavor()) {
                throw new IllegalArgumentException("All destinations in a destination group must share the same properties. Found: " + destinationGroup);
            }
        }
    }

    private static boolean brokerExists(Collection<DestinationCluster> clusterList, DestinationGroup destinationGroup) {
        return clusterList.stream()
                .anyMatch(cluster -> cluster.getDestinationGroup().equals(destinationGroup));
    }

    private void createBrokers(Collection<DestinationCluster> clusterList, Collection<DestinationGroup> newDestinationGroups) {
        newDestinationGroups.stream()
                .filter(group -> !brokerExists(clusterList, group))
                .map(generator::generateCluster)
                .forEach(DestinationCluster::create);
    }


    private void updateBrokers(Collection<DestinationCluster> clusterList, Collection<DestinationGroup> newDestinationGroups) {
        clusterList.forEach(cluster -> newDestinationGroups.forEach(destinationGroup -> {
            if (cluster.getDestinationGroup().equals(destinationGroup)) {
                cluster.updateDestinations(destinationGroup);
            }
        }));
    }

    private void deleteBrokers(Collection<DestinationCluster> clusterList, Collection<DestinationGroup> newDestinationGroups) {
        clusterList.stream()
                .filter(cluster -> newDestinationGroups.stream()
                        .noneMatch(destinationGroup -> cluster.getDestinationGroup().equals(destinationGroup)))
                .forEach(DestinationCluster::delete);
    }

    public synchronized Set<DestinationGroup> listDestinationGroups() {
        return openShift.listClusters().stream().map(DestinationCluster::getDestinationGroup).collect(Collectors.toSet());
    }
}
