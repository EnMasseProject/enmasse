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

import java.util.Set;

/**
 * Generates clusters for a set of addresses.
 */
public interface AddressClusterGenerator {

    /**
     * Generate cluster for a given address.
     *
     * @param clusterId The id of the cluster
     * @param addressSet The set of addresses to generate a cluster for.
     */
    AddressCluster generateCluster(String clusterId, Set<Address> addressSet);
}
