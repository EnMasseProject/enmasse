/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.*;

import java.util.Arrays;

public class StandardControllerSchema {

    private AddressSpacePlan plan;
    private AddressSpaceType type;
    private Schema schema;

    public StandardControllerSchema() {
        plan = new AddressSpacePlan.Builder()
                .setName("plan1")
                .setResources(Arrays.asList(
                        new ResourceAllowance("router", 0.0, 1.0),
                        new ResourceAllowance("broker", 0.0, 3.0),
                        new ResourceAllowance("aggregate", 0.0, 3.0)))
                .setAddressSpaceType("standard")
                .setAddressPlans(Arrays.asList(
                        "small-anycast",
                        "small-queue"
                ))
                .build();

        type = new AddressSpaceType.Builder()
                .setName("standard")
                .setDescription("standard")
                .setAddressSpacePlans(Arrays.asList(plan))
                .setServiceNames(Arrays.asList("messaging"))
                .setAddressTypes(Arrays.asList(
                        new AddressType.Builder()
                                .setName("anycast")
                                .setDescription("anycast")
                                .setAddressPlans(Arrays.asList(
                                        new AddressPlan.Builder()
                                        .setName("small-anycast")
                                        .setAddressType("anycast")
                                        .setRequestedResources(Arrays.asList(
                                                new ResourceRequest("router", 0.2)))
                                        .build()))
                                .build(),
                        new AddressType.Builder()
                                .setName("queue")
                                .setDescription("queue")
                                .setAddressPlans(Arrays.asList(
                                        new AddressPlan.Builder()
                                                .setName("small-queue")
                                                .setAddressType("queue")
                                                .setRequestedResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.2),
                                                        new ResourceRequest("broker", 0.4)))
                                                .build(),
                                        new AddressPlan.Builder()
                                                .setName("large-queue")
                                                .setAddressType("queue")
                                                .setRequestedResources(Arrays.asList(
                                                        new ResourceRequest("router", 0.2),
                                                        new ResourceRequest("broker", 2.0)))
                                                .build()))
                                .build()))
                .build();

        schema = new Schema.Builder()
                .setAddressSpaceTypes(Arrays.asList(type))
                .setResourceDefinitions(Arrays.asList(
                        new ResourceDefinition.Builder()
                            .setName("router")
                            .build(),
                        new ResourceDefinition.Builder()
                            .setName("broker")
                            .build()))
                .build();
    }

    public AddressSpacePlan getPlan() {
        return plan;
    }

    public AddressSpaceType getType() {
        return type;
    }

    public Schema getSchema() {
        return schema;
    }
}
