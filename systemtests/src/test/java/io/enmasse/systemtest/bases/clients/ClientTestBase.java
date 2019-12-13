/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.clients;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestBaseShared;
import io.enmasse.systemtest.broker.ArtemisManagement;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExternalClients
public abstract class ClientTestBase extends TestBase implements ITestBaseShared {
    protected Path logPath = null;
    private List<AbstractClient> clients;

    @BeforeEach
    public void setUpClientBase(TestInfo info) {
        clients = new ArrayList<>();
        String clientFolder = "clients_tests";
        logPath = environment.testLogDir().resolve(
                Paths.get(
                        clientFolder,
                        info.getTestClass().get().getName(),
                        info.getDisplayName()));
    }

    @AfterEach
    public void teardownClient() {
        if (clients != null) {
            clients.forEach(AbstractClient::stop);
            clients.clear();
        }
    }

    private Endpoint getMessagingRoute(AddressSpace addressSpace, boolean websocket) throws Exception {
        if (addressSpace.getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) && websocket) {
            Endpoint messagingEndpoint = AddressSpaceUtils.getEndpointByName(addressSpace, "messaging-wss");
            if (TestUtils.resolvable(messagingEndpoint)) {
                return messagingEndpoint;
            } else {
                return kubernetes.getEndpoint("messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), addressSpace.getMetadata().getNamespace(), "https");
            }
        } else {
            return getMessagingRoute(addressSpace);
        }
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        doBasicMessageTest(sender, receiver, false);
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver, boolean websocket) throws Exception {
        clients.addAll(Arrays.asList(sender, receiver));
        int expectedMsgCount = 10;

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "message-basic" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("message-basic" + ClientType.getAddressName(sender))
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), websocket))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withTimeout(30)
                .withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET, websocket)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "ANYCAST");

        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withClientEngine(receiver)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), websocket))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withTimeout(30)
                .withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET, websocket)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "ANYCAST");


        assertTrue(senderClient.run(), "Sender failed, expected return code 0");
        assertTrue(receiverClient.run(), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    protected void doRoundRobinReceiverTest(ArtemisManagement artemisManagement, AbstractClient sender, AbstractClient receiver, AbstractClient receiver2)
            throws Exception {
        clients.addAll(Arrays.asList(sender, receiver, receiver2));
        int expectedMsgCount = 10;

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "round-robin" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("round-robin" + ClientType.getAddressName(sender))
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withTimeout(30);

        ExternalMessagingClient receiverClient1 = new ExternalMessagingClient()
                .withClientEngine(receiver)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount / 2)
                .withTimeout(150);

        ExternalMessagingClient receiverClient2 = new ExternalMessagingClient()
                .withClientEngine(receiver2)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount / 2)
                .withTimeout(150);

        Future<Boolean> recResult = receiverClient1.runAsync();
        Future<Boolean> rec2Result = receiverClient2.runAsync();

        if (AddressSpaceUtils.isBrokered(getSharedAddressSpace())) {
            waitForSubscribers(artemisManagement, getSharedAddressSpace(), dest.getSpec().getAddress(), 2);
        } else {
            waitForSubscribersConsole(getSharedAddressSpace(), dest, 2);
        }

        assertTrue(senderClient.run(), "Sender failed, expected return code 0");
        assertTrue(recResult.get(), "Receiver failed, expected return code 0");
        assertTrue(rec2Result.get(), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));

        assertAll(
                () -> assertEquals(expectedMsgCount / 2, receiverClient1.getMessages().size(),
                        String.format("Expected %d received messages", expectedMsgCount / 2)),
                () -> assertEquals(expectedMsgCount / 2, receiverClient2.getMessages().size(),
                        String.format("Expected %d sent messages", expectedMsgCount / 2)));
    }

    protected void doTopicSubscribeTest(ArtemisManagement artemisManagement, AbstractClient sender, AbstractClient subscriber, AbstractClient subscriber2,
                                        boolean hasTopicPrefix) throws Exception {
        clients.addAll(Arrays.asList(sender, subscriber, subscriber2));
        int expectedMsgCount = 10;

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "topic-sub" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("topic-sub" + ClientType.getAddressName(sender))
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withTimeout(30)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "MULTICAST");

        ExternalMessagingClient receiverClient1 = new ExternalMessagingClient()
                .withClientEngine(subscriber)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withTimeout(150)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "MULTICAST");

        ExternalMessagingClient receiverClient2 = new ExternalMessagingClient()
                .withClientEngine(subscriber2)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withTimeout(150)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "MULTICAST");

        Future<Boolean> recResult = receiverClient1.runAsync();
        Future<Boolean> recResult2 = receiverClient2.runAsync();

        if (AddressSpaceUtils.isBrokered(getSharedAddressSpace())) {
            waitForSubscribers(artemisManagement, getSharedAddressSpace(), dest.getSpec().getAddress(), 2);
        } else {
            waitForSubscribersConsole(getSharedAddressSpace(), dest, 2);
        }

        assertAll(
                () -> assertTrue(senderClient.run(), "Producer failed, expected return code 0"),
                () -> assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                        String.format("Expected %d sent messages", expectedMsgCount)));
        assertAll(
                () -> assertTrue(recResult.get(), "Subscriber failed, expected return code 0"),
                () -> assertTrue(recResult2.get(), "Subscriber failed, expected return code 0"),
                () -> assertEquals(expectedMsgCount, receiverClient1.getMessages().size(),
                        String.format("Expected %d received messages", expectedMsgCount)),
                () -> assertEquals(expectedMsgCount, receiverClient2.getMessages().size(),
                        String.format("Expected %d received messages", expectedMsgCount)));
    }

    protected void doMessageBrowseTest(AbstractClient sender, AbstractClient receiver_browse, AbstractClient receiver_receive)
            throws Exception {
        clients.addAll(Arrays.asList(sender, receiver_browse, receiver_receive));
        int expectedMsgCount = 10;

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "browse" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("browse" + ClientType.getAddressName(sender))
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d");

        ExternalMessagingClient receiverClientBrowse = new ExternalMessagingClient()
                .withClientEngine(receiver_browse)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withAdditionalArgument(ClientArgument.RECV_BROWSE, "true");

        ExternalMessagingClient receiverClientReceive = new ExternalMessagingClient()
                .withClientEngine(receiver_receive)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withAdditionalArgument(ClientArgument.RECV_BROWSE, "false");

        assertAll(
                () -> assertTrue(senderClient.run(), "Sender failed, expected return code 0"),
                () -> assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                        String.format("Expected %d sent messages", expectedMsgCount)));
        assertAll(
                () -> assertTrue(receiverClientBrowse.run(), "Browse receiver failed, expected return code 0"),
                () -> assertTrue(receiverClientReceive.run(), "Receiver failed, expected return code 0"),
                () -> assertEquals(expectedMsgCount, receiverClientBrowse.getMessages().size(),
                        String.format("Expected %d browsed messages", expectedMsgCount)),
                () -> assertEquals(expectedMsgCount, receiverClientReceive.getMessages().size(),
                        String.format("Expected %d received messages", expectedMsgCount)));
    }

    protected void doDrainQueueTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "drain" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("drain" + ClientType.getAddressName(sender))
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        clients.addAll(Arrays.asList(sender, receiver));
        int expectedMsgCount = 50;

        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d");

        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withClientEngine(receiver)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCount(0)
                .withTimeout(10);

        assertTrue(senderClient.run(), "Sender failed, expected return code 0");
        assertTrue(receiverClient.run(), "Drain receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    protected void doMessageSelectorQueueTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        int expectedMsgCount = 10;

        clients.addAll(Arrays.asList(sender, receiver));
        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "selector-queue" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("selector-queue" + ClientType.getAddressName(sender))
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(queue);

        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withCount(expectedMsgCount)
                .withAddress(queue)
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "colour~red")
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "number~12.65")
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "a~true")
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "b~false")
                .withMessageBody("msg no. %d");

        //send messages
        assertTrue(senderClient.run(), "Sender failed, expected return code 0");
        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));

        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withClientEngine(receiver)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withCount(0)
                .withAddress(queue)
                .withAdditionalArgument(ClientArgument.RECV_BROWSE, "true");

        //receiver with selector colour = red
        receiverClient.withAdditionalArgument(ClientArgument.SELECTOR, "colour = 'red'");
        final Executable executable = () -> assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                String.format("Expected %d received messages 'colour = red'", expectedMsgCount));
        assertAll(
                () -> assertTrue(receiverClient.run(), "Receiver 'colour = red' failed, expected return code 0"),
                executable);

        //receiver with selector number > 12.5
        receiverClient.withAdditionalArgument(ClientArgument.SELECTOR, "number > 12.5");
        assertAll(
                () -> assertTrue(receiverClient.run(), "Receiver 'number > 12.5' failed, expected return code 0"),
                executable);


        //receiver with selector a AND b
        receiverClient.withAdditionalArgument(ClientArgument.SELECTOR, "a AND b");
        assertAll(
                () -> assertTrue(receiverClient.run(), "Receiver 'a AND b' failed, expected return code 0"),
                () -> assertEquals(0, receiverClient.getMessages().size(),
                        String.format("Expected %d received messages 'a AND b'", 0)));

        //receiver with selector a OR b
        receiverClient.withAdditionalArgument(ClientArgument.RECV_BROWSE, "false");
        receiverClient.withAdditionalArgument(ClientArgument.SELECTOR, "a OR b");

        assertAll(
                () -> assertTrue(receiverClient.run(), "Receiver 'a OR b' failed, expected return code 0"),
                () -> assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                        String.format("Expected %d received messages 'a OR b'", expectedMsgCount)));
    }

    protected void doMessageSelectorTopicTest(ArtemisManagement artemisManagement, AbstractClient sender, AbstractClient sender2,
                                              AbstractClient subscriber, AbstractClient subscriber2, boolean hasTopicPrefix) throws Exception {
        clients.addAll(Arrays.asList(sender, sender2, subscriber, subscriber2));
        int expectedMsgCount = 5;

        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "selector-topic" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("selector-topic" + ClientType.getAddressName(sender))
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        resourcesManager.setAddresses(topic);

        //set up senders
        ExternalMessagingClient senderClient1 = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withCount(expectedMsgCount)
                .withAddress(topic)
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "colour~red")
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "number~12.65")
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "a~true")
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "b~false")
                .withTimeout(150)
                .withMessageBody("msg no. %d");

        ExternalMessagingClient senderClient2 = new ExternalMessagingClient()
                .withClientEngine(sender2)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withCount(expectedMsgCount)
                .withAddress(topic)
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "colour~blue")
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "number~11.65")
                .withTimeout(150)
                .withMessageBody("msg no. %d");

        //set up subscriber1
        ExternalMessagingClient receiverClient1 = new ExternalMessagingClient()
                .withClientEngine(subscriber)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withCount(expectedMsgCount)
                .withAddress(topic)
                .withTimeout(150)
                .withAdditionalArgument(ClientArgument.SELECTOR, "colour = 'red' AND a");

        //set up subscriber2
        ExternalMessagingClient receiverClient2 = new ExternalMessagingClient()
                .withClientEngine(subscriber2)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace()))
                .withCount(expectedMsgCount)
                .withAddress(topic)
                .withTimeout(150)
                .withAdditionalArgument(ClientArgument.SELECTOR, "number < 12.5");

        Future<Boolean> result1 = receiverClient1.runAsync();
        Future<Boolean> result2 = receiverClient2.runAsync();

        if (AddressSpaceUtils.isBrokered(getSharedAddressSpace())) {
            waitForSubscribers(artemisManagement, getSharedAddressSpace(), topic.getSpec().getAddress(), 2);
        } else {
            waitForSubscribersConsole(getSharedAddressSpace(), topic, 2);
        }

        assertTrue(senderClient1.run(), "Sender failed, expected return code 0");
        assertTrue(senderClient2.run(), "Sender2 failed, expected return code 0");
        assertTrue(result1.get(), "Receiver 'colour = red' failed, expected return code 0");
        assertTrue(result2.get(), "Receiver 'number < 12.5' failed, expected return code 0");

        assertEquals(expectedMsgCount, senderClient1.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, senderClient2.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiverClient1.getMessages().size(),
                String.format("Expected %d received messages 'colour = red' AND a", expectedMsgCount));
        assertEquals(expectedMsgCount, receiverClient2.getMessages().size(),
                String.format("Expected %d received messages 'number < 12.5'", expectedMsgCount));
    }

    protected void doTestUserPermissions(AbstractClient sender, AbstractClient receiver) throws Exception {
        int expectedMsgCount = 5;
        UserCredentials publishCred = new UserCredentials("publisher", "publish");
        UserCredentials consumCred = new UserCredentials("consumer", "consume");
        createPublisherAndConsumer(publishCred, consumCred);

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "message-basic" + ClientType.getAddressName(sender)))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("message-basic" + ClientType.getAddressName(sender))
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        ExternalMessagingClient senderClient = new ExternalMessagingClient()
                .withClientEngine(sender)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), false))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withTimeout(30)
                .withCredentials(consumCred)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "ANYCAST");

        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), false))
                .withAddress(dest)
                .withCount(expectedMsgCount)
                .withTimeout(30)
                .withCredentials(publishCred)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "ANYCAST");


        assertAll(
                () -> assertFalse(senderClient.run(), "Sender failed. Specified user is not allowed to write"),
                () -> assertFalse(receiverClient.run(), "Receiver failed. Specified user is not allowed to read"));

        senderClient.withCredentials(publishCred);

        receiverClient.withCredentials(consumCred);

        assertTrue(senderClient.run(), "Sender failed, expected return code 0");
        assertTrue(receiverClient.run(), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    private void createPublisherAndConsumer(UserCredentials publishCred, UserCredentials consumCred) throws Exception {
        User publisher = (UserUtils.createUserResource(publishCred)
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.send)
                        .build()))
                .endSpec()
                .done());

        User consumer = (UserUtils.createUserResource(consumCred)
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(Operation.recv)
                        .build()))
                .endSpec()
                .done());

        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), publisher);
        resourcesManager.createOrUpdateUser(getSharedAddressSpace(), consumer);
    }

}
