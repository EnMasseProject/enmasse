/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpaceTypeBuilder;
import io.enmasse.address.model.AddressTypeBuilder;
import io.enmasse.address.model.EndpointSpecBuilder;
import io.enmasse.address.model.Schema;
import io.enmasse.address.model.SchemaBuilder;
import io.enmasse.admin.model.v1.AddressPlanBuilder;
import io.enmasse.admin.model.v1.AddressSpacePlanBuilder;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class TestSchemaApi implements SchemaApi {
    public Schema getSchema() {
        return new SchemaBuilder()
                .withAddressSpaceTypes(Arrays.asList(
                        new AddressSpaceTypeBuilder()
                                .withName("brokered")
                                .withDescription("Test Type")
                                .withAddressTypes(Collections.singletonList(
                                        new AddressTypeBuilder()
                                                .withName("queue")
                                                .withPlans(Arrays.asList(
                                                        new AddressPlanBuilder()
                                                                .withMetadata(new ObjectMetaBuilder()
                                                                        .withName("plan1")
                                                                        .build())
                                                                .editOrNewSpec()
                                                                .withAddressType("queue")
                                                                .withResources(Map.of("broker", 0.1))
                                                                .endSpec()
                                                                .build()
                                                ))
                                                .build()))
                            .build(),
                        new AddressSpaceTypeBuilder()
                                .withName("type1")
                                .withDescription("Test Type")
                                .withAvailableEndpoints(Collections.singletonList(new EndpointSpecBuilder()
                                        .withName("messaging")
                                        .withService("messaging")
                                        .build()))
                                .withAddressTypes(Arrays.asList(
                                        new AddressTypeBuilder()
                                                .withName("anycast")
                                                .withDescription("Test direct")
                                                .withPlans(Arrays.asList(
                                                        new AddressPlanBuilder()
                                                                .withMetadata(new ObjectMetaBuilder()
                                                                        .withName("plan1")
                                                                        .build())
                                                                .editOrNewSpec()
                                                                .withAddressType("anycast")
                                                                .withResources(Map.of("router", 1.0))
                                                                .endSpec()
                                                                .build()
                                                ))
                                                .build(),
                                        new AddressTypeBuilder()
                                                .withName("queue")
                                                .withDescription("Test queue")
                                                .withPlans(Arrays.asList(
                                                        new AddressPlanBuilder()
                                                                .withMetadata(new ObjectMetaBuilder()
                                                                        .withName("pooled-inmemory")
                                                                        .build())
                                                                .editOrNewSpec()
                                                                .withAddressType("queue")
                                                                .withResources(Map.of("broker", 0.1))
                                                                .endSpec()
                                                                .build(),
                                                        new AddressPlanBuilder()
                                                                .withMetadata(new ObjectMetaBuilder()
                                                                        .withName("plan1")
                                                                        .build())
                                                                .editOrNewSpec()
                                                                .withAddressType("queue")
                                                                .withResources(Map.of("broker", 1.0))
                                                                .endSpec()
                                                                .build())
                                                ).build()
                                ))
                                .withInfraConfigs(Arrays.asList(new StandardInfraConfigBuilder()
                                        .withMetadata(new ObjectMetaBuilder()
                                                .withName("infra")
                                                .build())
                                        .withSpec(new StandardInfraConfigSpecBuilder()
                                                .withVersion("1.0")
                                                .build())
                                        .build()))
                                .withPlans(Arrays.asList(
                                        new AddressSpacePlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder()
                                                        .withName("myplan")
                                                        .build())
                                                .editOrNewSpec()
                                                .withInfraConfigRef("infra")
                                                .withAddressSpaceType("brokered")
                                                .withResourceLimits(Map.of("broker", 1.0))
                                                .withAddressPlans(Arrays.asList("plan1"))
                                                .endSpec()
                                                .build(),
                                        new AddressSpacePlanBuilder()
                                                .withMetadata(new ObjectMetaBuilder()
                                                        .withName("myplan")
                                                        .build())
                                                .editOrNewSpec()
                                                .withInfraConfigRef("infra")
                                                .withAddressSpaceType("type1")
                                                .withResourceLimits(Map.of("broker", 1.0))
                                                .withAddressPlans(Arrays.asList("plan1"))
                                                .endSpec()
                                                .build()
                                ))
                                .build()

                ))
                .withAuthenticationServices(new AuthenticationServiceBuilder()
                        .withNewMetadata()
                        .withName("standard")
                        .endMetadata()
                        .withNewSpec()
                        .withNewStandard()
                        .endStandard()
                        .endSpec()
                        .withNewStatus()
                        .withHost("example.com")
                        .withPort(5671)
                        .endStatus()
                        .build())
                .build();
    }

    @Override
    public Watch watchSchema(Watcher<Schema> schemaStore, Duration resyncInterval) throws Exception {
        return null;
    }

}
