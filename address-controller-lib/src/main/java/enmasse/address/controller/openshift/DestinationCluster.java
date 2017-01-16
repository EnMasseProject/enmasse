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

package enmasse.address.controller.openshift;

import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.Flavor;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a cluster of resources for a given destination.
 */
public class DestinationCluster {
    private static final Logger log = LoggerFactory.getLogger(DestinationCluster.class.getName());

    private final OpenShiftClient client;
    private final Destination destination;
    private final KubernetesList resources;
    private final boolean shared;

    public DestinationCluster(OpenShiftClient osClient, Destination destination, KubernetesList resources, boolean shared) {
        this.client = osClient;
        this.destination = destination;
        this.resources = resources;
        this.shared = shared;
    }

    public void create() {
        log.info("Adding " + resources.getItems().size() + " resources: " + resources.getItems().stream().map(r -> "name=" + r.getMetadata().getName() + ",kind=" + r.getKind()).collect(Collectors.joining(",")));
        client.lists().create(resources);
    }

    public void createReplace() {
        for (Object item : resources.getItems()) {
            client.resource((HasMetadata) item).createOrReplace();
        }
    }

    public void delete() {
        log.info("Deleting " + resources.getItems().size() + " resources: " + resources.getItems().stream().map(r -> "name=" + r.getMetadata().getName() + ",kind=" + r.getKind()).collect(Collectors.joining(",")));
        client.lists().delete(resources);
    }

    public Destination getDestination() {
        return destination;
    }

    public List<HasMetadata> getResources() {
        return resources.getItems();
    }

    public boolean isShared() {
        return shared;
    }
}
