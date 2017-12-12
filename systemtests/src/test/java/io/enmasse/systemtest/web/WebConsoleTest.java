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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public abstract class WebConsoleTest extends TestBaseWithDefault {

    protected SeleniumProvider selenium = new SeleniumProvider();

    protected ConsoleWebPage consoleWebPage;

    protected abstract WebDriver buildDriver();

    @Rule
    public TestWatcher watchman = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            selenium.onFailed(e, description);
        }
    };

    @Before
    public void setUpWebConsoleTests() throws Exception {
        selenium.setupDriver(environment, openShift, buildDriver());
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
        consoleWebPage.addFilter(FilterType.TYPE, "queue");
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount / 2));

        consoleWebPage.removeFilterByType("queue");
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount));

        consoleWebPage.addFilter(FilterType.TYPE, "topic");
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount / 2));

        consoleWebPage.removeFilterByType("topic");
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount));
    }

    public void doTestFilterAddressesByName() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        consoleWebPage.addFilter(FilterType.NAME, "web");
        assertEquals(addressCount, consoleWebPage.getAddressItems().size());

        consoleWebPage.addFilter(FilterType.NAME, "via");
        assertEquals(addressCount, consoleWebPage.getAddressItems().size());

        consoleWebPage.removeFilterByName("web");
        assertEquals(addressCount, consoleWebPage.getAddressItems().size());

        consoleWebPage.addFilter(FilterType.NAME, "queue");
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

        consoleWebPage.sortItems(SortType.RECEIVERS, true);
        assertSorted(consoleWebPage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getReceiversCount));

        consoleWebPage.sortItems(SortType.RECEIVERS, false);
        assertSorted(consoleWebPage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getReceiversCount));

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

        consoleWebPage.sortItems(SortType.SENDERS, true);
        assertSorted(consoleWebPage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getSendersCount));

        consoleWebPage.sortItems(SortType.SENDERS, false);
        assertSorted(consoleWebPage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getSendersCount));

        senders.forEach(AbstractClient::stop);
    }
}
