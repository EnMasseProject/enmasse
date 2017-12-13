package io.enmasse.systemtest.web;


import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.TestBaseWithDefault;
import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.Argument;
import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.rhea.RheaClientReceiver;
import io.enmasse.systemtest.executor.client.rhea.RheaClientSender;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.WebDriver;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public abstract class WebConsoleTest extends TestBaseWithDefault implements ISeleniumProvider {

    protected SeleniumProvider selenium = new SeleniumProvider();
    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            selenium.onFailed(e, description);
        }
    };
    protected ConsoleWebPage consoleWebPage;

    @Override
    public WebDriver buildDriver() {
        throw new NotImplementedException();
    }

    @Before
    public void setUpWebConsoleTests() throws Exception {
        selenium.setupDriver(environment, kubernetes, buildDriver());
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(defaultAddressSpace), addressApiClient, defaultAddressSpace);
    }

    @After
    public void tearDownDrivers() throws Exception {
        selenium.tearDownDrivers();
    }

    public void doTestCreateDeleteAddress(Destination destination) throws Exception {
        consoleWebPage.createAddressWebConsole(destination);
        consoleWebPage.deleteAddressWebConsole(destination);
    }

    public void doTestFilterAddressesByType() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount));
        consoleWebPage.addAddressesFilter(FilterType.TYPE, "queue");
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount / 2));

        consoleWebPage.removeFilterByType("queue");
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount));

        consoleWebPage.addAddressesFilter(FilterType.TYPE, "topic");
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount / 2));

        consoleWebPage.removeFilterByType("topic");
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount));
    }

    public void doTestFilterAddressesByName() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        consoleWebPage.addAddressesFilter(FilterType.NAME, "web");
        assertEquals(addressCount, consoleWebPage.getAddressItems().size());

        consoleWebPage.addAddressesFilter(FilterType.NAME, "via");
        assertEquals(addressCount, consoleWebPage.getAddressItems().size());

        consoleWebPage.removeFilterByName("web");
        assertEquals(addressCount, consoleWebPage.getAddressItems().size());

        consoleWebPage.addAddressesFilter(FilterType.NAME, "queue");
        assertEquals(addressCount / 2, consoleWebPage.getAddressItems().size());

        consoleWebPage.clearAllFilters();
        assertEquals(addressCount, consoleWebPage.getAddressItems().size());
    }

    public void doTestSortAddressesByName() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        consoleWebPage.sortItems(SortType.NAME, true);
        assertSorted(consoleWebPage.getAddressItems());

        consoleWebPage.sortItems(SortType.NAME, false);
        assertSorted(consoleWebPage.getAddressItems(), true);
    }

    public void doTestSortAddressesByClients() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        consoleWebPage.openAddressesPageWebConsole();

        List<AbstractClient> receivers = attachReceivers(addresses);

        Thread.sleep(15000);

        consoleWebPage.sortItems(SortType.RECEIVERS, true);
        assertSorted(consoleWebPage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getReceiversCount));

        consoleWebPage.sortItems(SortType.RECEIVERS, false);
        assertSorted(consoleWebPage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getReceiversCount));

        receivers.forEach(AbstractClient::stop);

        List<AbstractClient> senders = attachSenders(addresses);

        Thread.sleep(15000);

        consoleWebPage.sortItems(SortType.SENDERS, true);
        assertSorted(consoleWebPage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getSendersCount));

        consoleWebPage.sortItems(SortType.SENDERS, false);
        assertSorted(consoleWebPage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getSendersCount));

        senders.forEach(AbstractClient::stop);
    }

    public void doTestSortConnectionsBySenders() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        consoleWebPage.openConnectionsPageWebConsole();

        List<AbstractClient> senders = attachSenders(addresses);

        Thread.sleep(15000);

        consoleWebPage.sortItems(SortType.SENDERS, true);
        assertSorted(consoleWebPage.getConnectionItems(), Comparator.comparingInt(ConnectionWebItem::getSendersCount));

        consoleWebPage.sortItems(SortType.SENDERS, false);
        assertSorted(consoleWebPage.getConnectionItems(), true, Comparator.comparingInt(ConnectionWebItem::getSendersCount));

        senders.forEach(AbstractClient::stop);
    }

    public void doTestSortConnectionsByReceivers() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        consoleWebPage.openConnectionsPageWebConsole();

        List<AbstractClient> receivers = attachReceivers(addresses);

        Thread.sleep(15000);

        consoleWebPage.sortItems(SortType.RECEIVERS, true);
        assertSorted(consoleWebPage.getConnectionItems(), Comparator.comparingInt(ConnectionWebItem::getReceiversCount));

        consoleWebPage.sortItems(SortType.RECEIVERS, false);
        assertSorted(consoleWebPage.getConnectionItems(), true, Comparator.comparingInt(ConnectionWebItem::getReceiversCount));

        receivers.forEach(AbstractClient::stop);
    }


    public void doTestFilterConnectionsByEncrypted() throws Exception {
        Destination queue = Destination.queue("queue-via-web-connections-encrypted");
        consoleWebPage.createAddressesWebConsole(queue);
        consoleWebPage.openConnectionsPageWebConsole();

        List<RheaClientReceiver> receivers = new ArrayList<>();
        int receiverCount = 5;

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getRouteEndpoint(defaultAddressSpace).toString());
        arguments.put(Argument.TIMEOUT, "60");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.ADDRESS, queue.getAddress());

        for (int i = 0; i < receiverCount; i++) {
            RheaClientReceiver rec = new RheaClientReceiver();
            rec.setArguments(arguments);
            rec.runAsync();
            receivers.add(rec);
        }

        Thread.sleep(15000);

        consoleWebPage.addConnectionsFilter(FilterType.ENCRYPTED, "unencrypted");
        assertThat(consoleWebPage.getConnectionItems().size(), is(receiverCount));

        consoleWebPage.clearAllFilters();
        assertThat(consoleWebPage.getConnectionItems().size(), is(receiverCount));

        consoleWebPage.addConnectionsFilter(FilterType.ENCRYPTED, "encrypted");
        assertThat(consoleWebPage.getConnectionItems().size(), is(0));

        receivers.forEach(AbstractClient::stop);
    }

    //============================================================================================
    //============================ Help methods ==================================================
    //============================================================================================

    private List<AbstractClient> attachSenders(List<Destination> destinations) throws Exception {
        List<AbstractClient> senders = new ArrayList<>();

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getRouteEndpoint(defaultAddressSpace).toString());
        arguments.put(Argument.TIMEOUT, "60");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.MSG_CONTENT, "msg no.%d");
        arguments.put(Argument.COUNT, "30");
        arguments.put(Argument.DURATION, "30");

        for (int i = 0; i < destinations.size(); i++) {
            arguments.put(Argument.ADDRESS, destinations.get(i).getAddress());
            for (int j = 0; j < i + 1; j++) {
                RheaClientSender send = new RheaClientSender();
                send.setArguments(arguments);
                send.runAsync();
                senders.add(send);
            }
        }


        return senders;
    }

    private List<AbstractClient> attachReceivers(List<Destination> destinations) throws Exception {
        List<AbstractClient> receivers = new ArrayList<>();

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getRouteEndpoint(defaultAddressSpace).toString());
        arguments.put(Argument.TIMEOUT, "60");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.LOG_MESSAGES, "json");

        for (int i = 0; i < destinations.size(); i++) {
            arguments.put(Argument.ADDRESS, destinations.get(i).getAddress());
            for (int j = 0; j < i + 1; j++) {
                RheaClientReceiver rec = new RheaClientReceiver();
                rec.setArguments(arguments);
                rec.runAsync();
                receivers.add(rec);
            }
        }

        return receivers;
    }
}
