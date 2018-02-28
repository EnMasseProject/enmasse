/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.plans;

import io.enmasse.systemtest.*;
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

    @Test
    public void testQuotaLimits() throws Exception {
        //define and create address plans
        AddressPlan queuePlan = new AddressPlan("queue-test1", AddressType.QUEUE,
                Collections.singletonList(new AddressResource("broker", 0.6)));

        AddressPlan topicPlan = new AddressPlan("queue-test2", AddressType.TOPIC,
                Arrays.asList(
                        new AddressResource("broker", 0.4),
                        new AddressResource("router", 0.2)));

        AddressPlan anycastPlan = new AddressPlan("anycast-test1", AddressType.ANYCAST,
                Collections.singletonList(new AddressResource("router", 0.3)));

        plansProvider.createAddressPlanConfig(queuePlan);
        plansProvider.createAddressPlanConfig(topicPlan);
        plansProvider.createAddressPlanConfig(anycastPlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 0.0, 2.0),
                new AddressSpaceResource("router", 1.0, 1.0),
                new AddressSpaceResource("aggregate", 0.0, 2.0));
        List<AddressPlan> addressPlans = Arrays.asList(queuePlan, topicPlan, anycastPlan);
        AddressSpacePlan addressSpacePlan = new AddressSpacePlan("test1", "test",
                "standard-space", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlanConfig(addressSpacePlan);

        //create address space with new plan
        AddressSpace addressSpace = new AddressSpace("test1", AddressSpaceType.STANDARD,
                addressSpacePlan.getName());
        createAddressSpace(addressSpace, AuthService.STANDARD.toString());

        //check router limits
        checkLimits(addressSpace,
                Arrays.asList(
                        Destination.anycast("a1", anycastPlan.getName()),
                        Destination.anycast("a2", anycastPlan.getName()),
                        Destination.anycast("a3", anycastPlan.getName())
                ),
                Collections.singletonList(
                        Destination.anycast("a4", anycastPlan.getName())
                ));

        //check broker limits
        checkLimits(addressSpace,
                Arrays.asList(
                        Destination.queue("q1", queuePlan.getName()),
                        Destination.queue("q2", queuePlan.getName())
                ),
                Collections.singletonList(
                        Destination.queue("q3", queuePlan.getName())
                ));

        //check aggregate limits
        checkLimits(addressSpace,
                Arrays.asList(
                        Destination.topic("t1", topicPlan.getName()),
                        Destination.topic("t2", topicPlan.getName())
                ),
                Collections.singletonList(
                        Destination.topic("t3", topicPlan.getName())
                ));
    }

    private void checkLimits(AddressSpace addressSpace, List<Destination> allowedDest, List<Destination> notAllowedDest)
            throws Exception {

        setAddresses(addressSpace, allowedDest.toArray(new Destination[0]));
        List<Future<List<Address>>> getAddresses = new ArrayList<>();
        for (Destination dest : allowedDest) {
            getAddresses.add(getAddressesObjects(addressSpace, Optional.of(dest.getAddress())));
        }

        for (Future<List<Address>> getAddress : getAddresses) {
            List<Address> address = getAddress.get(20, TimeUnit.SECONDS);
            String assertMessage = String.format("Address from notAllowed %s is not ready", address.get(0).getName());
            assertEquals(assertMessage, "Active", address.get(0).getPhase());
        }

        getAddresses.clear();
        try {
            appendAddresses(addressSpace, new TimeoutBudget(2, TimeUnit.MINUTES), notAllowedDest.toArray(new Destination[0]));
        } catch (IllegalStateException ex) {
            if (!ex.getMessage().contains("addresses are not ready") && !ex.getMessage().contains("Unable to find")) {
                throw ex;
            }
        }

        for (Destination dest : notAllowedDest) {
            getAddresses.add(getAddressesObjects(addressSpace, Optional.of(dest.getAddress())));
        }

        for (Future<List<Address>> getAddress : getAddresses) {
            List<Address> address = getAddress.get(20, TimeUnit.SECONDS);
            String assertMessage = String.format("Address from notAllowed %s is ready", address.get(0).getName());
            assertEquals(assertMessage, "Pending", address.get(0).getPhase());
        }

        setAddresses(addressSpace);
    }
}
