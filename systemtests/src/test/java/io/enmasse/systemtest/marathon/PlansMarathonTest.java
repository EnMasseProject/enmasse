/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.AdminResourcesManager;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class PlansMarathonTest extends MarathonTestBase {

    private static final AdminResourcesManager adminManager = AdminResourcesManager.getInstance();

    @AfterEach
    void tearDown() throws Exception {
        logCollector.collectRouterState("planMarathonTearDown");
        logCollector.collectConfigMaps("plansMarathonTearDown");
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
        AddressSpace manyAddressesSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("many-plan-space")
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

        createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        createOrUpdateUser(manyAddressesSpace, cred);

        ArrayList<Address> dest = new ArrayList<>();
        int destCount = 3900;
        int toDeleteCount = 2000;
        for (int i = 0; i < destCount; i++) {
            dest.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(manyAddressesSpace, "xxs-queue-" + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("xxs-queue-" + i)
                    .withPlan(xxsQueuePlan.getMetadata().getName())
                    .endSpec()
                    .build());
        }
        setAddresses(dest.toArray(new Address[0]));

        for (int i = 0; i < destCount; i += 1000) {
            waitForBrokerReplicas(manyAddressesSpace, dest.get(i), 1);
        }

        AmqpClient queueClient = amqpClientFactory.createQueueClient(manyAddressesSpace);
        queueClient.getConnectOptions().setCredentials(cred);
        for (int i = 0; i < destCount; i += 100) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }

        deleteAddresses(dest.subList(0, toDeleteCount).toArray(new Address[0]));
        for (int i = toDeleteCount; i < destCount; i += 1000) {
            waitForBrokerReplicas(manyAddressesSpace, dest.get(i), 1);
        }

        for (int i = toDeleteCount; i < destCount; i += 50) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }
        queueClient.close();
    }
}
