package io.enmasse.systemtest;


import com.google.common.collect.Ordering;
import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.Argument;
import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.rhea.RheaClientReceiver;
import io.enmasse.systemtest.executor.client.rhea.RheaClientSender;
import io.enmasse.systemtest.web.AddressWebItem;
import io.enmasse.systemtest.web.FilterType;
import io.enmasse.systemtest.web.SortType;
import net.bytebuddy.implementation.bytecode.Throw;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public abstract class WebConsoleTest extends SeleniumTestBase {

    public void doTestCreateDeleteAddress(Destination destination) throws Exception {
        createAddressWebConsole(destination);
        deleteAddressWebConsole(destination);
    }

    public void doTestFilterAddressesByType() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        createAddressesWebConsole(addresses.toArray(new Destination[0]));
        assertThat(getAddressItems().size(), is(addressCount));
        addFilter(FilterType.TYPE, "queue");
        assertThat(getAddressItems().size(), is(addressCount / 2));

        removeFilterByType("queue");
        assertThat(getAddressItems().size(), is(addressCount));

        addFilter(FilterType.TYPE, "topic");
        assertThat(getAddressItems().size(), is(addressCount / 2));

        removeFilterByType("topic");
        assertThat(getAddressItems().size(), is(addressCount));
    }

    public void doTestFilterAddressesByName() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        createAddressesWebConsole(addresses.toArray(new Destination[0]));

        addFilter(FilterType.NAME, "web");
        assertEquals(addressCount, getAddressItems().size());

        addFilter(FilterType.NAME, "via");
        assertEquals(addressCount, getAddressItems().size());

        removeFilterByName("web");
        assertEquals(addressCount, getAddressItems().size());

        addFilter(FilterType.NAME, "queue");
        assertEquals(addressCount / 2, getAddressItems().size());

        clearAllFilters();
        assertEquals(addressCount, getAddressItems().size());
    }

    public void doTestSortAddressesByName() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        createAddressesWebConsole(addresses.toArray(new Destination[0]));

        sortItems(SortType.NAME, true);
        assertTrue(Ordering.natural().isOrdered(getAddressItems()));

        sortItems(SortType.NAME, false);
        assertTrue(Ordering.natural().reverse().isOrdered(getAddressItems()));
    }

    public void doTestSortAddressesByClients() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        createAddressesWebConsole(addresses.toArray(new Destination[0]));

        List<RheaClientReceiver> receivers = new ArrayList<>();
        List<RheaClientSender> senders = new ArrayList<>();

        ArgumentMap receiverArgumets = new ArgumentMap();
        receiverArgumets.put(Argument.BROKER, getRouteEndpoint(defaultAddressSpace).toString());
        receiverArgumets.put(Argument.TIMEOUT, "60");

        for (int i = 0; i < addressCount; i++) {
            receiverArgumets.put(Argument.ADDRESS, addresses.get(i).getAddress());
            for(int j = 0; j < i + 1; j++){
                RheaClientReceiver rec = new RheaClientReceiver();
                rec.setArguments(receiverArgumets);
                rec.runAsync();
                receivers.add(rec);
            }
        }

        Thread.sleep(15000);

        sortItems(SortType.RECEIVERS, true);
        assertEquals(addressCount, getAddressItems().size());

        receivers.forEach(AbstractClient::stop);

        ArgumentMap senderArguments = new ArgumentMap();
        senderArguments.put(Argument.BROKER, getRouteEndpoint(defaultAddressSpace).toString());
        senderArguments.put(Argument.TIMEOUT, "60");
        senderArguments.put(Argument.MSG_CONTENT, "msg no.%d");
        senderArguments.put(Argument.COUNT, "30");
        senderArguments.put(Argument.DURATION, "30");

        for (int i = 0; i < addressCount; i++) {
            senderArguments.put(Argument.ADDRESS, addresses.get(i).getAddress());
            for(int j = 0; j < i + 1; j++){
                RheaClientSender send = new RheaClientSender();
                send.setArguments(senderArguments);
                send.runAsync();
                senders.add(send);
            }
        }

        Thread.sleep(15000);

        sortItems(SortType.SENDERS, true);
        assertEquals(addressCount, getAddressItems().size());

        senders.forEach(AbstractClient::stop);
    }
}
