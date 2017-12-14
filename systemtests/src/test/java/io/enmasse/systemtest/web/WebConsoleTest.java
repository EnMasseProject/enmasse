package io.enmasse.systemtest.web;


import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.KeycloakCredentials;
import io.enmasse.systemtest.TestBaseWithDefault;
import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.Argument;
import io.enmasse.systemtest.executor.client.ArgumentMap;
import io.enmasse.systemtest.executor.client.rhea.RheaClientConnector;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

        consoleWebPage.addAddressesFilter(FilterType.TYPE, AddressType.QUEUE.toString());
        List<AddressWebItem> items = consoleWebPage.getAddressItems();
        assertThat(items.size(), is(addressCount / 2)); //assert correct count
        assertAddressType(items, AddressType.QUEUE); //assert correct type

        consoleWebPage.removeFilterByType(AddressType.QUEUE.toString());
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount));

        consoleWebPage.addAddressesFilter(FilterType.TYPE, AddressType.TOPIC.toString());
        items = consoleWebPage.getAddressItems();
        assertThat(items.size(), is(addressCount / 2)); //assert correct count
        assertAddressType(items, AddressType.TOPIC); //assert correct type

        consoleWebPage.removeFilterByType(AddressType.TOPIC.toString());
        assertThat(consoleWebPage.getAddressItems().size(), is(addressCount));
    }

    public void doTestFilterAddressesByName() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        String subText = "web";
        consoleWebPage.addAddressesFilter(FilterType.NAME, subText);
        List<AddressWebItem> items = consoleWebPage.getAddressItems();
        assertEquals(addressCount, items.size());
        assertAddressName(items, subText);

        subText = "via";
        consoleWebPage.addAddressesFilter(FilterType.NAME, subText);
        items = consoleWebPage.getAddressItems();
        assertEquals(addressCount, items.size());
        assertAddressName(items, subText);

        subText = "web";
        consoleWebPage.removeFilterByName(subText);
        items = consoleWebPage.getAddressItems();
        assertEquals(addressCount, items.size());
        assertAddressName(items, subText);

        subText = "queue";
        consoleWebPage.addAddressesFilter(FilterType.NAME, subText);
        items = consoleWebPage.getAddressItems();
        assertEquals(addressCount / 2, items.size());
        assertAddressName(items, subText);

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
        int addressCount = 2;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        consoleWebPage.openConnectionsPageWebConsole();

        List<AbstractClient> senders = attachClients(addresses);

        consoleWebPage.sortItems(SortType.SENDERS, true);
        assertSorted(consoleWebPage.getConnectionItems(), Comparator.comparingInt(ConnectionWebItem::getSendersCount));

        consoleWebPage.sortItems(SortType.SENDERS, false);
        assertSorted(consoleWebPage.getConnectionItems(), true, Comparator.comparingInt(ConnectionWebItem::getSendersCount));

        senders.forEach(AbstractClient::stop);
    }

    public void doTestSortConnectionsByReceivers() throws Exception {
        int addressCount = 2;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        consoleWebPage.openConnectionsPageWebConsole();

        List<AbstractClient> clients = attachClients(addresses);

        consoleWebPage.sortItems(SortType.RECEIVERS, true);
        assertSorted(consoleWebPage.getConnectionItems(), Comparator.comparingInt(ConnectionWebItem::getReceiversCount));

        consoleWebPage.sortItems(SortType.RECEIVERS, false);
        assertSorted(consoleWebPage.getConnectionItems(), true, Comparator.comparingInt(ConnectionWebItem::getReceiversCount));

        clients.forEach(AbstractClient::stop);
    }


    public void doTestFilterConnectionsByEncrypted() throws Exception {
        Destination queue = Destination.queue("queue-via-web-connections-encrypted");
        consoleWebPage.createAddressesWebConsole(queue);
        consoleWebPage.openConnectionsPageWebConsole();

        int receiverCount = 5;
        List<AbstractClient> receivers = attachReceivers(queue, receiverCount);

        consoleWebPage.addConnectionsFilter(FilterType.ENCRYPTED, "unencrypted");
        List<ConnectionWebItem> items = consoleWebPage.getConnectionItems();
        assertThat(items.size(), is(receiverCount));
        assertConnectionUnencrypted(items);

        consoleWebPage.clearAllFilters();
        assertThat(consoleWebPage.getConnectionItems().size(), is(receiverCount));

        consoleWebPage.addConnectionsFilter(FilterType.ENCRYPTED, "encrypted");
        items = consoleWebPage.getConnectionItems();
        assertThat(items.size(), is(0));
        assertConnectionEncrypted(items);

        receivers.forEach(AbstractClient::stop);
    }

    public void doTestFilterConnectionsByUser() throws Exception {
        Destination queue = Destination.queue("queue-via-web-connections-users");
        consoleWebPage.createAddressesWebConsole(queue);
        consoleWebPage.openConnectionsPageWebConsole();

        KeycloakCredentials pavel = new KeycloakCredentials("pavel", "enmasse");
        createUser(defaultAddressSpace, pavel.getUsername(), pavel.getPassword());
        List<AbstractClient> receiversPavel = null;
        List<AbstractClient> receiversTest = null;
        try {
            int receiversBatch1 = 5;
            int receiversBatch2 = 10;
            receiversPavel = attachReceivers(queue, receiversBatch1, pavel.getUsername(), pavel.getPassword());
            receiversTest = attachReceivers(queue, receiversBatch2);
            assertThat(consoleWebPage.getConnectionItems().size(), is(receiversBatch1 + receiversBatch2));

            consoleWebPage.addConnectionsFilter(FilterType.USER, username);
            List<ConnectionWebItem> items = consoleWebPage.getConnectionItems();
            assertThat(items.size(), is(receiversBatch2));
            assertConnectionUsers(items, username);

            consoleWebPage.addConnectionsFilter(FilterType.USER, pavel.getUsername());
            assertThat(consoleWebPage.getConnectionItems().size(), is(0));

            consoleWebPage.removeFilterByUser(username);
            items = consoleWebPage.getConnectionItems();
            assertThat(items.size(), is(receiversBatch1));
            assertConnectionUsers(items, pavel.getUsername());

            consoleWebPage.clearAllFilters();
            assertThat(consoleWebPage.getConnectionItems().size(), is(receiversBatch1 + receiversBatch2));

        } catch (Exception ex) {
            removeUser(defaultAddressSpace, pavel.getUsername());
            receiversTest.forEach(AbstractClient::stop);
            receiversPavel.forEach(AbstractClient::stop);
        }

    }

    public void doTestSortConnectionsByHostname() throws Exception {
        int addressCount = 2;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        consoleWebPage.openConnectionsPageWebConsole();

        List<AbstractClient> clients = attachClients(addresses);

        consoleWebPage.sortItems(SortType.HOSTNAME, true);
        assertSorted(consoleWebPage.getConnectionItems(), Comparator.comparing(ConnectionWebItem::getName));

        consoleWebPage.sortItems(SortType.HOSTNAME, false);
        assertSorted(consoleWebPage.getConnectionItems(), true, Comparator.comparing(ConnectionWebItem::getName));

        clients.forEach(AbstractClient::stop);
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
                AbstractClient send = new RheaClientSender();
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
                AbstractClient rec = new RheaClientReceiver();
                rec.setArguments(arguments);
                rec.runAsync();
                receivers.add(rec);
            }
        }

        return receivers;
    }

    private List<AbstractClient> attachReceivers(Destination destination, int receiverCount) throws Exception {
        return attachReceivers(destination, receiverCount, username, password);
    }

    private List<AbstractClient> attachReceivers(Destination destination, int receiverCount, String username, String password) throws Exception {
        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getRouteEndpoint(defaultAddressSpace).toString());
        arguments.put(Argument.TIMEOUT, "180");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.ADDRESS, destination.getAddress());

        List<AbstractClient> receivers = new ArrayList<>();
        for (int i = 0; i < receiverCount; i++) {
            RheaClientReceiver rec = new RheaClientReceiver();
            rec.setArguments(arguments);
            rec.runAsync();
            receivers.add(rec);
        }

        Thread.sleep(15000); //wait for attached
        return receivers;
    }

    private List<AbstractClient> attachClients(List<Destination> destinations) throws Exception {
        List<AbstractClient> clients = new ArrayList<>();
        for (Destination destination : destinations) {
            clients.add(attachConnector(destination, 1, 6, 1));
            clients.add(attachConnector(destination, 1, 4, 4));
            clients.add(attachConnector(destination, 1, 1, 6));
        }

        Thread.sleep(10000);

        return clients;
    }

    private AbstractClient attachConnector(Destination destination, int connectionCount,
                                           int senderCount, int receiverCount) throws Exception {

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getRouteEndpoint(defaultAddressSpace).toString());
        arguments.put(Argument.TIMEOUT, "120");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.OBJECT_CONTROL, "CESR");
        arguments.put(Argument.ADDRESS, destination.getAddress());
        arguments.put(Argument.COUNT, Integer.toString(connectionCount));
        arguments.put(Argument.SENDER_COUNT, Integer.toString(senderCount));
        arguments.put(Argument.RECEIVER_COUNT, Integer.toString(receiverCount));

        AbstractClient cli = new RheaClientConnector();
        cli.setArguments(arguments);
        cli.runAsync();

        return cli;
    }

    private void assertAddressType(List<AddressWebItem> allItems, AddressType type) {
        assertThat(getAddressProperty(allItems, (item -> item.getType().contains(type.toString()))).size(), is(allItems.size()));
    }

    private void assertAddressName(List<AddressWebItem> allItems, String subString) {
        assertThat(getAddressProperty(allItems, (item -> item.getName().contains(subString))).size(), is(allItems.size()));
    }

    private void assertConnectionEncrypted(List<ConnectionWebItem> allItems) {
        assertThat(getConnectionProperty(allItems, (item -> item.isEncrypted())).size(), is(allItems.size()));
    }

    private void assertConnectionUnencrypted(List<ConnectionWebItem> allItems) {
        assertThat(getConnectionProperty(allItems, (item -> !item.isEncrypted())).size(), is(allItems.size()));
    }

    private void assertConnectionUsers(List<ConnectionWebItem> allItems, String userName) {
        assertThat(getConnectionProperty(allItems, (item -> item.getType().contains(userName))).size(), is(allItems.size()));
    }

    private List<ConnectionWebItem> getConnectionProperty(List<ConnectionWebItem> allItems, Predicate<ConnectionWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }

    private List<AddressWebItem> getAddressProperty(List<AddressWebItem> allItems, Predicate<AddressWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }
}
