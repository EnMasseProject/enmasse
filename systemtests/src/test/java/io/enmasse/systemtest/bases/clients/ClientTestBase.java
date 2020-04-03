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
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientType;
import io.enmasse.systemtest.messagingclients.ExternalClients;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClientRun;
import io.enmasse.systemtest.messagingclients.ReceiverTester;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.ThrowingSupplier;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExternalClients
public abstract class ClientTestBase extends TestBase implements ITestBaseShared {
    Logger LOGGER = CustomLogger.getLogger();
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

    protected abstract AbstractClient senderFactory() throws Exception;

    protected abstract AbstractClient receiverFactory() throws Exception;

    private Endpoint getMessagingRoute(AddressSpace addressSpace, boolean websocket) throws Exception {
        return websocket ? AddressSpaceUtils.getMessagingWssRoute(addressSpace) : AddressSpaceUtils.getMessagingRoute(addressSpace);
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
                .withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS, getSharedAddressSpace().getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) ? "binary" : "")
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "ANYCAST");

        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withClientEngine(receiver)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), websocket))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withTimeout(30)
                .withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET, websocket)
                .withAdditionalArgument(ClientArgument.CONN_WEB_SOCKET_PROTOCOLS, getSharedAddressSpace().getSpec().getType().equals(AddressSpaceType.STANDARD.toString()) ? "binary" : "")
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "ANYCAST");


        assertTrue(senderClient.run(), "Sender failed, expected return code 0");
        assertTrue(receiverClient.run(), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiverClient.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    protected void doRoundRobinReceiverTest() throws Exception {
        var sender = getSender();
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
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withTimeout(30);

        Supplier<ReceiverTester> receiverTesterSupplier = () -> new ReceiverTester(expectedMsgCount / 2,
                () -> new ExternalMessagingClient()
                .withClientEngine(getSender())
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withTimeout(30));

        var receivers = new ArrayList<ExternalMessagingClientRun>();
        {
            var receiverTester = receiverTesterSupplier.get();
            ExternalMessagingClient receiverClient1 = new ExternalMessagingClient()
                    .withClientEngine(getReceiver())
                    .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                    .withAddress(dest)
                    .withCredentials(defaultCredentials)
                    .withStreamSubscriber(receiverTester);
            receivers.add(ExternalMessagingClientRun.of(receiverClient1, receiverTester));
        }

        {
            var receiverTester = receiverTesterSupplier.get();
            ExternalMessagingClient receiverClient2 = new ExternalMessagingClient()
                    .withClientEngine(getReceiver())
                    .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                    .withAddress(dest)
                    .withCredentials(defaultCredentials)
                    .withStreamSubscriber(receiverTester);
            receivers.add(ExternalMessagingClientRun.of(receiverClient2, receiverTester));
        }

        receivers.forEach(receiver -> receiver.runAsync());

        for (var receiver : receivers) {
            receiver.getReceiverTester().waitForReceiverAttached();
        }

        LOGGER.info("Starting test, running sender");
        LOGGER.info("Waiting for sender to finish");
        assertTrue(senderClient.run(), "Sender failed, expected return code 0");
        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        senderClient.stop();

        LOGGER.info("Waiting for receivers to finish");

        for (var receiver : receivers) {
            assertTrue(receiver.getReceiverTester().getExpectedMessagesResult().get(200, TimeUnit.SECONDS),
                    String.format("Subscriber didn't receive expected messages in time, received %d expected %d", receiver.getReceiverTester().getReceivedMessages(), expectedMsgCount / 2));
        }

        for (var receiver : receivers) {
            if (receiver.getResult().isDone()) {
                assertFalse(receiver.getResult().get(), "Subscriber had return code 1, which is unexpected at this point");
                Assertions.fail("Subscriber failed");
            }
        }

        LOGGER.info("Stopping receivers");

        for (var receiver : receivers) {
            receiver.getClient().stop();
            receiver.getResult().get(10, TimeUnit.SECONDS);
            receiver.getClient().gatherResults();
        }

        LOGGER.info("Verifiying receivers results");

        receivers.forEach(receiver -> {
            int totalMessages = (expectedMsgCount / 2) + receiver.getReceiverTester().getTestMessagesReceived();
            assertEquals(totalMessages, receiver.getClient().getMessages().size(),
                    String.format("Expected %d total received messages", totalMessages));
            assertEquals(expectedMsgCount / 2, receiver.getReceiverTester().getReceivedMessages(),
                    String.format("Expected %d received messages", expectedMsgCount / 2));
        });

    }

    protected void doTopicSubscribeTest() throws Exception {
        var sender = getSender();
        var subscribers = getReceivers(2);
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
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withTimeout(30)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "MULTICAST");

        Supplier<ReceiverTester> receiverTesterSupplier = () -> new ReceiverTester(expectedMsgCount,
                () -> new ExternalMessagingClient()
                .withClientEngine(getSender())
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withTimeout(30)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "MULTICAST"));

        var receivers = new ArrayList<ExternalMessagingClientRun>();
        for (var subscriber : subscribers) {
            var receiverTester = receiverTesterSupplier.get();
            var receiverClient = new ExternalMessagingClient()
                    .withClientEngine(subscriber)
                    .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                    .withAddress(dest)
                    .withCredentials(defaultCredentials)
                    .withAdditionalArgument(ClientArgument.DEST_TYPE, "MULTICAST")
                    .withStreamSubscriber(receiverTester);
            receivers.add(ExternalMessagingClientRun.of(receiverClient, receiverTester));
        }

        receivers.forEach(receiver -> receiver.runAsync());

        LOGGER.info("Waiting for receivers to attach");
        for (var receiver : receivers) {
            receiver.getReceiverTester().waitForReceiverAttached();
        }

        LOGGER.info("Starting test, running sender");
        LOGGER.info("Waiting for sender to finish");
        assertTrue(senderClient.run(), "Producer failed, expected return code 0");
        assertEquals(expectedMsgCount, senderClient.getMessages().size(),
                        String.format("Expected %d sent messages", expectedMsgCount));
        senderClient.stop();

        LOGGER.info("Waiting for receivers to finish");

        for (var receiver : receivers) {
            assertTrue(receiver.getReceiverTester().getExpectedMessagesResult().get(30, TimeUnit.SECONDS),
                    String.format("Subscriber didn't receive expected messages in time, received %d expected %d", receiver.getReceiverTester().getReceivedMessages(), expectedMsgCount));
        }

        for (var receiver : receivers) {
            if (receiver.getResult().isDone()) {
                assertFalse(receiver.getResult().get(), "Subscriber had return code 1, which is unexpected at this point");
                Assertions.fail("Subscriber failed");
            }
        }

        LOGGER.info("Stopping receivers");

        for (var receiver : receivers) {
            receiver.getClient().stop();
            receiver.getResult().get(10, TimeUnit.SECONDS);
            receiver.getClient().gatherResults();
        }

        LOGGER.info("Verifiying receivers results");

        receivers.forEach(receiver -> {
            int totalMessages = expectedMsgCount + receiver.getReceiverTester().getTestMessagesReceived();
            assertEquals(totalMessages, receiver.getClient().getMessages().size(),
                    String.format("Expected %d total received messages", totalMessages));
            assertEquals(expectedMsgCount, receiver.getReceiverTester().getReceivedMessages(),
                    String.format("Expected %d received messages", expectedMsgCount));
        });

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
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d");

        ExternalMessagingClient receiverClientBrowse = new ExternalMessagingClient()
                .withClientEngine(receiver_browse)
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withAdditionalArgument(ClientArgument.RECV_BROWSE, "true");

        ExternalMessagingClient receiverClientReceive = new ExternalMessagingClient()
                .withClientEngine(receiver_receive)
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
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
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d");

        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withClientEngine(receiver)
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(dest)
                .withCredentials(defaultCredentials)
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
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withCount(expectedMsgCount)
                .withAddress(queue)
                .withCredentials(defaultCredentials)
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
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withCount(0)
                .withCredentials(defaultCredentials)
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

    protected void doMessageSelectorTopicTest() throws Exception {
        var sender1 = getSender();
        int expectedMsgCount = 5;

        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "selector-topic" + ClientType.getAddressName(sender1)))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("selector-topic" + ClientType.getAddressName(sender1))
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();
        resourcesManager.setAddresses(topic);

        //set up senders
        var senders = new ArrayList<ExternalMessagingClientRun>();
        {
            ExternalMessagingClient senderClient1 = new ExternalMessagingClient()
                    .withClientEngine(sender1)
                    .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                    .withCount(expectedMsgCount)
                    .withAddress(topic)
                    .withCredentials(defaultCredentials)
                    .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "colour~red")
                    .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "number~12.65")
                    .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "a~true")
                    .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "b~false")
                    .withTimeout(250)
                    .withMessageBody("msg no. %d");
            senders.add(ExternalMessagingClientRun.of(senderClient1));
        }

        {
            ExternalMessagingClient senderClient2 = new ExternalMessagingClient()
                    .withClientEngine(getSender())
                    .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                    .withCount(expectedMsgCount)
                    .withAddress(topic)
                    .withCredentials(defaultCredentials)
                    .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "colour~blue")
                    .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "number~11.65")
                    .withTimeout(250)
                    .withMessageBody("msg no. %d");
            senders.add(ExternalMessagingClientRun.of(senderClient2));
        }

        Supplier<ReceiverTester> receiverTesterSupplier = () -> new ReceiverTester(expectedMsgCount,
                () -> new ExternalMessagingClient()
                .withClientEngine(getSender())
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                .withAddress(topic)
                .withCredentials(defaultCredentials)
                .withTimeout(30)
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "colour~red")
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "a~true")
                .withAdditionalArgument(ClientArgument.MSG_PROPERTY, "number~11.65"));

        var receivers = new ArrayList<ExternalMessagingClientRun>();

        //set up subscriber1
        {
            var receiverTester1 = receiverTesterSupplier.get();
            ExternalMessagingClient receiverClient1 = new ExternalMessagingClient()
                    .withClientEngine(getReceiver())
                    .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                    .withCredentials(defaultCredentials)
                    .withAddress(topic)
                    .withAdditionalArgument(ClientArgument.SELECTOR, "colour = 'red' AND a")
                    .withStreamSubscriber(receiverTester1);
            receivers.add(ExternalMessagingClientRun.of(receiverClient1, receiverTester1, "colour = 'red' AND a"));
        }
        //set up subscriber2
        {
            var receiverTester2 = receiverTesterSupplier.get();
            ExternalMessagingClient receiverClient2 = new ExternalMessagingClient()
                    .withClientEngine(getReceiver())
                    .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(getSharedAddressSpace()))
                    .withCredentials(defaultCredentials)
                    .withAddress(topic)
                    .withAdditionalArgument(ClientArgument.SELECTOR, "number < 12.5")
                    .withStreamSubscriber(receiverTester2);
            receivers.add(ExternalMessagingClientRun.of(receiverClient2, receiverTester2, "number < 12.5"));
        }

        receivers.forEach(receiver -> receiver.runAsync());

        LOGGER.info("Waiting for receivers to attach");
        CompletableFuture.allOf(receivers.stream()
                .map(ExternalMessagingClientRun::getReceiverTester)
                .map(tester -> {
                    return CompletableFuture.runAsync(() ->{
                        try {
                            tester.waitForReceiverAttached();
                        } catch(Exception e) {
                            LOGGER.error("Error waiting for receiver to attach", e);
                        }
                    },r-> new Thread(r).run());
                })
                .toArray(CompletableFuture[]::new)).join();

//        for (var receiver : receivers) {
//            receiver.getReceiverTester().waitForReceiverAttached();
//        }

        LOGGER.info("Starting test, running sender");
        senders.forEach(sender -> sender.runAsync());

        LOGGER.info("Waiting for sender to finish");
        for (var sender : senders) {
            assertTrue(sender.getResult().get(), "Producer failed, expected return code 0");
            assertEquals(expectedMsgCount, sender.getClient().getMessages().size(),
                            String.format("Expected %d sent messages", expectedMsgCount));
            sender.getClient().stop();
        }

        LOGGER.info("Waiting for receivers to finish");

        for (var receiver : receivers) {
            assertTrue(receiver.getReceiverTester().getExpectedMessagesResult().get(200, TimeUnit.SECONDS),
                    String.format("Subscriber didn't receive expected messages in time, received %d expected %d", receiver.getReceiverTester().getReceivedMessages(), expectedMsgCount));
        }

        for (var receiver : receivers) {
            if (receiver.getResult().isDone()) {
                assertFalse(receiver.getResult().get(), "Subscriber had return code 1, which is unexpected at this point");
                Assertions.fail("Subscriber failed");
            }
        }

        LOGGER.info("Stopping receivers");

        for (var receiver : receivers) {
            receiver.getClient().stop();
            receiver.getResult().get(10, TimeUnit.SECONDS);
            receiver.getClient().gatherResults();
        }

        LOGGER.info("Verifiying receivers results");

        for (var receiver : receivers) {

            int totalMessages = expectedMsgCount + receiver.getReceiverTester().getTestMessagesReceived();
            assertEquals(totalMessages, receiver.getClient().getMessages().size(),
                    String.format("Expected %d total received messages %s", totalMessages, receiver.getDescriptor()));

            assertEquals(expectedMsgCount, receiver.getReceiverTester().getReceivedMessages(),
                    String.format("Expected %d received messages %s", expectedMsgCount, receiver.getDescriptor()));
        }

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
                .withCredentials(publishCred)
                .withCount(expectedMsgCount)
                .withMessageBody("msg no. %d")
                .withTimeout(30)
                .withCredentials(consumCred)
                .withAdditionalArgument(ClientArgument.DEST_TYPE, "ANYCAST");

        ExternalMessagingClient receiverClient = new ExternalMessagingClient()
                .withClientEngine(receiver)
                .withMessagingRoute(getMessagingRoute(getSharedAddressSpace(), false))
                .withAddress(dest)
                .withCredentials(consumCred)
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

    private AbstractClient getSender() {
        return getSenders(1).get(0);
    }

    private AbstractClient getReceiver() {
        return getReceivers(1).get(0);
    }

    private List<AbstractClient> getSenders(int num) {
        return getClients(num, this::senderFactory);
    }

    private List<AbstractClient> getReceivers(int num) {
        return getClients(num, this::receiverFactory);
    }

    private List<AbstractClient> getClients(int num, ThrowingSupplier<AbstractClient> factory) {
        var c = IntStream.range(0, num).mapToObj(i -> {
            try {
                return factory.get();
            } catch ( Exception e ) {
                throw new IllegalStateException(e);
            }
        }).collect(Collectors.toList());
        clients.addAll(c);
        return c;
    }

}
