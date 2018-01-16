package io.enmasse.systemtest.web;


import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.executor.client.AbstractClient;
import io.enmasse.systemtest.executor.client.rhea.RheaClientConnector;
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
import java.util.concurrent.TimeUnit;
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

        stopClients(receivers);

        List<AbstractClient> senders = attachSenders(addresses);

        Thread.sleep(15000);

        consoleWebPage.sortItems(SortType.SENDERS, true);
        assertSorted(consoleWebPage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getSendersCount));

        consoleWebPage.sortItems(SortType.SENDERS, false);
        assertSorted(consoleWebPage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getSendersCount));

        stopClients(senders);
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

        stopClients(senders);
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

        stopClients(clients);
    }


    public void doTestFilterConnectionsByEncrypted() throws Exception {
        Destination queue = Destination.queue("queue-via-web-connections-encrypted");
        consoleWebPage.createAddressesWebConsole(queue);
        consoleWebPage.openConnectionsPageWebConsole();

        List<AbstractClient> receivers = null;
        int receiverCount = 5;
        try {
            receivers = attachReceivers(queue, receiverCount);

            consoleWebPage.addConnectionsFilter(FilterType.ENCRYPTED, "encrypted");
            List<ConnectionWebItem> items = consoleWebPage.getConnectionItems();
            assertThat(items.size(), is(receiverCount));
            assertConnectionUnencrypted(items);

            consoleWebPage.clearAllFilters();
            assertThat(consoleWebPage.getConnectionItems().size(), is(receiverCount));

            consoleWebPage.addConnectionsFilter(FilterType.ENCRYPTED, "unencrypted");
            items = consoleWebPage.getConnectionItems();
            assertThat(items.size(), is(0));
            assertConnectionEncrypted(items);

        } catch (Exception ex) {
            throw ex;
        } finally {
            stopClients(receivers);

        }
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
            throw ex;
        } finally {
            removeUser(defaultAddressSpace, pavel.getUsername());
            stopClients(receiversTest);
            stopClients(receiversPavel);
        }

    }

    public void doTestFilterConnectionsByHostname() throws Exception {
        int addressCount = 2;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        consoleWebPage.openConnectionsPageWebConsole();

        List<AbstractClient> clients = attachClients(addresses);

        String hostname = consoleWebPage.getConnectionItems().get(0).getName();

        consoleWebPage.addConnectionsFilter(FilterType.HOSTNAME, hostname);
        assertThat(consoleWebPage.getConnectionItems().size(), is(1));

        consoleWebPage.clearAllFilters();
        assertThat(consoleWebPage.getConnectionItems().size(), is(6));

        stopClients(clients);
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

        stopClients(clients);
    }

    public void doTestFilterConnectionsByContainerId() throws Exception {
        int connectionCount = 5;

        Destination dest = Destination.queue("queue-via-web");
        consoleWebPage.createAddressWebConsole(dest);
        consoleWebPage.openConnectionsPageWebConsole();

        AbstractClient client = attachConnector(dest, connectionCount, 1, 1);
        selenium.waitUntilPropertyPresent(60, connectionCount, () -> consoleWebPage.getConnectionItems().size());

        String containerID = consoleWebPage.getConnectionItems().get(0).getContainerID();

        consoleWebPage.addConnectionsFilter(FilterType.CONTAINER, containerID);
        assertThat(consoleWebPage.getConnectionItems().size(), is(1));

        consoleWebPage.clearAllFilters();
        assertThat(consoleWebPage.getConnectionItems().size(), is(5));

        client.stop();
    }

    public void doTestSortConnectionsByContainerId() throws Exception {
        int connectionCount = 5;

        Destination dest = Destination.queue("queue-via-web");
        consoleWebPage.createAddressWebConsole(dest);
        consoleWebPage.openConnectionsPageWebConsole();

        AbstractClient client = attachConnector(dest, connectionCount, 1, 1);
        selenium.waitUntilPropertyPresent(60, connectionCount, () -> consoleWebPage.getConnectionItems().size());

        consoleWebPage.sortItems(SortType.CONTAINER_ID, true);
        assertSorted(consoleWebPage.getConnectionItems(), Comparator.comparing(ConnectionWebItem::getContainerID));

        consoleWebPage.sortItems(SortType.CONTAINER_ID, false);
        assertSorted(consoleWebPage.getConnectionItems(), true, Comparator.comparing(ConnectionWebItem::getContainerID));

        client.stop();
    }

    public void doTestMessagesMetrics() throws Exception {
        int msgCount = 19;
        Destination dest = Destination.queue("queue-via-web");
        consoleWebPage.createAddressWebConsole(dest);
        consoleWebPage.openAddressesPageWebConsole();

        AmqpClient client = amqpClientFactory.createQueueClient(defaultAddressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);
        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        int sent = client.sendMessages(dest.getAddress(), msgBatch, 1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES);
        selenium.waitUntilPropertyPresent(60, msgCount, () -> consoleWebPage.getAddressItem(dest).getMessagesIn());
        assertEquals(sent, consoleWebPage.getAddressItem(dest).getMessagesIn());

        selenium.waitUntilPropertyPresent(60, msgCount, () -> consoleWebPage.getAddressItem(dest).getMessagesStored());
        assertEquals(msgCount, consoleWebPage.getAddressItem(dest).getMessagesStored());

        int received = client.recvMessages(dest.getAddress(), msgCount).get(1, TimeUnit.MINUTES).size();
        selenium.waitUntilPropertyPresent(60, msgCount, () -> consoleWebPage.getAddressItem(dest).getMessagesOut());
        assertEquals(received, consoleWebPage.getAddressItem(dest).getMessagesOut());

    }

    public void doTestClientsMetrics() throws Exception {
        int senderCount = 5;
        int receiverCount = 10;
        Destination dest = Destination.queue("queue-via-web");
        consoleWebPage.createAddressWebConsole(dest);
        consoleWebPage.openAddressesPageWebConsole();

        AbstractClient client = new RheaClientConnector();
        try {
            client = attachConnector(dest, 1, senderCount, receiverCount);
            selenium.waitUntilPropertyPresent(60, senderCount, () -> consoleWebPage.getAddressItem(dest).getSendersCount());

            assertEquals(10, consoleWebPage.getAddressItem(dest).getReceiversCount());
            assertEquals(5, consoleWebPage.getAddressItem(dest).getSendersCount());
        } finally {
            client.stop();
        }
    }

    //============================================================================================
    //============================ Help methods ==================================================
    //============================================================================================


    protected List<AbstractClient> attachClients(List<Destination> destinations) throws Exception {
        List<AbstractClient> clients = new ArrayList<>();
        for (Destination destination : destinations) {
            clients.add(attachConnector(destination, 1, 6, 1));
            clients.add(attachConnector(destination, 1, 4, 4));
            clients.add(attachConnector(destination, 1, 1, 6));
        }

        Thread.sleep(10000);

        return clients;
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
        assertThat(getConnectionProperty(allItems, (item -> item.getUser().contains(userName))).size(), is(allItems.size()));
    }

    private List<ConnectionWebItem> getConnectionProperty(List<ConnectionWebItem> allItems, Predicate<ConnectionWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }

    private List<AddressWebItem> getAddressProperty(List<AddressWebItem> allItems, Predicate<AddressWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }
}
