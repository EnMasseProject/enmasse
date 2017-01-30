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

import enmasse.address.controller.admin.OpenShiftHelper;
import enmasse.address.controller.model.DestinationGroup;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a cluster of resources for a given destination.
 */
public class DestinationCluster {
    private static final Logger log = LoggerFactory.getLogger(DestinationCluster.class.getName());

    private final OpenShiftHelper helper;
    private final KubernetesList resources;
    private DestinationGroup destinationGroup;

    public DestinationCluster(OpenShiftHelper helper, DestinationGroup destinationGroup, KubernetesList resources) {
        this.helper = helper;
        this.destinationGroup = destinationGroup;
        this.resources = resources;
    }

    public void create() {
        log.info("Adding " + resources.getItems().size() + " resources: " + resources.getItems().stream().map(r -> "name=" + r.getMetadata().getName() + ",kind=" + r.getKind()).collect(Collectors.joining(",")));
        helper.create(resources);
        updateDestinations(destinationGroup);
    }

    public void delete() {
        log.info("Deleting " + resources.getItems().size() + " resources: " + resources.getItems().stream().map(r -> "name=" + r.getMetadata().getName() + ",kind=" + r.getKind()).collect(Collectors.joining(",")));
        helper.delete(resources);
    }

    public DestinationGroup getDestinationGroup() {
        return destinationGroup;
    }

    public List<HasMetadata> getResources() {
        return resources.getItems();
    }

    public void updateDestinations(DestinationGroup destinationGroup) {
        this.destinationGroup = destinationGroup;
        helper.updateDestinations(destinationGroup);
    }
}
