/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.infra.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
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
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.ReceiverStatus;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.bases.infra.InfraTestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.infra.InfraConfiguration;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientReceiver;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudget;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Source;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InfraTestStandard extends InfraTestBase implements ITestIsolatedStandard {
    private static Logger log = CustomLogger.getLogger();

    private Address exampleAddress;

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

        AddressSpacePlan exampleSpacePlan = buildExampleAddressSpacePlan(testInfra,
                "example-space-plan-standard", Collections.singletonList(exampleAddressPlan));
        resourcesManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = buildExampleAddressSpace(exampleSpacePlan, "example-address-space");

        resourcesManager.createAddressSpace(exampleAddressSpace);

        exampleAddress = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(exampleSpacePlan.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(exampleAddressSpace, "example-queue"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withAddress("example-queue")
                .withPlan(exampleAddressPlan.getMetadata().getName())
                .endSpec()
                .build();

        resourcesManager.setAddresses(exampleAddress);

        resourcesManager.createOrUpdateUser(exampleAddressSpace, exampleUser);

        assertInfra(InfraConfiguration.broker(null, "512Mi", brokerTemplateSpec, "1Gi", "-Dsystemtest=property"),
                InfraConfiguration.router(null, "256Mi", routerTemplateSpec, 1),
                InfraConfiguration.admin(null, "512Mi", adminTemplateSpec));
    }

    @ExternalClients
    @ParameterizedTest(name = "testRouterMaxMessageSize-with-messageSize-{0}")
    @ValueSource(ints = {10, 1000})
    void testRouterMaxMessageSize(int maxMessageSize) throws  Exception {

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
                        .withNewPolicy()
                        .withMaxMessageSize(maxMessageSize)
                        .endPolicy()
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

        AddressSpacePlan exampleSpacePlan = buildExampleAddressSpacePlan(testInfra,
                "example-space-plan-standard", Collections.singletonList(exampleAddressPlan));
        resourcesManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = buildExampleAddressSpace(exampleSpacePlan, "example-address-space");

        resourcesManager.createAddressSpace(exampleAddressSpace);

        exampleAddress = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(exampleSpacePlan.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(exampleAddressSpace, "example-queue"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withAddress("example-queue")
                .withPlan(exampleAddressPlan.getMetadata().getName())
                .endSpec()
                .build();

        resourcesManager.setAddresses(exampleAddress);

        resourcesManager.createOrUpdateUser(exampleAddressSpace, exampleUser);


        String alphabetChoice = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder stringLoad = new StringBuilder();
        Random rnd = new Random();

        if (maxMessageSize == 10){
            log.info("Test type: MessageOverflow");

            while (stringLoad.length() < maxMessageSize+5) {
                int index = (int) (rnd.nextFloat() * alphabetChoice.length());
                stringLoad.append(alphabetChoice.charAt(index));
            }
            String comleteStringLoad = stringLoad.toString();
            log.info("Sending string: "+comleteStringLoad);

            try(ExternalMessagingClient client = new ExternalMessagingClient()
                    .withClientEngine(new RheaClientSender())
                    .withCredentials(exampleUser)
                    .withMessagingRoute(Objects.requireNonNull(AddressSpaceUtils.getInternalEndpointByName(exampleAddressSpace, "messaging", "amqps")))
                    .withAdditionalArgument(ClientArgument.MSG_DURABLE, "true")
                    .withCount(1)
                    .withMessageBody(stringLoad)
                    .withTimeout(300)
                    .withAddress(exampleAddress)){
                assertFalse(client.run());
            }
        }
        else {
            log.info("Test type: MessageInRange");
            while (stringLoad.length() < maxMessageSize/10) {
                int index = (int) (rnd.nextFloat() * alphabetChoice.length());
                stringLoad.append(alphabetChoice.charAt(index));
            }
            String comleteStringLoad = stringLoad.toString();
            log.info("Sending string: "+comleteStringLoad);

            ExternalMessagingClient senderClient = new ExternalMessagingClient()
                    .withClientEngine(new RheaClientSender())
                    .withCredentials(exampleUser)
                    .withMessagingRoute(Objects.requireNonNull(AddressSpaceUtils.getInternalEndpointByName(exampleAddressSpace, "messaging", "amqps")))
                    .withAdditionalArgument(ClientArgument.MSG_DURABLE, "true")
                    .withCount(1)
                    .withMessageBody(stringLoad)
                    .withTimeout(300)
                    .withAddress(exampleAddress)
                    .withAdditionalArgument(ClientArgument.DEST_TYPE, "ANYCAST");

            ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                    .withClientEngine(new RheaClientReceiver())
                    .withMessagingRoute(Objects.requireNonNull(AddressSpaceUtils.getInternalEndpointByName(exampleAddressSpace, "messaging", "amqps")))
                    .withAddress(exampleAddress)
                    .withCredentials(exampleUser)
                    .withCount(1)
                    .withTimeout(300)
                    .withAdditionalArgument(ClientArgument.DEST_TYPE, "ANYCAST");

            assertTrue(senderClient.run());
            assertTrue(receiverClient.run());
        }
    }


    @Test
    @Tag(ACCEPTANCE)
    void testCreateInfraSubPlan() throws Exception {
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

        exampleAddressPlan = PlanUtils.createAddressPlanObject("example-topic-plan-standard", AddressType.TOPIC,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 1.0)));

        AddressPlan exampleAddressPlanSub = PlanUtils.createAddressPlanObject("example-sub-plan-standard1", AddressType.SUBSCRIPTION,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 1.0)));

        resourcesManager.createAddressPlan(exampleAddressPlan);
        resourcesManager.createAddressPlan(exampleAddressPlanSub);

        AddressSpacePlan spacePlan = buildExampleAddressSpacePlan(testInfra, "example-space-plan-st",
                Arrays.asList(exampleAddressPlan, exampleAddressPlanSub));
        resourcesManager.createAddressSpacePlan(spacePlan);
        exampleAddressSpace = buildExampleAddressSpace(spacePlan, "example-space-1");
        resourcesManager.createAddressSpace(exampleAddressSpace);

        exampleAddress = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(spacePlan.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(exampleAddressSpace, "example-topic"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.TOPIC.toString())
                .withAddress("example-topic")
                .withPlan(exampleAddressPlan.getMetadata().getName())
                .endSpec()
                .build();

        Address exampleAddress1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(spacePlan.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(exampleAddressSpace, "example-sub"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.SUBSCRIPTION.toString())
                .withTopic(exampleAddress.getSpec().getAddress())
                .withAddress("example-sub")
                .withPlan(exampleAddressPlanSub.getMetadata().getName())
                .endSpec()
                .build();

        resourcesManager.setAddresses(exampleAddress, exampleAddress1);
        resourcesManager.createOrUpdateUser(exampleAddressSpace, exampleUser);

    }
    @Test
    @ExternalClients
    void testIncrementInfra() throws Exception {
        testReplaceInfra(InfraConfiguration.broker("500m", "1Gi", null, "2Gi", null),
                InfraConfiguration.router("500m", "512Mi", null, 3),
                InfraConfiguration.admin("500m", "768Mi", null));
    }

    @Test
    @Tag(ACCEPTANCE)
    @ExternalClients
    void testDecrementInfra() throws Exception {
        testReplaceInfra(InfraConfiguration.broker("250m", "256Mi", null, "1Gi", null),
                InfraConfiguration.router("250m", "128Mi", null, 1),
                InfraConfiguration.admin("250m", "256Mi", null));
    }

    void testReplaceInfra(InfraConfiguration brokerConfig, InfraConfiguration routerConfig, InfraConfiguration adminConfig) throws Exception {
        testCreateInfra();

        try (ExternalMessagingClient client = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientSender())
                .withCredentials(exampleUser)
                .withMessagingRoute(Objects.requireNonNull(AddressSpaceUtils.getInternalEndpointByName(exampleAddressSpace, "messaging", "amqps")))
                .withAdditionalArgument(ClientArgument.MSG_DURABLE, "true")
                .withCount(5)
                .withAddress(exampleAddress)) {
            assertTrue(client.run());
        }

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
                        .withMemory(brokerConfig.getMemory())
                        .withCpu(brokerConfig.getCpu())
                        .endResources().build())
                .withRouter(PlanUtils.createStandardRouterResourceObject(routerConfig.getCpu(), routerConfig.getMemory(), 200, routerConfig.getRouterReplicas()))
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory(adminConfig.getMemory())
                        .withCpu(adminConfig.getCpu())
                        .endResources()
                        .build())
                .endSpec()
                .build();

        if (updatePersistentVolumeClaim && brokerConfig.getBrokerStorage() != null) {
            infra.getSpec().getBroker().getResources().setStorage(brokerConfig.getBrokerStorage());
        }
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

        exampleAddressSpace = new AddressSpaceBuilder(exampleAddressSpace).editSpec().withPlan(exampleSpacePlan.getMetadata().getName()).endSpec().build();
        isolatedResourcesManager.replaceAddressSpace(exampleAddressSpace);

        if (!updatePersistentVolumeClaim) {
            brokerConfig.setBrokerStorage(null);
        }

        waitUntilInfraReady(
                () -> assertInfra(brokerConfig, routerConfig, adminConfig),
                new TimeoutBudget(5, TimeUnit.MINUTES));

        try (ExternalMessagingClient client = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientReceiver())
                .withCredentials(exampleUser)
                .withMessagingRoute(Objects.requireNonNull(AddressSpaceUtils.getInternalEndpointByName(exampleAddressSpace, "messaging", "amqps")))
                .withCount(5)
                .withAddress(exampleAddress)) {
            assertTrue(client.run());
        }
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
                .withRouter(PlanUtils.createStandardRouterResourceObject(null, "256Mi", 200, 2))
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

        exampleAddressSpace = new AddressSpaceBuilder(exampleAddressSpace).editSpec().withPlan(spacePlanWithOutPdb.getMetadata().getName()).endSpec().build();
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

    @ParameterizedTest(name = "testTreatRejectedAsModifiedOverride-{0}")
    @ValueSource(booleans = {true, false})
    void testTreatRejectedAsModifiedOverride(boolean treatRejectedAsModified) throws Exception {
        String name = String.format("treat-rejected-modified-%b", treatRejectedAsModified);
        StandardInfraConfig testInfra = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .withVersion(environment.enmasseVersion())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withTreatRejectAsUnmodifiedDeliveryFailed(treatRejectedAsModified)
                        .build())
                .withGlobalDLQ(true)
                .endSpec()
                .build();

        resourcesManager.createInfraConfig(testInfra);

        exampleAddressPlan = PlanUtils.createAddressPlanObject(name, AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 1.0)));

        resourcesManager.createAddressPlan(exampleAddressPlan);

        AddressSpacePlan exampleSpacePlan = buildExampleAddressSpacePlan(testInfra,
                name, Collections.singletonList(exampleAddressPlan));
        resourcesManager.createAddressSpacePlan(exampleSpacePlan);

        exampleAddressSpace = buildExampleAddressSpace(exampleSpacePlan, name);

        resourcesManager.createAddressSpace(exampleAddressSpace);

        Address exampleAddress = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(exampleSpacePlan.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(exampleAddressSpace, "testqueue"))
                .endMetadata()
                .withNewSpec()
                .withType(AddressType.QUEUE.toString())
                .withAddress("testqueue")
                .withPlan(exampleAddressPlan.getMetadata().getName())
                .endSpec()
                .build();

        resourcesManager.setAddresses(exampleAddress);

        resourcesManager.createOrUpdateUser(exampleAddressSpace, exampleUser);

        AmqpClient client = getAmqpClientFactory().createQueueClient(exampleAddressSpace);
        client.getConnectOptions()
                .setUsername(exampleUser.getUsername())
                .setPassword(exampleUser.getPassword());


        assertEquals(1, client.sendMessages(exampleAddress.getSpec().getAddress(), 1).get(1, TimeUnit.MINUTES));

        Source source = new Source();
        source.setAddress(exampleAddress.getSpec().getAddress());
        ReceiverStatus status = client.recvMessages(source, message -> true, Optional.empty(), protonDelivery -> protonDelivery.disposition(new Rejected(), true));
        assertEquals(1, status.getResult().get(1, TimeUnit.MINUTES).size());

        // Depending on the behavior, it should either be received again (retried), or removed.
        if (treatRejectedAsModified) {
            assertEquals(1, client.recvMessages(source, 1, Optional.empty()).get(1, TimeUnit.MINUTES).size());
        } else {
            assertThrows(TimeoutException.class, () -> client.recvMessages(source, 1, Optional.empty()).get(1, TimeUnit.MINUTES));
            Source dlqSource = new Source();
            dlqSource.setAddress("!!GLOBAL_DLQ");
            assertEquals(1, client.recvMessages(dlqSource, 1, Optional.empty()).get(1, TimeUnit.MINUTES).size());
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

    private boolean assertInfra(InfraConfiguration brokerConfig, InfraConfiguration routerConfiguration, InfraConfiguration adminConfig) {
        log.info("Checking router infra");
        List<Pod> routerPods = TestUtils.listRouterPods(kubernetes, exampleAddressSpace);
        assertEquals(routerConfiguration.getRouterReplicas(), routerPods.size(), "incorrect number of routers");

        for (Pod router : routerPods) {
            ResourceRequirements resources = router.getSpec().getContainers().stream()
                    .filter(container -> container.getName().equals("router"))
                    .findFirst()
                    .map(Container::getResources)
                    .get();
            assertEquals(new Quantity(routerConfiguration.getMemory()), resources.getLimits().get("memory"),
                    "Router memory limit incorrect");
            assertEquals(new Quantity(routerConfiguration.getMemory()), resources.getRequests().get("memory"),
                    "Router memory requests incorrect");
            if (routerConfiguration.getCpu() != null) {
                assertEquals(new Quantity(routerConfiguration.getCpu()), resources.getLimits().get("cpu"),
                        "Router cpu limit incorrect");
                assertEquals(new Quantity(routerConfiguration.getCpu()), resources.getRequests().get("cpu"),
                        "Router cpu requests incorrect");
            }
            if (routerConfiguration.getTemplateSpec() != null) {
                assertTemplateSpec(router, routerConfiguration.getTemplateSpec());
            }
        }
        log.info("Checking admin infra");
        assertAdminConsole(adminConfig);
        log.info("Checking broker infra");
        assertBroker(brokerConfig);
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

    private AddressSpacePlan buildExampleAddressSpacePlan(StandardInfraConfig testInfra, String name, List<AddressPlan> addressPlans) {
        return new AddressSpacePlanBuilder()
                .withNewMetadata()
                .withName(name)
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
                .withAddressPlans(addressPlans
                        .stream().map(addressPlan -> addressPlan.getMetadata().getName()).collect(Collectors.toList()))
                .endSpec()
                .build();
    }

    private AddressSpace buildExampleAddressSpace(AddressSpacePlan plan, String name) {
        return new AddressSpaceBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(plan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
    }
}