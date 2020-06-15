/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.brokered;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedBrokered;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.proton.java.ProtonJMSClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.NON_PR;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(NON_PR)
@Tag(SMOKE)
@Tag(ACCEPTANCE)
@ExternalClients
class SmokeTest extends TestBase implements ITestIsolatedBrokered {

    @Test
    void testAddressTypes() throws Exception {
        int messagesCount = 10;
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("smoke-space-brokered")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        Address queueA = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "brokeredqueuea"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("brokeredqueueq")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        Address topicB = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "brokeredtopicb"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("brokeredtopicb")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(addressSpace);
        resourcesManager.setAddresses(queueA);
        UserCredentials cred = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        Endpoint route = AddressSpaceUtils.getInternalMessagingRoute(addressSpace, "amqps");

        ExternalMessagingClient sender = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientSender())
                .withAddress(queueA)
                .withCredentials(cred)
                .withMessagingRoute(route)
                .withCount(messagesCount);

        ExternalMessagingClient receiver = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientSender())
                .withAddress(queueA)
                .withCredentials(cred)
                .withMessagingRoute(route)
                .withCount(messagesCount);

        assertTrue(sender.run());
        assertTrue(receiver.run());

        resourcesManager.setAddresses(topicB);

        sender.withAddress(topicB);
        receiver.withAddress(topicB);

        Future<Boolean> recResult = receiver.runAsync();
        receiver.getLinkAttachedProbe().get(15000, TimeUnit.MILLISECONDS);

        assertTrue(sender.run(), "Sender failed, expected return code 0");
        assertTrue(recResult.get(), "Receiver failed, expected return code 0");
    }

    @Test
    @Tag(ACCEPTANCE)
    void testCreateDeleteAddressSpace() throws Exception {
        int messagesCount = 10;
        AddressSpace addressSpaceA = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("smoke-space-brokered-a")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        AddressSpace addressSpaceB = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("smoke-space-brokered-b")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        isolatedResourcesManager.createAddressSpaceList(addressSpaceA, addressSpaceB);

        Address queueA = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpaceA.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpaceA, "queue-a"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-a")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        Address queueB = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpaceB.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpaceB, "queue-b"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-b")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(queueA, queueB);
        UserCredentials user = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(addressSpaceA, user);
        resourcesManager.createOrUpdateUser(addressSpaceB, user);

        Endpoint routeAddressSpaceA = AddressSpaceUtils.getInternalMessagingRoute(addressSpaceA, "amqps");
        Endpoint routeAddressSpaceB = AddressSpaceUtils.getInternalMessagingRoute(addressSpaceB, "amqps");

        ExternalMessagingClient sender = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientSender())
                .withAddress(queueA)
                .withCredentials(user)
                .withMessageBody("{'data': '1525154', 'test': '6598959565', 'test2': '989898###RRRRASADS'}")
                .withMessagingRoute(routeAddressSpaceA)
                .withCount(messagesCount);

        ExternalMessagingClient receiver = new ExternalMessagingClient()
                .withClientEngine(new ProtonJMSClientSender())
                .withAddress(queueA)
                .withCredentials(user)
                .withMessagingRoute(routeAddressSpaceA)
                .withCount(messagesCount);

        assertTrue(sender.run());
        assertTrue(receiver.run());


        sender.withMessagingRoute(routeAddressSpaceB).withAddress(queueB);
        receiver.withMessagingRoute(routeAddressSpaceB).withAddress(queueB);

        assertTrue(sender.run());
        assertTrue(receiver.run());

        resourcesManager.deleteAddressSpace(addressSpaceA);

        assertTrue(sender.run());
        assertTrue(receiver.run());
    }
}
