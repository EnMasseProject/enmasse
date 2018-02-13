/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestSchemaApi implements SchemaApi {
    private final Set<String> copiedTo = new HashSet<>();

    public Set<String> getCopiedTo() {
        return new HashSet<>(copiedTo);
    }

    @Override
    public void copyIntoNamespace(AddressSpacePlan plan, String otherNamespace) {
        copiedTo.add(otherNamespace);
    }

    @Override
    public Schema getSchema() {
        return new Schema.Builder()
                .setAddressSpaceTypes(Collections.singletonList(
                        new AddressSpaceType.Builder()
                                .setName("type1")
                                .setDescription("Test Type")
                                .setServiceNames(Collections.singletonList("messaging"))
                                .setAddressTypes(Arrays.asList(
                                        new AddressType.Builder()
                                                .setName("anycast")
                                                .setDescription("Test direct")
                                                .setAddressPlans(Arrays.asList(
                                                        new AddressPlan.Builder()
                                                                .setName("plan1")
                                                                .setAddressType("anycast")
                                                                .setRequestedResources(Collections.singletonList(
                                                                        new ResourceRequest("router", 1.0)
                                                                ))
                                                                .build()
                                                ))
                                                .build(),
                                        new AddressType.Builder()
                                                .setName("queue")
                                                .setDescription("Test queue")
                                                .setAddressPlans(Arrays.asList(
                                                        new AddressPlan.Builder()
                                                                .setName("pooled-inmemory")
                                                                .setAddressType("queue")
                                                                .setRequestedResources(Collections.singletonList(
                                                                        new ResourceRequest("broker", 0.1)
                                                                ))
                                                                .build(),
                                                        new AddressPlan.Builder()
                                                                .setName("plan1")
                                                                .setAddressType("queue")
                                                                .setRequestedResources(Collections.singletonList(
                                                                        new ResourceRequest("broker", 1.0)
                                                                ))
                                                                .build()
                                                ))
                                                .build()
                                ))
                                .setAddressSpacePlans(Collections.singletonList(
                                        new AddressSpacePlan.Builder()
                                                .setName("myplan")
                                                .setAddressSpaceType("type1")
                                                .setResources(Collections.singletonList(
                                                        new ResourceAllowance("broker", 0.0, 1.0)
                                                ))
                                                .setAddressPlans(Collections.singletonList("plan1"))
                                                .build()
                                ))
                                .build()

                ))
                .setResourceDefinitions(Collections.singletonList(
                        new ResourceDefinition.Builder()
                                .setName("broker")
                                .setTemplateName("broker-template")
                        .build()
                ))
                .build();
    }
}
