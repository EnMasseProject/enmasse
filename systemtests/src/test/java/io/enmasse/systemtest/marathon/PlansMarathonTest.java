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
import io.enmasse.systemtest.selenium.ISeleniumProviderFirefox;
import io.enmasse.systemtest.standard.QueueTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class PlansMarathonTest extends MarathonTestBase implements ISeleniumProviderFirefox {

    private static Logger log = CustomLogger.getLogger();
    private static final PlansProvider plansProvider = new PlansProvider(kubernetes);

    @BeforeEach
    void setUp() {
        plansProvider.setUp();
    }

    @AfterEach
    void tearDown() {
        plansProvider.tearDown();
    }

    @Test
    void testHighLoadAddresses() throws Exception {
        //define and create address plans
        List<AddressResource> addressResourcesQueue = Collections.singletonList(new AddressResource("broker", 0.001));
        AddressPlan xxsQueuePlan = new AddressPlan("xxs-queue", AddressType.QUEUE, addressResourcesQueue);
        plansProvider.createAddressPlanConfig(xxsQueuePlan);

        //define and create address space plan
        List<AddressSpaceResource> resources = Arrays.asList(
                new AddressSpaceResource("broker", 0.0, 5.0),
                new AddressSpaceResource("router", 1.0, 1.0),
                new AddressSpaceResource("aggregate", 0.0, 5.0));
        List<AddressPlan> addressPlans = Collections.singletonList(xxsQueuePlan);
        AddressSpacePlan manyAddressesPlan = new AddressSpacePlan("many-brokers-plan", "manybrokeresplan",
                "standard-space", AddressSpaceType.STANDARD, resources, addressPlans);
        plansProvider.createAddressSpacePlanConfig(manyAddressesPlan);

        //create address space plan with new plan
        AddressSpace manyAddressesSpace = new AddressSpace("many-addresses-standard", AddressSpaceType.STANDARD,
                manyAddressesPlan.getName(), AuthService.STANDARD);
        createAddressSpace(manyAddressesSpace);

        ArrayList<Destination> dest = new ArrayList<>();
        int destCount = 4000;
        for (int i = 0; i < destCount; i++) {
            dest.add(Destination.queue("xxs-queue-" + i, xxsQueuePlan.getName()));//broker credit = 0.001 => 4 pods
        }
        setAddresses(manyAddressesSpace, dest.toArray(new Destination[0]));

//        TODO once getAddressPlanConfig() method will be implemented
//        double requiredCredit = getAddressPlanConfig("pooled-queue").getRequiredCreditFromResource("broker");
//        int replicasCount = (int) (destCount * requiredCredit);
//        waitForBrokerReplicas(sharedAddressSpace, dest.get(0), replicasCount);
        waitForBrokerReplicas(manyAddressesSpace, dest.get(0), 4);

        AmqpClient queueClient = amqpClientFactory.createQueueClient();
        for (int i = 0; i < destCount; i += 100) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }

        deleteAddresses(manyAddressesSpace, dest.subList(0, destCount / 2).toArray(new Destination[0])); //broker credit = 0.001 => 2 pods
        waitForBrokerReplicas(manyAddressesSpace, dest.get(0), 2);

        for (int i = destCount / 2; i < destCount; i += 50) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }
        queueClient.close();
    }
}
