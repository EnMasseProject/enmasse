/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class PlansMarathonTest extends MarathonTestBase {

    private static final AdminResourcesManager adminManager = new AdminResourcesManager(kubernetes);

    @BeforeEach
    void setUp() {
        adminManager.setUp();
    }

    @AfterEach
    void tearDown() throws Exception {
        logCollector.collectRouterState("planMarathonTearDown");
        logCollector.collectConfigMaps("plansMarathonTearDown");
        if (!environment.skipCleanup()) {
            adminManager.tearDown();
        }
    }

    @Test
    void testHighLoadAddresses() throws Exception {
        //define and create address plans
        List<ResourceRequest> addressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 0.001), new ResourceRequest("router", 0.0));
        AddressPlan xxsQueuePlan = PlanUtils.createAddressPlanObject("pooled-xxs-queue", AddressType.QUEUE, addressResourcesQueue);
        adminManager.createAddressPlan(xxsQueuePlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 10.0),
                new ResourceAllowance("router", 2.0),
                new ResourceAllowance("aggregate", 12.0));
        List<AddressPlan> addressPlans = Collections.singletonList(xxsQueuePlan);
        AddressSpacePlan manyAddressesPlan = PlanUtils.createAddressSpacePlanObject("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        adminManager.createAddressSpacePlan(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = AddressSpaceUtils.createAddressSpaceObject("many-addresses-standard", AddressSpaceType.STANDARD,
                manyAddressesPlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createUser(manyAddressesSpace, cred);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 3900;
        int toDeleteCount = 2000;
        for (int i = 0; i < destCount; i++) {
            dest.add(AddressUtils.createQueueAddressObject("xxs-queue-" + i, xxsQueuePlan.getMetadata().getName()));
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
        List<ResourceRequest> addressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 0.001), new ResourceRequest("router", 0.0));
        AddressPlan xxsQueuePlan = PlanUtils.createAddressPlanObject("pooled-xxs-queue", AddressType.QUEUE, addressResourcesQueue);
        adminManager.createAddressPlan(xxsQueuePlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 10.0),
                new ResourceAllowance("router", 2.0),
                new ResourceAllowance("aggregate", 12.0));
        List<AddressPlan> addressPlans = Collections.singletonList(xxsQueuePlan);
        AddressSpacePlan manyAddressesPlan = PlanUtils.createAddressSpacePlanObject("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        adminManager.createAddressSpacePlan(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = AddressSpaceUtils.createAddressSpaceObject("many-addresses-standard", AddressSpaceType.STANDARD,
                manyAddressesPlan.getMetadata().getName(), AuthenticationServiceType.STANDARD);
        createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createUser(manyAddressesSpace, cred);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 3900;
        int toDeleteCount = 2000;
        for (int i = 0; i < destCount; i++) {
            dest.add(AddressUtils.createQueueAddressObject("xxs-queue-" + i, xxsQueuePlan.getMetadata().getName()));
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
