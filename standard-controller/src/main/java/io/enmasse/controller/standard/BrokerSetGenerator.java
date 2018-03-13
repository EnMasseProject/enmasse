/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.ResourceDefinition;

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
    BrokerCluster generateCluster(String clusterId, ResourceDefinition resourceDefinition, int numReplicas, Address address);
}
