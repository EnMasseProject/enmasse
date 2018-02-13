/*
 * Copyright 2016, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
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
