/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.enmasse.address.model.Address;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.StandardInfraConfig;

/**
 * Generates clusters for a set of addresses.
 */
public interface BrokerSetGenerator {

    /**
     * Generate broker set
     *  @param clusterId The id of the cluster
     * @param numReplicas Number of replicas for the initial set
     * @param address Address to pass as template parameter (null is allowed)
     * @param addressPlan
     */
    BrokerCluster generateCluster(String clusterId, int numReplicas, Address address, AddressPlan addressPlan, StandardInfraConfig standardInfraConfig) throws Exception;
}
