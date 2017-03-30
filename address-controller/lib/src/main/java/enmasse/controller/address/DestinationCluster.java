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
import enmasse.controller.model.Destination;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a cluster of resources for a given destination.
 */
public class DestinationCluster {
    private static final Logger log = LoggerFactory.getLogger(DestinationCluster.class.getName());

    private final OpenShift openShift;
    private final KubernetesList resources;
    private Set<Destination> destinations;

    public DestinationCluster(OpenShift openShift, Set<Destination> destinations, KubernetesList resources) {
        this.openShift = openShift;
        this.destinations = destinations;
        this.resources = resources;
    }

    public void create() {
        log.info("Adding " + resources.getItems().size() + " resources: " + resources.getItems().stream().map(r -> "name=" + r.getMetadata().getName() + ",kind=" + r.getKind()).collect(Collectors.joining(",")));
        openShift.create(resources);
        updateDestinations(destinations);
    }

    public void delete() {
        log.info("Deleting " + resources.getItems().size() + " resources: " + resources.getItems().stream().map(r -> "name=" + r.getMetadata().getName() + ",kind=" + r.getKind()).collect(Collectors.joining(",")));
        openShift.delete(resources);
    }

    public Set<Destination> getDestinations() {
        return destinations;
    }

    public List<HasMetadata> getResources() {
        return resources.getItems();
    }

    public void updateDestinations(Set<Destination> destinations) {
        this.destinations = destinations;
        Destination first = destinations.iterator().next();
        // This is a workaround for direct addresses, which store everything in a single configmap that
        if (first.storeAndForward()) {
            openShift.updateDestinations(destinations);
        }
    }

    public String getClusterId() {
        return destinations.iterator().next().group();
    }
}
