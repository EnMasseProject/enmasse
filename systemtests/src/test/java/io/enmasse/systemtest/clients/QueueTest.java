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

import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueueTest extends MultiTenantTestBase {

    @Test
    public void basicMessageTest() throws Exception {
        AddressSpace addressSpace = new AddressSpace("brokered-send-receive",
                "brokered-send-receive",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("messageBasic");
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
        AddressSpace addressSpace = new AddressSpace("brokered-send-receive",
                "brokered-send-receive",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");
        Destination dest = Destination.queue("messageBasic");
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
}
