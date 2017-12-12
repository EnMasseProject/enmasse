package io.enmasse.systemtest;


import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.Argument;
import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.rhea.RheaClientReceiver;
import io.enmasse.systemtest.executor.client.rhea.RheaClientSender;
import io.enmasse.systemtest.web.AddressWebItem;
import io.enmasse.systemtest.web.FilterType;
import io.enmasse.systemtest.web.SortType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
        assertSorted(getAddressItems());

        sortItems(SortType.NAME, false);
        assertSorted(getAddressItems(), true);
    }

    public void doTestSortAddressesByClients() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        createAddressesWebConsole(addresses.toArray(new Destination[0]));

        List<RheaClientReceiver> receivers = new ArrayList<>();
        List<RheaClientSender> senders = new ArrayList<>();

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getRouteEndpoint(defaultAddressSpace).toString());
        arguments.put(Argument.TIMEOUT, "60");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.LOG_MESSAGES, "json");

        for (int i = 0; i < addressCount; i++) {
            arguments.put(Argument.ADDRESS, addresses.get(i).getAddress());
            for (int j = 0; j < i + 1; j++) {
                RheaClientReceiver rec = new RheaClientReceiver();
                rec.setArguments(arguments);
                rec.runAsync();
                receivers.add(rec);
            }
        }

        Thread.sleep(15000);

        sortItems(SortType.RECEIVERS, true);
        assertSorted(getAddressItems(), Comparator.comparingInt(AddressWebItem::getReceiversCount));

        sortItems(SortType.RECEIVERS, false);
        assertSorted(getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getReceiversCount));

        receivers.forEach(AbstractClient::stop);

        arguments.put(Argument.MSG_CONTENT, "msg no.%d");
        arguments.put(Argument.COUNT, "30");
        arguments.put(Argument.DURATION, "30");

        for (int i = 0; i < addressCount; i++) {
            arguments.put(Argument.ADDRESS, addresses.get(i).getAddress());
            for (int j = 0; j < i + 1; j++) {
                RheaClientSender send = new RheaClientSender();
                send.setArguments(arguments);
                send.runAsync();
                senders.add(send);
            }
        }

        Thread.sleep(15000);

        sortItems(SortType.SENDERS, true);
        assertSorted(getAddressItems(), Comparator.comparingInt(AddressWebItem::getSendersCount));

        sortItems(SortType.SENDERS, false);
        assertSorted(getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getSendersCount));

        senders.forEach(AbstractClient::stop);
    }
}
