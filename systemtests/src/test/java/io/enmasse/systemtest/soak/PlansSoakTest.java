/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.soak;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AddressPlan;
import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.ResourceAllowance;
import io.enmasse.admin.model.v1.ResourceRequest;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.bases.soak.SoakTestBase;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.shared.standard.QueueTest;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.PlanUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class PlansSoakTest extends SoakTestBase implements ITestIsolatedStandard {

    @AfterEach
    void tearDown() throws Exception {
        logCollector.collectRouterState("planMarathonTearDown");
        logCollector.collectConfigMaps("plansMarathonTearDown");
    }

    @Test
    void testThousandAddresses() throws Exception {
        List<ResourceRequest> addressResourceQueue = Arrays.asList(
                new ResourceRequest("broker", 0.001),
                new ResourceRequest("router", 0.00001));
        AddressPlan extraSmallQueuePlan = PlanUtils.createAddressPlanObject("extra-extra-small-queue", AddressType.QUEUE, addressResourceQueue);
        isolatedResourcesManager.createAddressPlan(extraSmallQueuePlan);

        List<ResourceRequest> addressResourceAnycast = Collections.singletonList(
                new ResourceRequest("router", 0.0005));
        AddressPlan extraSmallAnycastPlan = PlanUtils.createAddressPlanObject("extra-extra-small-anycast", AddressType.ANYCAST, addressResourceAnycast);
        isolatedResourcesManager.createAddressPlan(extraSmallAnycastPlan);

        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 10.0),
                new ResourceAllowance("router", 10.0),
                new ResourceAllowance("aggregate", 20.0));
        List<AddressPlan> addressPlans = Arrays.asList(extraSmallQueuePlan, extraSmallAnycastPlan);
        AddressSpacePlan thousandAddressPlan = PlanUtils.createAddressSpacePlanObject("thousand-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        resourcesManager.createAddressSpacePlan(thousandAddressPlan);

        AddressSpace thousandAddressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("thousand-address-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(thousandAddressPlan.getMetadata().getName())
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpace(thousandAddressSpace);

        UserCredentials credentials = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(thousandAddressSpace, credentials);

        ArrayList<Address> addressesQueue = new ArrayList<>();
        int countQueue = 2500;
        for (int i = 0; i < countQueue; i++) {
            addressesQueue.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(thousandAddressSpace, "extra-extra-small-queue-" + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress("extra-extra-small-queue-" + i)
                    .withPlan(extraSmallQueuePlan.getMetadata().getName())
                    .endSpec()
                    .build());
        }
        resourcesManager.setAddresses(new TimeoutBudget(30, TimeUnit.MINUTES), addressesQueue.toArray(new Address[0]));

        ArrayList<Address> addressesAnycast = new ArrayList<>();
        int countAnycast = 7500;
        for (int i = 0; i < countAnycast; i++) {
            addressesAnycast.add(new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(kubernetes.getInfraNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(thousandAddressSpace, "extra-extra-small-anycast-" + i))
                    .endMetadata()
                    .withNewSpec()
                    .withType("anycast")
                    .withAddress("extra-extra-small-anycast-" + i)
                    .withPlan(extraSmallAnycastPlan.getMetadata().getName())
                    .endSpec()
                    .build());
        }
        resourcesManager.appendAddresses(new TimeoutBudget(30, TimeUnit.MINUTES), addressesAnycast.toArray(new Address[0]));

        AmqpClient queueClient = getAmqpClientFactory().createQueueClient(thousandAddressSpace);
        queueClient.getConnectOptions().setCredentials(credentials);
        for (int i = 0; i < countQueue; i += 100) {
            QueueTest.runQueueTest(queueClient, addressesQueue.get(i), 42);
        }
        queueClient.close();

        for (int i = 0; i <countAnycast; i+= 50) {
            getClientUtils().assertCanConnect(thousandAddressSpace, credentials, Collections.singletonList(addressesAnycast.get(i)), resourcesManager);
        }


    }

    @Test
    void testHighLoadAddresses() throws Exception {
        //define and create address plans
        List<ResourceRequest> addressResourcesQueue = Arrays.asList(new ResourceRequest("broker", 0.001), new ResourceRequest("router", 0.0));
        AddressPlan xxsQueuePlan = PlanUtils.createAddressPlanObject("pooled-xxs-queue", AddressType.QUEUE, addressResourcesQueue);
        isolatedResourcesManager.createAddressPlan(xxsQueuePlan);

        //define and create address space plan
        List<ResourceAllowance> resources = Arrays.asList(
                new ResourceAllowance("broker", 10.0),
                new ResourceAllowance("router", 2.0),
                new ResourceAllowance("aggregate", 12.0));
        List<AddressPlan> addressPlans = Collections.singletonList(xxsQueuePlan);
        AddressSpacePlan manyAddressesPlan = PlanUtils.createAddressSpacePlanObject("many-brokers-plan",
                "default", AddressSpaceType.STANDARD, resources, addressPlans);
        resourcesManager.createAddressSpacePlan(manyAddressesPlan);

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

        isolatedResourcesManager.createAddressSpace(manyAddressesSpace);

        UserCredentials cred = new UserCredentials("testus", "papyrus");
        resourcesManager.createOrUpdateUser(manyAddressesSpace, cred);

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
        resourcesManager.setAddresses(dest.toArray(new Address[0]));

        for (int i = 0; i < destCount; i += 1000) {
            waitForBrokerReplicas(manyAddressesSpace, dest.get(i), 1);
        }

        AmqpClient queueClient = getAmqpClientFactory().createQueueClient(manyAddressesSpace);
        queueClient.getConnectOptions().setCredentials(cred);
        for (int i = 0; i < destCount; i += 100) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }

        isolatedResourcesManager.deleteAddresses(dest.subList(0, toDeleteCount).toArray(new Address[0]));
        for (int i = toDeleteCount; i < destCount; i += 1000) {
            waitForBrokerReplicas(manyAddressesSpace, dest.get(i), 1);
        }

        for (int i = toDeleteCount; i < destCount; i += 50) {
            QueueTest.runQueueTest(queueClient, dest.get(i), 42);
        }
        queueClient.close();
    }
}
