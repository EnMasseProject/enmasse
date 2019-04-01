/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.plans;

import io.enmasse.address.model.*;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.admin.model.v1.*;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.selenium.ISeleniumProviderChrome;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.resources.AddressWebItem;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.standard.TopicTest;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.vertx.core.json.JsonObject;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TestTag.isolated;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Tag(isolated)
class PlansTest extends TestBase implements ISeleniumProviderChrome {

    private static Logger log = CustomLogger.getLogger();
    private static final PlansProvider plansProvider = new PlansProvider(kubernetes);

    @BeforeEach
    void setUp() throws Exception {
        plansProvider.setUp();
        if (selenium.getDriver() == null)
            selenium.setupDriver(environment, kubernetes, TestUtils.getChromeDriver());
        else
            selenium.clearScreenShots();
    }

    @AfterEach
    void tearDown() throws Exception {
        plansProvider.tearDown();
    }

    @Test
    void testCreateAddressSpacePlan() throws Exception {
        StandardInfraConfig infra = PlanUtils.createStandardInfraConfigObject("kornys",
                PlanUtils.createStandardBrokerResourceObject("750Mi", "2Gi", null),
                PlanUtils.createStandardAdminResourceObject("1Gi", null),
                PlanUtils.createStandardRouterResourceObject("1Gi", 300, 1),
                environment.enmasseVersion());

        plansProvider.createInfraConfig(infra);

        //define and create address plans
        List<ResourceRequest> addressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 0.0));
        List<ResourceRequest> addressResourcesTopic = Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 1.0));
        AddressPlan weakQueuePlan = PlanUtils.createAddressPlanObject("standard-queue-weak", AddressType.QUEUE, addressResourcesQueue);
        AddressPlan weakTopicPlan = PlanUtils.createAddressPlanObject("standard-topic-weak", AddressType.TOPIC, addressResourcesTopic);

        plansProvider.createAddressPlan(weakQueuePlan);
        plansProvider.createAddressPlan(weakTopicPlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 9.0),
                new ResourceAllowance("router", 5.0),
                new ResourceAllowance("aggregate", 10.0));
        List<AddressPlan> addressPlans = Arrays.asList(weakQueuePlan, weakTopicPlan);

        AddressSpacePlan weakSpacePlan = PlanUtils.createAddressSpacePlanObject("weak-plan", infra.getMetadata().getName(), AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(weakSpacePlan);

        //create address space plan with new plan
        AddressSpace weakAddressSpace = AddressSpaceUtils.createAddressSpaceObject("weak-address-space", AddressSpaceType.STANDARD,
                weakSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(weakAddressSpace);

        //deploy destinations
        Address weakQueueDest = AddressUtils.createQueueAddressObject("weak-queue", weakQueuePlan.getMetadata().getName());
        Address weakTopicDest = AddressUtils.createTopicAddressObject("weak-topic", weakTopicPlan.getMetadata().getName());
        setAddresses(weakAddressSpace, weakQueueDest, weakTopicDest);

        //get destinations
        Future<List<Address>> getWeakQueue = getAddressesObjects(weakAddressSpace, Optional.of(weakQueueDest.getMetadata().getName()));
        Future<List<Address>> getWeakTopic = getAddressesObjects(weakAddressSpace, Optional.of(weakTopicDest.getMetadata().getName()));

        String assertMessage = "Queue plan wasn't set properly";
        assertAll("Both destination should contain right addressPlan",
                () -> assertEquals(getWeakQueue.get(20, TimeUnit.SECONDS).get(0).getSpec().getPlan(),
                        weakQueuePlan.getMetadata().getName(), assertMessage),
                () -> assertEquals(getWeakTopic.get(20, TimeUnit.SECONDS).get(0).getSpec().getPlan(),
                        weakTopicPlan.getMetadata().getName(), assertMessage));

        //simple send/receive
        UserCredentials user = new UserCredentials("test-newplan-name", "test_newplan_password");
        createUser(weakAddressSpace, user);

        AmqpClient queueClient = amqpClientFactory.createQueueClient(weakAddressSpace);
        queueClient.getConnectOptions().setCredentials(user);
        QueueTest.runQueueTest(queueClient, weakQueueDest, 42);

        AmqpClient topicClient = amqpClientFactory.createTopicClient(weakAddressSpace);
        topicClient.getConnectOptions().setCredentials(user);
        TopicTest.runTopicTest(topicClient, weakTopicDest, 42);
    }

    @Test
    void testQuotaLimitsPooled() throws Exception {
        //define and create address plans
        AddressPlan queuePlan = PlanUtils.createAddressPlanObject("queue-pooled-test1", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.6), new ResourceRequest("router", 0.0)));

        AddressPlan queuePlan2 = PlanUtils.createAddressPlanObject("queue-pooled-test2", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.1), new ResourceRequest("router", 0.0)));

        AddressPlan queuePlan3 = PlanUtils.createAddressPlanObject("queue-pooled-test3", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.049), new ResourceRequest("router", 0.0)));

        AddressPlan topicPlan = PlanUtils.createAddressPlanObject("topic-pooled-test1", AddressType.TOPIC,
                Arrays.asList(
                        new ResourceRequest("broker", 0.4),
                        new ResourceRequest("router", 0.2)));

        AddressPlan anycastPlan = PlanUtils.createAddressPlanObject("anycast-test1", AddressType.ANYCAST,
                Collections.singletonList(new ResourceRequest("router", 0.3)));

        plansProvider.createAddressPlan(queuePlan);
        plansProvider.createAddressPlan(queuePlan2);
        plansProvider.createAddressPlan(queuePlan3);
        plansProvider.createAddressPlan(topicPlan);
        plansProvider.createAddressPlan(anycastPlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 2.0),
                new ResourceAllowance("router", 1.0),
                new ResourceAllowance("aggregate", 2.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlan, queuePlan2, queuePlan3, topicPlan, anycastPlan);
        AddressSpacePlan addressSpacePlan = PlanUtils.createAddressSpacePlanObject("quota-limits-pooled-plan",
                "default-minimal", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-pooled-space", AddressSpaceType.STANDARD,
                addressSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(addressSpace);
        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        //check router limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createAnycastAddressObject("a1", anycastPlan.getMetadata().getName()),
                        AddressUtils.createAnycastAddressObject("a2", anycastPlan.getMetadata().getName()),
                        AddressUtils.createAnycastAddressObject("a3", anycastPlan.getMetadata().getName())
                ),
                Collections.singletonList(
                        AddressUtils.createAnycastAddressObject("a4", anycastPlan.getMetadata().getName())
                ), user);

        //check broker limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createQueueAddressObject("q1", queuePlan.getMetadata().getName()),
                        AddressUtils.createQueueAddressObject("q2", queuePlan.getMetadata().getName())
                ),
                Collections.singletonList(
                        AddressUtils.createQueueAddressObject("q3", queuePlan.getMetadata().getName())
                ), user);

        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createQueueAddressObject("q1", queuePlan.getMetadata().getName()), // 0.6
                        AddressUtils.createQueueAddressObject("q2", queuePlan.getMetadata().getName()), // 0.6
                        AddressUtils.createQueueAddressObject("q3", queuePlan2.getMetadata().getName()), // 0.1
                        AddressUtils.createQueueAddressObject("q4", queuePlan2.getMetadata().getName()), // 0.1
                        AddressUtils.createQueueAddressObject("q5", queuePlan2.getMetadata().getName()), // 0.1
                        AddressUtils.createQueueAddressObject("q6", queuePlan2.getMetadata().getName()), // 0.1
                        AddressUtils.createQueueAddressObject("q7", queuePlan3.getMetadata().getName()), // 0.049
                        AddressUtils.createQueueAddressObject("q8", queuePlan3.getMetadata().getName()), // 0.049
                        AddressUtils.createQueueAddressObject("q9", queuePlan3.getMetadata().getName()), // 0.049
                        AddressUtils.createQueueAddressObject("q10", queuePlan3.getMetadata().getName()), // 0.049
                        AddressUtils.createQueueAddressObject("q11", queuePlan3.getMetadata().getName()), // 0.049
                        AddressUtils.createQueueAddressObject("q12", queuePlan3.getMetadata().getName()) // 0.049
                ), Collections.emptyList(), user);

        //check aggregate limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createTopicAddressObject("t1", topicPlan.getMetadata().getName()),
                        AddressUtils.createTopicAddressObject("t2", topicPlan.getMetadata().getName())
                ),
                Collections.singletonList(
                        AddressUtils.createTopicAddressObject("t3", topicPlan.getMetadata().getName())
                ), user);
    }

    @Test
    void testQuotaLimitsSharded() throws Exception {
        //define and create address plans
        AddressPlan queuePlan = PlanUtils.createAddressPlanObject("queue-sharded-test1", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 0.0)));

        AddressPlan topicPlan = PlanUtils.createAddressPlanObject("topic-sharded-test2", AddressType.TOPIC,
                Arrays.asList(
                        new ResourceRequest("broker", 1.0),
                        new ResourceRequest("router", 0.01)));

        plansProvider.createAddressPlan(queuePlan);
        plansProvider.createAddressPlan(topicPlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 2.0),
                new ResourceAllowance("router", 2.0),
                new ResourceAllowance("aggregate", 3.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlan, topicPlan);
        AddressSpacePlan addressSpacePlan = PlanUtils.createAddressSpacePlanObject("quota-limits-sharded-plan",
                "default-minimal", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-sharded-space", AddressSpaceType.STANDARD,
                addressSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(addressSpace);
        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        //check broker limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createQueueAddressObject("q1", queuePlan.getMetadata().getName()),
                        AddressUtils.createQueueAddressObject("q2", queuePlan.getMetadata().getName())
                ),
                Collections.singletonList(
                        AddressUtils.createQueueAddressObject("q3", queuePlan.getMetadata().getName())
                ), user);

        //check aggregate limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createTopicAddressObject("t1", topicPlan.getMetadata().getName()),
                        AddressUtils.createTopicAddressObject("t2", topicPlan.getMetadata().getName())
                ),
                Collections.singletonList(
                        AddressUtils.createTopicAddressObject("t3", topicPlan.getMetadata().getName())
                ), user);
    }

    @Test
    void testScalePlanPartitions() throws Exception {
        //define and create address plans
        List<ResourceRequest> addressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 0.99), new ResourceRequest("router", 0.0));
        AddressPlan addressPlan = new AddressPlanBuilder()
                .editOrNewMetadata()
                .withName("partitioned-queue")
                .endMetadata()
                .editOrNewSpec()
                .withAddressType("queue")
                .withPartitions(1)
                .addToResources("router", 0.001)
                .addToResources("broker", 0.6)
                .endSpec()
                .build();

        plansProvider.createAddressPlan(addressPlan);

        //define and create address space plan
        AddressSpacePlan partitionedAddressesPlan = new AddressSpacePlanBuilder()
                .editOrNewMetadata()
                .withName("partitioned-addresses")
                .endMetadata()
                .editOrNewSpec()
                .withAddressSpaceType("standard")
                .withInfraConfigRef("default")
                .addToResourceLimits("broker", 10.0)
                .addToResourceLimits("router", 2.0)
                .addToResourceLimits("aggregate", 12.0)
                .addToAddressPlans("partitioned-queue")
                .endSpec()
                .build();

        plansProvider.createAddressSpacePlan(partitionedAddressesPlan);

        //create address space plan with new plan
        AddressSpace partitioned = AddressSpaceUtils.createAddressSpaceObject("partitioned", AddressSpaceType.STANDARD,
                partitionedAddressesPlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(partitioned);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createUser(partitioned, cred);

        Address address = new AddressBuilder()
                .editOrNewMetadata()
                .withName("partitioned.myqueue")
                .endMetadata()
                .editOrNewSpec()
                .withAddress("myqueue")
                .withPlan("partitioned-queue")
                .withType("queue")
                .endSpec()
                .build();
        appendAddresses(partitioned, address);
        waitForBrokerReplicas(partitioned, address, 1);
        assertCanConnect(partitioned, cred, Collections.singletonList(address));

        addressPlan = plansProvider.getAddressPlan(addressPlan.getMetadata().getName());
        // Increase number of partitions and expect broker to be created
        addressPlan.getSpec().setPartitions(2);
        plansProvider.replaceAddressPlan(addressPlan);
        waitForBrokerReplicas(partitioned, address, 2);
        assertCanConnect(partitioned, cred, Collections.singletonList(address));


        // Decrease number of partitions and expect broker to disappear
        addressPlan.getSpec().setPartitions(1);
        plansProvider.replaceAddressPlan(addressPlan);
        waitForBrokerReplicas(partitioned, address, 1);
        assertCanConnect(partitioned, cred, Collections.singletonList(address));
    }

    @Test
    void testScalePooledBrokers() throws Exception {
        //define and create address plans
        List<ResourceRequest> addressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 0.99), new ResourceRequest("router", 0.0));
        AddressPlan xlQueuePlan = PlanUtils.createAddressPlanObject("pooled-xl-queue", AddressType.QUEUE, addressResourcesQueue);
        plansProvider.createAddressPlan(xlQueuePlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 10.0),
                new ResourceAllowance("router", 2.0),
                new ResourceAllowance("aggregate", 12.0));
        List<AddressPlan> addressPlans = Collections.singletonList(xlQueuePlan);
        AddressSpacePlan manyAddressesPlan = PlanUtils.createAddressSpacePlanObject("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = AddressSpaceUtils.createAddressSpaceObject("many-addresses-standard", AddressSpaceType.STANDARD,
                manyAddressesPlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createUser(manyAddressesSpace, cred);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 4;
        int toDeleteCount = 2;
        for (int i = 0; i < destCount; i++) {
            dest.add(AddressUtils.createQueueAddressObject("xl-queue-" + i, xlQueuePlan.getMetadata().getName()));
        }

        setAddresses(manyAddressesSpace, dest.toArray(new Address[0]));
        for (Address destination : dest) {
            waitForBrokerReplicas(manyAddressesSpace, destination, 1);
        }

        assertCanConnect(manyAddressesSpace, cred, dest);

        deleteAddresses(manyAddressesSpace, dest.subList(0, toDeleteCount).toArray(new Address[0]));
        for (Address destination : dest.subList(toDeleteCount, destCount)) {
            waitForBrokerReplicas(manyAddressesSpace, destination, 1);
        }

        assertCanConnect(manyAddressesSpace, cred, dest.subList(toDeleteCount, destCount));
    }

    @Test
    void testMessagePersistenceAfterAutoScale() throws Exception {
        //define and create address plans
        List<ResourceRequest> addressResourcesQueueAlpha = Arrays.asList(new ResourceRequest("broker", 0.3), new ResourceRequest("router", 0));
        List<ResourceRequest> addressResourcesQueueBeta = Arrays.asList(new ResourceRequest("broker", 0.6), new ResourceRequest("router", 0));

        AddressPlan queuePlanAlpha = PlanUtils.createAddressPlanObject("pooled-standard-queue-alpha", AddressType.QUEUE, addressResourcesQueueAlpha);
        plansProvider.createAddressPlan(queuePlanAlpha);
        AddressPlan queuePlanBeta = PlanUtils.createAddressPlanObject("pooled-standard-queue-beta", AddressType.QUEUE, addressResourcesQueueBeta);
        plansProvider.createAddressPlan(queuePlanBeta);


        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 3.0),
                new ResourceAllowance("router", 5.0),
                new ResourceAllowance("aggregate", 5.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlanAlpha, queuePlanBeta);
        AddressSpacePlan scaleSpacePlan = PlanUtils.createAddressSpacePlanObject("scale-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(scaleSpacePlan);

        //create address space plan with new plan
        AddressSpace messagePersistAddressSpace = AddressSpaceUtils.createAddressSpaceObject("persist-messages-space-standard", AddressSpaceType.STANDARD,
                scaleSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(messagePersistAddressSpace);

        //deploy destinations
        Address queue1 = AddressUtils.createQueueAddressObject("queue1-beta", queuePlanBeta.getMetadata().getName());
        Address queue2 = AddressUtils.createQueueAddressObject("queue2-beta", queuePlanBeta.getMetadata().getName());
        Address queue3 = AddressUtils.createQueueAddressObject("queue3-alpha", queuePlanAlpha.getMetadata().getName());
        Address queue4 = AddressUtils.createQueueAddressObject("queue4-alpha", queuePlanAlpha.getMetadata().getName());

        setAddresses(messagePersistAddressSpace, queue1, queue2);
        appendAddresses(messagePersistAddressSpace, queue3, queue4);

        //send 1000 messages to each queue
        UserCredentials user = new UserCredentials("test-scale-user-name", "test_scale_user_pswd");
        createUser(messagePersistAddressSpace, user);

        AmqpClient queueClient = amqpClientFactory.createQueueClient(messagePersistAddressSpace);
        queueClient.getConnectOptions().setCredentials(user);

        List<String> msgs = TestUtils.generateMessages(1000);
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
        deleteAddresses(messagePersistAddressSpace, queue1, queue2);
        TestUtils.waitForNBrokerReplicas(addressApiClient, kubernetes, messagePersistAddressSpace, 1, queue4, new TimeoutBudget(2, TimeUnit.MINUTES));

        //validate count of addresses
        Future<List<String>> addresses = getAddresses(messagePersistAddressSpace, Optional.empty());
        List<String> addressNames = addresses.get(15, TimeUnit.SECONDS);
        assertThat(String.format("Unexpected count of destinations, got following: %s", addressNames),
                addressNames.size(), is(2));

        //receive messages from remaining addresses
        Future<List<Message>> recvResult3 = queueClient.recvMessages(queue3.getSpec().getAddress(), msgs.size());
        Future<List<Message>> recvResult4 = queueClient.recvMessages(queue4.getSpec().getAddress(), msgs.size());
        assertThat("Incorrect count of messages received", recvResult3.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat("Incorrect count of messages received", recvResult4.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    @Test
    @Disabled("test disabled due to issue: #1136")
    void testMessagePersistenceAfterChangePlan() throws Exception {
        List<ResourceRequest> addressResourcesQueueDistributed = Arrays.asList(new ResourceRequest("broker", 2.0), new ResourceRequest("router", 0));
        List<ResourceRequest> addressResourcesSharded = Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 0));

        AddressPlan queuePlanDistributed = PlanUtils.createAddressPlanObject("distributed-standard-queue-alpha", AddressType.QUEUE, addressResourcesQueueDistributed);
        plansProvider.createAddressPlan(queuePlanDistributed);

        AddressPlan queuePlanSharded = PlanUtils.createAddressPlanObject("sharded-standard-queue", AddressType.QUEUE, addressResourcesSharded);
        plansProvider.createAddressPlan(queuePlanSharded);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 5.0),
                new ResourceAllowance("router", 5.0),
                new ResourceAllowance("aggregate", 5.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlanDistributed, queuePlanSharded);
        AddressSpacePlan scaleSpacePlan = PlanUtils.createAddressSpacePlanObject("scale-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(scaleSpacePlan);

        //create address space plan with new plan
        AddressSpace messagePersistAddressSpace = AddressSpaceUtils.createAddressSpaceObject("persist-messages-space-standard", AddressSpaceType.STANDARD,
                scaleSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(messagePersistAddressSpace);

        //deploy destinations
        Address queue = AddressUtils.createQueueAddressObject("distributed-queue", queuePlanDistributed.getMetadata().getName());
        setAddresses(messagePersistAddressSpace, queue);

        //pod should have 2 replicas
        TestUtils.waitForNBrokerReplicas(addressApiClient, kubernetes, messagePersistAddressSpace, 2, queue, new TimeoutBudget(2, TimeUnit.MINUTES));

        //send 100000 messages to queue
        UserCredentials user = new UserCredentials("test-change-plan-user", "test_change_plan_pswd");
        createUser(messagePersistAddressSpace, user);

        AmqpClient queueClient = amqpClientFactory.createQueueClient(messagePersistAddressSpace);
        queueClient.getConnectOptions().setCredentials(user);

        List<String> msgs = TestUtils.generateMessages(100_000);
        Future<Integer> sendResult1 = queueClient.sendMessages(queue.getSpec().getAddress(), msgs);
        assertThat("Incorrect count of messages sent", sendResult1.get(1, TimeUnit.MINUTES), is(msgs.size()));

        //replace original plan in address by another
        plansProvider.replaceAddressPlan(queuePlanSharded);

        assertEquals(getAddressesObjects(
                messagePersistAddressSpace,
                Optional.of(queue.getMetadata().getName())).get(10, TimeUnit.SECONDS).get(0).getSpec().getPlan(),
                queuePlanSharded.getMetadata().getName(),
                "New plan wasn't set correctly");

        //wait until address will be scaled down to 1 pod
        TestUtils.waitForNBrokerReplicas(
                addressApiClient,
                kubernetes,
                messagePersistAddressSpace, 1, queue, new TimeoutBudget(2, TimeUnit.MINUTES));
        //test failed in command above ^, functionality of test code below wasn't verified :) !TODO

        //receive messages
        Future<List<Message>> recvResult = queueClient.recvMessages(queue.getSpec().getAddress(), msgs.size());
        assertThat("Incorrect count of messages received", recvResult.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
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

        plansProvider.createAddressPlan(beforeQueuePlan);
        plansProvider.createAddressPlan(beforeTopicPlan);
        plansProvider.createAddressPlan(afterQueuePlan);
        plansProvider.createAddressPlan(afterTopicPlan);
        plansProvider.createAddressPlan(pooledQueuePlan);

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


        plansProvider.createAddressSpacePlan(beforeAddressSpacePlan);
        plansProvider.createAddressSpacePlan(afterAddressSpacePlan);
        plansProvider.createAddressSpacePlan(pooledAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-sharded-space", AddressSpaceType.STANDARD,
                beforeAddressSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        Address queue = AddressUtils.createQueueAddressObject("test-queue", beforeQueuePlan.getMetadata().getName());
        Address topic = AddressUtils.createTopicAddressObject("test-topic", beforeTopicPlan.getMetadata().getName());

        setAddresses(addressSpace, queue, topic);

        sendDurableMessages(addressSpace, queue, user, 16);

        addressSpace = new DoneableAddressSpace(addressSpace).editSpec().withPlan(afterAddressSpacePlan.getMetadata().getName()).endSpec().done();
        replaceAddressSpace(addressSpace);

        receiveDurableMessages(addressSpace, queue, user, 16);

        Address afterQueue = AddressUtils.createQueueAddressObject("test-queue-2", afterQueuePlan.getMetadata().getName());
        appendAddresses(addressSpace, afterQueue);

        assertCanConnect(addressSpace, user, Arrays.asList(afterQueue, queue, topic));

        addressSpace = new DoneableAddressSpace(addressSpace).editSpec().withPlan(pooledAddressSpacePlan.getMetadata().getName()).endSpec().done();
        replaceAddressSpace(addressSpace);

        Address pooledQueue = AddressUtils.createQueueAddressObject("test-queue-3", pooledQueuePlan.getMetadata().getName());
        appendAddresses(addressSpace, pooledQueue);

        assertCanConnect(addressSpace, user, Arrays.asList(queue, topic, afterQueue, pooledQueue));
    }

    @Test
    void testReplaceAddressSpacePlanBrokered() throws Exception {
        //define and create address plans
        AddressPlan beforeQueuePlan = PlanUtils.createAddressPlanObject("small-queue", AddressType.QUEUE,
                Collections.singletonList(new ResourceRequest("broker", 0.4)));

        AddressPlan afterQueuePlan = PlanUtils.createAddressPlanObject("bigger-queue", AddressType.QUEUE,
                Collections.singletonList(new ResourceRequest("broker", 0.7)));

        plansProvider.createAddressPlan(beforeQueuePlan);
        plansProvider.createAddressPlan(afterQueuePlan);

        //define and create address space plans

        AddressSpacePlan beforeAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("before-update-brokered-plan",
                "default", AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 5.0)),
                Collections.singletonList(beforeQueuePlan));

        AddressSpacePlan afterAddressSpacePlan = PlanUtils.createAddressSpacePlanObject("after-update-standard-plan",
                "default", AddressSpaceType.BROKERED,
                Collections.singletonList(new ResourceAllowance("broker", 5.0)),
                Collections.singletonList(afterQueuePlan));

        plansProvider.createAddressSpacePlan(beforeAddressSpacePlan);
        plansProvider.createAddressSpacePlan(afterAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-sharded-space", AddressSpaceType.BROKERED,
                beforeAddressSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        Address queue = AddressUtils.createQueueAddressObject("test-queue", beforeQueuePlan.getMetadata().getName());

        setAddresses(addressSpace, queue);

        sendDurableMessages(addressSpace, queue, user, 16);

        addressSpace = new DoneableAddressSpace(addressSpace).editSpec().withPlan(afterAddressSpacePlan.getMetadata().getName()).endSpec().done();
        replaceAddressSpace(addressSpace);

        receiveDurableMessages(addressSpace, queue, user, 16);

        Address afterQueue = AddressUtils.createQueueAddressObject("test-queue-2", afterQueuePlan.getMetadata().getName());
        appendAddresses(addressSpace, afterQueue);

        assertCanConnect(addressSpace, user, Arrays.asList(afterQueue, queue));
    }

    @Test
    void testCannotReplaceAddressSpacePlanStandard() throws Exception {
        //define and create address plans
        AddressPlan afterQueuePlan = PlanUtils.createAddressPlanObject("after-small-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 1.0), new ResourceRequest("router", 0)));

        AddressPlan beforeQueuePlan = PlanUtils.createAddressPlanObject("before-large-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 2.0), new ResourceRequest("router", 0)));

        plansProvider.createAddressPlan(beforeQueuePlan);
        plansProvider.createAddressPlan(afterQueuePlan);

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


        plansProvider.createAddressSpacePlan(beforeAddressSpacePlan);
        plansProvider.createAddressSpacePlan(afterAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-sharded-space", AddressSpaceType.STANDARD,
                beforeAddressSpacePlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        List<Address> queues = Arrays.asList(
                AddressUtils.createQueueAddressObject("test-queue-1", beforeQueuePlan.getMetadata().getName()),
                AddressUtils.createQueueAddressObject("test-queue-2", beforeQueuePlan.getMetadata().getName())
        );


        setAddresses(addressSpace, queues.toArray(new Address[0]));
        assertCanConnect(addressSpace, user, queues);

        addressSpace = new DoneableAddressSpace(addressSpace).editSpec().withPlan(afterAddressSpacePlan.getMetadata().getName()).endSpec().done();
        replaceAddressSpace(addressSpace, false);

        JsonObject data = addressApiClient.getAddressSpace(addressSpace.getMetadata().getName());
        assertEquals(beforeAddressSpacePlan.getMetadata().getName(),
                data.getJsonObject("metadata").getJsonObject("annotations").getString("enmasse.io/applied-plan"));
        assertEquals(String.format("Unable to apply plan [%s] to address space %s:%s: quota exceeded for resource broker",
                afterQueuePlan.getMetadata().getName(), environment.namespace(), addressSpace.getMetadata().getName()),
                data.getJsonObject("status").getJsonArray("messages").getString(0));
    }

    @Test
    void testSwitchQueuePlan() throws Exception {
        AddressPlan beforeQueuePlan = PlanUtils.createAddressPlanObject("small-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.2), new ResourceRequest("router", 0.0)));

        AddressPlan afterQueuePlan = PlanUtils.createAddressPlanObject("bigger-queue", AddressType.QUEUE,
                Arrays.asList(new ResourceRequest("broker", 0.8), new ResourceRequest("router", 0.0)));

        plansProvider.createAddressPlan(beforeQueuePlan);
        plansProvider.createAddressPlan(afterQueuePlan);

        AddressSpacePlan addressPlan = PlanUtils.createAddressSpacePlanObject("address-switch-address-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new ResourceAllowance("broker", 5.0),
                        new ResourceAllowance("router", 5.0),
                        new ResourceAllowance("aggregate", 10.0)),
                Arrays.asList(beforeQueuePlan, afterQueuePlan));

        plansProvider.createAddressSpacePlan(addressPlan);

        AddressSpace addressSpace = AddressSpaceUtils.createAddressSpaceObject("test-pooled-space", AddressSpaceType.STANDARD,
                addressPlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(addressSpace);
        UserCredentials cred = new UserCredentials("test-user", "test-password");
        createUser(addressSpace, cred);

        List<Address> queues = IntStream.range(0, 8).boxed().map(i ->
                AddressUtils.createQueueAddressObject("queue-" + i, beforeQueuePlan.getMetadata().getName()))
                .collect(Collectors.toList());
        setAddresses(addressSpace, queues.toArray(new Address[0]));

        assertThat("Failed there are no 2 broker pods", TestUtils.listBrokerPods(kubernetes, addressSpace).size(), is(2));

        for (Address queue : queues) {
            sendDurableMessages(addressSpace, queue, cred, 400);
        }

        Address queueAfter = AddressUtils.createQueueAddressObject("queue-1", afterQueuePlan.getMetadata().getName());
        replaceAddress(addressSpace, queueAfter);

        assertThat("Failed there are no 3 broker pods", TestUtils.listBrokerPods(kubernetes, addressSpace).size(), is(3));

        for (Address queue : queues) {
            receiveDurableMessages(addressSpace, queue, cred, 400);
        }
    }

    //------------------------------------------------------------------------------------------------
    // Help methods
    //------------------------------------------------------------------------------------------------

    private void checkLimits(AddressSpace addressSpace, List<Address> allowedDest, List<Address> notAllowedDest, UserCredentials credentials)
            throws Exception {

        log.info("Try to create {} addresses, and make sure that {} addresses will be not created",
                Arrays.toString(allowedDest.stream().map(address -> address.getMetadata().getName()).toArray(String[]::new)),
                Arrays.toString(notAllowedDest.stream().map(address -> address.getMetadata().getName()).toArray(String[]::new)));

        setAddresses(addressSpace, new TimeoutBudget(10, TimeUnit.MINUTES), allowedDest.toArray(new Address[0]));
        List<Future<List<Address>>> getAddresses = new ArrayList<>();
        for (Address dest : allowedDest) {
            getAddresses.add(getAddressesObjects(addressSpace, Optional.of(dest.getMetadata().getName())));
        }

        for (Future<List<Address>> getAddress : getAddresses) {
            Address address = getAddress.get(20, TimeUnit.SECONDS).get(0);
            log.info("Address {} with plan {} is in phase {}", address.getMetadata().getName(), address.getSpec().getPlan(), address.getStatus().getPhase());
            String assertMessage = String.format("Address from allowed %s is not ready", address.getMetadata().getName());
            assertEquals(Phase.Active, address.getStatus().getPhase(), assertMessage);
        }

        assertCanConnect(addressSpace, credentials, allowedDest);

        getAddresses.clear();
        if (notAllowedDest.size() > 0) {
            try {
                appendAddresses(addressSpace, new TimeoutBudget(30, TimeUnit.SECONDS), notAllowedDest.toArray(new Address[0]));
            } catch (IllegalStateException ex) {
                if (!ex.getMessage().contains("addresses are not matched")) {
                    throw ex;
                }
            }

            for (Address dest : notAllowedDest) {
                getAddresses.add(getAddressesObjects(addressSpace, Optional.of(dest.getMetadata().getName())));
            }

            for (Future<List<Address>> getAddress : getAddresses) {
                Address address = getAddress.get(20, TimeUnit.SECONDS).get(0);
                log.info("Address {} with plan {} is in phase {}", address.getMetadata().getName(), address.getSpec().getPlan(), address.getStatus().getPhase());
                String assertMessage = String.format("Address from notAllowed %s is ready", address.getMetadata().getName());
                assertEquals(Phase.Pending, address.getStatus().getPhase(), assertMessage);
                assertTrue(address.getStatus().getMessages().contains("Quota exceeded"), "No status message is present");
            }
        }

        ConsoleWebPage page = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressApiClient, addressSpace, credentials);
        page.openWebConsolePage();
        page.openAddressesPageWebConsole();

        for (Address dest : allowedDest) {
            AddressWebItem item = (AddressWebItem) selenium.waitUntilItemPresent(25, () -> page.getAddressItem(dest));
            assertNotNull(item, String.format("Address '%s' is not visible in console", dest));
            assertThat("Item is not in state Ready", item.getStatus(), is(AddressStatus.READY));
        }

        for (Address dest : notAllowedDest) {
            AddressWebItem item = (AddressWebItem) selenium.waitUntilItemPresent(25, () -> page.getAddressItem(dest));
            assertNotNull(item, String.format("Address '%s' is not visible in console", dest));
            assertThat("Item is not in state Pending", item.getStatus(), is(AddressStatus.PENDING));
        }

        setAddresses(addressSpace);
    }
}
