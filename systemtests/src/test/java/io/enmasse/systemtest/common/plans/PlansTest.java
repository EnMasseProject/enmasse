/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.common.plans;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.Argument;
import io.enmasse.systemtest.clients.ArgumentMap;
import io.enmasse.systemtest.clients.rhea.RheaClientSender;
import io.enmasse.systemtest.resources.*;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.standard.TopicTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@Category(IsolatedAddressSpace.class)
public class PlansTest extends TestBase {

    private static Logger log = CustomLogger.getLogger();

    @Before
    public void setUp() {
        plansProvider.setUp();
    }

    @After
    public void tearDown() {
        plansProvider.tearDown();
    }

    @Override
    protected String getDefaultPlan(AddressType addressType) {
        return null;
    }

    @Test
    public void testCreateAddressSpacePlan() throws Exception {
        //define and create address plans
        List<AddressResource> addressResourcesQueue = Arrays.asList(new AddressResource("broker", 1.0));
        List<AddressResource> addressResourcesTopic = Arrays.asList(
                new AddressResource("broker", 1.0),
                new AddressResource("router", 1.0));
        AddressPlan weakQueuePlan = new AddressPlan("standard-queue-weak", AddressType.QUEUE, addressResourcesQueue);
        AddressPlan weakTopicPlan = new AddressPlan("standard-topic-weak", AddressType.TOPIC, addressResourcesTopic);

        plansProvider.createAddressPlanConfig(weakQueuePlan);
        plansProvider.createAddressPlanConfig(weakTopicPlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 0.0, 9.0),
                new AddressSpaceResource("router", 1.0, 5.0),
                new AddressSpaceResource("aggregate", 0.0, 10.0));
        List<AddressPlan> addressPlans = Arrays.asList(weakQueuePlan, weakTopicPlan);
        AddressSpacePlan weakSpacePlan = new AddressSpacePlan("weak-plan", "weak",
                "standard-space", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlanConfig(weakSpacePlan);

        //create address space plan with new plan
        AddressSpace weakAddressSpace = new AddressSpace("weak-address-space", AddressSpaceType.STANDARD,
                weakSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(weakAddressSpace);

        //deploy destinations
        Destination weakQueueDest = Destination.queue("weak-queue", weakQueuePlan.getName());
        Destination weakTopicDest = Destination.topic("weak-topic", weakTopicPlan.getName());
        setAddresses(weakAddressSpace, weakQueueDest, weakTopicDest);

        //get destinations
        Future<List<Address>> getWeakQueue = getAddressesObjects(weakAddressSpace, Optional.of(weakQueueDest.getAddress()));
        Future<List<Address>> getWeakTopic = getAddressesObjects(weakAddressSpace, Optional.of(weakTopicDest.getAddress()));

        String assertMessage = "Queue plan wasn't set properly";
        assertEquals(assertMessage, getWeakQueue.get(20, TimeUnit.SECONDS).get(0).getPlan(),
                weakQueuePlan.getName());
        assertEquals(assertMessage, getWeakTopic.get(20, TimeUnit.SECONDS).get(0).getPlan(),
                weakTopicPlan.getName());

        //simple send/receive
        String username = "test_newplan_name";
        String password = "test_newplan_password";
        getKeycloakClient().createUser(weakAddressSpace.getName(), username, password, 20, TimeUnit.SECONDS);

        AmqpClient queueClient = amqpClientFactory.createQueueClient(weakAddressSpace);
        queueClient.getConnectOptions().setUsername(username);
        queueClient.getConnectOptions().setPassword(password);
        QueueTest.runQueueTest(queueClient, weakQueueDest, 42);

        AmqpClient topicClient = amqpClientFactory.createTopicClient(weakAddressSpace);
        topicClient.getConnectOptions().setUsername(username);
        topicClient.getConnectOptions().setPassword(password);
        TopicTest.runTopicTest(topicClient, weakTopicDest, 42);
    }

    @Test
    public void testQuotaLimitsPooled() throws Exception {
        String username = "quota_user";
        String password = "quotaPa55";
        //define and create address plans
        AddressPlan queuePlan = new AddressPlan("queue-pooled-test1", AddressType.QUEUE,
                Collections.singletonList(new AddressResource("broker", 0.6)));

        AddressPlan queuePlan2 = new AddressPlan("queue-pooled-test2", AddressType.QUEUE,
                Collections.singletonList(new AddressResource("broker", 0.1)));

        AddressPlan queuePlan3 = new AddressPlan("queue-pooled-test3", AddressType.QUEUE,
                Collections.singletonList(new AddressResource("broker", 0.05)));

        AddressPlan topicPlan = new AddressPlan("topic-pooled-test1", AddressType.TOPIC,
                Arrays.asList(
                        new AddressResource("broker", 0.4),
                        new AddressResource("router", 0.2)));

        AddressPlan anycastPlan = new AddressPlan("anycast-test1", AddressType.ANYCAST,
                Collections.singletonList(new AddressResource("router", 0.3)));

        plansProvider.createAddressPlanConfig(queuePlan);
        plansProvider.createAddressPlanConfig(queuePlan2);
        plansProvider.createAddressPlanConfig(queuePlan3);
        plansProvider.createAddressPlanConfig(topicPlan);
        plansProvider.createAddressPlanConfig(anycastPlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 0.0, 2.0),
                new AddressSpaceResource("router", 1.0, 1.0),
                new AddressSpaceResource("aggregate", 0.0, 2.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlan, queuePlan2, queuePlan3, topicPlan, anycastPlan);
        AddressSpacePlan addressSpacePlan = new AddressSpacePlan("quota-limits-pooled-plan", "quota-limits-pooled-plan",
                "standard-space", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlanConfig(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("test-quota-limits-pooled", AddressSpaceType.STANDARD,
                addressSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(addressSpace);

        getKeycloakClient().createUser(addressSpace.getName(), username, password, 20, TimeUnit.SECONDS);

        //check router limits
        checkLimits(addressSpace,
                Arrays.asList(
                        Destination.anycast("a1", anycastPlan.getName()),
                        Destination.anycast("a2", anycastPlan.getName()),
                        Destination.anycast("a3", anycastPlan.getName())
                ),
                Collections.singletonList(
                        Destination.anycast("a4", anycastPlan.getName())
                ), username, password);

        //check broker limits
        checkLimits(addressSpace,
                Arrays.asList(
                        Destination.queue("q1", queuePlan.getName()),
                        Destination.queue("q2", queuePlan.getName())
                ),
                Collections.singletonList(
                        Destination.queue("q3", queuePlan.getName())
                ), username, password);

        checkLimits(addressSpace,
                Arrays.asList(
                        Destination.queue("q1", queuePlan.getName()), // 0.6
                        Destination.queue("q2", queuePlan.getName()), // 0.6
                        Destination.queue("q3", queuePlan2.getName()), // 0.1
                        Destination.queue("q4", queuePlan2.getName()), // 0.1
                        Destination.queue("q5", queuePlan2.getName()), // 0.1
                        Destination.queue("q6", queuePlan2.getName()), // 0.1
                        Destination.queue("q7", queuePlan3.getName()), // 0.05
                        Destination.queue("q8", queuePlan3.getName()), // 0.05
                        Destination.queue("q9", queuePlan3.getName()), // 0.05
                        Destination.queue("q10", queuePlan3.getName()), // 0.05
                        Destination.queue("q11", queuePlan3.getName()), // 0.05
                        Destination.queue("q12", queuePlan3.getName()) // 0.05
                ), Collections.emptyList(), username, password);

        //check aggregate limits
        checkLimits(addressSpace,
                Arrays.asList(
                        Destination.topic("t1", topicPlan.getName()),
                        Destination.topic("t2", topicPlan.getName())
                ),
                Collections.singletonList(
                        Destination.topic("t3", topicPlan.getName())
                ), username, password);
    }

    @Test
    public void testQuotaLimitsSharded() throws Exception {
        String username = "quota_user";
        String password = "quotaPa55";
        //define and create address plans
        AddressPlan queuePlan = new AddressPlan("queue-sharded-test1", AddressType.QUEUE,
                Collections.singletonList(new AddressResource("broker", 1.0)));

        AddressPlan topicPlan = new AddressPlan("topic-sharded-test2", AddressType.TOPIC,
                Arrays.asList(
                        new AddressResource("broker", 1.0),
                        new AddressResource("router", 0.01)));

        plansProvider.createAddressPlanConfig(queuePlan);
        plansProvider.createAddressPlanConfig(topicPlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 0.0, 2.0),
                new AddressSpaceResource("router", 1.0, 2.0),
                new AddressSpaceResource("aggregate", 0.0, 3.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlan, topicPlan);
        AddressSpacePlan addressSpacePlan = new AddressSpacePlan("quota-limits-sharded-plan", "quota-limits-sharded-plan",
                "standard-space", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlanConfig(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("test-quota-limits-sharded", AddressSpaceType.STANDARD,
                addressSpacePlan.getName(), AuthService.STANDARD);
        createAddressSpace(addressSpace);

        getKeycloakClient().createUser(addressSpace.getName(), username, password, 20, TimeUnit.SECONDS);

        //check broker limits
        checkLimits(addressSpace,
                Arrays.asList(
                        Destination.queue("q1", queuePlan.getName()),
                        Destination.queue("q2", queuePlan.getName())
                ),
                Collections.singletonList(
                        Destination.queue("q3", queuePlan.getName())
                ), username, password);

        //check aggregate limits
        checkLimits(addressSpace,
                Arrays.asList(
                        Destination.topic("t1", topicPlan.getName()),
                        Destination.topic("t2", topicPlan.getName())
                ),
                Collections.singletonList(
                        Destination.topic("t3", topicPlan.getName())
                ), username, password);
    }

    @Test
    public void testGlobalSizeLimitations() throws Exception {
        String username = "test";
        String password = "test";
        ResourceDefinition limitedResource = new ResourceDefinition(
                "broker",
                "queue-persisted",
                Arrays.asList(
                        new ResourceParameter("GLOBAL_MAX_SIZE", "1Mb")
                ));
        plansProvider.replaceResourceDefinitionConfig(limitedResource);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("global-size-limited-space", AddressSpaceType.STANDARD, AuthService.STANDARD);
        createAddressSpace(addressSpace);
        getKeycloakClient().createUser(addressSpace.getName(), username, password, 20, TimeUnit.SECONDS);


        Destination queue = Destination.queue("test-queue", "sharded-queue");
        setAddresses(addressSpace, queue);

        AbstractClient client = new RheaClientSender();
        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.MSG_CONTENT, String.join("", Collections.nCopies(1024, "F")));
        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, queue.getAddress());
        arguments.put(Argument.COUNT, "1000");
        client.setArguments(arguments);

        assertFalse("Client does not fail", client.run(false));
    }

    //------------------------------------------------------------------------------------------------
    // Help methods
    //------------------------------------------------------------------------------------------------

    private void checkLimits(AddressSpace addressSpace, List<Destination> allowedDest, List<Destination> notAllowedDest, String username, String password)
            throws Exception {

        log.info("Try to create {} addresses, and make sure that {} addresses will be not created",
                Arrays.toString(allowedDest.stream().map(Destination::getName).toArray(String[]::new)),
                Arrays.toString(notAllowedDest.stream().map(Destination::getName).toArray(String[]::new)));

        setAddresses(addressSpace, new TimeoutBudget(10, TimeUnit.MINUTES), allowedDest.toArray(new Destination[0]));
        List<Future<List<Address>>> getAddresses = new ArrayList<>();
        for (Destination dest : allowedDest) {
            getAddresses.add(getAddressesObjects(addressSpace, Optional.of(dest.getAddress())));
        }

        for (Future<List<Address>> getAddress : getAddresses) {
            Address address = getAddress.get(20, TimeUnit.SECONDS).get(0);
            log.info("Address {} with plan {} is in phase {}", address.getName(), address.getPlan(), address.getPhase());
            String assertMessage = String.format("Address from allowed %s is not ready", address.getName());
            assertEquals(assertMessage, "Active", address.getPhase());
        }

        assertCanConnect(addressSpace, username, password, allowedDest);

        getAddresses.clear();
        if (notAllowedDest.size() > 0) {
            try {
                appendAddresses(addressSpace, new TimeoutBudget(30, TimeUnit.SECONDS), notAllowedDest.toArray(new Destination[0]));
            } catch (IllegalStateException ex) {
                if (!ex.getMessage().contains("addresses are not ready")) {
                    throw ex;
                }
            }

            for (Destination dest : notAllowedDest) {
                getAddresses.add(getAddressesObjects(addressSpace, Optional.of(dest.getAddress())));
            }

            for (Future<List<Address>> getAddress : getAddresses) {
                Address address = getAddress.get(20, TimeUnit.SECONDS).get(0);
                log.info("Address {} with plan {} is in phase {}", address.getName(), address.getPlan(), address.getPhase());
                String assertMessage = String.format("Address from notAllowed %s is ready", address.getName());
                assertEquals(assertMessage, "Pending", address.getPhase());
                assertTrue("No status message is present", address.getStatusMessages().contains("Quota exceeded"));
            }
        }

        setAddresses(addressSpace);
    }
}
