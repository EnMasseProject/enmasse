/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.isolated.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.annotations.ExternalClients;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.isolated.ITestIsolatedStandard;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.ACCEPTANCE;
import static io.enmasse.systemtest.TestTag.NON_PR;
import static io.enmasse.systemtest.TestTag.SMOKE;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This is a simple smoketest of EnMasse. If this passes, the chances of something being
 * very wrong is minimized. The test should not take to long too execute
 */
@Tag(NON_PR)
@Tag(SMOKE)
@Tag(ACCEPTANCE)
@ExternalClients
class SmokeTest extends TestBase implements ITestIsolatedStandard {

    private Address queue;
    private Address topic;
    private Address anycast;
    private Address multicast;
    private AddressSpace addressSpace;
    private UserCredentials cred;
    private Endpoint route;

    @Test
    void smoketest() throws Exception {
        addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("smoke-space-standard")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_UNLIMITED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "smoke-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("smoke_queue1")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "smoketopic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("smoketopic")
                .withPlan(DestinationPlan.STANDARD_SMALL_TOPIC)
                .endSpec()
                .build();
        anycast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-anycast"))
                .endMetadata()
                .withNewSpec()
                .withType("anycast")
                .withAddress("test-anycast")
                .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                .endSpec()
                .build();
        multicast = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-multicast"))
                .endMetadata()
                .withNewSpec()
                .withType("multicast")
                .withAddress("test-multicast")
                .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(addressSpace);
        resourcesManager.setAddresses(queue, topic, anycast, multicast);
        Thread.sleep(60_000);

        route = AddressSpaceUtils.getInternalMessagingRoute(addressSpace, "amqps");

        cred = new UserCredentials("test", "test");
        resourcesManager.createOrUpdateUser(addressSpace, cred);

        testAnycast();
        testQueue();
        testMulticast();
        testTopic();
    }

    private void testQueue() throws Exception {
        testSmokeMessaging(queue, 1);
    }

    private void testTopic() throws Exception {
        testSmokeMessaging(topic, 3);
    }

    private void testAnycast() throws Exception {
        testSmokeMessaging(anycast, 1);
    }

    private void testMulticast() throws Exception {
        testSmokeMessaging(multicast, 3);
    }

    private void testSmokeMessaging(Address address, int receiverCount) throws Exception {
        ExternalMessagingClient sender = new ExternalMessagingClient()
                .withClientEngine(new RheaClientSender())
                .withAddress(address)
                .withCredentials(cred)
                .withMessagingRoute(route)
                .withMessageBody("{'data': '1525154', 'test': '6598959565', 'test2': '989898###RRRRASADS'}")
                .withAdditionalArgument(ClientArgument.LINK_AT_LEAST_ONCE, "true")
                .withCount(10);

        List<ExternalMessagingClient> receivers = new LinkedList<>();
        List<Future<Boolean>> recvResults = new LinkedList<>();
        for (int i = 0; i < receiverCount; i++) {
            receivers.add(new ExternalMessagingClient()
                    .withClientEngine(new RheaClientReceiver())
                    .withAddress(address)
                    .withCredentials(cred)
                    .withMessagingRoute(route)
                    .withTimeout(120_000)
                    .withAdditionalArgument(
                            address.getSpec().getType().equals(AddressType.MULTICAST.toString().toLowerCase()) ? ClientArgument.LINK_AT_MOST_ONCE : ClientArgument.LINK_AT_LEAST_ONCE, "true")
                    .withCount(10));
        }

        receivers.forEach(receiver -> recvResults.add(receiver.runAsync()));
        for (ExternalMessagingClient rcv : receivers) {
            rcv.getLinkAttachedProbe().get(15000, TimeUnit.MILLISECONDS);
        }
        Thread.sleep(10_000);

        assertTrue(sender.run(), "Sender failed, expected return code 0");
        for (Future<Boolean> rcvr : recvResults) {
            assertTrue(rcvr.get(), "Receiver failed, expected return code 0");
        }
    }
}
