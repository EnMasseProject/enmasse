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

package enmasse.storage.controller.openshift;

import com.openshift.restclient.model.IResource;
import enmasse.storage.controller.model.Destination;

import java.util.Collection;

/**
 * Represents a storage cluster of broker and volume resources for a given destination.
 */
public class StorageCluster {

    private final OpenshiftClient client;
    private final Destination destination;
    private final Collection<IResource> resources;

    public StorageCluster(OpenshiftClient osClient, Destination destination, Collection<IResource> resources) {
        this.client = osClient;
        this.destination = destination;
        this.resources = resources;
    }

    public void create() {
        client.createResources(resources);
    }

    public void delete() {
        client.deleteResources(resources);
    }

    public Destination getDestination() {
        return destination;
    }
}
