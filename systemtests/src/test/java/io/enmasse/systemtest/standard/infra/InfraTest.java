/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.infra;

import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.admin.model.v1.*;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.infra.InfraTestBase;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(isolated)
class InfraTest extends InfraTestBase implements ITestBaseStandard {

    @Test
    void testCreateInfra() throws Exception {
        PodTemplateSpec brokerTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "broker"), "mybrokernode", "broker");
        PodTemplateSpec adminTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "admin"), "myadminnode", "admin");
        PodTemplateSpec routerTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "router"), "myrouternode", "router");
        testInfra = PlanUtils.createStandardInfraConfigObject("test-infra-1",
                PlanUtils.createStandardBrokerResourceObject("512Mi", "1Gi", brokerTemplateSpec),
                PlanUtils.createStandardAdminResourceObject("512Mi", adminTemplateSpec),
                PlanUtils.createStandardRouterResourceObject("256Mi", routerTemplateSpec),
                environment.enmasseVersion());
        adminManager.createInfraConfig(testInfra);

        exampleAddressPlan = PlanUtils.createAddressPlanObject("example-queue-plan", AddressType.TOPIC, Arrays.asList(
                new ResourceRequest("broker", 1.0),
                new ResourceRequest("router", 1.0)));

        adminManager.createAddressPlan(exampleAddressPlan);

        AddressSpacePlan exampleSpacePlan = PlanUtils.createAddressSpacePlanObject("example-space-plan",
                testInfra.getMetadata().getName(),
                AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 3.0),
                        new ResourceAllowance("router", 3.0),
                        new ResourceAllowance("aggregate", 5.0)),
                Collections.singletonList(exampleAddressPlan));

        adminManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = AddressSpaceUtils.createAddressSpaceObject("example-address-space", AddressSpaceType.STANDARD,
                exampleSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(exampleAddressSpace);

        setAddresses(exampleAddressSpace, AddressUtils.createTopicAddressObject("example-queue", exampleAddressPlan.getMetadata().getName()));

        assertInfra("512Mi", "1Gi", brokerTemplateSpec, 1, "256Mi", routerTemplateSpec, "512Mi", adminTemplateSpec);
    }

    @Test
    void testIncrementInfra() throws Exception {
        testReplaceInfra("1Gi", "2Gi", 3, "512Mi", "768Mi");
    }

    @Test
    void testDecrementInfra() throws Exception {
        testReplaceInfra("256Mi", "512Mi", 1, "128Mi", "256Mi");
    }

    void testReplaceInfra(String brokerMemory, String brokerStorage, int routerReplicas, String routerMemory, String adminMemory) throws Exception {
        testCreateInfra();

        Boolean updatePersistentVolumeClaim = volumeResizingSupported();

        InfraConfig infra = PlanUtils.createStandardInfraConfigObject("test-infra-2",
                PlanUtils.createStandardBrokerResourceObject(brokerMemory, brokerStorage, updatePersistentVolumeClaim),
                PlanUtils.createStandardAdminResourceObject(adminMemory, null),
                PlanUtils.createStandardRouterResourceObject(routerMemory, 200, routerReplicas),
                environment.enmasseVersion());

        adminManager.createInfraConfig(infra);

        AddressSpacePlan exampleSpacePlan = PlanUtils.createAddressSpacePlanObject("example-space-plan-2",
                infra.getMetadata().getName(), AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 3.0),
                        new ResourceAllowance("router", 3.0),
                        new ResourceAllowance("aggregate", 5.0)),
                Collections.singletonList(exampleAddressPlan));
        adminManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = new DoneableAddressSpace(exampleAddressSpace).editSpec().withPlan(exampleSpacePlan.getMetadata().getName()).endSpec().done();
        replaceAddressSpace(exampleAddressSpace);

        waitUntilInfraReady(
                () -> assertInfra(brokerMemory,
                        updatePersistentVolumeClaim ? brokerStorage : null,
                        null,
                        routerReplicas,
                        routerMemory,
                        null,
                        adminMemory,
                        null),
                new TimeoutBudget(5, TimeUnit.MINUTES));

    }

    @Test
    void testReadInfra() throws Exception {
        testInfra = PlanUtils.createStandardInfraConfigObject("test-infra-1",
                PlanUtils.createStandardBrokerResourceObject("512Mi", "1Gi", null),
                PlanUtils.createStandardAdminResourceObject("512Mi", null),
                PlanUtils.createStandardRouterResourceObject("256Mi", 200, 2),
                environment.enmasseVersion());
        adminManager.createInfraConfig(testInfra);

        StandardInfraConfig actualInfra = adminManager.getStandardInfraConfig(testInfra.getMetadata().getName());

        assertEquals(testInfra.getMetadata().getName(), actualInfra.getMetadata().getName());

        StandardInfraConfigSpecAdmin expectedAdmin = ((StandardInfraConfig) testInfra).getSpec().getAdmin();
        StandardInfraConfigSpecAdmin actualAdmin = actualInfra.getSpec().getAdmin();
        assertEquals(expectedAdmin.getResources().getMemory(), actualAdmin.getResources().getMemory());

        StandardInfraConfigSpecBroker expectedBroker = ((StandardInfraConfig) testInfra).getSpec().getBroker();
        StandardInfraConfigSpecBroker actualBroker = actualInfra.getSpec().getBroker();
        assertEquals(expectedBroker.getResources().getMemory(), actualBroker.getResources().getMemory());
        assertEquals(expectedBroker.getResources().getStorage(), actualBroker.getResources().getStorage());
        assertEquals(expectedBroker.getAddressFullPolicy(), actualBroker.getAddressFullPolicy());
        assertEquals(expectedBroker.getStorageClassName(), actualBroker.getStorageClassName());

        StandardInfraConfigSpecRouter expectedRouter = ((StandardInfraConfig) testInfra).getSpec().getRouter();
        StandardInfraConfigSpecRouter actualRouter = actualInfra.getSpec().getRouter();
        assertEquals(expectedRouter.getResources().getMemory(), actualRouter.getResources().getMemory());
        assertEquals(expectedRouter.getLinkCapacity(), actualRouter.getLinkCapacity());
        assertEquals(expectedRouter.getMinReplicas(), actualRouter.getMinReplicas());
        assertEquals(expectedRouter.getPolicy(), actualRouter.getPolicy());
    }

    private boolean assertInfra(String brokerMemory, String brokerStorage, PodTemplateSpec brokerTemplateSpec, int routerReplicas, String routermemory, PodTemplateSpec routerTemplateSpec, String adminMemory, PodTemplateSpec adminTemplateSpec) {
        log.info("Checking router infra");
        List<Pod> routerPods = TestUtils.listRouterPods(kubernetes, exampleAddressSpace);
        assertEquals(routerReplicas, routerPods.size(), "incorrect number of routers");

        for (Pod router : routerPods) {
            ResourceRequirements resources = router.getSpec().getContainers().stream()
                    .filter(container -> container.getName().equals("router"))
                    .findFirst()
                    .map(Container::getResources)
                    .get();
            assertEquals(routermemory, resources.getLimits().get("memory").getAmount(),
                    "Router memory limit incorrect");
            assertEquals(routermemory, resources.getRequests().get("memory").getAmount(),
                    "Router memory requests incorrect");
            if (routerTemplateSpec != null) {
                assertTemplateSpec(router, routerTemplateSpec);
            }
        }
        assertAdminConsole(adminMemory, adminTemplateSpec);
        assertBroker(brokerMemory, brokerStorage, brokerTemplateSpec);
        return true;
    }

}
