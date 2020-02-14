/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.infra.standard;

import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanBuilder;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecAdmin;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecAdminBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecBroker;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecBrokerBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecRouter;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecRouterBuilder;
import io.enmasse.systemtest.bases.infra.InfraTestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudget;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InfraTestStandard extends InfraTestBase implements ITestIsolatedStandard {
    private static Logger log = CustomLogger.getLogger();

    @Test
    @Tag(ACCEPTANCE)
    void testCreateInfra() throws Exception {
        PodTemplateSpec brokerTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "broker"), "mybrokernode", "broker");
        PodTemplateSpec adminTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "admin"), "myadminnode", "admin");
        PodTemplateSpec routerTemplateSpec = PlanUtils.createTemplateSpec(Collections.singletonMap("mycomponent", "router"), "myrouternode", "router");
        StandardInfraConfig testInfra = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("test-infra-1-standard")
                .endMetadata()
                .withNewSpec()
                .withVersion(environment.enmasseVersion())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withAddressFullPolicy("FAIL")
                        .withNewResources()
                        .withMemory("512Mi")
                        .withStorage("1Gi")
                        .endResources()
                        .withPodTemplate(brokerTemplateSpec)
                        .withJavaOpts("-Dsystemtest=property")
                        .build())
                .withRouter(new StandardInfraConfigSpecRouterBuilder()
                        .withNewResources()
                        .withMemory("256Mi")
                        .endResources()
                        .withPodTemplate(routerTemplateSpec)
                        .build())
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory("512Mi")
                        .endResources()
                        .withPodTemplate(adminTemplateSpec)
                        .build())
                .endSpec()
                .build();
        resourcesManager.createInfraConfig(testInfra);

        exampleAddressPlan = PlanUtils.createAddressPlanObject("example-queue-plan-standard", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 1.0)));

        resourcesManager.createAddressPlan(exampleAddressPlan);

        AddressSpacePlan exampleSpacePlan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("example-space-plan-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(AddressSpaceType.STANDARD.toString())
                .withShortDescription("Custom systemtests defined address space plan")
                .withInfraConfigRef(testInfra.getMetadata().getName())
                .withResourceLimits(Arrays.asList(
                        new ResourceAllowance("broker", 3.0),
                        new ResourceAllowance("router", 3.0),
                        new ResourceAllowance("aggregate", 5.0))
                        .stream().collect(Collectors.toMap(ResourceAllowance::getName, ResourceAllowance::getMax)))
                .withAddressPlans(Collections.singletonList(exampleAddressPlan)
                        .stream().map(addressPlan -> addressPlan.getMetadata().getName()).collect(Collectors.toList()))
                .endSpec()
                .build();
        resourcesManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("example-address-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(exampleSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(exampleAddressSpace);

        resourcesManager.setAddresses(new AddressBuilder()
                .withNewMetadata()
                .withNamespace(exampleSpacePlan.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(exampleAddressSpace, "example-queue"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withAddress("example-queue")
                .withPlan(exampleAddressPlan.getMetadata().getName())
                .endSpec()
                .build());

        assertInfra("512Mi", "1Gi", brokerTemplateSpec, 1, "256Mi", routerTemplateSpec, "512Mi", adminTemplateSpec, "-Dsystemtest=property");
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

        StandardInfraConfig infra = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("test-infra-2-standard")
                .endMetadata()
                .withNewSpec()
                .withVersion(environment.enmasseVersion())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withAddressFullPolicy("FAIL")
                        .withUpdatePersistentVolumeClaim(updatePersistentVolumeClaim)
                        .withNewResources()
                        .withMemory(brokerMemory)
                        .withStorage(brokerStorage)
                        .endResources()
                        .build())
                .withRouter(PlanUtils.createStandardRouterResourceObject(routerMemory, 200, routerReplicas))
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory(adminMemory)
                        .endResources()
                        .build())
                .endSpec()
                .build();
        resourcesManager.createInfraConfig(infra);

        AddressSpacePlan exampleSpacePlan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("example-space-plan-2-standard")
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(AddressSpaceType.STANDARD.toString())
                .withShortDescription("Custom systemtests defined address space plan")
                .withInfraConfigRef(infra.getMetadata().getName())
                .withResourceLimits(Arrays.asList(
                        new ResourceAllowance("broker", 3.0),
                        new ResourceAllowance("router", 3.0),
                        new ResourceAllowance("aggregate", 5.0))
                        .stream().collect(Collectors.toMap(ResourceAllowance::getName, ResourceAllowance::getMax)))
                .withAddressPlans(Collections.singletonList(exampleAddressPlan)
                        .stream().map(addressPlan -> addressPlan.getMetadata().getName()).collect(Collectors.toList()))
                .endSpec()
                .build();
        resourcesManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = new DoneableAddressSpace(exampleAddressSpace).editSpec().withPlan(exampleSpacePlan.getMetadata().getName()).endSpec().done();
        isolatedResourcesManager.replaceAddressSpace(exampleAddressSpace);

        waitUntilInfraReady(
                () -> assertInfra(brokerMemory,
                        updatePersistentVolumeClaim ? brokerStorage : null,
                        null,
                        routerReplicas,
                        routerMemory,
                        null,
                        adminMemory,
                        null,
                        null),
                new TimeoutBudget(5, TimeUnit.MINUTES));

    }

    @Test
    void testReadInfra() throws Exception {
        StandardInfraConfig testInfra = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("test-infra-3-standard")
                .endMetadata()
                .withNewSpec()
                .withVersion(environment.enmasseVersion())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withAddressFullPolicy("FAIL")
                        .withNewResources()
                        .withMemory("512Mi")
                        .withStorage("1Gi")
                        .endResources()
                        .build())
                .withRouter(PlanUtils.createStandardRouterResourceObject("256Mi", 200, 2))
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory("512Mi")
                        .endResources()
                        .build())
                .endSpec()
                .build();
        isolatedResourcesManager.createInfraConfig(testInfra);

        StandardInfraConfig actualInfra = isolatedResourcesManager.getStandardInfraConfig(testInfra.getMetadata().getName());

        assertEquals(testInfra.getMetadata().getName(), actualInfra.getMetadata().getName());

        StandardInfraConfigSpecAdmin expectedAdmin = testInfra.getSpec().getAdmin();
        StandardInfraConfigSpecAdmin actualAdmin = actualInfra.getSpec().getAdmin();
        assertEquals(expectedAdmin.getResources().getMemory(), actualAdmin.getResources().getMemory());

        StandardInfraConfigSpecBroker expectedBroker = testInfra.getSpec().getBroker();
        StandardInfraConfigSpecBroker actualBroker = actualInfra.getSpec().getBroker();
        assertEquals(expectedBroker.getResources().getMemory(), actualBroker.getResources().getMemory());
        assertEquals(expectedBroker.getResources().getStorage(), actualBroker.getResources().getStorage());
        assertEquals(expectedBroker.getAddressFullPolicy(), actualBroker.getAddressFullPolicy());
        assertEquals(expectedBroker.getStorageClassName(), actualBroker.getStorageClassName());

        StandardInfraConfigSpecRouter expectedRouter = testInfra.getSpec().getRouter();
        StandardInfraConfigSpecRouter actualRouter = actualInfra.getSpec().getRouter();
        assertEquals(expectedRouter.getResources().getMemory(), actualRouter.getResources().getMemory());
        assertEquals(expectedRouter.getLinkCapacity(), actualRouter.getLinkCapacity());
        assertEquals(expectedRouter.getMinReplicas(), actualRouter.getMinReplicas());
        assertEquals(expectedRouter.getPolicy(), actualRouter.getPolicy());
    }

    @Test
    void testCreateDeletePodDisruptionBudget() throws Exception {

        testCreatePdb();

        resourcesManager.deleteAddressSpace(exampleAddressSpace);

        String pdbRouterName = getRouterPdbName();
        String pdbBrokerName = getBrokerPdbName();
        try {
            TestUtils.waitUntilCondition("Router PodDisruptionBudget deleted", phase -> {
                return getPodDisruptionBudget(pdbRouterName) == null;
            }, new TimeoutBudget(30, TimeUnit.SECONDS));
            TestUtils.waitUntilCondition("Broker PodDisruptionBudget deleted", phase -> {
                return getPodDisruptionBudget(pdbBrokerName) == null;
            }, new TimeoutBudget(30, TimeUnit.SECONDS));
        } finally {
            var pdb = getPodDisruptionBudget(pdbRouterName);
            if (pdb != null) {
                deletePodDisruptionBudget(pdbRouterName);
            }
            pdb = getPodDisruptionBudget(pdbBrokerName);
            if (pdb != null) {
                deletePodDisruptionBudget(pdbBrokerName);
            }
        }

    }

    @Test
    void testAddRemovePodDisruptionBudget() throws Exception {

        testCreatePdb();

        StandardInfraConfig infraWithOutPdb = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("test-infra-no-pdb")
                .endMetadata()
                .withNewSpec()
                .withVersion(environment.enmasseVersion())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .build())
                .withRouter(new StandardInfraConfigSpecRouterBuilder()
                        .build())
                .endSpec()
                .build();
        resourcesManager.createInfraConfig(infraWithOutPdb);

        AddressSpacePlan spacePlanWithOutPdb = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("example-space-plan-standard-no-pdb")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(AddressSpaceType.STANDARD.toString())
                .withShortDescription("Custom systemtests defined address space plan")
                .withInfraConfigRef(infraWithOutPdb.getMetadata().getName())
                .withResourceLimits(Arrays.asList(
                        new ResourceAllowance("broker", 2.0),
                        new ResourceAllowance("router", 2.0),
                        new ResourceAllowance("aggregate", 5.0))
                        .stream().collect(Collectors.toMap(ResourceAllowance::getName, ResourceAllowance::getMax)))
                .withAddressPlans(Collections.singletonList(exampleAddressPlan)
                        .stream().map(addressPlan -> addressPlan.getMetadata().getName()).collect(Collectors.toList()))
                .endSpec()
                .build();
        resourcesManager.createAddressSpacePlan(spacePlanWithOutPdb);

        exampleAddressSpace = new DoneableAddressSpace(exampleAddressSpace).editSpec().withPlan(spacePlanWithOutPdb.getMetadata().getName()).endSpec().done();
        isolatedResourcesManager.replaceAddressSpace(exampleAddressSpace);

        String pdbRouterName = getRouterPdbName();
        String pdbBrokerName = getBrokerPdbName();
        try {
            TestUtils.waitUntilCondition("Router PodDisruptionBudget deleted", phase -> {
                return getPodDisruptionBudget(pdbRouterName) == null;
            }, new TimeoutBudget(30, TimeUnit.SECONDS));
            TestUtils.waitUntilCondition("Broker PodDisruptionBudget deleted", phase -> {
                return getPodDisruptionBudget(pdbBrokerName) == null;
            }, new TimeoutBudget(30, TimeUnit.SECONDS));
        } finally {
            var pdb = getPodDisruptionBudget(pdbRouterName);
            if (pdb != null) {
                deletePodDisruptionBudget(pdbRouterName);
            }
            pdb = getPodDisruptionBudget(pdbBrokerName);
            if (pdb != null) {
                deletePodDisruptionBudget(pdbBrokerName);
            }
        }

    }

    private void testCreatePdb() throws Exception {
        String maxUnavailable = "50%";
        StandardInfraConfig infraWithPdb = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("test-infra-pdb")
                .endMetadata()
                .withNewSpec()
                .withVersion(environment.enmasseVersion())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withMaxUnavailable(new IntOrString(maxUnavailable))
                        .build())
                .withRouter(new StandardInfraConfigSpecRouterBuilder()
                        .withMaxUnavailable(new IntOrString(maxUnavailable))
                        .build())
                .endSpec()
                .build();
        resourcesManager.createInfraConfig(infraWithPdb);

        exampleAddressPlan = PlanUtils.createAddressPlanObject("example-queue-plan-standard", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 1.0)));

        resourcesManager.createAddressPlan(exampleAddressPlan);

        AddressSpacePlan exampleSpacePlan = new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName("example-space-plan-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withAddressSpaceType(AddressSpaceType.STANDARD.toString())
                .withShortDescription("Custom systemtests defined address space plan")
                .withInfraConfigRef(infraWithPdb.getMetadata().getName())
                .withResourceLimits(Arrays.asList(
                        new ResourceAllowance("broker", 2.0),
                        new ResourceAllowance("router", 2.0),
                        new ResourceAllowance("aggregate", 5.0))
                        .stream().collect(Collectors.toMap(ResourceAllowance::getName, ResourceAllowance::getMax)))
                .withAddressPlans(Collections.singletonList(exampleAddressPlan)
                        .stream().map(addressPlan -> addressPlan.getMetadata().getName()).collect(Collectors.toList()))
                .endSpec()
                .build();
        resourcesManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("example-address-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(exampleSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(exampleAddressSpace);

        String pdbRouterName = getRouterPdbName();

        TestUtils.waitUntilCondition("Router PodDisruptionBudget created", phase -> {
            return getPodDisruptionBudget(pdbRouterName) != null;
        }, new TimeoutBudget(30, TimeUnit.SECONDS));

        PodDisruptionBudget pdbRouter = getPodDisruptionBudget(pdbRouterName);

        assertNotNull(pdbRouter);
        assertEquals(maxUnavailable, pdbRouter.getSpec().getMaxUnavailable().getStrVal());

        String pdbBrokerName = getBrokerPdbName();

        TestUtils.waitUntilCondition("Router PodDisruptionBudget created", phase -> {
            return getPodDisruptionBudget(pdbBrokerName) != null;
        }, new TimeoutBudget(30, TimeUnit.SECONDS));

        PodDisruptionBudget pdbBroker = getPodDisruptionBudget(pdbBrokerName);

        assertNotNull(pdbBroker);
        assertEquals(maxUnavailable, pdbBroker.getSpec().getMaxUnavailable().getStrVal());

    }

    private boolean assertInfra(String brokerMemory, String brokerStorage, PodTemplateSpec brokerTemplateSpec, int routerReplicas, String routermemory, PodTemplateSpec routerTemplateSpec, String adminMemory, PodTemplateSpec adminTemplateSpec, String javaOpts) {
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
        assertBroker(brokerMemory, brokerStorage, brokerTemplateSpec, javaOpts);
        return true;
    }

    private PodDisruptionBudget getPodDisruptionBudget(String name) {
        return kubernetes.getPodDisruptionBudget(kubernetes.getInfraNamespace(), name);
    }

    private void deletePodDisruptionBudget(String name) {
        kubernetes.deletePodDisruptionBudget(kubernetes.getInfraNamespace(), name);
    }

    private String getBrokerPdbName() {
        return String.format("enmasse.%s.%s.broker", exampleAddressSpace.getMetadata().getNamespace(), exampleAddressSpace.getMetadata().getName());
    }

    private String getRouterPdbName() {
        return String.format("enmasse.%s.%s.qdrouterd", exampleAddressSpace.getMetadata().getNamespace(), exampleAddressSpace.getMetadata().getName());
    }

}
