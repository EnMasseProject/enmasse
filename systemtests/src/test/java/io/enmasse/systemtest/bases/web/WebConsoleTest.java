/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.rhea.RheaClientConnector;
import io.enmasse.systemtest.selenium.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public abstract class WebConsoleTest extends TestBaseWithShared implements ISeleniumProvider {

    private static Logger log = CustomLogger.getLogger();
    private static SeleniumProvider selenium = new SeleniumProvider();
    private List<AbstractClient> clientsList;


    private ConsoleWebPage consoleWebPage;

    @AfterAll
    public static void TearDownDrivers() {
        selenium.tearDownDrivers();
    }

    @BeforeEach
    public void setUpWebConsoleTests() throws Exception {
        if (selenium.getDriver() == null)
            selenium.setupDriver(environment, kubernetes, buildDriver());
        else
            selenium.clearScreenShots();
        super.setAddresses(sharedAddressSpace);
    }

    @AfterEach
    public void tearDownWebConsoleTests(ExtensionContext context) throws Exception {
        if (context.getExecutionException().isPresent()) { //test failed
            selenium.onFailed(context);
        }
        if (clientsList != null) {
            stopClients(clientsList);
            clientsList.clear();
        }
    }

    //============================================================================================
    //============================ do test methods ===============================================
    //============================================================================================

    protected void doTestCreateDeleteAddress(Destination destination) throws Exception {
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressWebConsole(destination);
        consoleWebPage.deleteAddressWebConsole(destination);
        assertWaitForValue(0, () -> consoleWebPage.getResultsCount(), new TimeoutBudget(20, TimeUnit.SECONDS));
    }

    protected void doTestAddressStatus(Destination destination) throws Exception {
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressWebConsole(destination, false);
        assertThat("Console failed, expected PENDING or READY state",
                consoleWebPage.getAddressItem(destination).getStatus(),
                either(is(AddressStatus.PENDING)).or(is(AddressStatus.READY)));

        TestUtils.waitForDestinationsReady(addressApiClient, sharedAddressSpace,
                new TimeoutBudget(5, TimeUnit.MINUTES), destination);

        assertEquals(AddressStatus.READY, consoleWebPage.getAddressItem(destination).getStatus(),
                "Console failed, expected READY state");
    }

    protected void doTestFilterAddressesByType() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
                consoleWebPage.getAddressItems().size(), is(addressCount));

        consoleWebPage.addAddressesFilter(FilterType.TYPE, AddressType.QUEUE.toString());
        List<AddressWebItem> items = consoleWebPage.getAddressItems();
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount / 2),
                items.size(), is(addressCount / 2)); //assert correct count
        assertAddressType("Console failed, does not contains only address type queue",
                items, AddressType.QUEUE); //assert correct type

        consoleWebPage.removeFilterByType(AddressType.QUEUE.toString());
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
                consoleWebPage.getAddressItems().size(), is(addressCount));

        consoleWebPage.addAddressesFilter(FilterType.TYPE, AddressType.TOPIC.toString());
        items = consoleWebPage.getAddressItems();
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount / 2),
                items.size(), is(addressCount / 2)); //assert correct count
        assertAddressType("Console failed, does not contains only address type topic",
                items, AddressType.TOPIC); //assert correct type

        consoleWebPage.removeFilterByType(AddressType.TOPIC.toString());
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
                consoleWebPage.getAddressItems().size(), is(addressCount));
    }

    protected void doTestFilterAddressesByName() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        String subText = "web";
        consoleWebPage.addAddressesFilter(FilterType.NAME, subText);
        List<AddressWebItem> items = consoleWebPage.getAddressItems();
        assertEquals(addressCount, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount));
        assertAddressName("Console failed, does not contain addresses contain " + subText, items, subText);

        subText = "via";
        consoleWebPage.addAddressesFilter(FilterType.NAME, subText);
        items = consoleWebPage.getAddressItems();
        assertEquals(addressCount, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount));
        assertAddressName("Console failed, does not contain addresses contain " + subText, items, subText);

        subText = "web";
        consoleWebPage.removeFilterByName(subText);
        items = consoleWebPage.getAddressItems();
        assertEquals(addressCount, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount));
        assertAddressName("Console failed, does not contain addresses contain " + subText, items, subText);

        subText = "queue";
        consoleWebPage.addAddressesFilter(FilterType.NAME, subText);
        items = consoleWebPage.getAddressItems();
        assertEquals(addressCount / 2, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount / 2));
        assertAddressName("Console failed, does not contain addresses contain " + subText, items, subText);

        consoleWebPage.clearAllFilters();
        assertEquals(addressCount, consoleWebPage.getAddressItems().size(),
                String.format("Console failed, does not contain %d addresses", addressCount));
    }

    protected void doTestSortAddressesByName() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));

        consoleWebPage.sortItems(SortType.NAME, true);
        assertSorted("Console failed, items are not sorted by name asc", consoleWebPage.getAddressItems());

        consoleWebPage.sortItems(SortType.NAME, false);
        assertSorted("Console failed, items are not sorted by name desc", consoleWebPage.getAddressItems(), true);
    }

    protected void doTestSortAddressesByClients() throws Exception {
        int addressCount = 4;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        consoleWebPage.openAddressesPageWebConsole();

        clientsList = attachReceivers(addresses);

        Thread.sleep(15000);

        consoleWebPage.sortItems(SortType.RECEIVERS, true);
        assertSorted("Console failed, items are not sorted by count of receivers asc",
                consoleWebPage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getReceiversCount));

        consoleWebPage.sortItems(SortType.RECEIVERS, false);
        assertSorted("Console failed, items are not sorted by count of receivers desc",
                consoleWebPage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getReceiversCount));

        stopClients(clientsList);

        clientsList = attachSenders(addresses);

        Thread.sleep(15000);

        consoleWebPage.sortItems(SortType.SENDERS, true);
        assertSorted("Console failed, items are not sorted by count of senders asc",
                consoleWebPage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getSendersCount));

        consoleWebPage.sortItems(SortType.SENDERS, false);
        assertSorted("Console failed, items are not sorted by count of senders desc",
                consoleWebPage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getSendersCount));

        stopClients(clientsList);
    }

    protected void doTestSortConnectionsBySenders() throws Exception {
        int addressCount = 2;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        consoleWebPage.openConnectionsPageWebConsole();

        clientsList = attachClients(addresses);

        consoleWebPage.sortItems(SortType.SENDERS, true);
        assertSorted("Console failed, items are not sorted by count of senders asc",
                consoleWebPage.getConnectionItems(), Comparator.comparingInt(ConnectionWebItem::getSendersCount));

        consoleWebPage.sortItems(SortType.SENDERS, false);
        assertSorted("Console failed, items are not sorted by count of senders desc",
                consoleWebPage.getConnectionItems(), true, Comparator.comparingInt(ConnectionWebItem::getSendersCount));
    }

    protected void doTestSortConnectionsByReceivers() throws Exception {
        int addressCount = 2;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        consoleWebPage.openConnectionsPageWebConsole();

        clientsList = attachClients(addresses);

        consoleWebPage.sortItems(SortType.RECEIVERS, true);
        assertSorted("Console failed, items are not sorted by count of receivers asc",
                consoleWebPage.getConnectionItems(), Comparator.comparingInt(ConnectionWebItem::getReceiversCount));

        consoleWebPage.sortItems(SortType.RECEIVERS, false);
        assertSorted("Console failed, items are not sorted by count of receivers desc",
                consoleWebPage.getConnectionItems(), true, Comparator.comparingInt(ConnectionWebItem::getReceiversCount));
    }


    protected void doTestFilterConnectionsByEncrypted() throws Exception {
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        Destination queue = Destination.queue("queue-via-web-connections-encrypted", getDefaultPlan(AddressType.QUEUE));
        consoleWebPage.createAddressesWebConsole(queue);
        consoleWebPage.openConnectionsPageWebConsole();

        int receiverCount = 5;
        clientsList = attachReceivers(queue, receiverCount);

        consoleWebPage.addConnectionsFilter(FilterType.ENCRYPTED, "encrypted");
        List<ConnectionWebItem> items = consoleWebPage.getConnectionItems();
        assertThat(String.format("Console failed, does not contain %d connections", receiverCount),
                items.size(), is(receiverCount));
        assertConnectionUnencrypted("Console failed, does not show only Encrypted connections", items);

        consoleWebPage.clearAllFilters();
        assertThat(consoleWebPage.getConnectionItems().size(), is(receiverCount));

        consoleWebPage.addConnectionsFilter(FilterType.ENCRYPTED, "unencrypted");
        items = consoleWebPage.getConnectionItems();
        assertThat(String.format("Console failed, does not contain %d connections", 0),
                items.size(), is(0));
        assertConnectionEncrypted("Console failed, does not show only Encrypted connections", items);
    }

    protected void doTestFilterConnectionsByUser() throws Exception {
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        Destination queue = Destination.queue("queue-via-web-connections-users", getDefaultPlan(AddressType.QUEUE));
        consoleWebPage.createAddressesWebConsole(queue);
        consoleWebPage.openConnectionsPageWebConsole();

        KeycloakCredentials pavel = new KeycloakCredentials("pavel", "enmasse");
        createUser(sharedAddressSpace, pavel.getUsername(), pavel.getPassword());
        List<AbstractClient> receiversPavel = null;
        List<AbstractClient> receiversTest = null;
        try {
            int receiversBatch1 = 5;
            int receiversBatch2 = 10;
            receiversPavel = attachReceivers(queue, receiversBatch1, pavel.getUsername(), pavel.getPassword());
            receiversTest = attachReceivers(queue, receiversBatch2);
            assertThat(String.format("Console failed, does not contain %d connections", receiversBatch1 + receiversBatch2),
                    consoleWebPage.getConnectionItems().size(), is(receiversBatch1 + receiversBatch2));

            consoleWebPage.addConnectionsFilter(FilterType.USER, username);
            List<ConnectionWebItem> items = consoleWebPage.getConnectionItems();
            assertThat(String.format("Console failed, does not contain %d connections", receiversBatch2),
                    items.size(), is(receiversBatch2));
            assertConnectionUsers("Console failed, does not contain connections for user " + username,
                    items, username);

            consoleWebPage.addConnectionsFilter(FilterType.USER, pavel.getUsername());
            assertThat(String.format("Console failed, does not contain %d connections", 0),
                    consoleWebPage.getConnectionItems().size(), is(0));

            consoleWebPage.removeFilterByUser(username);
            items = consoleWebPage.getConnectionItems();
            assertThat(String.format("Console failed, does not contain %d connections", receiversBatch1),
                    items.size(), is(receiversBatch1));
            assertConnectionUsers("Console failed, does not contain connections for user " + pavel.getUsername(),
                    items, pavel.getUsername());

            consoleWebPage.clearAllFilters();
            assertThat(String.format("Console failed, does not contain %d connections", receiversBatch1 + receiversBatch2),
                    consoleWebPage.getConnectionItems().size(), is(receiversBatch1 + receiversBatch2));
        } catch (Exception ex) {
            throw ex;
        } finally {
            removeUser(sharedAddressSpace, pavel.getUsername());
            stopClients(receiversTest);
            stopClients(receiversPavel);
        }

    }

    protected void doTestFilterConnectionsByHostname() throws Exception {
        int addressCount = 2;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        consoleWebPage.openConnectionsPageWebConsole();

        clientsList = attachClients(addresses);

        String hostname = consoleWebPage.getConnectionItems().get(0).getName();

        consoleWebPage.addConnectionsFilter(FilterType.HOSTNAME, hostname);
        assertThat(String.format("Console failed, does not contain %d connections", 1),
                consoleWebPage.getConnectionItems().size(), is(1));

        consoleWebPage.clearAllFilters();
        assertThat(String.format("Console failed, does not contain %d connections", 6),
                consoleWebPage.getConnectionItems().size(), is(6));
    }

    protected void doTestSortConnectionsByHostname() throws Exception {
        int addressCount = 2;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
        consoleWebPage.openConnectionsPageWebConsole();

        clientsList = attachClients(addresses);

        consoleWebPage.sortItems(SortType.HOSTNAME, true);
        assertSorted("Console failed, items are not sorted by hostname asc",
                consoleWebPage.getConnectionItems(), Comparator.comparing(ConnectionWebItem::getName));

        consoleWebPage.sortItems(SortType.HOSTNAME, false);
        assertSorted("Console failed, items are not sorted by hostname desc",
                consoleWebPage.getConnectionItems(), true, Comparator.comparing(ConnectionWebItem::getName));
    }

    protected void doTestFilterConnectionsByContainerId() throws Exception {
        int connectionCount = 5;

        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        Destination dest = Destination.queue("queue-via-web", getDefaultPlan(AddressType.QUEUE));
        consoleWebPage.createAddressWebConsole(dest);
        consoleWebPage.openConnectionsPageWebConsole();

        clientsList = new ArrayList<>();
        clientsList.add(attachConnector(dest, connectionCount, 1, 1));
        selenium.waitUntilPropertyPresent(60, connectionCount, () -> consoleWebPage.getConnectionItems().size());

        String containerID = consoleWebPage.getConnectionItems().get(0).getContainerID();

        consoleWebPage.addConnectionsFilter(FilterType.CONTAINER, containerID);
        assertThat(String.format("Console failed, does not contain %d connections", 1),
                consoleWebPage.getConnectionItems().size(), is(1));

        consoleWebPage.clearAllFilters();
        assertThat(String.format("Console failed, does not contain %d connections", 5),
                consoleWebPage.getConnectionItems().size(), is(5));
    }

    protected void doTestSortConnectionsByContainerId() throws Exception {
        int connectionCount = 5;

        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        Destination dest = Destination.queue("queue-via-web", getDefaultPlan(AddressType.QUEUE));
        consoleWebPage.createAddressWebConsole(dest);
        consoleWebPage.openConnectionsPageWebConsole();

        clientsList = new ArrayList<>();
        clientsList.add(attachConnector(dest, connectionCount, 1, 1));
        selenium.waitUntilPropertyPresent(60, connectionCount, () -> consoleWebPage.getConnectionItems().size());

        consoleWebPage.sortItems(SortType.CONTAINER_ID, true);
        assertSorted("Console failed, items are not sorted by containerID asc",
                consoleWebPage.getConnectionItems(), Comparator.comparing(ConnectionWebItem::getContainerID));

        consoleWebPage.sortItems(SortType.CONTAINER_ID, false);
        assertSorted("Console failed, items are not sorted by containerID desc",
                consoleWebPage.getConnectionItems(), true, Comparator.comparing(ConnectionWebItem::getContainerID));
    }

    protected void doTestMessagesMetrics() throws Exception {
        int msgCount = 19;
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        Destination dest = Destination.queue("queue-via-web", getDefaultPlan(AddressType.QUEUE));
        consoleWebPage.createAddressWebConsole(dest);
        consoleWebPage.openAddressesPageWebConsole();

        AmqpClient client = amqpClientFactory.createQueueClient(sharedAddressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);
        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        int sent = client.sendMessages(dest.getAddress(), msgBatch, 1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES);
        selenium.waitUntilPropertyPresent(60, msgCount, () -> consoleWebPage.getAddressItem(dest).getMessagesIn());
        assertEquals(sent, consoleWebPage.getAddressItem(dest).getMessagesIn(),
                String.format("Console failed, does not contain %d messagesIN", sent));

        selenium.waitUntilPropertyPresent(60, msgCount, () -> consoleWebPage.getAddressItem(dest).getMessagesStored());
        assertEquals(msgCount, consoleWebPage.getAddressItem(dest).getMessagesStored(),
                String.format("Console failed, does not contain %d messagesStored", msgCount));

        int received = client.recvMessages(dest.getAddress(), msgCount).get(1, TimeUnit.MINUTES).size();
        selenium.waitUntilPropertyPresent(60, msgCount, () -> consoleWebPage.getAddressItem(dest).getMessagesOut());
        assertEquals(received, consoleWebPage.getAddressItem(dest).getMessagesOut(),
                String.format("Console failed, does not contain %d messagesOUT", received));

    }

    protected void doTestClientsMetrics() throws Exception {
        int senderCount = 5;
        int receiverCount = 10;
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
        consoleWebPage.openWebConsolePage();
        Destination dest = Destination.queue("queue-via-web", getDefaultPlan(AddressType.QUEUE));
        consoleWebPage.createAddressWebConsole(dest);
        consoleWebPage.openAddressesPageWebConsole();

        AbstractClient client = new RheaClientConnector();
        try {
            client = attachConnector(dest, 1, senderCount, receiverCount);
            selenium.waitUntilPropertyPresent(60, senderCount, consoleWebPage.getAddressItem(dest)::getSendersCount);

            assertEquals(10, consoleWebPage.getAddressItem(dest).getReceiversCount(),
                    String.format("Console failed, does not contain %d receivers", 10));
            assertEquals(5, consoleWebPage.getAddressItem(dest).getSendersCount(),
                    String.format("Console failed, does not contain %d senders", 5));
        } finally {
            client.stop();
        }
    }

    protected void doTestCannotCreateAddresses() throws Exception {
        Destination destination = Destination.queue("authz-queue", getDefaultPlan(AddressType.QUEUE));
        KeycloakCredentials monitorUser = new KeycloakCredentials("monitor_user_test_1", "monitorPa55");

        getKeycloakClient().createUser(sharedAddressSpace.getName(),
                monitorUser.getUsername(), monitorUser.getPassword(), Group.MONITOR.toString());

        consoleWebPage = new ConsoleWebPage(selenium,
                getConsoleRoute(sharedAddressSpace),
                addressApiClient, sharedAddressSpace, monitorUser.getUsername(), monitorUser.getPassword());
        consoleWebPage.openWebConsolePage();
        consoleWebPage.openAddressesPageWebConsole();

        assertElementDisabled("Console failed, create button is enabled for user " + monitorUser.getUsername(),
                consoleWebPage.getCreateButton());
    }

    protected void doTestCannotDeleteAddresses() throws Exception {
        Destination destination = Destination.queue("test-cannot-delete-address", getDefaultPlan(AddressType.QUEUE));
        KeycloakCredentials monitorUser = new KeycloakCredentials("monitor_user_test_2", "monitorPa55");

        getKeycloakClient().createUser(sharedAddressSpace.getName(),
                monitorUser.getUsername(), monitorUser.getPassword(), Group.MONITOR.toString());
        setAddresses(destination);

        consoleWebPage = new ConsoleWebPage(selenium,
                getConsoleRoute(sharedAddressSpace),
                addressApiClient, sharedAddressSpace, monitorUser.getUsername(), monitorUser.getPassword());
        consoleWebPage.openWebConsolePage();
        consoleWebPage.openAddressesPageWebConsole();

        assertElementDisabled("Console failed, delete button is enabled for user " + monitorUser.getUsername(),
                consoleWebPage.getRemoveButton());
    }

    protected void doTestViewAddresses() throws Exception {
        Destination allowedDestination = Destination.queue("test-view-queue", getDefaultPlan(AddressType.QUEUE));
        Destination notAllowedDestination = Destination.queue("test-not-view-queue", getDefaultPlan(AddressType.QUEUE));
        String username = "view_user_addresses";
        String password = "viewPa55";

        prepareViewItemTest(username, password, allowedDestination, notAllowedDestination);

        consoleWebPage.openWebConsolePage();
        consoleWebPage.openAddressesPageWebConsole();

        assertWaitForValue(1, () -> consoleWebPage.getResultsCount());
        selenium.refreshPage();
        assertWaitForValue(1, () -> consoleWebPage.getResultsCount());

        assertThat(String.format("Console failed, does not contain %d addresses", 1),
                consoleWebPage.getAddressItems().size(), is(1));
        assertViewOnlyUsersAddresses(String.format("Console failed, user %s see not only his addresses", username),
                "view_test-view-queue", consoleWebPage.getAddressItems());
    }

    protected void doTestViewConnections() throws Exception {
        Destination destination = Destination.queue("test-queue-view-connections", getDefaultPlan(AddressType.QUEUE));
        String username = "view_user_connections";
        String password = "viewPa55";
        prepareViewItemTest(username, password, destination, null);

        consoleWebPage.openWebConsolePage();
        consoleWebPage.openConnectionsPageWebConsole();

        int connections = 5;
        AbstractClient noUsersConnections = attachConnector(destination, connections, 1, 0);
        AbstractClient usersConnections = attachConnector(sharedAddressSpace, destination,
                connections, 1, 0, "view_user_connections", "viewPa55");
        selenium.waitUntilPropertyPresent(60, 5, () -> consoleWebPage.getConnectionItems().size());

        assertWaitForValue(connections, () -> consoleWebPage.getResultsCount());
        selenium.refreshPage();
        assertWaitForValue(connections, () -> consoleWebPage.getResultsCount());

        log.info("Check if connection count is {}", connections);
        assertEquals(connections, consoleWebPage.getConnectionItems().size(),
                String.format("Console failed, does not contain %d connections", connections));
        assertViewOnlyUsersConnections(String.format("Console failed, user %s see not only his connections", username),
                "view_user_connections", consoleWebPage.getConnectionItems());

        noUsersConnections.stop();
        usersConnections.stop();
    }

    protected void doTestViewAddressesWildcards() throws Exception {
        List<Destination> addresses = getAddressesWildcard();
        setAddresses(addresses.toArray(new Destination[0]));

        List<KeycloakCredentials> users = createUsersWildcard(sharedAddressSpace, "view");

        for (KeycloakCredentials user : users) {
            consoleWebPage = new ConsoleWebPage(selenium,
                    getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, user.getUsername(), user.getPassword());
            consoleWebPage.openWebConsolePage();
            consoleWebPage.openAddressesPageWebConsole();

            assertViewOnlyUsersAddresses(String.format("Console failed, user %s see not only his addresses", user.getUsername()),
                    user.getUsername().replace("user_", ""), consoleWebPage.getAddressItems());
        }
    }

    protected void doTestCanOpenConsolePage(String username, String password) throws Exception {
        try {
            consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(sharedAddressSpace), addressApiClient, sharedAddressSpace, username, password);
            consoleWebPage.openWebConsolePage();
            log.info(String.format("User %s successfully authenticated", username));
        } catch (IllegalAccessException | org.openqa.selenium.UnhandledAlertException ex) {
            selenium.tearDownDrivers();
            log.info(String.format("User %s can't authenticate", username));
            throw new IllegalAccessException();
        }
    }

    //============================================================================================
    //============================ Help methods ==================================================
    //============================================================================================


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


    private void assertAddressType(String message, List<AddressWebItem> allItems, AddressType type) {
        assertThat(message, getAddressProperty(allItems, (item -> item.getType().contains(type.toString()))).size(), is(allItems.size()));
    }

    private void assertAddressName(String message, List<AddressWebItem> allItems, String subString) {
        assertThat(message, getAddressProperty(allItems, (item -> item.getName().contains(subString))).size(), is(allItems.size()));
    }

    private void assertConnectionEncrypted(String message, List<ConnectionWebItem> allItems) {
        assertThat(message, getConnectionProperty(allItems, (item -> item.isEncrypted())).size(), is(allItems.size()));
    }

    private void assertConnectionUnencrypted(String message, List<ConnectionWebItem> allItems) {
        assertThat(message, getConnectionProperty(allItems, (item -> !item.isEncrypted())).size(), is(allItems.size()));
    }

    private void assertConnectionUsers(String message, List<ConnectionWebItem> allItems, String userName) {
        assertThat(message, getConnectionProperty(allItems, (item -> item.getUser().contains(userName))).size(), is(allItems.size()));
    }

    private List<ConnectionWebItem> getConnectionProperty(List<ConnectionWebItem> allItems, Predicate<ConnectionWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }

    private List<AddressWebItem> getAddressProperty(List<AddressWebItem> allItems, Predicate<AddressWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }

    private void assertElementDisabled(String message, WebElement element) {
        assertFalse(element.isEnabled(), message);
    }

    private void assertElementEnabled(String message, WebElement element) {
        assertTrue(element.isEnabled(), message);
    }

    private void assertViewOnlyUsersAddresses(String message, String group, List<AddressWebItem> addresses) {
        log.info("Check if user {} see only addresses he has rights on", username);
        if (addresses == null || addresses.size() == 0)
            fail("No address items in console for group " + group);
        for (AddressWebItem item : addresses) {
            if (group.contains("*")) {
                String rights = username.replace("user_view_", "")
                        .replace("*", "")
                        .replace("#", "");
                assertTrue(rights.equals("") || item.getName().contains(rights), message);
            } else {
                assertTrue(item.getName().equals(group.replace("view_", "")), message);
            }
        }
    }

    private void assertViewOnlyUsersConnections(String message, String username, List<ConnectionWebItem> connections) {
        log.info("Check if user {} see only his connections", username);
        if (connections == null || connections.size() == 0)
            fail("No connection items in console under user " + username);
        for (ConnectionWebItem conn : connections) {
            assertTrue(conn.getUser().equals(username), message);
        }
    }

    private void prepareViewItemTest(String username, String password, Destination allowedAddress,
                                     Destination noAllowedAddress) throws Exception {
        prepareAddress(allowedAddress, noAllowedAddress);

        KeycloakCredentials monitorUser = new KeycloakCredentials(username, password);
        getKeycloakClient().createUser(sharedAddressSpace.getName(),
                monitorUser.getUsername(), monitorUser.getPassword(),
                "view_" + allowedAddress.getAddress(), "send_*");

        consoleWebPage = new ConsoleWebPage(selenium,
                getConsoleRoute(sharedAddressSpace),
                addressApiClient, sharedAddressSpace, monitorUser.getUsername(), monitorUser.getPassword());
    }

    private void prepareAddress(Destination... dest) throws Exception {
        setAddresses(Arrays.stream(dest).filter(Objects::nonNull).toArray(Destination[]::new));
    }
}
