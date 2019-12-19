/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.clients;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestBaseShared;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.mqtt.PahoMQTTClientReceiver;
import io.enmasse.systemtest.messagingclients.mqtt.PahoMQTTClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;

import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExternalClients
public abstract class ClusterClientTestBase extends TestBase implements ITestBaseShared {

    private Endpoint getMessagingRoute(AddressSpace addressSpace, boolean websocket, boolean ssl, boolean mqtt) throws Exception {
        int port = ssl ? 5671 : 5672;
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) && mqtt) {
            port = ssl ? 8883 : 1883;
        }
        return new Endpoint(String.format("%s-%s.%s.svc.cluster.local",
                (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) && mqtt) ? "mqtt" : "messaging",
                AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace),
                environment.namespace()),
                websocket && addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) ? 443 : port);
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        doBasicMessageTest(sender, receiver, false);
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver, boolean websocket) throws Exception {
        int expectedMsgCount = 10;

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "message-basic-" + ClientType.getAddressName(sender) + (websocket ? "-ws" : "")))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("message-basic-" + ClientType.getAddressName(sender) + "-" + (websocket ? "ws" : ""))
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), websocket, true, false))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withTimeout(30)
                .withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET, websocket);

        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withClientEngine(receiver)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), websocket, true, false))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withTimeout(30)
                .withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET, websocket);

        assertTrue(senderClient.run());
        assertTrue(receiverClient.run());

        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    protected void doMqttMessageTest() throws Exception {
        int expectedMsgCount = 10;
        AbstractClient sender = new PahoMQTTClientSender();
        AbstractClient receiver = new PahoMQTTClientReceiver();

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "basic-mqtt" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("basic-mqtt-" + ClientType.getAddressName(sender))
                .withPlan(getSharedAddressSpace().getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) ? DestinationPlan.STANDARD_LARGE_TOPIC : getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();

        resourcesManager.setAddresses(dest);

        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), false, false, false))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withTimeout(30);

        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withClientEngine(receiver)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), false, false, false))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withTimeout(40);

        Future<Boolean> recResult = receiverClient.runAsync();
        Thread.sleep(20_000);

        assertAll(
                () -> assertTrue(senderClient.run(), "Producer failed, expected return code 0"),
                () -> assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                        String.format("Expected %d sent messages", expectedMsgCount)));
        assertAll(
                () -> assertTrue(recResult.get(), "Subscriber failed, expected return code 0"),
                () -> assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                        String.format("Expected %d received messages", expectedMsgCount)));
    }
}
