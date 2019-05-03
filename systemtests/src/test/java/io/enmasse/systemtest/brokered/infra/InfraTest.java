/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.infra;

import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.admin.model.v1.*;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.infra.InfraTestBase;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(isolated)
class InfraTest extends InfraTestBase implements ITestBaseBrokered {

    @Test
    void testCreateInfra() throws Exception {
        PodTemplateSpec brokerTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "broker"), "mybrokernode", "broker");
        PodTemplateSpec adminTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "admin"), "myadminnode", "admin");
        testInfra = PlanUtils.createBrokeredInfraConfigObject("test-infra-1",
                PlanUtils.createBrokeredBrokerResourceObject("512Mi", "1Gi", brokerTemplateSpec),
                PlanUtils.createBrokeredAdminResourceObject("512Mi", adminTemplateSpec),
                environment.enmasseVersion());

        adminManager.createInfraConfig(testInfra);

        exampleAddressPlan = PlanUtils.createAddressPlanObject("example-queue-plan", AddressType.TOPIC,
                Collections.singletonList(new ResourceRequest("broker", 1.0)));

        adminManager.createAddressPlan(exampleAddressPlan);

        AddressSpacePlan exampleSpacePlan = PlanUtils.createAddressSpacePlanObject("example-space-plan",
                testInfra.getMetadata().getName(),
                AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 3.0)),
                Collections.singletonList(exampleAddressPlan));

        adminManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("example-address-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(exampleSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        createAddressSpace(exampleAddressSpace);

        setAddresses(exampleAddressSpace, AddressUtils.createTopicAddressObject("example-queue", exampleAddressPlan.getMetadata().getName()));

        assertInfra("512Mi", "1Gi", brokerTemplateSpec, "512Mi", adminTemplateSpec);
    }

    @Test
    void testIncrementInfra() throws Exception {
        testReplaceInfra("1Gi", "2Gi", "768Mi");
    }

    @Test
    void testDecrementInfra() throws Exception {
        testReplaceInfra("256Mi", "512Mi", "256Mi");
    }

    void testReplaceInfra(String brokerMemory, String brokerStorage, String adminMemory) throws Exception {
        testCreateInfra();

        Boolean updatePersistentVolumeClaim = volumeResizingSupported();

        BrokeredInfraConfig infra = PlanUtils.createBrokeredInfraConfigObject("test-infra-2",
                PlanUtils.createBrokeredBrokerResourceObject(brokerMemory, brokerStorage, updatePersistentVolumeClaim),
                PlanUtils.createBrokeredAdminResourceObject(adminMemory, null),
                environment.enmasseVersion());

        adminManager.createInfraConfig(infra);

        AddressSpacePlan exampleSpacePlan = PlanUtils.createAddressSpacePlanObject("example-space-plan-2",
                infra.getMetadata().getName(), AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 3.0)),
                Collections.singletonList(exampleAddressPlan));

        adminManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = new DoneableAddressSpace(exampleAddressSpace).editSpec().withPlan(exampleSpacePlan.getMetadata().getName()).endSpec().done();
        replaceAddressSpace(exampleAddressSpace);

        waitUntilInfraReady(
                () -> assertInfra(brokerMemory, updatePersistentVolumeClaim ? brokerStorage : null, null, adminMemory, null),
                new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    @Test
    void testReadInfra() throws Exception {
        testInfra = PlanUtils.createBrokeredInfraConfigObject("test-infra-1",
                PlanUtils.createBrokeredBrokerResourceObject("512Mi", "1Gi", null),
                PlanUtils.createBrokeredAdminResourceObject("512Mi", null),
                environment.enmasseVersion());
        adminManager.createInfraConfig(testInfra);

        BrokeredInfraConfig actualInfra = adminManager.getBrokeredInfraConfig(testInfra.getMetadata().getName());

        assertEquals(testInfra.getMetadata().getName(), actualInfra.getMetadata().getName());

        BrokeredInfraConfigSpecAdmin expectedAdmin = ((BrokeredInfraConfig) testInfra).getSpec().getAdmin();
        BrokeredInfraConfigSpecAdmin actualAdmin = actualInfra.getSpec().getAdmin();
        assertEquals(expectedAdmin.getResources().getMemory(), actualAdmin.getResources().getMemory());

        BrokeredInfraConfigSpecBroker expectedBroker = ((BrokeredInfraConfig) testInfra).getSpec().getBroker();
        BrokeredInfraConfigSpecBroker actualBroker = actualInfra.getSpec().getBroker();
        assertEquals(expectedBroker.getResources().getMemory(), actualBroker.getResources().getMemory());
        assertEquals(expectedBroker.getResources().getStorage(), actualBroker.getResources().getStorage());
        assertEquals(expectedBroker.getAddressFullPolicy(), actualBroker.getAddressFullPolicy());
        assertEquals(expectedBroker.getStorageClassName(), actualBroker.getStorageClassName());

    }

    private boolean assertInfra(String brokerMemory, String brokerStorage, PodTemplateSpec brokerTemplateSpec, String adminMemory, PodTemplateSpec adminTemplateSpec) {
        assertAdminConsole(adminMemory, adminTemplateSpec);
        assertBroker(brokerMemory, brokerStorage, brokerTemplateSpec);
        return true;
    }

}
