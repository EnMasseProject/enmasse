/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.AddressSpaceResource;
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

import static org.junit.Assert.assertEquals;

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
                weakSpacePlan.getName());
        createAddressSpace(weakAddressSpace, AuthService.STANDARD.toString());

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
        TopicTest.runTopicTest(topicClient, weakQueueDest, 42);
    }
}
