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

package enmasse.storage.controller.admin;

import enmasse.storage.controller.generator.StorageGenerator;
import enmasse.storage.controller.model.Destination;
import enmasse.storage.controller.openshift.StorageCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@link AddressManagerImpl} maintains the number of destinations to be consistent with the number of destinations in config.
 */
public class AddressManagerImpl implements AddressManager {
    private static final Logger log = LoggerFactory.getLogger(AddressManagerImpl.class.getName());

    private final OpenShiftHelper helper;
    private final StorageGenerator generator;

    public AddressManagerImpl(OpenShiftHelper openshiftHelper, StorageGenerator generator) {
        this.helper = openshiftHelper;
        this.generator = generator;
    }

    @Override
    public synchronized void destinationsUpdated(Set<Destination> destinations) {
        List<StorageCluster> clusterList = helper.listClusters();
        log.info("Brokers got updated to " + destinations.size() + " destinations, we have " + clusterList.size() + " destinations: " + clusterList.stream().map(StorageCluster::getDestination).collect(Collectors.toList()));
        createBrokers(clusterList, destinations);
        deleteBrokers(clusterList, destinations);
    }

    private static boolean brokerExists(Collection<StorageCluster> clusterList, Destination destination) {
        return clusterList.stream()
                .filter(cluster -> destination.equals(cluster.getDestination()))
                .findAny()
                .isPresent();
    }

    private void createBrokers(Collection<StorageCluster> clusterList, Collection<Destination> newDestinations) {
        newDestinations.stream()
                .filter(destination -> !brokerExists(clusterList, destination))
                .map(destination -> generator.generateStorage(destination))
                .forEach(StorageCluster::create);
    }



    private void deleteBrokers(Collection<StorageCluster> clusterList, Collection<Destination> newDestinations) {
        clusterList.stream()
                .filter(cluster -> !newDestinations.stream()
                        .filter(destination -> destination.equals(cluster.getDestination()))
                        .findAny()
                        .isPresent())
                .forEach(StorageCluster::delete);

    }

    public synchronized Set<Destination> listDestinations() {
        return helper.listClusters().stream().map(c -> c.getDestination()).collect(Collectors.toSet());
    }
}
