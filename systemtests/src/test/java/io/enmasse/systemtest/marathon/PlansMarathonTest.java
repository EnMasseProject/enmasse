/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.AddressSpaceResource;
import io.enmasse.systemtest.standard.QueueTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.*;

class PlansMarathonTest extends MarathonTestBase {

    private static Logger log = CustomLogger.getLogger();
    private static final PlansProvider plansProvider = new PlansProvider(kubernetes);

    @BeforeEach
    void setUp() {
        plansProvider.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        logCollector.collectRouterState("planMarathonTearDown");
        logCollector.collectConfigMaps("plansMarathonTearDown");
        if (!environment.skipCleanup()) {
            plansProvider.tearDown();
        }
    }

    @Test
    void testHighLoadAddresses() throws Exception {
        //define and create address plans
        List<AddressResource> addressResourcesQueue = Collections.singletonList(new AddressResource("broker", 0.001));
        AddressPlan xxsQueuePlan = new AddressPlan("pooled-xxs-queue", AddressType.QUEUE, addressResourcesQueue);
        plansProvider.createAddressPlan(xxsQueuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 10.0),
                new AddressSpaceResource("router", 2.0),
                new AddressSpaceResource("aggregate", 12.0));
        List<AddressPlan> addressPlans = Collections.singletonList(xxsQueuePlan);
        AddressSpacePlan manyAddressesPlan = new AddressSpacePlan("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = new AddressSpace("many-addresses-standard", AddressSpaceType.STANDARD,
                manyAddressesPlan.getName(), AuthService.STANDARD);
        createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createUser(manyAddressesSpace, cred);

        ArrayList<Destination> dest = new ArrayList<>();
        int destCount = 3900;
        for (int i = 0; i < destCount; i++) {
            dest.add(Destination.queue("xxs-queue-" + i, xxsQueuePlan.getName()));
        }
        setAddresses(manyAddressesSpace, dest.toArray(new Destination[0]));

        waitForBrokerReplicas(manyAddressesSpace, dest.get(0), 4);

        AmqpClient queueClient = amqpClientFactory.createQueueClient(manyAddressesSpace);
        queueClient.getConnectOptions().setCredentials(cred);
        for (int i = 0; i < destCount; i += 100) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }

        deleteAddresses(manyAddressesSpace, dest.subList(0, destCount / 2).toArray(new Destination[0]));
        waitForBrokerReplicas(manyAddressesSpace, dest.get(0), 2);

        for (int i = destCount / 2; i < destCount; i += 50) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }
        queueClient.close();
    }

    @Test
    void testHighLoadAddressesInBatches() throws Exception {
        //define and create address plans
        List<AddressResource> addressResourcesQueue = Collections.singletonList(new AddressResource("broker", 0.001));
        AddressPlan xxsQueuePlan = new AddressPlan("pooled-xxs-queue", AddressType.QUEUE, addressResourcesQueue);
        plansProvider.createAddressPlan(xxsQueuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 10.0),
                new AddressSpaceResource("router", 2.0),
                new AddressSpaceResource("aggregate", 12.0));
        List<AddressPlan> addressPlans = Collections.singletonList(xxsQueuePlan);
        AddressSpacePlan manyAddressesPlan = new AddressSpacePlan("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = new AddressSpace("many-addresses-standard", AddressSpaceType.STANDARD,
                manyAddressesPlan.getName(), AuthService.STANDARD);
        createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createUser(manyAddressesSpace, cred);

        ArrayList<Destination> dest = new ArrayList<>();
        int destCount = 3900;
        for (int i = 0; i < destCount; i++) {
            dest.add(Destination.queue("xxs-queue-" + i, xxsQueuePlan.getName()));
        }
        setAddresses(manyAddressesSpace);
        appendAddresses(manyAddressesSpace, true, 10, dest.toArray(new Destination[0]));

        waitForBrokerReplicas(manyAddressesSpace, dest.get(0), 6);

        AmqpClient queueClient = amqpClientFactory.createQueueClient(manyAddressesSpace);
        queueClient.getConnectOptions().setCredentials(cred);
        for (int i = 0; i < destCount; i += 100) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }

        deleteAddresses(manyAddressesSpace, dest.subList(0, destCount / 2).toArray(new Destination[0]));
        waitForBrokerReplicas(manyAddressesSpace, dest.get(0), 4);

        for (int i = destCount / 2; i < destCount; i += 50) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }
        queueClient.close();
    }
}
