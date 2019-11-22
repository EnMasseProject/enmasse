/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.plans.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.address.model.DoneableAddressSpace;
import io.enmasse.address.model.Phase;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressPlanBuilder;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.AddressSpacePlanBuilder;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfigBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecAdminBuilder;
import io.enmasse.admin.model.v1.StandardInfraConfigSpecBrokerBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressStatus;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.selenium.SeleniumFirefox;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.resources.AddressWebItem;
import io.enmasse.systemtest.shared.standard.QueueTest;
import io.enmasse.systemtest.shared.standard.TopicTest;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class PlansTestStandard extends TestBase implements ITestIsolatedStandard {
    private static Logger log = CustomLogger.getLogger();
    SeleniumProvider selenium = SeleniumProvider.getInstance();

    @Test
    void testCreateAddressSpacePlan() throws Exception {
        StandardInfraConfig infra = new StandardInfraConfigBuilder()
                .withNewMetadata()
                .withName("kornys")
                .endMetadata()
                .withNewSpec()
                .withVersion(environment.enmasseVersion())
                .withBroker(new StandardInfraConfigSpecBrokerBuilder()
                        .withAddressFullPolicy("FAIL")
                        .withNewResources()
                        .withMemory("750Mi")
                        .withStorage("2Gi")
                        .endResources()
                        .build())
                .withRouter(PlanUtils.createStandardRouterResourceObject("1Gi", 300, 1))
                .withAdmin(new StandardInfraConfigSpecAdminBuilder()
                        .withNewResources()
                        .withMemory("1Gi")
                        .endResources()
                        .build())
                .endSpec()
                .build();

        resourcesManager.createInfraConfig(infra);

        //define and create address plans
        List<ResourceRequest> addressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 0.0));
        List<ResourceRequest> addressResourcesTopic = Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 1.0));
        AddressPlan weakQueuePlan = PlanUtils.createAddressPlanObject("standard-queue-weak", AddressType.QUEUE, addressResourcesQueue);
        AddressPlan weakTopicPlan = PlanUtils.createAddressPlanObject("standard-topic-weak", AddressType.TOPIC, addressResourcesTopic);

        isolatedResourcesManager.createAddressPlan(weakQueuePlan);
        isolatedResourcesManager.createAddressPlan(weakTopicPlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 9.0),
                new ResourceAllowance("router", 5.0),
                new ResourceAllowance("aggregate", 10.0));
        List<AddressPlan> addressPlans = Arrays.asList(weakQueuePlan, weakTopicPlan);

        AddressSpacePlan weakSpacePlan = PlanUtils.createAddressSpacePlanObject("weak-plan", infra.getMetadata().getName(), AddressSpaceType.STANDARD, resources, addressPlans);
        resourcesManager.createAddressSpacePlan(weakSpacePlan);

        //create address space plan with new plan
        AddressSpace weakAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("weak-plan-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(weakSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(weakAddressSpace);

        //deploy destinations
        Address weakQueueDest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(weakAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(weakAddressSpace, "weak-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("weak-queue")
                .withPlan(weakQueuePlan.getMetadata().getName())
                .endSpec()
                .build();
        Address weakTopicDest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(weakAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(weakAddressSpace, "weak-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("weak-topic")
                .withPlan(weakTopicPlan.getMetadata().getName())
                .endSpec()
                .build();
        resourcesManager.setAddresses(weakQueueDest, weakTopicDest);

        //get destinations
        Address getWeakQueue = kubernetes.getAddressClient(weakAddressSpace.getMetadata().getNamespace()).withName(weakQueueDest.getMetadata().getName()).get();
        Address getWeakTopic = kubernetes.getAddressClient(weakAddressSpace.getMetadata().getNamespace()).withName(weakTopicDest.getMetadata().getName()).get();

        String assertMessage = "Queue plan wasn't set properly";
        assertAll("Both destination should contain right addressPlan",
                () -> assertEquals(getWeakQueue.getSpec().getPlan(),
                        weakQueuePlan.getMetadata().getName(), assertMessage),
                () -> assertEquals(getWeakTopic.getSpec().getPlan(),
                        weakTopicPlan.getMetadata().getName(), assertMessage));

        //simple send/receive
        UserCredentials user = new UserCredentials("test-newplan-name", "test_newplan_password");
        resourcesManager.createOrUpdateUser(weakAddressSpace, user);

        AmqpClient queueClient = getAmqpClientFactory().createQueueClient(weakAddressSpace);
        queueClient.getConnectOptions().setCredentials(user);
        QueueTest.runQueueTest(queueClient, weakQueueDest, 42);

        AmqpClient topicClient = getAmqpClientFactory().createTopicClient(weakAddressSpace);
        topicClient.getConnectOptions().setCredentials(user);
        TopicTest.runTopicTest(topicClient, weakTopicDest, 42);
    }

    @Test
    @SeleniumFirefox
    void testQuotaLimitsPooled() throws Exception {
        //define and create address plans
        AddressPlan queuePlan = PlanUtils.createAddressPlanObject("queue-pooled-test1", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.6), new ResourceRequest("router", 0.0)));

        AddressPlan topicPlan = PlanUtils.createAddressPlanObject("topic-pooled-test1", AddressType.TOPIC,
                Arrays.asList(
                        new ResourceRequest("broker", 0.4),
                        new ResourceRequest("router", 0.2)));

        AddressPlan anycastPlan = PlanUtils.createAddressPlanObject("anycast-test1", AddressType.ANYCAST,
                Collections.singletonList(new ResourceRequest("router", 0.3)));

        isolatedResourcesManager.createAddressPlan(queuePlan);
        isolatedResourcesManager.createAddressPlan(topicPlan);
        isolatedResourcesManager.createAddressPlan(anycastPlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 2.0),
                new ResourceAllowance("router", 1.0),
                new ResourceAllowance("aggregate", 2.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlan, topicPlan, anycastPlan);
        AddressSpacePlan addressSpacePlan = PlanUtils.createAddressSpacePlanObject("quota-limits-pooled-plan",
                "default-minimal", AddressSpaceType.STANDARD, resources, addressPlans);
        isolatedResourcesManager.createAddressSpacePlan(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-pooled-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(addressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);
        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        //check router limits
        checkLimits(addressSpace,
                Arrays.asList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "a1"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("anycast")
                                .withAddress("a1")
                                .withPlan(anycastPlan.getMetadata().getName())
                                .endSpec()
                                .build(),
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "a2"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("anycast")
                                .withAddress("a2")
                                .withPlan(anycastPlan.getMetadata().getName())
                                .endSpec()
                                .build(),
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "a3"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("anycast")
                                .withAddress("a3")
                                .withPlan(anycastPlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ),
                Collections.singletonList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "a4"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("anycast")
                                .withAddress("a4")
                                .withPlan(anycastPlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ), user);

        //check broker limits
        checkLimits(addressSpace,
                Arrays.asList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "q1"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("queue")
                                .withAddress("q1")
                                .withPlan(queuePlan.getMetadata().getName())
                                .endSpec()
                                .build(),
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "q2"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("queue")
                                .withAddress("q2")
                                .withPlan(queuePlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ),
                Collections.singletonList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "q3"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("queue")
                                .withAddress("q3")
                                .withPlan(queuePlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ), user);

        //check aggregate limits
        checkLimits(addressSpace,
                Arrays.asList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "t1"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("topic")
                                .withAddress("t1")
                                .withPlan(topicPlan.getMetadata().getName())
                                .endSpec()
                                .build(),
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "t2"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("topic")
                                .withAddress("t2")
                                .withPlan(topicPlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ),
                Collections.singletonList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "t3"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("topic")
                                .withAddress("t3")
                                .withPlan(topicPlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ), user);
    }

    @Test
    @SeleniumFirefox
    void testQuotaLimitsSharded() throws Exception {
        //define and create address plans
        AddressPlan queuePlan = PlanUtils.createAddressPlanObject("queue-sharded-test1", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 0.0)));

        AddressPlan topicPlan = PlanUtils.createAddressPlanObject("topic-sharded-test2", AddressType.TOPIC,
                Arrays.asList(
                        new ResourceRequest("broker", 1.0),
                        new ResourceRequest("router", 0.01)));

        isolatedResourcesManager.createAddressPlan(queuePlan);
        isolatedResourcesManager.createAddressPlan(topicPlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 2.0),
                new ResourceAllowance("router", 2.0),
                new ResourceAllowance("aggregate", 3.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlan, topicPlan);
        AddressSpacePlan addressSpacePlan = PlanUtils.createAddressSpacePlanObject("quota-limits-sharded-plan",
                "default-minimal", AddressSpaceType.STANDARD, resources, addressPlans);
        resourcesManager.createAddressSpacePlan(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-quota-sharded-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(addressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);
        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        //check broker limits
        checkLimits(addressSpace,
                Arrays.asList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "q1"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("queue")
                                .withAddress("q1")
                                .withPlan(queuePlan.getMetadata().getName())
                                .endSpec()
                                .build(),
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "q2"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("queue")
                                .withAddress("q2")
                                .withPlan(queuePlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ),
                Collections.singletonList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "q3"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("queue")
                                .withAddress("q3")
                                .withPlan(queuePlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ), user);

        //check aggregate limits
        checkLimits(addressSpace,
                Arrays.asList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "t1"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("topic")
                                .withAddress("t1")
                                .withPlan(topicPlan.getMetadata().getName())
                                .endSpec()
                                .build(),
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "t2"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("topic")
                                .withAddress("t2")
                                .withPlan(topicPlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ),
                Collections.singletonList(
                        new AddressBuilder()
                                .withNewMetadata()
                                .withNamespace(addressSpace.getMetadata().getNamespace())
                                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "t3"))
                                .endMetadata()
                                .withNewSpec()
                                .withType("topic")
                                .withAddress("t3")
                                .withPlan(topicPlan.getMetadata().getName())
                                .endSpec()
                                .build()
                ), user);
    }

    @Test
    void testScalePlanPartitions() throws Exception {
        //define and create address plans
        AddressPlan partitionedQueue = new AddressPlanBuilder()
                .editOrNewMetadata()
                .withName("partitioned-queue")
                .endMetadata()
                .editOrNewSpec()
                .withAddressType("queue")
                .withPartitions(2)
                .addToResources("router", 0.001)
                .addToResources("broker", 0.6)
                .endSpec()
                .build();

        AddressPlan manyPartitionedQueue = new AddressPlanBuilder()
                .editOrNewMetadata()
                .withName("many-partitioned-queue")
                .endMetadata()
                .editOrNewSpec()
                .withAddressType("queue")
                .withPartitions(4)
                .addToResources("router", 0.001)
                .addToResources("broker", 0.6)
                .endSpec()
                .build();

        AddressPlan simpleQueue = new AddressPlanBuilder()
                .editOrNewMetadata()
                .withName("simple-queue")
                .endMetadata()
                .editOrNewSpec()
                .withAddressType("queue")
                .withPartitions(1)
                .addToResources("router", 0.001)
                .addToResources("broker", 0.6)
                .endSpec()
                .build();

        isolatedResourcesManager.createAddressPlan(simpleQueue);
        isolatedResourcesManager.createAddressPlan(partitionedQueue);
        isolatedResourcesManager.createAddressPlan(manyPartitionedQueue);

        //define and create address space plan
        AddressSpacePlan partitionedAddressesPlan = new AddressSpacePlanBuilder()
                .editOrNewMetadata()
                .withName("partitioned-addresses")
                .endMetadata()
                .editOrNewSpec()
                .withAddressSpaceType("standard")
                .withInfraConfigRef("default-minimal")
                .addToResourceLimits("broker", 2.0)
                .addToResourceLimits("router", 2.0)
                .addToResourceLimits("aggregate", 12.0)
                .addToAddressPlans(simpleQueue.getMetadata().getName(), partitionedQueue.getMetadata().getName(), manyPartitionedQueue.getMetadata().getName())
                .endSpec()
                .build();

        resourcesManager.createAddressSpacePlan(partitionedAddressesPlan);

        Thread.sleep(30_000);

        //create address space plan with new plan
        AddressSpace partitioned = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("partitioned")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(partitionedAddressesPlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(partitioned);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        resourcesManager.createOrUpdateUser(partitioned, cred);

        Address address = new AddressBuilder()
                .editOrNewMetadata()
                .withNamespace(partitioned.getMetadata().getNamespace())
                .withName(partitioned.getMetadata().getName() + "." + "myqueue")
                .endMetadata()
                .editOrNewSpec()
                .withAddress("myqueue")
                .withPlan(simpleQueue.getMetadata().getName())
                .withType("queue")
                .endSpec()
                .build();
        resourcesManager.appendAddresses(address);
        waitForBrokerReplicas(partitioned, address, 1);
        getClientUtils().assertCanConnect(partitioned, cred, Collections.singletonList(address), resourcesManager);

        // Increase number of partitions and expect broker to be created
        address.getSpec().setPlan(partitionedQueue.getMetadata().getName());
        resourcesManager.replaceAddress(address);

        waitForBrokerReplicas(partitioned, address, 1);
        getClientUtils().assertCanConnect(partitioned, cred, Collections.singletonList(address), resourcesManager);


        // Decrease number of partitions and expect broker to disappear
        address.getSpec().setPlan(simpleQueue.getMetadata().getName());
        resourcesManager.replaceAddress(address);
        waitForBrokerReplicas(partitioned, address, 1);
        getClientUtils().assertCanConnect(partitioned, cred, Collections.singletonList(address), resourcesManager);

        // Increase to too many partitions
        address.getSpec().setPlan(manyPartitionedQueue.getMetadata().getName());
        TimeoutBudget budget = new TimeoutBudget(2, TimeUnit.MINUTES);
        AddressUtils.replaceAddress(address, false, budget);
        Address replaced = null;
        while (!budget.timeoutExpired()) {
            replaced = kubernetes.getAddressClient(partitioned.getMetadata().getNamespace()).withName(address.getMetadata().getName()).get();
            if (replaced.getStatus().getMessages().contains("Quota exceeded")) {
                break;
            }
        }
        replaced = kubernetes.getAddressClient(partitioned.getMetadata().getNamespace()).withName(address.getMetadata().getName()).get();
        assertNotNull(replaced);
        assertTrue(replaced.getStatus().getMessages().contains("Quota exceeded"), "No status message is present");
    }

    @Test
    void testScalePooledBrokers() throws Exception {
        //define and create address plans
        List<ResourceRequest> addressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 0.99), new ResourceRequest("router", 0.0));
        AddressPlan xlQueuePlan = PlanUtils.createAddressPlanObject("pooled-xl-queue", AddressType.QUEUE, addressResourcesQueue);
        isolatedResourcesManager.createAddressPlan(xlQueuePlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 10.0),
                new ResourceAllowance("router", 2.0),
                new ResourceAllowance("aggregate", 12.0));
        List<AddressPlan> addressPlans = Collections.singletonList(xlQueuePlan);
        AddressSpacePlan manyAddressesPlan = PlanUtils.createAddressSpacePlanObject("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        resourcesManager.createAddressSpacePlan(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("many-addresses-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(manyAddressesPlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        resourcesManager.createOrUpdateUser(manyAddressesSpace, cred);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 4;
        int toDeleteCount = 2;
        for (int i = 0; i < destCount; i++) {
            dest.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(manyAddressesSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(manyAddressesSpace, "xl-queue-" + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("xl-queue-" + i)
                    .withPlan(xlQueuePlan.getMetadata().getName())
                    .endSpec()
                    .build());
        }

        resourcesManager.setAddresses(dest.toArray(new Address[0]));
        for (Address destination : dest) {
            waitForBrokerReplicas(manyAddressesSpace, destination, 1);
        }

        getClientUtils().assertCanConnect(manyAddressesSpace, cred, dest, resourcesManager);

        resourcesManager.deleteAddresses(dest.subList(0, toDeleteCount).toArray(new Address[0]));
        for (Address destination : dest.subList(toDeleteCount, destCount)) {
            waitForBrokerReplicas(manyAddressesSpace, destination, 1);
        }

        getClientUtils().assertCanConnect(manyAddressesSpace, cred, dest.subList(toDeleteCount, destCount), resourcesManager);
    }

    @Test
    void testMessagePersistenceAfterAutoScale() throws Exception {
        //define and create address plans
        List<ResourceRequest> addressResourcesQueueAlpha = Arrays.asList(new ResourceRequest("broker", 0.3), new ResourceRequest("router", 0));
        List<ResourceRequest> addressResourcesQueueBeta = Arrays.asList(new ResourceRequest("broker", 0.6), new ResourceRequest("router", 0));

        AddressPlan queuePlanAlpha = PlanUtils.createAddressPlanObject("pooled-standard-queue-alpha", AddressType.QUEUE, addressResourcesQueueAlpha);
        isolatedResourcesManager.createAddressPlan(queuePlanAlpha);
        AddressPlan queuePlanBeta = PlanUtils.createAddressPlanObject("pooled-standard-queue-beta", AddressType.QUEUE, addressResourcesQueueBeta);
        isolatedResourcesManager.createAddressPlan(queuePlanBeta);


        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 3.0),
                new ResourceAllowance("router", 5.0),
                new ResourceAllowance("aggregate", 5.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlanAlpha, queuePlanBeta);
        AddressSpacePlan scaleSpacePlan = PlanUtils.createAddressSpacePlanObject("scale-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        resourcesManager.createAddressSpacePlan(scaleSpacePlan);

        //create address space plan with new plan
        AddressSpace messagePersistAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("persist-plan-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(scaleSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(messagePersistAddressSpace);

        //deploy destinations
        Address queue1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(messagePersistAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(messagePersistAddressSpace, "queue1-beta"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue1-beta")
                .withPlan(queuePlanBeta.getMetadata().getName())
                .endSpec()
                .build();
        Address queue2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(messagePersistAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(messagePersistAddressSpace, "queue2-beta"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue2-beta")
                .withPlan(queuePlanBeta.getMetadata().getName())
                .endSpec()
                .build();
        Address queue3 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(messagePersistAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(messagePersistAddressSpace, "queue1-alpha"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue1-alpha")
                .withPlan(queuePlanAlpha.getMetadata().getName())
                .endSpec()
                .build();
        Address queue4 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(messagePersistAddressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(messagePersistAddressSpace, "queue2-alpha"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue2-alpha")
                .withPlan(queuePlanAlpha.getMetadata().getName())
                .endSpec()
                .build();

        resourcesManager.setAddresses(queue1, queue2);
        resourcesManager.appendAddresses(queue3, queue4);


        // Dump address/broker assignment to help understand occasional test failure.
        kubernetes.getAddressClient().list().getItems().forEach(q -> {
            Address a = kubernetes.getAddressClient(messagePersistAddressSpace.getMetadata().getNamespace()).withName(q.getMetadata().getName()).get();
            log.info("Address {} => {}", q.getMetadata().getName(), a.getStatus().getBrokerStatuses());
        });

        //send 500 messages to each queue
        UserCredentials user = new UserCredentials("test-scale-user-name", "test_scale_user_pswd");
        resourcesManager.createOrUpdateUser(messagePersistAddressSpace, user);

        AmqpClient queueClient = getAmqpClientFactory().createQueueClient(messagePersistAddressSpace);
        queueClient.getConnectOptions().setCredentials(user);

        List<String> msgs = TestUtils.generateMessages(350);
        Future<Integer> sendResult1 = queueClient.sendMessages(queue1.getSpec().getAddress(), msgs);
        Future<Integer> sendResult2 = queueClient.sendMessages(queue2.getSpec().getAddress(), msgs);
        Future<Integer> sendResult3 = queueClient.sendMessages(queue3.getSpec().getAddress(), msgs);
        Future<Integer> sendResult4 = queueClient.sendMessages(queue4.getSpec().getAddress(), msgs);
        assertAll("All senders should send all messages",
                () -> assertThat("Incorrect count of messages sent", sendResult1.get(1, TimeUnit.MINUTES), is(msgs.size())),
                () -> assertThat("Incorrect count of messages sent", sendResult2.get(1, TimeUnit.MINUTES), is(msgs.size())),
                () -> assertThat("Incorrect count of messages sent", sendResult3.get(1, TimeUnit.MINUTES), is(msgs.size())),
                () -> assertThat("Incorrect count of messages sent", sendResult4.get(1, TimeUnit.MINUTES), is(msgs.size())));

        //remove addresses from first pod and wait for scale down
        log.info("Deleting beta addresses");
        resourcesManager.deleteAddresses(queue1, queue2);

        try {
            TestUtils.waitForNBrokerReplicas(messagePersistAddressSpace, 1, queue4, new TimeoutBudget(5, TimeUnit.MINUTES));
        } finally {
            kubernetes.getAddressClient().list().getItems().forEach(q -> {
                Address a = kubernetes.getAddressClient(messagePersistAddressSpace.getMetadata().getNamespace()).withName(q.getMetadata().getName()).get();
                log.info("Address {} => {}", q.getMetadata().getName(), a.getStatus().getBrokerStatuses());
            });
        }

        //validate count of addresses
        List<Address> addresses = AddressUtils.getAddresses(messagePersistAddressSpace);
        assertThat(String.format("Unexpected count of destinations, got following: %s", addresses),
                addresses.size(), is(2));

        //receive messages from remaining addresses
        getClientUtils().receiveMessages(queueClient, queue3.getSpec().getAddress(), msgs.size());
        getClientUtils().receiveMessages(queueClient, queue4.getSpec().getAddress(), msgs.size());
    }

    @Test
    void testReplaceAddressSpacePlanStandard() throws Exception {
        //define and create address plans
        AddressPlan beforeQueuePlan = PlanUtils.createAddressPlanObject("before-small-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 0.0)));

        AddressPlan beforeTopicPlan = PlanUtils.createAddressPlanObject("before-small-sharded-topic", AddressType.TOPIC,
                Arrays.asList(
                        new ResourceRequest("broker", 1.0),
                        new ResourceRequest("router", 0.01)));

        AddressPlan afterQueuePlan = PlanUtils.createAddressPlanObject("after-large-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.5), new ResourceRequest("router", 0.0)));

        AddressPlan afterTopicPlan = PlanUtils.createAddressPlanObject("after-large-sharded-topic", AddressType.TOPIC,
                Arrays.asList(
                        new ResourceRequest("broker", 1.5),
                        new ResourceRequest("router", 0.01)));

        AddressPlan pooledQueuePlan = PlanUtils.createAddressPlanObject("after-pooled-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.44), new ResourceRequest("router", 0.0)));

        isolatedResourcesManager.createAddressPlan(beforeQueuePlan);
        isolatedResourcesManager.createAddressPlan(beforeTopicPlan);
        isolatedResourcesManager.createAddressPlan(afterQueuePlan);
        isolatedResourcesManager.createAddressPlan(afterTopicPlan);
        isolatedResourcesManager.createAddressPlan(pooledQueuePlan);

        //define and create address space plans

        AddressSpacePlan beforeAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("before-update-standard-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 5.0),
                        new ResourceAllowance("router", 5.0),
                        new ResourceAllowance("aggregate", 10.0)),
                Arrays.asList(beforeQueuePlan, beforeTopicPlan));

        AddressSpacePlan afterAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("after-update-standard-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 5.0),
                        new ResourceAllowance("router", 5.0),
                        new ResourceAllowance("aggregate", 10.0)),
                Arrays.asList(afterQueuePlan, afterTopicPlan));

        AddressSpacePlan pooledAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("after-update-standard-pooled-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 10.0),
                        new ResourceAllowance("router", 10.0),
                        new ResourceAllowance("aggregate", 10.0)),
                Collections.singletonList(pooledQueuePlan));


        isolatedResourcesManager.createAddressSpacePlan(beforeAddressSpacePlan);
        isolatedResourcesManager.createAddressSpacePlan(afterAddressSpacePlan);
        isolatedResourcesManager.createAddressSpacePlan(pooledAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(beforeAddressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(beforeQueuePlan.getMetadata().getName())
                .endSpec()
                .build();
        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic")
                .withPlan(beforeTopicPlan.getMetadata().getName())
                .endSpec()
                .build();

        resourcesManager.setAddresses(queue, topic);

        clientUtils.sendDurableMessages(isolatedResourcesManager, addressSpace, queue, user, 16);

        addressSpace = new DoneableAddressSpace(addressSpace).editSpec().withPlan(afterAddressSpacePlan.getMetadata().getName()).endSpec().done();
        isolatedResourcesManager.replaceAddressSpace(addressSpace);
        AddressUtils.waitForDestinationsReady(new TimeoutBudget(5, TimeUnit.MINUTES), queue, topic);

        clientUtils.receiveDurableMessages(isolatedResourcesManager, addressSpace, queue, user, 16);

        Address afterQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-2")
                .withPlan(afterQueuePlan.getMetadata().getName())
                .endSpec()
                .build();
        isolatedResourcesManager.appendAddresses(afterQueue);

        getClientUtils().assertCanConnect(addressSpace, user, Arrays.asList(afterQueue, queue, topic), resourcesManager);

        addressSpace = new DoneableAddressSpace(addressSpace).editSpec().withPlan(pooledAddressSpacePlan.getMetadata().getName()).endSpec().done();
        isolatedResourcesManager.replaceAddressSpace(addressSpace);
        AddressUtils.waitForDestinationsReady(new TimeoutBudget(5, TimeUnit.MINUTES), afterQueue, queue, topic);

        Address pooledQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-3"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue-3")
                .withPlan(pooledQueuePlan.getMetadata().getName())
                .endSpec()
                .build();
        resourcesManager.appendAddresses(pooledQueue);

        getClientUtils().assertCanConnect(addressSpace, user, Arrays.asList(queue, topic, afterQueue, pooledQueue), resourcesManager);
    }

    @Test
    void testCannotReplaceAddressSpacePlanStandard() throws Exception {
        //define and create address plans
        AddressPlan afterQueuePlan = PlanUtils.createAddressPlanObject("after-small-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 0)));

        AddressPlan beforeQueuePlan = PlanUtils.createAddressPlanObject("before-large-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 2.0), new ResourceRequest("router", 0)));

        isolatedResourcesManager.createAddressPlan(beforeQueuePlan);
        isolatedResourcesManager.createAddressPlan(afterQueuePlan);

        //define and create address space plans

        AddressSpacePlan beforeAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("before-update-standard-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 5.0),
                        new ResourceAllowance("router", 2.0),
                        new ResourceAllowance("aggregate", 7.0)),
                Collections.singletonList(beforeQueuePlan));

        AddressSpacePlan afterAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("after-update-standard-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 2.0),
                        new ResourceAllowance("router", 2.0),
                        new ResourceAllowance("aggregate", 4.0)),
                Collections.singletonList(afterQueuePlan));


        isolatedResourcesManager.createAddressSpacePlan(beforeAddressSpacePlan);
        isolatedResourcesManager.createAddressSpacePlan(afterAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-sharded-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(beforeAddressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        resourcesManager.createOrUpdateUser(addressSpace, user);

        List<Address> queues = Arrays.asList(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-1"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue-1")
                        .withPlan(beforeQueuePlan.getMetadata().getName())
                        .endSpec()
                        .build(),
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue-2"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue-2")
                        .withPlan(beforeQueuePlan.getMetadata().getName())
                        .endSpec()
                        .build());


        resourcesManager.setAddresses(queues.toArray(new Address[0]));
        getClientUtils().assertCanConnect(addressSpace, user, queues, resourcesManager);

        AddressSpace replaced = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-sharded-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(afterAddressSpacePlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        isolatedResourcesManager.replaceAddressSpace(replaced, false);

        String expected = String.format("Unable to apply plan [%s] to address space %s:%s: quota exceeded for resource broker",
                afterQueuePlan.getMetadata().getName(), environment.namespace(), replaced.getMetadata().getName());
        replaced = AddressSpaceUtils.waitForAddressSpaceStatusMessage(replaced, expected, new TimeoutBudget(2, TimeUnit.MINUTES));

        assertEquals(beforeAddressSpacePlan.getMetadata().getName(),
                replaced.getMetadata().getAnnotations().get("enmasse.io/applied-plan"));
    }

    @Test
    void testSwitchQueuePlan() throws Exception {
        AddressPlan beforeQueuePlan = PlanUtils.createAddressPlanObject("small-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.2), new ResourceRequest("router", 0.0)));

        AddressPlan afterQueuePlan = PlanUtils.createAddressPlanObject("bigger-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.8), new ResourceRequest("router", 0.0)));

        isolatedResourcesManager.createAddressPlan(beforeQueuePlan);
        isolatedResourcesManager.createAddressPlan(afterQueuePlan);

        AddressSpacePlan addressPlan = PlanUtils.createAddressSpacePlanObject("address-switch-address-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 5.0),
                        new ResourceAllowance("router", 5.0),
                        new ResourceAllowance("aggregate", 10.0)),
                Arrays.asList(beforeQueuePlan, afterQueuePlan));

        resourcesManager.createAddressSpacePlan(addressPlan);

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-pooled-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(addressPlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(addressSpace);
        UserCredentials cred = new UserCredentials("test-user", "test-password");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        List<Address> queues = IntStream.range(0, 8).boxed().map(i ->
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-" + i))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("queue-" + i)
                        .withPlan(beforeQueuePlan.getMetadata().getName())
                        .endSpec()
                        .build()).collect(Collectors.toList());
        resourcesManager.setAddresses(queues.toArray(new Address[0]));

        assertThat("Failed there are no 2 broker pods", TestUtils.listBrokerPods(kubernetes, addressSpace).size(), is(2));

        for (Address queue : queues) {
            clientUtils.sendDurableMessages(isolatedResourcesManager, addressSpace, queue, cred, 400);
        }

        Address queueAfter = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-1"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-1")
                .withPlan(afterQueuePlan.getMetadata().getName())
                .endSpec()
                .build();
        resourcesManager.replaceAddress(queueAfter);

        assertThat("Failed there are no 3 broker pods", TestUtils.listBrokerPods(kubernetes, addressSpace).size(), is(3));

        for (Address queue : queues) {
            clientUtils.receiveDurableMessages(isolatedResourcesManager, addressSpace, queue, cred, 400);
        }
    }

    //------------------------------------------------------------------------------------------------
    // Help methods
    //------------------------------------------------------------------------------------------------

    private void checkLimits(AddressSpace addressSpace, List<Address> allowedDest, List<Address> notAllowedDest, UserCredentials credentials)
            throws Exception {
        var client = kubernetes.getAddressClient(addressSpace.getMetadata().getNamespace());

        log.info("Try to create {} addresses, and make sure that {} addresses will be not created",
                Arrays.toString(allowedDest.stream().map(address -> address.getMetadata().getName()).toArray(String[]::new)),
                Arrays.toString(notAllowedDest.stream().map(address -> address.getMetadata().getName()).toArray(String[]::new)));

        resourcesManager.setAddresses(allowedDest.toArray(new Address[0]));
        List<Address> getAddresses = new ArrayList<>();
        for (Address dest : allowedDest) {
            getAddresses.add(client.withName(dest.getMetadata().getName()).get());
        }

        for (Address address : getAddresses) {
            log.info("Address {} with plan {} is in phase {}", address.getMetadata().getName(), address.getSpec().getPlan(), address.getStatus().getPhase());
            String assertMessage = String.format("Address from allowed %s is not ready", address.getMetadata().getName());
            assertEquals(Phase.Active, address.getStatus().getPhase(), assertMessage);
        }

        getClientUtils().assertCanConnect(addressSpace, credentials, allowedDest, resourcesManager);

        getAddresses.clear();
        if (notAllowedDest.size() > 0) {
            try {
                resourcesManager.appendAddresses(new TimeoutBudget(30, TimeUnit.SECONDS), notAllowedDest.toArray(new Address[0]));
            } catch (IllegalStateException ex) {
                if (!ex.getMessage().contains("match")) {
                    throw ex;
                }
            }

            for (Address dest : notAllowedDest) {
                getAddresses.add(client.withName(dest.getMetadata().getName()).get());
            }

            for (Address address : getAddresses) {
                log.info("Address {} with plan {} is in phase {}", address.getMetadata().getName(), address.getSpec().getPlan(), address.getStatus().getPhase());
                String assertMessage = String.format("Address from notAllowed %s is ready", address.getMetadata().getName());
                assertEquals(Phase.Pending, address.getStatus().getPhase(), assertMessage);
                assertTrue(address.getStatus().getMessages().contains("Quota exceeded"), "No status message is present");
            }
        }

        ConsoleWebPage page = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressSpace, clusterUser);
        page.openWebConsolePage();
        page.openAddressesPageWebConsole();

        for (Address dest : allowedDest) {
            AddressWebItem item = selenium.waitUntilItemPresent(25, () -> page.getAddressItem(dest));
            assertNotNull(item, String.format("Address '%s' is not visible in console", dest));
            assertThat("Item is not in state Ready", item.getStatus(), is(AddressStatus.READY));
        }

        for (Address dest : notAllowedDest) {
            AddressWebItem item = selenium.waitUntilItemPresent(25, () -> page.getAddressItem(dest));
            assertNotNull(item, String.format("Address '%s' is not visible in console", dest));
            assertThat("Item is not in state Pending", item.getStatus(), is(AddressStatus.PENDING));
        }

        resourcesManager.deleteAddresses(addressSpace);
    }
}
