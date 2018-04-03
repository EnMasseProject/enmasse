/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.clients;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.Argument;
import io.enmasse.systemtest.clients.ClientType;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class MsgPatternsTestBase extends ClientTestBase {

    @BeforeEach
    public void setUpCommonArguments() {
        arguments.put(Argument.USERNAME, "test");
        arguments.put(Argument.PASSWORD, "test");
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.CONN_SSL, "true");
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        clients.addAll(Arrays.asList(sender, receiver));
        int expectedMsgCount = 10;

        Destination dest = Destination.queue("message-basic" + ClientType.getAddressName(sender),
                getDefaultPlan(AddressType.QUEUE));
        setAddresses(dest);

        arguments.put(Argument.BROKER, getMessagingRoute(sharedAddressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(Argument.MSG_CONTENT, "msg no. %d");

        sender.setArguments(arguments);
        arguments.remove(Argument.MSG_CONTENT);
        receiver.setArguments(arguments);

        assertTrue(sender.run(), "Sender failed, expected return code 0");
        assertTrue(receiver.run(), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, sender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiver.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    protected void doRoundRobinReceiverTest(AbstractClient sender, AbstractClient receiver, AbstractClient receiver2)
            throws Exception {
        clients.addAll(Arrays.asList(sender, receiver, receiver2));
        int expectedMsgCount = 10;

        Destination dest = Destination.queue("receiver-round-robin" + ClientType.getAddressName(sender),
                getDefaultPlan(AddressType.QUEUE));
        setAddresses(dest);

        arguments.put(Argument.BROKER, getMessagingRoute(sharedAddressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.COUNT, Integer.toString(expectedMsgCount / 2));
        arguments.put(Argument.TIMEOUT, "60");


        receiver.setArguments(arguments);
        receiver2.setArguments(arguments);

        Future<Boolean> recResult = receiver.runAsync();
        Future<Boolean> rec2Result = receiver2.runAsync();

        arguments.put(Argument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(Argument.MSG_CONTENT, "msg no. %d");

        sender.setArguments(arguments);

        assertTrue(sender.run(), "Sender failed, expected return code 0");
        assertTrue(recResult.get(), "Receiver failed, expected return code 0");
        assertTrue(rec2Result.get(), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, sender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount / 2, receiver.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount / 2));
        assertEquals(expectedMsgCount / 2, receiver.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount / 2));
    }

    protected void doTopicSubscribeTest(AbstractClient sender, AbstractClient subscriber, AbstractClient subscriber2,
                                        boolean hasTopicPrefix) throws Exception {
        clients.addAll(Arrays.asList(sender, subscriber, subscriber2));
        int expectedMsgCount = 10;

        Destination dest = Destination.topic("topic-subscribe" + ClientType.getAddressName(sender),
                getDefaultPlan(AddressType.TOPIC));
        setAddresses(dest);

        arguments.put(Argument.BROKER, getMessagingRoute(sharedAddressSpace).toString());
        arguments.put(Argument.ADDRESS, getTopicPrefix(hasTopicPrefix) + dest.getAddress());
        arguments.put(Argument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(Argument.MSG_CONTENT, "msg no. %d");
        arguments.put(Argument.TIMEOUT, "100");

        sender.setArguments(arguments);
        arguments.remove(Argument.MSG_CONTENT);
        subscriber.setArguments(arguments);
        subscriber2.setArguments(arguments);

        Future<Boolean> recResult = subscriber.runAsync();
        Future<Boolean> recResult2 = subscriber2.runAsync();

        if (isBrokered(sharedAddressSpace)) {
            waitForSubscribers(sharedAddressSpace, dest.getAddress(), 2);
        } else {
            waitForSubscribersConsole(sharedAddressSpace, dest, 2);
        }

        assertTrue(sender.run(), "Producer failed, expected return code 0");
        assertTrue(recResult.get(), "Subscriber failed, expected return code 0");
        assertTrue(recResult2.get(), "Subscriber failed, expected return code 0");

        assertEquals(expectedMsgCount, sender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, subscriber.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
        assertEquals(expectedMsgCount, subscriber2.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    protected void doMessageBrowseTest(AbstractClient sender, AbstractClient receiver_browse, AbstractClient receiver_receive)
            throws Exception {
        clients.addAll(Arrays.asList(sender, receiver_browse, receiver_receive));
        int expectedMsgCount = 10;

        Destination dest = Destination.queue("message-browse" + ClientType.getAddressName(sender),
                getDefaultPlan(AddressType.QUEUE));
        setAddresses(dest);

        arguments.put(Argument.BROKER, getMessagingRoute(sharedAddressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(Argument.MSG_CONTENT, "msg no. %d");

        sender.setArguments(arguments);
        arguments.remove(Argument.MSG_CONTENT);

        arguments.put(Argument.RECV_BROWSE, "true");
        receiver_browse.setArguments(arguments);

        arguments.put(Argument.RECV_BROWSE, "false");
        receiver_receive.setArguments(arguments);

        assertTrue(sender.run(), "Sender failed, expected return code 0");
        assertTrue(receiver_browse.run(), "Browse receiver failed, expected return code 0");
        assertTrue(receiver_receive.run(), "Receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, sender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiver_browse.getMessages().size(),
                String.format("Expected %d browsed messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiver_receive.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    protected void doDrainQueueTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        Destination dest = Destination.queue("drain-queue" + ClientType.getAddressName(sender),
                getDefaultPlan(AddressType.QUEUE));
        setAddresses(dest);

        clients.addAll(Arrays.asList(sender, receiver));
        int expectedMsgCount = 50;

        arguments.put(Argument.BROKER, getMessagingRoute(sharedAddressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(Argument.MSG_CONTENT, "msg no. %d");

        sender.setArguments(arguments);
        arguments.remove(Argument.MSG_CONTENT);

        arguments.put(Argument.COUNT, "0");
        receiver.setArguments(arguments);

        assertTrue(sender.run(), "Sender failed, expected return code 0");
        assertTrue(receiver.run(), "Drain receiver failed, expected return code 0");

        assertEquals(expectedMsgCount, sender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, receiver.getMessages().size(),
                String.format("Expected %d received messages", expectedMsgCount));
    }

    protected void doMessageSelectorQueueTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        int expectedMsgCount = 10;

        clients.addAll(Arrays.asList(sender, receiver));
        Destination queue = Destination.queue("selector-queue" + ClientType.getAddressName(sender),
                getDefaultPlan(AddressType.QUEUE));
        setAddresses(queue);

        arguments.put(Argument.BROKER, getMessagingRoute(sharedAddressSpace).toString());
        arguments.put(Argument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(Argument.ADDRESS, queue.getAddress());
        arguments.put(Argument.MSG_PROPERTY, "colour~red");
        arguments.put(Argument.MSG_PROPERTY, "number~12.65");
        arguments.put(Argument.MSG_PROPERTY, "a~true");
        arguments.put(Argument.MSG_PROPERTY, "b~false");
        arguments.put(Argument.MSG_CONTENT, "msg no. %d");

        //send messages
        sender.setArguments(arguments);
        assertTrue(sender.run(), "Sender failed, expected return code 0");
        assertEquals(expectedMsgCount, sender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));

        arguments.remove(Argument.MSG_PROPERTY);
        arguments.remove(Argument.MSG_CONTENT);
        arguments.put(Argument.RECV_BROWSE, "true");
        arguments.put(Argument.COUNT, "0");

        //receiver with selector colour = red
        arguments.put(Argument.SELECTOR, "colour = 'red'");
        receiver.setArguments(arguments);
        assertTrue(receiver.run(), "Receiver 'colour = red' failed, expected return code 0");
        assertEquals(expectedMsgCount, receiver.getMessages().size(),
                String.format("Expected %d received messages 'colour = red'", expectedMsgCount));

        //receiver with selector number > 12.5
        arguments.put(Argument.SELECTOR, "number > 12.5");
        receiver.setArguments(arguments);
        assertTrue(receiver.run(), "Receiver 'number > 12.5' failed, expected return code 0");
        assertEquals(expectedMsgCount, receiver.getMessages().size(),
                String.format("Expected %d received messages 'colour = red'", expectedMsgCount));


        //receiver with selector a AND b
        arguments.put(Argument.SELECTOR, "a AND b");
        receiver.setArguments(arguments);
        assertTrue(receiver.run(), "Receiver 'a AND b' failed, expected return code 0");
        assertEquals(0, receiver.getMessages().size(),
                String.format("Expected %d received messages 'a AND b'", 0));

        //receiver with selector a OR b
        arguments.put(Argument.RECV_BROWSE, "false");
        arguments.put(Argument.SELECTOR, "a OR b");
        receiver.setArguments(arguments);
        assertTrue(receiver.run(), "Receiver 'a OR b' failed, expected return code 0");
        assertEquals(expectedMsgCount, receiver.getMessages().size(),
                String.format("Expected %d received messages 'a OR b'", expectedMsgCount));
    }

    protected void doMessageSelectorTopicTest(AbstractClient sender, AbstractClient subscriber,
                                              AbstractClient subscriber2, AbstractClient subscriber3, boolean hasTopicPrefix) throws Exception {
        clients.addAll(Arrays.asList(sender, subscriber, subscriber2, subscriber3));
        int expectedMsgCount = 10;

        Destination topic = Destination.topic("selector-topic" + ClientType.getAddressName(sender),
                getDefaultPlan(AddressType.TOPIC));
        setAddresses(topic);

        arguments.put(Argument.BROKER, getMessagingRoute(sharedAddressSpace).toString());
        arguments.put(Argument.COUNT, Integer.toString(expectedMsgCount));
        arguments.put(Argument.ADDRESS, getTopicPrefix(hasTopicPrefix) + topic.getAddress());
        arguments.put(Argument.MSG_PROPERTY, "colour~red");
        arguments.put(Argument.MSG_PROPERTY, "number~12.65");
        arguments.put(Argument.MSG_PROPERTY, "a~true");
        arguments.put(Argument.MSG_PROPERTY, "b~false");
        arguments.put(Argument.TIMEOUT, "100");
        arguments.put(Argument.MSG_CONTENT, "msg no. %d");

        //set up sender
        sender.setArguments(arguments);

        arguments.remove(Argument.MSG_PROPERTY);
        arguments.remove(Argument.MSG_CONTENT);

        //set up subscriber1
        arguments.put(Argument.SELECTOR, "colour = 'red'");
        subscriber.setArguments(arguments);

        //set up subscriber2
        arguments.put(Argument.SELECTOR, "number > 12.5");
        subscriber2.setArguments(arguments);

        //set up subscriber3
        arguments.put(Argument.SELECTOR, "a AND b");
        subscriber3.setArguments(arguments);

        Future<Boolean> result1 = subscriber.runAsync();
        Future<Boolean> result2 = subscriber2.runAsync();
        Future<Boolean> result3 = subscriber3.runAsync();

        if (isBrokered(sharedAddressSpace)) {
            waitForSubscribers(sharedAddressSpace, topic.getAddress(), 3);
        } else {
            waitForSubscribersConsole(sharedAddressSpace, topic, 3);
        }

        assertTrue(sender.run(), "Sender failed, expected return code 0");
        assertTrue(result1.get(), "Receiver 'colour = red' failed, expected return code 0");
        assertTrue(result2.get(), "Receiver 'number > 12.5' failed, expected return code 0");
        assertTrue(result3.get(), "Receiver 'a AND b' failed, expected return code 0");

        assertEquals(expectedMsgCount, sender.getMessages().size(),
                String.format("Expected %d sent messages", expectedMsgCount));
        assertEquals(expectedMsgCount, subscriber.getMessages().size(),
                String.format("Expected %d received messages 'colour = red'", expectedMsgCount));
        assertEquals(expectedMsgCount, subscriber2.getMessages().size(),
                String.format("Expected %d received messages 'number > 12.5'", expectedMsgCount));
        assertEquals(0, subscriber3.getMessages().size(),
                String.format("Expected %d received messages 'a AND b'", 0));
    }
}
