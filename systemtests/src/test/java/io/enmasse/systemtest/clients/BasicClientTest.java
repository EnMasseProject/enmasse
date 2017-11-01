package io.enmasse.systemtest.clients;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.MultiTenantTestBase;
import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.Argument;
import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.rhea.RheaClientReceiver;
import io.enmasse.systemtest.executor.client.rhea.RheaClientSender;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BasicClientTest extends MultiTenantTestBase {

    @Test
    public void basicMessageTest() throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-send-receive",
                "brokered-send-receive",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("message-basic");
        setAddresses(addressSpace, dest);

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.USERNAME, "test");
        arguments.put(Argument.PASSWORD, "test");
        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.COUNT, "10");
        arguments.put(Argument.CONN_SSL, "true");

        AbstractClient sender  = new RheaClientSender();
        AbstractClient receiver = new RheaClientReceiver();

        sender.setArguments(arguments);
        receiver.setArguments(arguments);

        assertTrue(sender.run());
        assertTrue(receiver.run());

        assertEquals(10, sender.getMessages().size());
        assertEquals(10, receiver.getMessages().size());
    }

    @Test
    public void roundRobinReceiverTest() throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-receiver-round-robin",
                "brokered-receiver-round-robin",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("receiver-round-robin");
        setAddresses(addressSpace, dest);

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.USERNAME, "test");
        arguments.put(Argument.PASSWORD, "test");
        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.COUNT, "5");
        arguments.put(Argument.CONN_SSL, "true");

        AbstractClient sender  = new RheaClientSender();
        AbstractClient receiver = new RheaClientReceiver();
        AbstractClient receiver2 = new RheaClientReceiver();

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

    @Test
    public void topicSubscribeTest() throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-topic-subscribe",
                "brokered-topic-subscribe",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.topic("topic-subscribe");
        setAddresses(addressSpace, dest);

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.USERNAME, "test");
        arguments.put(Argument.PASSWORD, "test");
        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, "topic://" + dest.getAddress());
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.COUNT, "10");
        arguments.put(Argument.CONN_SSL, "true");

        AbstractClient sender  = new RheaClientSender();
        AbstractClient receiver = new RheaClientReceiver();

        sender.setArguments(arguments);
        receiver.setArguments(arguments);

        Future<Boolean> recResult = receiver.runAsync();

        assertTrue(sender.run());
        assertTrue(recResult.get());

        assertEquals(10, sender.getMessages().size());
        assertEquals(10, receiver.getMessages().size());
    }

    @Test
    public void messageBrowseTest() throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-message-browse",
                "brokered-message-browse",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("message-browse");
        setAddresses(addressSpace, dest);

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.USERNAME, "test");
        arguments.put(Argument.PASSWORD, "test");
        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.COUNT, "10");
        arguments.put(Argument.CONN_SSL, "true");

        AbstractClient sender  = new RheaClientSender();
        AbstractClient receiver_browse = new RheaClientReceiver();
        AbstractClient receiver_receive = new RheaClientReceiver();

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

    @Test
    public void drainQueueTest() throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-drain-queue",
                "brokered-drain-queue",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("drain-queue");
        setAddresses(addressSpace, dest);

        int count = new Random().nextInt(200 - 10) + 10;

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.USERNAME, "test");
        arguments.put(Argument.PASSWORD, "test");
        arguments.put(Argument.BROKER, getRouteEndpoint(addressSpace).toString());
        arguments.put(Argument.ADDRESS, dest.getAddress());
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.COUNT, Integer.toString(count));
        arguments.put(Argument.CONN_SSL, "true");

        AbstractClient sender  = new RheaClientSender();
        AbstractClient receiver = new RheaClientReceiver();

        sender.setArguments(arguments);

        arguments.put(Argument.COUNT, "0");
        receiver.setArguments(arguments);

        assertTrue(sender.run());
        assertTrue(receiver.run());

        assertEquals(count, sender.getMessages().size());
        assertEquals(count, receiver.getMessages().size());
    }
}
