/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

public class TestSchemaApi implements SchemaApi {
    public Schema getSchema() {
        return new Schema.Builder()
                .setAddressSpaceTypes(Collections.singletonList(
                        new AddressSpaceType.Builder()
                                .setName("type1")
                                .setDescription("Test Type")
                                .setAvailableEndpoints(Collections.singletonList(new EndpointSpec.Builder()
                                        .setName("messaging")
                                        .setService("messaging")
                                        .build()))
                                .setAddressTypes(Arrays.asList(
                                        new AddressType.Builder()
                                                .setName("anycast")
                                                .setDescription("Test direct")
                                                .setAddressPlans(Arrays.asList(
                                                        new AddressPlanBuilder()
                                                                .withMetadata(new ObjectMetaBuilder()
                                                                        .withName("plan1")
                                                                        .build())
                                                                .withAddressType("anycast")
                                                                .withRequiredResources(Arrays.asList(
                                                                        new ResourceRequestBuilder()
                                                                        .withName("router")
                                                                        .withCredit(1.0)
                                                                        .build()))
                                                                .build()
                                                ))
                                                .build(),
                                        new AddressType.Builder()
                                                .setName("queue")
                                                .setDescription("Test queue")
                                                .setAddressPlans(Arrays.asList(
                                                        new AddressPlanBuilder()
                                                                .withMetadata(new ObjectMetaBuilder()
                                                                        .withName("pooled-inmemory")
                                                                        .build())
                                                                .withAddressType("queue")
                                                                .withRequiredResources(Arrays.asList(
                                                                        new ResourceRequestBuilder()
                                                                                .withName("broker")
                                                                                .withCredit(0.1)
                                                                                .build()))
                                                                .build(),
                                                        new AddressPlanBuilder()
                                                                .withMetadata(new ObjectMetaBuilder()
                                                                        .withName("plan1")
                                                                        .build())
                                                                .withAddressType("queue")
                                                                .withRequiredResources(Arrays.asList(
                                                                        new ResourceRequestBuilder()
                                                                                .withName("broker")
                                                                                .withCredit(1.0)
                                                                                .build()))
                                                                .build())
                                                ).build()
                                ))
                                .setInfraConfigs(Arrays.asList(new StandardInfraConfigBuilder()
                                        .withMetadata(new ObjectMetaBuilder()
                                                .withName("infra")
                                                .build())
                                        .withSpec(new StandardInfraConfigSpecBuilder()
                                                .withVersion("1.0")
                                                .build())
                                        .build()))
                                .setInfraConfigDeserializer(json -> null)
                                .setAddressSpacePlans(Collections.singletonList(
                                        new AddressSpacePlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder()
                                                        .addToAnnotations(AnnotationKeys.DEFINED_BY, "infra")
                                                        .withName("myplan")
                                                        .build())
                                                .withAddressSpaceType("type1")
                                                .withResources(Arrays.asList(new ResourceAllowanceBuilder()
                                                        .withName("broker")
                                                        .withMin(0.0)
                                                        .withMax(1.0)
                                                        .build()))
                                                .withAddressPlans(Arrays.asList("plan1"))
                                                .build()
                                ))
                                .build()

                ))
                .build();
    }

    @Override
    public Watch watchSchema(Watcher<Schema> schemaStore, Duration resyncInterval) throws Exception {
        return null;
    }

}
