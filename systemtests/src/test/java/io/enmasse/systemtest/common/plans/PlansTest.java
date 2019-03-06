/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.plans;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.resources.*;
import io.enmasse.systemtest.selenium.ISeleniumProviderChrome;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.resources.AddressWebItem;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.standard.TopicTest;
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
import static io.enmasse.systemtest.TestTag.nonPR;
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
        InfraConfigDefinition infra = new InfraConfigDefinition("kornys", AddressSpaceType.STANDARD, Arrays.asList(
                new BrokerInfraSpec(Arrays.asList(
                        new InfraResource("memory", "750Mi"),
                        new InfraResource("storage", "2Gi"))),
                new AdminInfraSpec(Collections.singletonList(
                        new InfraResource("memory", "1Gi"))),
                new RouterInfraSpec(Collections.singletonList(
                        new InfraResource("memory", "1Gi")), 300, 1)), environment.enmasseVersion());

        plansProvider.createInfraConfig(infra);

        //define and create address plans
        List<AddressResource> addressResourcesQueue = Arrays.asList(new AddressResource("broker", 1.0), new AddressResource("router", 0.0));
        List<AddressResource> addressResourcesTopic = Arrays.asList(
                new AddressResource("broker", 1.0),
                new AddressResource("router", 1.0));
        AddressPlanDefinition weakQueuePlan = new AddressPlanDefinition("standard-queue-weak", AddressType.QUEUE, addressResourcesQueue);
        AddressPlanDefinition weakTopicPlan = new AddressPlanDefinition("standard-topic-weak", AddressType.TOPIC, addressResourcesTopic);

        plansProvider.createAddressPlan(weakQueuePlan);
        plansProvider.createAddressPlan(weakTopicPlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 9.0),
                new AddressSpaceResource("router", 5.0),
                new AddressSpaceResource("aggregate", 10.0));
        List<AddressPlanDefinition> addressPlans = Arrays.asList(weakQueuePlan, weakTopicPlan);
        AddressSpacePlanDefinition weakSpacePlan = new AddressSpacePlanDefinition("weak-plan",
                infra.getName(), AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(weakSpacePlan);

        //create address space plan with new plan
        AddressSpace weakAddressSpace = new AddressSpace("weak-address-space", AddressSpaceType.STANDARD,
                weakSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(weakAddressSpace);

        //deploy destinations
        Address weakQueueDest = AddressUtils.createQueue("weak-queue", weakQueuePlan.getName());
        Address weakTopicDest = AddressUtils.createTopic("weak-topic", weakTopicPlan.getName());
        setAddresses(weakAddressSpace, weakQueueDest, weakTopicDest);

        //get destinations
        Future<List<Address>> getWeakQueue = getAddressesObjects(weakAddressSpace, Optional.of(weakQueueDest.getMetadata().getName()));
        Future<List<Address>> getWeakTopic = getAddressesObjects(weakAddressSpace, Optional.of(weakTopicDest.getMetadata().getName()));

        String assertMessage = "Queue plan wasn't set properly";
        assertAll("Both destination should contain right addressPlan",
                () -> assertEquals(getWeakQueue.get(20, TimeUnit.SECONDS).get(0).getSpec().getPlan(),
                        weakQueuePlan.getName(), assertMessage),
                () -> assertEquals(getWeakTopic.get(20, TimeUnit.SECONDS).get(0).getSpec().getPlan(),
                        weakTopicPlan.getName(), assertMessage));

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
        AddressPlanDefinition queuePlan = new AddressPlanDefinition("queue-pooled-test1", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 0.6), new AddressResource("router", 0.0)));

        AddressPlanDefinition queuePlan2 = new AddressPlanDefinition("queue-pooled-test2", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 0.1), new AddressResource("router", 0.0)));

        AddressPlanDefinition queuePlan3 = new AddressPlanDefinition("queue-pooled-test3", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 0.049), new AddressResource("router", 0.0)));

        AddressPlanDefinition topicPlan = new AddressPlanDefinition("topic-pooled-test1", AddressType.TOPIC,
                Arrays.asList(
                        new AddressResource("broker", 0.4),
                        new AddressResource("router", 0.2)));

        AddressPlanDefinition anycastPlan = new AddressPlanDefinition("anycast-test1", AddressType.ANYCAST,
                Collections.singletonList(new AddressResource("router", 0.3)));

        plansProvider.createAddressPlan(queuePlan);
        plansProvider.createAddressPlan(queuePlan2);
        plansProvider.createAddressPlan(queuePlan3);
        plansProvider.createAddressPlan(topicPlan);
        plansProvider.createAddressPlan(anycastPlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 2.0),
                new AddressSpaceResource("router", 1.0),
                new AddressSpaceResource("aggregate", 2.0));
        List<AddressPlanDefinition> addressPlans = Arrays.asList(queuePlan, queuePlan2, queuePlan3, topicPlan, anycastPlan);
        AddressSpacePlanDefinition addressSpacePlan = new AddressSpacePlanDefinition("quota-limits-pooled-plan",
                "default-minimal", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("test-pooled-space", AddressSpaceType.STANDARD,
                addressSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(addressSpace);
        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        //check router limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createAnycast("a1", anycastPlan.getName()),
                        AddressUtils.createAnycast("a2", anycastPlan.getName()),
                        AddressUtils.createAnycast("a3", anycastPlan.getName())
                ),
                Collections.singletonList(
                        AddressUtils.createAnycast("a4", anycastPlan.getName())
                ), user);

        //check broker limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createQueue("q1", queuePlan.getName()),
                        AddressUtils.createQueue("q2", queuePlan.getName())
                ),
                Collections.singletonList(
                        AddressUtils.createQueue("q3", queuePlan.getName())
                ), user);

        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createQueue("q1", queuePlan.getName()), // 0.6
                        AddressUtils.createQueue("q2", queuePlan.getName()), // 0.6
                        AddressUtils.createQueue("q3", queuePlan2.getName()), // 0.1
                        AddressUtils.createQueue("q4", queuePlan2.getName()), // 0.1
                        AddressUtils.createQueue("q5", queuePlan2.getName()), // 0.1
                        AddressUtils.createQueue("q6", queuePlan2.getName()), // 0.1
                        AddressUtils.createQueue("q7", queuePlan3.getName()), // 0.049
                        AddressUtils.createQueue("q8", queuePlan3.getName()), // 0.049
                        AddressUtils.createQueue("q9", queuePlan3.getName()), // 0.049
                        AddressUtils.createQueue("q10", queuePlan3.getName()), // 0.049
                        AddressUtils.createQueue("q11", queuePlan3.getName()), // 0.049
                        AddressUtils.createQueue("q12", queuePlan3.getName()) // 0.049
                ), Collections.emptyList(), user);

        //check aggregate limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createTopic("t1", topicPlan.getName()),
                        AddressUtils.createTopic("t2", topicPlan.getName())
                ),
                Collections.singletonList(
                        AddressUtils.createTopic("t3", topicPlan.getName())
                ), user);
    }

    @Test
    void testQuotaLimitsSharded() throws Exception {
        //define and create address plans
        AddressPlanDefinition queuePlan = new AddressPlanDefinition("queue-sharded-test1", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 1.0), new AddressResource("router", 0.0)));

        AddressPlanDefinition topicPlan = new AddressPlanDefinition("topic-sharded-test2", AddressType.TOPIC,
                Arrays.asList(
                        new AddressResource("broker", 1.0),
                        new AddressResource("router", 0.01)));

        plansProvider.createAddressPlan(queuePlan);
        plansProvider.createAddressPlan(topicPlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 2.0),
                new AddressSpaceResource("router", 2.0),
                new AddressSpaceResource("aggregate", 3.0));
        List<AddressPlanDefinition> addressPlans = Arrays.asList(queuePlan, topicPlan);
        AddressSpacePlanDefinition addressSpacePlan = new AddressSpacePlanDefinition("quota-limits-sharded-plan",
                "default-minimal", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("test-sharded-space", AddressSpaceType.STANDARD,
                addressSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(addressSpace);
        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        //check broker limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createQueue("q1", queuePlan.getName()),
                        AddressUtils.createQueue("q2", queuePlan.getName())
                ),
                Collections.singletonList(
                        AddressUtils.createQueue("q3", queuePlan.getName())
                ), user);

        //check aggregate limits
        checkLimits(addressSpace,
                Arrays.asList(
                        AddressUtils.createTopic("t1", topicPlan.getName()),
                        AddressUtils.createTopic("t2", topicPlan.getName())
                ),
                Collections.singletonList(
                        AddressUtils.createTopic("t3", topicPlan.getName())
                ), user);
    }

    @Test
    void testScalePooledBrokers() throws Exception {
        //define and create address plans
        List<AddressResource> addressResourcesQueue = Arrays.asList(new AddressResource("broker", 0.99), new AddressResource("router", 0.0));
        AddressPlanDefinition xlQueuePlan = new AddressPlanDefinition("pooled-xl-queue", AddressType.QUEUE, addressResourcesQueue);
        plansProvider.createAddressPlan(xlQueuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 10.0),
                new AddressSpaceResource("router", 2.0),
                new AddressSpaceResource("aggregate", 12.0));
        List<AddressPlanDefinition> addressPlans = Collections.singletonList(xlQueuePlan);
        AddressSpacePlanDefinition manyAddressesPlan = new AddressSpacePlanDefinition("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = new AddressSpace("many-addresses-standard", AddressSpaceType.STANDARD,
                manyAddressesPlan.getName(), AuthService.STANDARD);
        createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createUser(manyAddressesSpace, cred);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 4;
        int toDeleteCount = 2;
        for (int i = 0; i < destCount; i++) {
            dest.add(AddressUtils.createQueue("xl-queue-" + i, xlQueuePlan.getName()));
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
    @Tag(nonPR)
    @Disabled("test disabled as per-address limit enforcement has been removed")
    void testGlobalSizeLimitations() throws Exception {
        UserCredentials user = new UserCredentials("test", "test");
        String messageContent = String.join("", Collections.nCopies(1024, "F"));

        //redefine global max size for queue
        ResourceDefinition limitedResource = new ResourceDefinition(
                "broker",
                "queue-persisted",
                Collections.singletonList(
                        new ResourceParameter("GLOBAL_MAX_SIZE", "1Mb")
                ));
        //plansProvider.replaceResourceDefinitionConfig(limitedResource);

        //define address plans
        AddressPlanDefinition queuePlan = new AddressPlanDefinition("limited-queue", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 0.1), new AddressResource("router", 0.0))); //should reserve 100Kb

        plansProvider.createAddressPlan(queuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 1.0),
                new AddressSpaceResource("router", 1.0),
                new AddressSpaceResource("aggregate", 2.0));

        AddressSpacePlanDefinition addressSpacePlan = new AddressSpacePlanDefinition(
                "limited-space",
                "default",
                AddressSpaceType.STANDARD,
                resources,
                Collections.singletonList(queuePlan));
        plansProvider.createAddressSpacePlan(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("global-size-limited-space", AddressSpaceType.STANDARD,
                addressSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(addressSpace);
        createUser(addressSpace, user);

        Address queue = AddressUtils.createQueue("test-queue", queuePlan.getName());
        Address queue2 = AddressUtils.createQueue("test-queue2", queuePlan.getName());
        Address queue3 = AddressUtils.createQueue("test-queue3", queuePlan.getName());
        setAddresses(addressSpace, queue, queue2, queue3);

        assertAll(
                () -> assertFalse(sendMessage(addressSpace, new RheaClientSender(), user,
                        queue.getSpec().getAddress(), messageContent, 100, false),
                        "Client does not fail"),
                () -> assertFalse(sendMessage(addressSpace, new RheaClientSender(), user,
                        queue2.getSpec().getAddress(), messageContent, 100, false),
                        "Client does not fail"),
                () -> assertTrue(sendMessage(addressSpace, new RheaClientSender(), user,
                        queue3.getSpec().getAddress(), messageContent, 50, false),
                        "Client fails"));
    }

    @Test
    void testMessagePersistenceAfterAutoScale() throws Exception {
        //define and create address plans
        List<AddressResource> addressResourcesQueueAlpha = Arrays.asList(new AddressResource("broker", 0.3), new AddressResource("router", 0));
        List<AddressResource> addressResourcesQueueBeta = Arrays.asList(new AddressResource("broker", 0.6), new AddressResource("router", 0));

        AddressPlanDefinition queuePlanAlpha = new AddressPlanDefinition("pooled-standard-queue-alpha", AddressType.QUEUE, addressResourcesQueueAlpha);
        plansProvider.createAddressPlan(queuePlanAlpha);
        AddressPlanDefinition queuePlanBeta = new AddressPlanDefinition("pooled-standard-queue-beta", AddressType.QUEUE, addressResourcesQueueBeta);
        plansProvider.createAddressPlan(queuePlanBeta);


        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 3.0),
                new AddressSpaceResource("router", 5.0),
                new AddressSpaceResource("aggregate", 5.0));
        List<AddressPlanDefinition> addressPlans = Arrays.asList(queuePlanAlpha, queuePlanBeta);
        AddressSpacePlanDefinition scaleSpacePlan = new AddressSpacePlanDefinition("scale-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(scaleSpacePlan);

        //create address space plan with new plan
        AddressSpace messagePersistAddressSpace = new AddressSpace("persist-messages-space-standard", AddressSpaceType.STANDARD,
                scaleSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(messagePersistAddressSpace);

        //deploy destinations
        Address queue1 = AddressUtils.createQueue("queue1-beta", queuePlanBeta.getName());
        Address queue2 = AddressUtils.createQueue("queue2-beta", queuePlanBeta.getName());
        Address queue3 = AddressUtils.createQueue("queue3-alpha", queuePlanAlpha.getName());
        Address queue4 = AddressUtils.createQueue("queue4-alpha", queuePlanAlpha.getName());

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
        List<AddressResource> addressResourcesQueueDistributed = Arrays.asList(new AddressResource("broker", 2.0), new AddressResource("router", 0));
        List<AddressResource> addressResourcesSharded = Arrays.asList(new AddressResource("broker", 1.0), new AddressResource("router", 0));

        AddressPlanDefinition queuePlanDistributed = new AddressPlanDefinition("distributed-standard-queue-alpha", AddressType.QUEUE, addressResourcesQueueDistributed);
        plansProvider.createAddressPlan(queuePlanDistributed);

        AddressPlanDefinition queuePlanSharded = new AddressPlanDefinition("sharded-standard-queue", AddressType.QUEUE, addressResourcesSharded);
        plansProvider.createAddressPlan(queuePlanSharded);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 5.0),
                new AddressSpaceResource("router", 5.0),
                new AddressSpaceResource("aggregate", 5.0));
        List<AddressPlanDefinition> addressPlans = Arrays.asList(queuePlanDistributed, queuePlanSharded);
        AddressSpacePlanDefinition scaleSpacePlan = new AddressSpacePlanDefinition("scale-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(scaleSpacePlan);

        //create address space plan with new plan
        AddressSpace messagePersistAddressSpace = new AddressSpace("persist-messages-space-standard", AddressSpaceType.STANDARD,
                scaleSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(messagePersistAddressSpace);

        //deploy destinations
        Address queue = AddressUtils.createQueue("distributed-queue", queuePlanDistributed.getName());
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
                queuePlanSharded.getName(),
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
        AddressPlanDefinition beforeQueuePlan = new AddressPlanDefinition("before-small-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 1.0), new AddressResource("router", 0.0)));

        AddressPlanDefinition beforeTopicPlan = new AddressPlanDefinition("before-small-sharded-topic", AddressType.TOPIC,
                Arrays.asList(
                        new AddressResource("broker", 1.0),
                        new AddressResource("router", 0.01)));

        AddressPlanDefinition afterQueuePlan = new AddressPlanDefinition("after-large-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 1.5), new AddressResource("router", 0.0)));

        AddressPlanDefinition afterTopicPlan = new AddressPlanDefinition("after-large-sharded-topic", AddressType.TOPIC,
                Arrays.asList(
                        new AddressResource("broker", 1.5),
                        new AddressResource("router", 0.01)));

        AddressPlanDefinition pooledQueuePlan = new AddressPlanDefinition("after-pooled-queue", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 0.44), new AddressResource("router", 0.0)));

        plansProvider.createAddressPlan(beforeQueuePlan);
        plansProvider.createAddressPlan(beforeTopicPlan);
        plansProvider.createAddressPlan(afterQueuePlan);
        plansProvider.createAddressPlan(afterTopicPlan);
        plansProvider.createAddressPlan(pooledQueuePlan);

        //define and create address space plans

        AddressSpacePlanDefinition beforeAddressSpacePlan = new AddressSpacePlanDefinition("before-update-standard-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new AddressSpaceResource("broker", 5.0),
                        new AddressSpaceResource("router", 5.0),
                        new AddressSpaceResource("aggregate", 10.0)),
                Arrays.asList(beforeQueuePlan, beforeTopicPlan));

        AddressSpacePlanDefinition afterAddressSpacePlan = new AddressSpacePlanDefinition("after-update-standard-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new AddressSpaceResource("broker", 5.0),
                        new AddressSpaceResource("router", 5.0),
                        new AddressSpaceResource("aggregate", 10.0)),
                Arrays.asList(afterQueuePlan, afterTopicPlan));

        AddressSpacePlanDefinition pooledAddressSpacePlan = new AddressSpacePlanDefinition("after-update-standard-pooled-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new AddressSpaceResource("broker", 10.0),
                        new AddressSpaceResource("router", 10.0),
                        new AddressSpaceResource("aggregate", 10.0)),
                Collections.singletonList(pooledQueuePlan));


        plansProvider.createAddressSpacePlan(beforeAddressSpacePlan);
        plansProvider.createAddressSpacePlan(afterAddressSpacePlan);
        plansProvider.createAddressSpacePlan(pooledAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("test-sharded-space", AddressSpaceType.STANDARD,
                beforeAddressSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        Address queue = AddressUtils.createQueue("test-queue", beforeQueuePlan.getName());
        Address topic = AddressUtils.createTopic("test-topic", beforeTopicPlan.getName());

        setAddresses(addressSpace, queue, topic);

        sendDurableMessages(addressSpace, queue, user, 16);

        addressSpace.setPlan(afterAddressSpacePlan.getName());
        replaceAddressSpace(addressSpace);

        receiveDurableMessages(addressSpace, queue, user, 16);

        Address afterQueue = AddressUtils.createQueue("test-queue-2", afterQueuePlan.getName());
        appendAddresses(addressSpace, afterQueue);

        assertCanConnect(addressSpace, user, Arrays.asList(afterQueue, queue, topic));

        addressSpace.setPlan(pooledAddressSpacePlan.getName());
        replaceAddressSpace(addressSpace);

        Address pooledQueue = AddressUtils.createQueue("test-queue-3", pooledQueuePlan.getName());
        appendAddresses(addressSpace, pooledQueue);

        assertCanConnect(addressSpace, user, Arrays.asList(queue, topic, afterQueue, pooledQueue));
    }

    @Test
    void testReplaceAddressSpacePlanBrokered() throws Exception {
        //define and create address plans
        AddressPlanDefinition beforeQueuePlan = new AddressPlanDefinition("small-queue", AddressType.QUEUE,
                Collections.singletonList(new AddressResource("broker", 0.4)));

        AddressPlanDefinition afterQueuePlan = new AddressPlanDefinition("bigger-queue", AddressType.QUEUE,
                Collections.singletonList(new AddressResource("broker", 0.7)));

        plansProvider.createAddressPlan(beforeQueuePlan);
        plansProvider.createAddressPlan(afterQueuePlan);

        //define and create address space plans

        AddressSpacePlanDefinition beforeAddressSpacePlan = new AddressSpacePlanDefinition("before-update-brokered-plan",
                "default", AddressSpaceType.BROKERED,
                Collections.singletonList(new AddressSpaceResource("broker", 5.0)),
                Collections.singletonList(beforeQueuePlan));

        AddressSpacePlanDefinition afterAddressSpacePlan = new AddressSpacePlanDefinition("after-update-standard-plan",
                "default", AddressSpaceType.BROKERED,
                Collections.singletonList(new AddressSpaceResource("broker", 5.0)),
                Collections.singletonList(afterQueuePlan));

        plansProvider.createAddressSpacePlan(beforeAddressSpacePlan);
        plansProvider.createAddressSpacePlan(afterAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("test-sharded-space", AddressSpaceType.BROKERED,
                beforeAddressSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        Address queue = AddressUtils.createQueue("test-queue", beforeQueuePlan.getName());

        setAddresses(addressSpace, queue);

        sendDurableMessages(addressSpace, queue, user, 16);

        addressSpace.setPlan(afterAddressSpacePlan.getName());
        replaceAddressSpace(addressSpace);

        receiveDurableMessages(addressSpace, queue, user, 16);

        Address afterQueue = AddressUtils.createQueue("test-queue-2", afterQueuePlan.getName());
        appendAddresses(addressSpace, afterQueue);

        assertCanConnect(addressSpace, user, Arrays.asList(afterQueue, queue));
    }

    @Test
    void testCannotReplaceAddressSpacePlanStandard() throws Exception {
        //define and create address plans
        AddressPlanDefinition afterQueuePlan = new AddressPlanDefinition("after-small-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 1.0), new AddressResource("router", 0)));

        AddressPlanDefinition beforeQueuePlan = new AddressPlanDefinition("before-large-sharded-queue", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 2.0), new AddressResource("router", 0)));

        plansProvider.createAddressPlan(beforeQueuePlan);
        plansProvider.createAddressPlan(afterQueuePlan);

        //define and create address space plans

        AddressSpacePlanDefinition beforeAddressSpacePlan = new AddressSpacePlanDefinition("before-update-standard-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new AddressSpaceResource("broker", 5.0),
                        new AddressSpaceResource("router", 2.0),
                        new AddressSpaceResource("aggregate", 7.0)),
                Collections.singletonList(beforeQueuePlan));

        AddressSpacePlanDefinition afterAddressSpacePlan = new AddressSpacePlanDefinition("after-update-standard-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new AddressSpaceResource("broker", 2.0),
                        new AddressSpaceResource("router", 2.0),
                        new AddressSpaceResource("aggregate", 4.0)),
                Collections.singletonList(afterQueuePlan));


        plansProvider.createAddressSpacePlan(beforeAddressSpacePlan);
        plansProvider.createAddressSpacePlan(afterAddressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("test-sharded-space", AddressSpaceType.STANDARD,
                beforeAddressSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("quota-user", "quotaPa55");
        createUser(addressSpace, user);

        List<Address> queues = Arrays.asList(
                AddressUtils.createQueue("test-queue-1", beforeQueuePlan.getName()),
                AddressUtils.createQueue("test-queue-2", beforeQueuePlan.getName())
        );


        setAddresses(addressSpace, queues.toArray(new Address[0]));
        assertCanConnect(addressSpace, user, queues);

        addressSpace.setPlan(afterAddressSpacePlan.getName());
        replaceAddressSpace(addressSpace, false);

        JsonObject data = addressApiClient.getAddressSpace(addressSpace.getName());
        assertEquals(beforeAddressSpacePlan.getName(),
                data.getJsonObject("metadata").getJsonObject("annotations").getString("enmasse.io/applied-plan"));
        assertEquals(String.format("Unable to apply plan [%s] to address space %s:%s: quota exceeded for resource broker",
                afterQueuePlan.getName(), environment.namespace(), addressSpace.getName()),
                data.getJsonObject("status").getJsonArray("messages").getString(0));
    }

    @Test
    void testSwitchQueuePlan() throws Exception {
        AddressPlanDefinition beforeQueuePlan = new AddressPlanDefinition("small-queue", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 0.2), new AddressResource("router", 0.0)));

        AddressPlanDefinition afterQueuePlan = new AddressPlanDefinition("bigger-queue", AddressType.QUEUE,
                Arrays.asList(new AddressResource("broker", 0.8), new AddressResource("router", 0.0)));

        plansProvider.createAddressPlan(beforeQueuePlan);
        plansProvider.createAddressPlan(afterQueuePlan);

        AddressSpacePlanDefinition addressPlan = new AddressSpacePlanDefinition("address-switch-address-plan",
                "default-minimal", AddressSpaceType.STANDARD,
                Arrays.asList(
                        new AddressSpaceResource("broker", 5.0),
                        new AddressSpaceResource("router", 5.0),
                        new AddressSpaceResource("aggregate", 10.0)),
                Arrays.asList(beforeQueuePlan, afterQueuePlan));

        plansProvider.createAddressSpacePlan(addressPlan);

        AddressSpace addressSpace = new AddressSpace("test-pooled-space", AddressSpaceType.STANDARD,
                addressPlan.getName(), AuthService.STANDARD);
        createAddressSpace(addressSpace);
        UserCredentials cred = new UserCredentials("test-user", "test-password");
        createUser(addressSpace, cred);

        List<Address> queues = IntStream.range(0, 8).boxed().map(i ->
                AddressUtils.createQueue("queue-" + i, beforeQueuePlan.getName()))
                .collect(Collectors.toList());
        setAddresses(addressSpace, queues.toArray(new Address[0]));

        assertThat("Failed there are no 2 broker pods", TestUtils.listBrokerPods(kubernetes, addressSpace).size(), is(2));

        for (Address queue : queues) {
            sendDurableMessages(addressSpace, queue, cred, 400);
        }

        Address queueAfter = AddressUtils.createQueue("queue-1", afterQueuePlan.getName());
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
            assertEquals("Active", address.getStatus().getPhase(), assertMessage);
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
                assertEquals("Pending", address.getStatus().getPhase(), assertMessage);
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
