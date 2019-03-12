/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.standard.infra;

import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.bases.infra.InfraTestBase;
import io.enmasse.systemtest.resources.*;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(isolated)
class InfraTest extends InfraTestBase implements ITestBaseStandard {

    @Test
    void testCreateInfra() throws Exception {
        testInfra = new InfraConfigDefinition("test-infra-1", AddressSpaceType.STANDARD, Arrays.asList(
                new BrokerInfraSpec(Arrays.asList(
                        new InfraResource("memory", "512Mi"),
                        new InfraResource("storage", "1Gi"))),
                new RouterInfraSpec(Collections.singletonList(
                        new InfraResource("memory", "256Mi")), 200, 2),
                new AdminInfraSpec(Collections.singletonList(
                        new InfraResource("memory", "512Mi")))), environment.enmasseVersion());
        plansProvider.createInfraConfig(testInfra);

        exampleAddressPlan = PlanUtils.createAddressPlanObject("example-queue-plan", AddressType.TOPIC, Arrays.asList(
                new ResourceRequest("broker", 1.0),
                new ResourceRequest("router", 1.0)));

        plansProvider.createAddressPlan(exampleAddressPlan);

        AddressSpacePlan exampleSpacePlan = PlanUtils.createAddressSpacePlanObject("example-space-plan",
                testInfra.getName(),
                AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 3.0),
                        new ResourceAllowance("router", 3.0),
                        new ResourceAllowance("aggregate", 5.0)),
                Arrays.asList(exampleAddressPlan));

        plansProvider.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = AddressSpaceUtils.createAddressSpaceObject("example-address-space", AddressSpaceType.STANDARD,
                exampleSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(exampleAddressSpace);

        setAddresses(exampleAddressSpace, AddressUtils.createTopicAddressObject("example-queue", exampleAddressPlan.getMetadata().getName()));

        assertInfra("512Mi", Optional.of("1Gi"), 2, "256Mi", "512Mi");

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

        InfraConfigDefinition infra = new InfraConfigDefinition("test-infra-2", AddressSpaceType.STANDARD, Arrays.asList(
                new BrokerInfraSpec(Arrays.asList(
                        new InfraResource("memory", brokerMemory),
                        new InfraResource("storage", brokerStorage)), updatePersistentVolumeClaim),
                new RouterInfraSpec(Collections.singletonList(
                        new InfraResource("memory", routerMemory)), 200, routerReplicas),
                new AdminInfraSpec(Collections.singletonList(
                        new InfraResource("memory", adminMemory)))), environment.enmasseVersion());
        plansProvider.createInfraConfig(infra);

        AddressSpacePlan exampleSpacePlan = PlanUtils.createAddressSpacePlanObject("example-space-plan-2",
                infra.getName(), AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 3.0),
                        new ResourceAllowance("router", 3.0),
                        new ResourceAllowance("aggregate", 5.0)),
                Arrays.asList(exampleAddressPlan));
        plansProvider.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = new DoneableAddressSpace(exampleAddressSpace).editSpec().withPlan(exampleSpacePlan.getMetadata().getName()).endSpec().done();
        replaceAddressSpace(exampleAddressSpace);

        waitUntilInfraReady(
                () -> assertInfra(brokerMemory,
                        updatePersistentVolumeClaim != null && updatePersistentVolumeClaim ? Optional.of(brokerStorage) : Optional.empty(),
                        routerReplicas,
                        routerMemory,
                        adminMemory),
                new TimeoutBudget(5, TimeUnit.MINUTES));

    }

    @Test
    void testReadInfra() throws Exception {
        testInfra = new InfraConfigDefinition("test-infra-1", AddressSpaceType.STANDARD, Arrays.asList(
                new BrokerInfraSpec(Arrays.asList(
                        new InfraResource("memory", "512Mi"),
                        new InfraResource("storage", "1Gi"))),
                new RouterInfraSpec(Collections.singletonList(
                        new InfraResource("memory", "256Mi")), 200, 2),
                new AdminInfraSpec(Collections.singletonList(
                        new InfraResource("memory", "512Mi")))), environment.enmasseVersion());
        plansProvider.createInfraConfig(testInfra);

        InfraConfigDefinition actualInfra = plansProvider.getStandardInfraConfig(testInfra.getName());

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

        RouterInfraSpec expectedRouter = (RouterInfraSpec) getInfraComponent(testInfra, InfraSpecComponent.ROUTER_INFRA_RESOURCE);
        RouterInfraSpec actualRouter = (RouterInfraSpec) getInfraComponent(actualInfra, InfraSpecComponent.ROUTER_INFRA_RESOURCE);
        assertEquals(expectedRouter.getRequiredValueFromResource("memory"), actualRouter.getRequiredValueFromResource("memory"));
        assertEquals(expectedRouter.getLinkCapacity(), actualRouter.getLinkCapacity());
        assertEquals(expectedRouter.getMinReplicas(), actualRouter.getMinReplicas());

    }

    private boolean assertInfra(String brokerMemory, Optional<String> brokerStorage, int routerReplicas, String routermemory, String adminMemory) {
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
        }
        assertAdminConsole(adminMemory);
        assertBroker(brokerMemory, brokerStorage);
        return true;
    }

}
