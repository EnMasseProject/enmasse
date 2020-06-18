/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.ttl;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestBaseIsolated;
import io.enmasse.systemtest.broker.ArtemisUtils;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.utils.AddressUtils;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

import java.util.Map;

import static io.enmasse.systemtest.TestTag.ISOLATED;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag(ISOLATED)
class TtlTest extends TestBase implements ITestBaseIsolated {
    private static Logger log = CustomLogger.getLogger();

    @ParameterizedTest(name = "tesAddressSpecified-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void tesAddressSpecified(String type) throws Exception {
        doTestTtl(type, null, new TtlBuilder().withMinimum(500).withMaximum(5000).build(),
                new TtlBuilder().withMinimum(500).withMaximum(5000).build());
    }

    @ParameterizedTest(name = "tesAddressPlanSpecifiedTtl-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void tesAddressPlanSpecified(String type) throws Exception {
        doTestTtl(type, new TtlBuilder().withMinimum(500).withMaximum(5000).build(), null,
                new TtlBuilder().withMinimum(500).withMaximum(5000).build());
    }

    @ParameterizedTest(name = "testOverriding-{0}-space")
    @ValueSource(strings = {"standard", "brokered"})
    void testOverriding(String type) throws Exception {
        doTestTtl(type, new TtlBuilder().withMinimum(500).withMaximum(5000).build(), new TtlBuilder().withMinimum(550).withMaximum(6000).build(),
                new TtlBuilder().withMinimum(550 /* higher addr min takes priority */).withMaximum(5000 /* lower max plan takes priority */).build());
    }

    private void doTestTtl(String type, Ttl addrPlanTtl, Ttl addrTtl, Ttl expectedTtl) throws Exception {
        final String infraConfigName = "ttl-infra";
        final String spacePlanName = "space-plan-ttl";
        final String addrPlanName = "addr-plan-ttl";

        final String baseSpacePlan;
        final String baseAddressPlan;

        PodTemplateSpec brokerInfraTtlOverride = new PodTemplateSpecBuilder()
                .withNewSpec()
                .withInitContainers(new ContainerBuilder()
                        .withName("broker-plugin")
                        .withEnv(new EnvVar("MESSAGE_EXPIRY_SCAN_PERIOD", "1000", null)).build()).endSpec().build();

        if ("standard".equals(type)) {
            baseSpacePlan =  AddressSpacePlans.STANDARD_SMALL;
            baseAddressPlan = DestinationPlan.STANDARD_SMALL_QUEUE;
            StandardInfraConfig infraConfig = isolatedResourcesManager.getStandardInfraConfig("default");
            StandardInfraConfig ttlInfra = new StandardInfraConfigBuilder()
                    .withNewMetadata()
                    .withName(infraConfigName)
                    .endMetadata()
                    .withNewSpecLike(infraConfig.getSpec())
                    .withNewBrokerLike(infraConfig.getSpec().getBroker())
                    .withPodTemplate(brokerInfraTtlOverride)
                    .endBroker()
                    .endSpec()
                    .build();
            isolatedResourcesManager.createInfraConfig(ttlInfra);
        } else {
            baseSpacePlan =  AddressSpacePlans.BROKERED;
            baseAddressPlan = DestinationPlan.BROKERED_QUEUE;
            BrokeredInfraConfig infraConfig = isolatedResourcesManager.getBrokeredInfraConfig("default");
            BrokeredInfraConfig ttlInfra = new BrokeredInfraConfigBuilder()
                    .withNewMetadata()
                    .withName(infraConfigName)
                    .endMetadata()
                    .withNewSpecLike(infraConfig.getSpec())
                    .withNewBrokerLike(infraConfig.getSpec().getBroker())
                    .withPodTemplate(brokerInfraTtlOverride)
                    .endBroker()
                    .endSpec()
                    .build();
            isolatedResourcesManager.createInfraConfig(ttlInfra);
        }

        AddressSpacePlan smallSpacePlan = kubernetes.getAddressSpacePlanClient().withName(baseSpacePlan).get();
        AddressPlan smallPlan = kubernetes.getAddressPlanClient().withName(baseAddressPlan).get();


        AddressPlan addrPlan = new AddressPlanBuilder()
                .withNewMetadata()
                .withName(addrPlanName)
                .endMetadata()
                .withNewSpecLike(smallPlan.getSpec())
                .withTtl(addrPlanTtl)
                .endSpec()
                .build();

        AddressSpacePlan spacePlan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName(spacePlanName)
                .endMetadata()
                .withNewSpecLike(smallSpacePlan.getSpec())
                .withAddressPlans(addrPlan.getMetadata().getName())
                .withInfraConfigRef(infraConfigName)
                .endSpec()
                .build();


        isolatedResourcesManager.createAddressPlan(addrPlan);
        isolatedResourcesManager.createAddressSpacePlan(spacePlan);

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("message-ttl-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(spacePlan.getAddressSpaceType())
                .withPlan(spacePlan.getMetadata().getName())
                .endSpec()
                .build();

        Address addrWithTtl = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(kubernetes.getInfraNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "message-ttl"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withTtl(addrTtl)
                .withAddress("message-ttl")
                .withPlan(addrPlan.getMetadata().getName())
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(addressSpace);
        isolatedResourcesManager.setAddresses(addrWithTtl);

        addrWithTtl = resourcesManager.getAddress(addrWithTtl.getMetadata().getNamespace(), addrWithTtl);
            assertThat(addrWithTtl.getStatus().getTtl(), notNullValue());
        Map<String, Object> actualSettings = ArtemisUtils.getAddressSettings(kubernetes, addressSpace, addrWithTtl.getSpec().getAddress());
        if (expectedTtl.getMinimum() != null) {
            assertThat(addrWithTtl.getStatus().getTtl().getMinimum(), is(expectedTtl.getMinimum()));
            assertThat(((Number) actualSettings.get("minExpiryDelay")).longValue(), is(expectedTtl.getMinimum()));
        } else {
            assertThat(addrWithTtl.getStatus().getTtl().getMinimum(), nullValue());
        }
        if (expectedTtl.getMaximum() != null) {
            assertThat(addrWithTtl.getStatus().getTtl().getMaximum(), is(expectedTtl.getMaximum()));
            assertThat(((Number) actualSettings.get("maxExpiryDelay")).longValue(), is(expectedTtl.getMaximum()));
        } else {
            assertThat(addrWithTtl.getStatus().getTtl().getMaximum(), nullValue());
        }


    }


}

