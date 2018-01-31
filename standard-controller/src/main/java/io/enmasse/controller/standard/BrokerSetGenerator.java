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
package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.ResourceDefinition;

import java.util.Optional;
import java.util.Set;

/**
 * Generates clusters for a set of addresses.
 */
public interface BrokerSetGenerator {

    /**
     * Generate broker set
     *
     * @param clusterId The id of the cluster
     * @param resourceDefinition Definition of the broker resource
     * @param numReplicas Number of replicas for the initial set
     * @param address Address to pass as template parameter (null is allowed)
     */
    AddressCluster generateCluster(String clusterId, ResourceDefinition resourceDefinition, int numReplicas, Address address);
}
