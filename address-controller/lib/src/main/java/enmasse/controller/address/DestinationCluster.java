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

import enmasse.controller.model.Destination;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Represents a cluster of resources for a given destination.
 */
public class DestinationCluster {
    private static final Logger log = LoggerFactory.getLogger(DestinationCluster.class.getName());

    private final KubernetesList resources;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DestinationCluster that = (DestinationCluster) o;

        if (resources != null ? !resources.equals(that.resources) : that.resources != null) return false;
        return destinations != null ? destinations.equals(that.destinations) : that.destinations == null;
    }

    @Override
    public int hashCode() {
        int result = resources != null ? resources.hashCode() : 0;
        result = 31 * result + (destinations != null ? destinations.hashCode() : 0);
        return result;
    }

    private final Set<Destination> destinations;

    public DestinationCluster(Set<Destination> destinations, KubernetesList resources) {
        this.destinations = destinations;
        this.resources = resources;
    }

    public Set<Destination> getDestinations() {
        return destinations;
    }

    public KubernetesList getResources() {
        return resources;
    }

    public String getClusterId() {
        return destinations.iterator().next().group();
    }
}
