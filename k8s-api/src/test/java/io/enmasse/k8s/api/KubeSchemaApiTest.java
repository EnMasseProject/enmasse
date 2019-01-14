/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.Schema;
import io.enmasse.admin.model.v1.*;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class KubeSchemaApiTest {

    private AddressSpacePlanApi addressSpacePlanApi;
    private AddressPlanApi addressPlanApi;
    private StandardInfraConfigApi standardInfraConfigApi;
    private BrokeredInfraConfigApi brokeredInfraConfigApi;

    @BeforeEach
    public void setup() {
        addressSpacePlanApi = mock(AddressSpacePlanApi.class);
        addressPlanApi = mock(AddressPlanApi.class);
        standardInfraConfigApi = mock(StandardInfraConfigApi.class);
        brokeredInfraConfigApi = mock(BrokeredInfraConfigApi.class);
    }

    @Test
    public void testSchemaAssemble() {
        KubeSchemaApi schemaApi = new KubeSchemaApi(addressSpacePlanApi, addressPlanApi, brokeredInfraConfigApi, standardInfraConfigApi, Clock.systemUTC(), false);

        List<AddressSpacePlan> addressSpacePlans = Arrays.asList(
                new AddressSpacePlanBuilder()
                        .withNewMetadata()
                        .withName("spaceplan1")
                        .addToAnnotations(AnnotationKeys.DEFINED_BY, "infra1")
                        .endMetadata()
                        .withAddressSpaceType("standard")
                        .withAddressPlans(Arrays.asList("plan1", "plan2", "plan4"))
                        .withResources(Arrays.asList(new ResourceAllowance("broker", 1), new ResourceAllowance("router", 1), new ResourceAllowance("aggregate", 1)))
                        .build(),
                new AddressSpacePlanBuilder()
                        .withNewMetadata()
                        .withName("spaceplan2")
                        .addToAnnotations(AnnotationKeys.DEFINED_BY, "infra1")
                        .endMetadata()
                        .withAddressSpaceType("brokered")
                        .withAddressPlans(Arrays.asList( "plan3"))
                        .withResources(Arrays.asList(new ResourceAllowance("broker", 1)))
                        .build());

        List<AddressPlan> addressPlans = Arrays.asList(
                new AddressPlanBuilder()
                        .withNewMetadata()
                        .withName("plan1")
                        .endMetadata()
                        .withAddressType("queue")
                        .withRequiredResources(new ResourceRequest("broker", 0.1), new ResourceRequest("router", 0.01))
                        .build(),
                new AddressPlanBuilder()
                        .withNewMetadata()
                        .withName("plan2")
                        .endMetadata()
                        .withAddressType("topic")
                        .withRequiredResources(new ResourceRequest("broker", 0.1), new ResourceRequest("router", 0.01))
                        .build(),
                new AddressPlanBuilder()
                        .withMetadata(new ObjectMetaBuilder()
                                .withName("plan4")
                                .build())
                        .withAddressType("anycast")
                        .withRequiredResources(new ResourceRequest("router", 0.01))
                        .build(),
                new AddressPlanBuilder()
                        .withNewMetadata()
                        .withName("plan3")
                        .endMetadata()
                        .withAddressType("queue")
                        .withRequiredResources(new ResourceRequest("broker", 0.1))
                        .build());

        List<StandardInfraConfig> standardInfraConfigs = Arrays.asList(
                new StandardInfraConfigBuilder()
                        .withNewMetadata()
                        .withName("infra1")
                        .endMetadata()
                        .build());

        List<BrokeredInfraConfig> brokeredInfraConfigs = Arrays.asList(
                new BrokeredInfraConfigBuilder()
                        .withNewMetadata()
                        .withName("infra1")
                        .endMetadata()
                        .build());

        Schema schema = schemaApi.assembleSchema(addressSpacePlans, addressPlans, standardInfraConfigs, brokeredInfraConfigs);

        assertTrue(schema.findAddressSpaceType("standard").isPresent());
        assertTrue(schema.findAddressSpaceType("brokered").isPresent());

        {
            AddressSpaceType type = schema.findAddressSpaceType("standard").get();
            assertTrue(type.findAddressSpacePlan("spaceplan1").isPresent());
            assertFalse(type.findAddressSpacePlan("spaceplan2").isPresent());
            assertTrue(type.findAddressSpacePlan("spaceplan1").get().getAddressPlans().contains("plan1"));
            assertTrue(type.findAddressSpacePlan("spaceplan1").get().getAddressPlans().contains("plan2"));
            assertTrue(type.findAddressSpacePlan("spaceplan1").get().getAddressPlans().contains("plan4"));
            assertTrue(type.findInfraConfig("infra1").isPresent());

            assertTrue(type.findAddressType("queue").get().findAddressPlan("plan1").isPresent());
            assertTrue(type.findAddressType("topic").get().findAddressPlan("plan2").isPresent());
            assertTrue(type.findAddressType("anycast").get().findAddressPlan("plan4").isPresent());
        }
        {
            AddressSpaceType type = schema.findAddressSpaceType("brokered").get();
            assertTrue(type.findAddressSpacePlan("spaceplan2").isPresent());
            assertFalse(type.findAddressSpacePlan("spaceplan1").isPresent());
            assertTrue(type.findAddressSpacePlan("spaceplan2").get().getAddressPlans().contains("plan3"));
            assertFalse(type.findAddressSpacePlan("spaceplan2").get().getAddressPlans().contains("plan1"));
            assertFalse(type.findAddressSpacePlan("spaceplan2").get().getAddressPlans().contains("plan2"));
            assertTrue(type.findInfraConfig("infra1").isPresent());

            assertTrue(type.findAddressType("queue").get().findAddressPlan("plan3").isPresent());
        }
    }

    @Test
    public void testWatchCreated() throws Exception {
        AddressSpacePlanApi addressSpacePlanApi = mock(AddressSpacePlanApi.class);
        AddressPlanApi addressPlanApi = mock(AddressPlanApi.class);
        StandardInfraConfigApi standardInfraConfigApi = mock(StandardInfraConfigApi.class);
        BrokeredInfraConfigApi brokeredInfraConfigApi = mock(BrokeredInfraConfigApi.class);

        Watch mockWatch = mock(Watch.class);

        when(addressSpacePlanApi.watchAddressSpacePlans(any(), any())).thenReturn(mockWatch);
        when(addressPlanApi.watchAddressPlans(any(), any())).thenReturn(mockWatch);
        when(brokeredInfraConfigApi.watchBrokeredInfraConfigs(any(), any())).thenReturn(mockWatch);
        when(standardInfraConfigApi.watchStandardInfraConfigs(any(), any())).thenReturn(mockWatch);

        SchemaApi schemaApi = new KubeSchemaApi(addressSpacePlanApi, addressPlanApi, brokeredInfraConfigApi, standardInfraConfigApi, Clock.systemUTC(), true);

        schemaApi.watchSchema(items -> { }, Duration.ofSeconds(5));
        verify(addressSpacePlanApi).watchAddressSpacePlans(any(), eq(Duration.ofSeconds(5)));
        verify(addressPlanApi).watchAddressPlans(any(), eq(Duration.ofSeconds(5)));
        verify(standardInfraConfigApi).watchStandardInfraConfigs(any(), eq(Duration.ofSeconds(5)));
        verify(brokeredInfraConfigApi).watchBrokeredInfraConfigs(any(), eq(Duration.ofSeconds(5)));
    }
}
