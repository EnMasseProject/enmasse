/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.resources.AddressPlanDefinition;
import io.enmasse.systemtest.resources.AddressResource;
import io.enmasse.systemtest.resources.AddressSpacePlanDefinition;
import io.enmasse.systemtest.resources.AddressSpaceResource;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        List<AddressResource> addressResourcesQueue = Arrays.asList(new AddressResource("broker", 0.001), new AddressResource("router", 0.0));
        AddressPlanDefinition xxsQueuePlan = new AddressPlanDefinition("pooled-xxs-queue", AddressType.QUEUE, addressResourcesQueue);
        plansProvider.createAddressPlan(xxsQueuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 10.0),
                new AddressSpaceResource("router", 2.0),
                new AddressSpaceResource("aggregate", 12.0));
        List<AddressPlanDefinition> addressPlans = Collections.singletonList(xxsQueuePlan);
        AddressSpacePlanDefinition manyAddressesPlan = new AddressSpacePlanDefinition("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = AddressSpaceUtils.createAddressSpaceObject("many-addresses-standard", AddressSpaceType.STANDARD,
                manyAddressesPlan.getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createUser(manyAddressesSpace, cred);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 3900;
        int toDeleteCount = 2000;
        for (int i = 0; i < destCount; i++) {
            dest.add(AddressUtils.createQueueAddressObject("xxs-queue-" + i, xxsQueuePlan.getName()));
        }
        setAddresses(manyAddressesSpace, dest.toArray(new Address[0]));

        for (int i = 0; i < destCount; i += 1000) {
            waitForBrokerReplicas(manyAddressesSpace, dest.get(i), 1);
        }

        AmqpClient queueClient = amqpClientFactory.createQueueClient(manyAddressesSpace);
        queueClient.getConnectOptions().setCredentials(cred);
        for (int i = 0; i < destCount; i += 100) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }

        deleteAddresses(manyAddressesSpace, dest.subList(0, toDeleteCount).toArray(new Address[0]));
        for (int i = toDeleteCount; i < destCount; i += 1000) {
            waitForBrokerReplicas(manyAddressesSpace, dest.get(i), 1);
        }

        for (int i = toDeleteCount; i < destCount; i += 50) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }
        queueClient.close();
    }

    @Test
    void testHighLoadAddressesInBatches() throws Exception {
        //define and create address plans
        List<AddressResource> addressResourcesQueue = Arrays.asList(new AddressResource("broker", 0.001), new AddressResource("router", 0.0));
        AddressPlanDefinition xxsQueuePlan = new AddressPlanDefinition("pooled-xxs-queue", AddressType.QUEUE, addressResourcesQueue);
        plansProvider.createAddressPlan(xxsQueuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 10.0),
                new AddressSpaceResource("router", 2.0),
                new AddressSpaceResource("aggregate", 12.0));
        List<AddressPlanDefinition> addressPlans = Collections.singletonList(xxsQueuePlan);
        AddressSpacePlanDefinition manyAddressesPlan = new AddressSpacePlanDefinition("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlan(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = AddressSpaceUtils.createAddressSpaceObject("many-addresses-standard", AddressSpaceType.STANDARD,
                manyAddressesPlan.getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createUser(manyAddressesSpace, cred);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 3900;
        int toDeleteCount = 2000;
        for (int i = 0; i < destCount; i++) {
            dest.add(AddressUtils.createQueueAddressObject("xxs-queue-" + i, xxsQueuePlan.getName()));
        }

        appendAddresses(manyAddressesSpace, true, 10, dest.toArray(new Address[0]));

        for (int i = 0; i < destCount; i += 1000) {
            waitForBrokerReplicas(manyAddressesSpace, dest.get(i), 1);
        }

        AmqpClient queueClient = amqpClientFactory.createQueueClient(manyAddressesSpace);
        queueClient.getConnectOptions().setCredentials(cred);
        for (int i = 0; i < destCount; i += 100) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }

        deleteAddresses(manyAddressesSpace, dest.subList(0, toDeleteCount).toArray(new Address[0]));
        for (int i = toDeleteCount; i < destCount; i += 1000) {
            waitForBrokerReplicas(manyAddressesSpace, dest.get(i), 1);
        }

        for (int i = toDeleteCount; i < destCount; i += 50) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }
        queueClient.close();
    }
}
