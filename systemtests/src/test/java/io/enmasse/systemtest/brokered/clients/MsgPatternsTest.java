package io.enmasse.systemtest.brokered.clients;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.ClientTestBase;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.Argument;
import org.junit.Before;

import javax.validation.constraints.AssertTrue;
import java.util.Random;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MsgPatternsTest extends ClientTestBase {

    @Before
    public void setUpCommonArguments(){
        arguments.put(Argument.USERNAME, "test");
        arguments.put(Argument.PASSWORD, "test");
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.CONN_SSL, "true");
    }

    protected void doBasicMessageTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-send-receive",
                "brokered-send-receive",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("message-basic");
        setAddresses(addressSpace, dest);

        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.COUNT, "10");

        sender.setArguments(arguments);
        receiver.setArguments(arguments);

        assertTrue(sender.run());
        assertTrue(receiver.run());

        assertEquals(10, sender.getMessages().size());
        assertEquals(10, receiver.getMessages().size());
    }

    protected void doRoundRobinReceiverTest(AbstractClient sender, AbstractClient receiver, AbstractClient receiver2)
            throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-receiver-round-robin",
                "brokered-receiver-round-robin",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("receiver-round-robin");
        setAddresses(addressSpace, dest);

        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.COUNT, "5");


        receiver.setArguments(arguments);
        receiver2.setArguments(arguments);

        Future<Boolean> recResult = receiver.runAsync();
        Future<Boolean> rec2Result = receiver2.runAsync();

        arguments.put(Argument.COUNT, "10");

        sender.setArguments(arguments);

        assertTrue(sender.run());
        assertTrue(recResult.get());
        assertTrue(rec2Result.get());

        assertEquals(10, sender.getMessages().size());
        assertEquals(5, receiver.getMessages().size());
        assertEquals(5, receiver.getMessages().size());
    }

    protected void doTopicSubscribeTest(AbstractClient sender, AbstractClient subscriber, AbstractClient subscriber2)
            throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-topic-subscribe",
                "brokered-topic-subscribe",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.topic("topic-subscribe");
        setAddresses(addressSpace, dest);

        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.COUNT, "10");

        sender.setArguments(arguments);
        subscriber.setArguments(arguments);
        subscriber2.setArguments(arguments);

        Future<Boolean> recResult = subscriber.runAsync();
        Future<Boolean> recResult2 = subscriber2.runAsync();

        assertTrue(sender.run());
        assertTrue(recResult.get());
        assertTrue(recResult2.get());

        assertEquals(10, sender.getMessages().size());
        assertEquals(10, subscriber.getMessages().size());
        assertEquals(10, subscriber2.getMessages().size());
    }

    protected void doMessageBrowseTest(AbstractClient sender, AbstractClient receiver_browse, AbstractClient receiver_receive)
            throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-message-browse",
                "brokered-message-browse",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("message-browse");
        setAddresses(addressSpace, dest);

        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.COUNT, "10");

        sender.setArguments(arguments);

        arguments.put(Argument.RECV_BROWSE, "true");
        receiver_browse.setArguments(arguments);

        arguments.put(Argument.RECV_BROWSE, "false");
        receiver_receive.setArguments(arguments);

        assertTrue(sender.run());
        assertTrue(receiver_browse.run());
        assertTrue(receiver_receive.run());

        assertEquals(10, sender.getMessages().size());
        assertEquals(10, receiver_browse.getMessages().size());
        assertEquals(10, receiver_receive.getMessages().size());
    }

    protected void doDrainQueueTest(AbstractClient sender, AbstractClient receiver) throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-drain-queue",
                "brokered-drain-queue",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("drain-queue");
        setAddresses(addressSpace, dest);

        int count = new Random().nextInt(200 - 10) + 10;

        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.COUNT, Integer.toString(count));

        sender.setArguments(arguments);

        arguments.put(Argument.COUNT, "0");
        receiver.setArguments(arguments);

        assertTrue(sender.run());
        assertTrue(receiver.run());

        assertEquals(count, sender.getMessages().size());
        assertEquals(count, receiver.getMessages().size());
    }

    protected void doMessageSelectorQueueTest(AbstractClient sender, AbstractClient receiver) throws Exception{
        AddressSpace addressSpace = new AddressSpace("brokered-selectors-queue",
                "brokered-selectors-queue",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination queue = Destination.queue("selector-queue");
        setAddresses(addressSpace, queue);

        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.COUNT, "10");
        arguments.put(Argument.ADDRESS, queue.getAddress());
        arguments.put(Argument.MSG_PROPERTY, "colour~red");
        arguments.put(Argument.MSG_PROPERTY, "number~12.65");
        arguments.put(Argument.MSG_PROPERTY, "a~true");
        arguments.put(Argument.MSG_PROPERTY, "b~false");

        //send messages
        sender.setArguments(arguments);
        assertTrue(sender.run());
        assertEquals(10, sender.getMessages().size());

        arguments.remove(Argument.MSG_PROPERTY);
        arguments.put(Argument.RECV_BROWSE, "true");
        arguments.put(Argument.COUNT, "0");

        //receiver with selector colour = red
        arguments.put(Argument.SELECTOR, "colour = 'red'");
        receiver.setArguments(arguments);
        assertTrue(receiver.run());
        assertEquals(10, receiver.getMessages().size());

        //receiver with selector number > 12.5
        arguments.put(Argument.SELECTOR, "number > 12.5");
        receiver.setArguments(arguments);
        assertTrue(receiver.run());
        assertEquals(10, receiver.getMessages().size());


        //receiver with selector a AND b
        arguments.put(Argument.SELECTOR, "a AND b");
        receiver.setArguments(arguments);
        assertTrue(receiver.run());
        assertEquals(0, receiver.getMessages().size());

        //receiver with selector a OR b
        arguments.put(Argument.RECV_BROWSE, "false");
        arguments.put(Argument.SELECTOR, "a OR b");
        receiver.setArguments(arguments);
        assertTrue(receiver.run());
        assertEquals(10, receiver.getMessages().size());
    }

    protected void doMessageSelectorTopicTest(AbstractClient sender, AbstractClient subscriber,
                                              AbstractClient subscriber2, AbstractClient subscriber3) throws Exception{
        AddressSpace addressSpace = new AddressSpace("brokered-selectors-topic",
                "brokered-selectors-topic",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination topic = Destination.topic("selector-topic");
        setAddresses(addressSpace, topic);

        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.COUNT, "10");
        arguments.put(Argument.ADDRESS, topic.getAddress());
        arguments.put(Argument.MSG_PROPERTY, "colour~red");
        arguments.put(Argument.MSG_PROPERTY, "number~12.65");
        arguments.put(Argument.MSG_PROPERTY, "a~true");
        arguments.put(Argument.MSG_PROPERTY, "b~false");

        //set up sender
        sender.setArguments(arguments);

        arguments.remove(Argument.MSG_PROPERTY);
        arguments.put(Argument.TIMEOUT, "1");

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

        assertTrue(sender.run());
        assertTrue(result1.get());
        assertTrue(result2.get());
        assertTrue(result3.get());

        assertEquals(10, sender.getMessages().size());
        assertEquals(10, subscriber.getMessages().size());
        assertEquals(10, subscriber2.getMessages().size());
        assertEquals(0, subscriber3.getMessages().size());
    }
}
