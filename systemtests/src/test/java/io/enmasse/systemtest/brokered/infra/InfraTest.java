/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered.infra;

import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.infra.InfraTestBase;
import io.enmasse.systemtest.resources.*;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(isolated)
class InfraTest extends InfraTestBase implements ITestBaseBrokered {

    @Test
    void testCreateInfra() throws Exception {
        testInfra = new InfraConfigDefinition("test-infra-1", AddressSpaceType.BROKERED, Arrays.asList(
                new BrokerInfraSpec(Arrays.asList(
                        new InfraResource("memory", "512Mi"),
                        new InfraResource("storage", "1Gi"))),
                new AdminInfraSpec(Collections.singletonList(
                        new InfraResource("memory", "512Mi")))), environment.enmasseVersion());
        plansProvider.createInfraConfig(testInfra);

        exampleAddressPlan = PlanUtils.createAddressPlanObject("example-queue-plan", AddressType.TOPIC,
                Arrays.asList(new ResourceRequest("broker", 1.0)));

        plansProvider.createAddressPlan(exampleAddressPlan);

        AddressSpacePlan exampleSpacePlan = PlanUtils.createAddressSpacePlanObject("example-space-plan",
                testInfra.getName(),
                AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 3.0)),
                Collections.singletonList(exampleAddressPlan));

        plansProvider.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = AddressSpaceUtils.createAddressSpaceObject("example-address-space", AddressSpaceType.BROKERED,
                exampleSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(exampleAddressSpace);

        setAddresses(exampleAddressSpace, AddressUtils.createTopicAddressObject("example-queue", exampleAddressPlan.getMetadata().getName()));

        assertInfra("512Mi", Optional.of("1Gi"), "512Mi");
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

        InfraConfigDefinition infra = new InfraConfigDefinition("test-infra-2", AddressSpaceType.BROKERED, Arrays.asList(
                new BrokerInfraSpec(Arrays.asList(
                        new InfraResource("memory", brokerMemory),
                        new InfraResource("storage", brokerStorage)), updatePersistentVolumeClaim),
                new AdminInfraSpec(Collections.singletonList(
                        new InfraResource("memory", adminMemory)))), environment.enmasseVersion());
        plansProvider.createInfraConfig(infra);

        AddressSpacePlan exampleSpacePlan = PlanUtils.createAddressSpacePlanObject("example-space-plan-2",
                infra.getName(), AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 3.0)),
                Collections.singletonList(exampleAddressPlan));

        plansProvider.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = new DoneableAddressSpace(exampleAddressSpace).editSpec().withPlan(exampleSpacePlan.getMetadata().getName()).endSpec().done();
        replaceAddressSpace(exampleAddressSpace);

        waitUntilInfraReady(
                () -> assertInfra(brokerMemory, updatePersistentVolumeClaim != null && updatePersistentVolumeClaim ? Optional.of(brokerStorage) : Optional.empty(), adminMemory),
                new TimeoutBudget(5, TimeUnit.MINUTES));
    }

    @Test
    void testReadInfra() throws Exception {
        testInfra = new InfraConfigDefinition("test-infra-1", AddressSpaceType.BROKERED, Arrays.asList(
                new BrokerInfraSpec(Arrays.asList(
                        new InfraResource("memory", "512Mi"),
                        new InfraResource("storage", "1Gi"))),
                new AdminInfraSpec(Collections.singletonList(
                        new InfraResource("memory", "512Mi")))), environment.enmasseVersion());
        plansProvider.createInfraConfig(testInfra);

        InfraConfigDefinition actualInfra = plansProvider.getBrokeredInfraConfig(testInfra.getName());

        assertEquals(testInfra.getName(), actualInfra.getName());
        assertEquals(testInfra.getType(), actualInfra.getType());

        AdminInfraSpec expectedAdmin = (AdminInfraSpec) getInfraComponent(testInfra, InfraSpecComponent.ADMIN_INFRA_RESOURCE);
        AdminInfraSpec actualAdmin = (AdminInfraSpec) getInfraComponent(actualInfra, InfraSpecComponent.ADMIN_INFRA_RESOURCE);
        assertEquals(expectedAdmin.getRequiredValueFromResource("memory"), actualAdmin.getRequiredValueFromResource("memory"));

        BrokerInfraSpec expectedBroker = (BrokerInfraSpec) getInfraComponent(testInfra, InfraSpecComponent.BROKER_INFRA_RESOURCE);
        BrokerInfraSpec actualBroker = (BrokerInfraSpec) getInfraComponent(actualInfra, InfraSpecComponent.BROKER_INFRA_RESOURCE);
        assertEquals(expectedBroker.getRequiredValueFromResource("memory"), actualBroker.getRequiredValueFromResource("memory"));
        assertEquals(expectedBroker.getRequiredValueFromResource("storage"), actualBroker.getRequiredValueFromResource("storage"));
        assertEquals(expectedBroker.getAddressFullPolicy(), actualBroker.getAddressFullPolicy());
        assertEquals(expectedBroker.getStorageClassName(), actualBroker.getStorageClassName());

    }

    private boolean assertInfra(String brokerMemory, Optional<String> brokerStorage, String adminMemory) {
        assertAdminConsole(adminMemory);
        assertBroker(brokerMemory, brokerStorage);
        return true;
    }

}
