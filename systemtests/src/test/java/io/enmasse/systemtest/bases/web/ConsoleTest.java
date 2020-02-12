/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.web;


import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.clients.ClientUtils.ClientAttacher;
import io.enmasse.systemtest.isolated.Credentials;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.messagingclients.ExternalMessagingClient;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressStatus;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;
import io.enmasse.systemtest.model.addressspace.AddressSpaceType;
import io.enmasse.systemtest.platform.KubeCMDClient;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.selenium.resources.AddressWebItem;
import io.enmasse.systemtest.selenium.resources.ConnectionWebItem;
import io.enmasse.systemtest.selenium.resources.FilterType;
import io.enmasse.systemtest.selenium.resources.SortType;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.AuthServiceUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public abstract class ConsoleTest extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    SeleniumProvider selenium = SeleniumProvider.getInstance();
    private List<ExternalMessagingClient> clientsList;
    private ConsoleWebPage consolePage;

    @AfterEach
    public void tearDownWebConsoleTests(ExtensionContext context) throws Exception {
        if (clientsList != null) {
            getClientUtils().stopClients(clientsList, context);
            clientsList.clear();
        }
    }

    //============================================================================================
    //============================ do test methods for addressspace part==========================
    //============================================================================================

    protected void doTestOpen() throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.getAddressSpaceItems();
        consolePage.logout();
    }

    protected void doTestCreateDeleteAddressSpace(AddressSpace addressSpace) throws Exception {
        resourcesManager.addToAddressSpaces(addressSpace);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
        consolePage.deleteAddressSpace(addressSpace);
    }

    protected void doTestCreateAddrSpaceWithCustomAuthService() throws Exception {
        AuthenticationService standardAuth = AuthServiceUtils.createStandardAuthServiceObject("test-standard-authservice", true);
        resourcesManager.createAuthService(standardAuth);

        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-custom-auth")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName(standardAuth.getMetadata().getName())
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.addToAddressSpaces(addressSpace);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
    }

    protected void doTestViewAddressSpace() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-view-console")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        resourcesManager.createAddressSpace(addressSpace);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        waitUntilAddressSpaceActive(addressSpace);
        consolePage.deleteAddressSpace(addressSpace);
    }

    protected void doTestCreateAddrSpaceNonClusterAdmin() throws Exception {
        String namespace = "test-namespace";
        UserCredentials user = Credentials.userCredentials();
        try {
            KubeCMDClient.loginUser(user.getUsername(), user.getPassword());
            KubeCMDClient.createNamespace(namespace);

            AddressSpace addressSpace = new AddressSpaceBuilder()
                    .withNewMetadata()
                    .withName("test-addr-space-api")
                    .withNamespace(namespace)
                    .endMetadata()
                    .withNewSpec()
                    .withType(AddressSpaceType.BROKERED.toString())
                    .withPlan(AddressSpacePlans.BROKERED)
                    .withNewAuthenticationService()
                    .withName("standard-authservice")
                    .endAuthenticationService()
                    .endSpec()
                    .build();

            consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), user);
            consolePage.openConsolePage();
            consolePage.createAddressSpace(addressSpace);
            waitUntilAddressSpaceActive(addressSpace);
            consolePage.deleteAddressSpace(addressSpace);

        } finally {
            KubeCMDClient.loginUser(environment.getApiToken());
            KubeCMDClient.switchProject(environment.namespace());
            kubernetes.deleteNamespace(namespace);
        }
    }

    protected void doTestSwitchAddressSpacePlan() throws Exception {
        AddressSpace addressSpace = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("test-addr-space-api")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_MEDIUM)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.addToAddressSpaces(addressSpace);
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.createAddressSpace(addressSpace);
        waitUntilAddressSpaceActive(addressSpace);
        assertEquals(AddressSpacePlans.STANDARD_MEDIUM,
                resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getPlan());
        String currentConfig = resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getPlan();
        consolePage.switchAddressSpacePlan(addressSpace, AddressSpacePlans.STANDARD_UNLIMITED);
        AddressSpaceUtils.waitForAddressSpaceConfigurationApplied(addressSpace, currentConfig);
        AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
        assertEquals(AddressSpacePlans.STANDARD_UNLIMITED,
                resourcesManager.getAddressSpace(addressSpace.getMetadata().getName()).getSpec().getPlan());
    }

    protected void doTestFilterAddrSpace() throws Exception {
        int addressSpaceCount = 2;
        AddressSpace brokered = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("brokered-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        AddressSpace standard = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("standard-test-addr-space")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.STANDARD.toString())
                .withPlan(AddressSpacePlans.STANDARD_SMALL)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        resourcesManager.createAddressSpace(brokered, standard);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        assertThat("Console does not show all addressspaces", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount));

        consolePage.addFilter(FilterType.NAMESPACE, "blah");
        assertThat("Console should show empty list", consolePage.getAddressSpaceItems().size(), is(0));

        consolePage.removeAllFilters();

        consolePage.addFilter(FilterType.TYPE, AddressSpaceType.BROKERED.toString());
        assertThat("Console should show not empty list", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount / 2));

        consolePage.addFilter(FilterType.TYPE, AddressSpaceType.STANDARD.toString());
        assertThat("Console should show not empty list", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount / 2));

        consolePage.removeAllFilters();

        consolePage.addFilter(FilterType.NAME, "brokered");
        assertThat("Console should show not empty list", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount / 2));

        consolePage.addFilter(FilterType.NAME, "standard");
        assertThat("Console should show not empty list", consolePage.getAddressSpaceItems().size(), is(addressSpaceCount));

        consolePage.deleteSelectedAddressSpaces(brokered, standard);
        assertThat("Console should show empty list", consolePage.getAddressSpaceItems().size(), is(0));
    }

    //============================================================================================
    //============================ do test methods for address part ==============================
    //============================================================================================

    protected void doTestCreateDeleteAddress(AddressSpace addressSpace, Address... destinations) throws Exception {
        Kubernetes.getInstance().getAddressClient().inNamespace(addressSpace.getMetadata().
                getNamespace()).list().getItems().forEach(address -> log.info("Add from list: " + address));
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        for (Address dest : destinations) {
            consolePage.createAddress(dest);
        }
        for (Address dest : destinations) {
            consolePage.deleteAddress(dest);
        }
    }

    protected void doTestAddressStatus(AddressSpace addressSpace, Address destination) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddress(destination, false);
        Thread.sleep(5_000);
        assertThat("Console failed, expected PENDING or READY state",
                consolePage.getAddressItem(destination).getStatus(),
                either(is(AddressStatus.PENDING)).or(is(AddressStatus.READY)));

        AddressUtils.waitForDestinationsReady(new TimeoutBudget(5, TimeUnit.MINUTES), destination);
        Thread.sleep(5000);
        assertEquals(AddressStatus.READY, consolePage.getAddressItem(destination).getStatus(),
                "Console failed, expected READY state");
    }

    protected void doTestFilterAddressesByType(AddressSpace addressSpace) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddressesAndWait(addresses.toArray(new Address[0]));
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
                consolePage.getAddressItems().size(), is(addressCount));

        consolePage.addFilter(FilterType.TYPE, AddressType.QUEUE.toString());
        List<AddressWebItem> items = consolePage.getAddressItems();
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount / 2),
                items.size(), is(addressCount / 2)); //assert correct count
        assertAddressType("Console failed, does not contains only address type queue",
                items, AddressType.QUEUE); //assert correct type

        consolePage.removeAddressFilter(FilterType.TYPE, AddressType.QUEUE.toString());
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
                consolePage.getAddressItems().size(), is(addressCount));

        consolePage.addFilter(FilterType.TYPE, AddressType.TOPIC.toString());
        items = consolePage.getAddressItems();
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount / 2),
                items.size(), is(addressCount / 2)); //assert correct count
        assertAddressType("Console failed, does not contains only address type topic",
                items, AddressType.TOPIC); //assert correct type

        consolePage.removeAddressFilter(FilterType.TYPE, AddressType.TOPIC.toString());
        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
                consolePage.getAddressItems().size(), is(addressCount));
    }

    protected void doTestFilterAddressesByName(AddressSpace addressSpace) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddressesAndWait(addresses.toArray(new Address[0]));

        String subText = "queue";
        consolePage.addFilter(FilterType.ADDRESS, subText);
        List<AddressWebItem> items = consolePage.getAddressItems();
        assertEquals(addressCount / 2, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount / 2));
        assertAddressName("Console failed, does not contain addresses contain " + subText, items, subText);

        subText = "topic";
        consolePage.addFilter(FilterType.ADDRESS, subText);
        items = consolePage.getAddressItems();
        assertEquals(addressCount, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount));


        consolePage.removeAddressFilter(FilterType.ADDRESS, "queue");
        items = consolePage.getAddressItems();
        assertEquals(addressCount / 2, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount / 2));
        assertAddressName("Console failed, does not contain addresses contain " + subText, items, subText);

        consolePage.removeAllFilters();
        assertEquals(addressCount, consolePage.getAddressItems().size(),
                String.format("Console failed, does not contain %d addresses", addressCount));

        consolePage.deleteSelectedAddresses(addresses.toArray(new Address[0]));
        assertEquals(0, consolePage.getAddressItems().size(),
                String.format("Console failed, does not contain %d addresses", 0));
    }

    protected void doTestFilterAddressesByStatus(AddressSpace addressSpace) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        getResourceManager().appendAddresses(false, addresses.toArray(new Address[0]));

        consolePage.addFilter(FilterType.STATUS, "Configuring");

        TestUtils.waitUntilCondition(()-> consolePage.getAddressItems().size()==addressCount, Duration.ofSeconds(30), Duration.ofMillis(500));

        List<AddressWebItem> items = consolePage.getAddressItems();
        assertEquals(addressCount, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount));

        AddressUtils.waitForDestinationsReady(addresses.toArray(new Address[0]));

        consolePage.addFilter(FilterType.STATUS, "Active");
        items = consolePage.getAddressItems();
        assertEquals(addressCount, items.size(),
                String.format("Console failed, does not contain %d addresses", addressCount));
    }

    protected void doTestPurgeMessages(AddressSpace addressSpace) throws Exception {
        Address queue1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue1"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue1")
                .withPlan(addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString()) ? DestinationPlan.BROKERED_QUEUE : DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        Address queue2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue2")
                .withPlan(addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString()) ? DestinationPlan.BROKERED_QUEUE : DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        Address queue3 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue3"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue3")
                .withPlan(addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString()) ? DestinationPlan.BROKERED_QUEUE : DestinationPlan.STANDARD_XLARGE_QUEUE)
                .endSpec()
                .build();

        resourcesManager.setAddresses(queue1, queue2, queue3);

        ExternalMessagingClient client = new ExternalMessagingClient()
                .withClientEngine(new RheaClientSender())
                .withMessagingRoute(AddressSpaceUtils.getMessagingRoute(addressSpace))
                .withCredentials(defaultCredentials)
                .withCount(1000)
                .withMessageBody("msg no. %d")
                .withTimeout(30);

        assertTrue(client.withAddress(queue1).run(false));
        assertTrue(client.withAddress(queue2).run(false));
        assertTrue(client.withAddress(queue3).run(false));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        assertThat(String.format("Console failed, does not contain %d addresses", 3),
                consolePage.getAddressItems().size(), is(3));

        consolePage.purgeSelectedAddresses(queue1, queue3);

        assertTrue(client.withAddress(queue2).withClientEngine(new RheaClientReceiver()).run(false));
        assertThat(client.getMessages().size(), is(1000));
        assertTrue(client.withAddress(queue3).withTimeout(10).withClientEngine(new RheaClientReceiver()).run(false));
        assertThat(client.getMessages().size(), is(0));
    }

    protected void doTestDeleteFilteredAddress(AddressSpace addressSpace) throws Exception {
        int addressTotal = 2;

        Address destQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("test-queue")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        Address destTopic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "test-topic"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("test-topic")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddresses(destQueue, destTopic);

        consolePage.addFilter(FilterType.TYPE, AddressType.QUEUE.toString());
        List<AddressWebItem> items = consolePage.getAddressItems();

        assertEquals(addressTotal / 2, items.size(),
                String.format("Console failed, filter does not contain %d addresses", addressTotal / 2));

        assertAddressName("Console failed, filter does not contain addresses", items, "queue");

        consolePage.deleteAddress(destQueue);
        items = consolePage.getAddressItems();
        assertEquals(0, items.size());
        log.info("filtered address has been deleted and no longer present in filter");

        consolePage.removeAllFilters();
        items = consolePage.getAddressItems();
        assertEquals(addressTotal / 2, items.size());
    }

    //cannot be tested because functionality not available
//    protected void doTestFilterAddressWithRegexSymbols(AddressSpace addressSpace) throws Exception {
//        int addressCount = 4;
//        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));
//
//        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
//        consolePage.openConsolePage();
//        consolePage.openAddressList(addressSpace);
//        consolePage.createAddresses(addresses.toArray(new Address[0]));
//        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
//                consolePage.getAddressItems().size(), is(addressCount));
//        // valid filter, will show 2 results
//        String subText = "topic";
//        consolePage.addFilter(FilterType.ADDRESS, subText);
//        List<AddressWebItem> items = consolePage.getAddressItems();
//        assertEquals(addressCount / 2, items.size(),
//                String.format("Console failed, does not contain %d addresses", addressCount / 2));
//        assertAddressName("Console failed, does not contain addresses contain " + subText, items, subText);
//        consolePage.removeAllFilters();
//        // invalid filter (not regex), error message is shown
//        subText = "*";
//        consolePage.addFilter(FilterType.ADDRESS, subText);
//        WebElement regexAlert = selenium.getWebElement(() -> selenium.getDriver().findElement(By.className("pficon-error-circle-o")));
//        assertTrue(regexAlert.isDisplayed());
//        // valid regex filter (.*), will show 4 results
//        subText = ".*";
//        consolePage.addFilter(FilterType.ADDRESS, subText);
//        items = consolePage.getAddressItems();
//        assertEquals(addressCount, items.size(),
//                String.format("Console failed, does not contain %d addresses", addressCount));
//        consolePage.removeAllFilters();
//        // valid regex filter ([0-9]\d*$) = any address ending with a number, will show 4 results
//        subText = "[0-9]\\d*$";
//        consolePage.addFilter(FilterType.ADDRESS, subText);
//        items = consolePage.getAddressItems();
//        assertEquals(addressCount, items.size(),
//                String.format("Console failed, does not contain %d addresses", addressCount));
//        consolePage.removeAllFilters();
//    }

    //cannot be tested because functionality not available
//    protected void doTestRegexAlertBehavesConsistently(AddressSpace addressSpace) throws Exception {
//        String subText = "*";
//        int addressCount = 2;
//        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));
//        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
//        consolePage.openConsolePage();
//        consolePage.openAddressList(addressSpace);
//        consolePage.createAddresses(addresses.toArray(new Address[0]));
//        assertThat(String.format("Console failed, does not contain %d addresses", addressCount),
//                consolePage.getAddressItems().size(), is(addressCount));
//        consolePage.addFilter(FilterType.ADDRESS, subText);
//        WebElement regexAlert = consolePage.getFilterRegexAlert();
//        assertTrue(regexAlert.isDisplayed());
//        consolePage.clickOnRegexAlertClose();
//        assertFalse(regexAlert.isDisplayed());
//        // check on connections tab filter
//        consolePage.openConnectionsPageWebConsole();
//        consolePage.addConnectionsFilter(FilterType.HOSTNAME, subText);
//        regexAlert = consolePage.getFilterRegexAlert();
//        assertTrue(regexAlert.isDisplayed());
//        consolePage.clickOnRegexAlertClose();
//        assertFalse(regexAlert.isDisplayed());
//    }

    protected void doTestSortAddressesByName(AddressSpace addressSpace) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.createAddresses(addresses.toArray(new Address[0]));
        consolePage.sortAddresses(SortType.ADDRESS, true);
        assertSorted("Console failed, items are not sorted by name asc", consolePage.getAddressItems());
        consolePage.sortAddresses(SortType.ADDRESS, false);
        assertSorted("Console failed, items are not sorted by name desc", consolePage.getAddressItems(), true);
    }

    //already tested with doTestSortAddressesBySenders and doTestSortAddressesByReceivers
//    protected void doTestSortAddressesByClients(AddressSpace addressSpace) throws Exception {
//        int addressCount = 4;
//        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));
//
//        getResourceManager().setAddresses(addresses.toArray(new Address[0]));
//
//        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
//        consolePage.openConsolePage();
//        consolePage.openAddressList(addressSpace);
//
//        assertEquals(addressCount, consolePage.getAddressItems().size(), "Unexpected number of addresses present before attaching clients");
//
//        List<ExternalMessagingClient> receivers = getClientUtils().attachReceivers(addressSpace, addresses, -1, defaultCredentials);
//        boolean error = true;
//        try {
//
//            TestUtils.waitUntilConditionOrFail(() -> consolePage.getAddressItems().stream()
//                                                   .allMatch(a -> a.getReceiversCount()>0),
//                                           Duration.ofSeconds(60),
//                                           Duration.ofSeconds(1),
//                                           ()->"Failed to wait for addresses count");
//
//            consolePage.sortAddresses(SortType.RECEIVERS, true);
//            assertSorted("Console failed, items are not sorted by count of receivers asc",
//                    consolePage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getReceiversCount));
//
//            consolePage.sortAddresses(SortType.RECEIVERS, false);
//            assertSorted("Console failed, items are not sorted by count of receivers desc",
//                    consolePage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getReceiversCount));
//            error = false;
//        } finally {
//            getClientUtils().stopClients(receivers, error);
//        }
//
//        List<ExternalMessagingClient> senders = getClientUtils().attachSenders(addressSpace, addresses, 360, defaultCredentials);
//        error = true;
//        try {
//
//            TestUtils.waitUntilConditionOrFail(() -> consolePage.getAddressItems().stream()
//                                                    .allMatch(a -> a.getSendersCount()>0),
//                                            Duration.ofSeconds(60),
//                                            Duration.ofSeconds(1),
//                                            ()->"Failed to wait for addresses count");
//
//            consolePage.sortAddresses(SortType.SENDERS, true);
//            assertSorted("Console failed, items are not sorted by count of senders asc",
//                    consolePage.getAddressItems(), Comparator.comparingInt(AddressWebItem::getSendersCount));
//
//            consolePage.sortAddresses(SortType.SENDERS, false);
//            assertSorted("Console failed, items are not sorted by count of senders desc",
//                    consolePage.getAddressItems(), true, Comparator.comparingInt(AddressWebItem::getSendersCount));
//            error = false;
//        } finally {
//            getClientUtils().stopClients(senders, error);
//        }
//
//    }

    protected void doTestSortAddressesBySenders(AddressSpace addressSpace) throws Exception {
        doTestSortAddresses(addressSpace,
                SortType.SENDERS,
//                this::attachClients,
                getClientUtils()::attachSenders,
                a -> a.getSendersCount()>0,
                Comparator.comparingInt(AddressWebItem::getSendersCount));
    }

    protected void doTestSortAddressesByReceivers(AddressSpace addressSpace) throws Exception {
        doTestSortAddresses(addressSpace,
                SortType.RECEIVERS,
//                this::attachClients,
                getClientUtils()::attachReceivers,
                a -> a.getReceiversCount()>0,
                Comparator.comparingInt(AddressWebItem::getReceiversCount));
    }

    private void doTestSortAddresses(AddressSpace addressSpace, SortType sortType, ClientAttacher attacher, Predicate<AddressWebItem> readyCondition, Comparator<AddressWebItem> sortingComparator) throws Exception {
        int addressCount = 4;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        getResourceManager().setAddresses(addresses.toArray(new Address[0]));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);

        assertEquals(addressCount, consolePage.getAddressItems().size(), "Unexpected number of addresses present before attaching clients");

        clientsList = attacher.attach(addressSpace, addresses, defaultCredentials);

        TestUtils.waitUntilConditionOrFail(() -> consolePage.getAddressItems().stream()
                                                .allMatch(readyCondition),
                                        Duration.ofSeconds(60),
                                        Duration.ofSeconds(1),
                                        ()->"Failed to wait for addresses count");

        consolePage.sortAddresses(sortType, true);
        assertSorted("Console failed, items are not sorted by count of senders asc",
                consolePage.getAddressItems(),
                sortingComparator);

        consolePage.sortAddresses(sortType, false);
        assertSorted("Console failed, items are not sorted by count of senders desc",
                consolePage.getAddressItems(),
                true,
                sortingComparator);
    }

    protected void doTestSortConnectionsBySenders(AddressSpace addressSpace) throws Exception {
        doTestSortConnections(addressSpace,
                SortType.SENDERS,
                this::attachClients,
//                getClientUtils()::attachSenders,
                c -> c.getSenders()>0,
                Comparator.comparingInt(ConnectionWebItem::getSenders));
    }

    protected void doTestSortConnectionsByReceivers(AddressSpace addressSpace) throws Exception {
        doTestSortConnections(addressSpace,
                SortType.RECEIVERS,
                this::attachClients,
//                getClientUtils()::attachReceivers,
                c -> c.getReceivers()>0,
                Comparator.comparingInt(ConnectionWebItem::getReceivers));
    }

    private void doTestSortConnections(AddressSpace addressSpace, SortType sortType, ClientAttacher attacher, Predicate<ConnectionWebItem> readyCondition, Comparator<ConnectionWebItem> sortingComparator) throws Exception {
        int addressCount = 2;
        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));

        getResourceManager().setAddresses(addresses.toArray(new Address[0]));

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openConnectionList(addressSpace);

        assertEquals(0, consolePage.getConnectionItems().size(), "Unexpected number of connections present before attaching clients");

        clientsList = attacher.attach(addressSpace, addresses, defaultCredentials);

        selenium.waitUntilPropertyPresent(60, clientsList.size(), ()->consolePage.getConnectionItems().size());

        TestUtils.waitUntilConditionOrFail(() -> consolePage.getConnectionItems().stream()
                                               .allMatch(readyCondition),
                                       Duration.ofSeconds(60),
                                       Duration.ofSeconds(1),
                                       ()->"Failed to wait for connections count");

        consolePage.sortConnections(sortType, true);
        assertSorted("Console failed, items are not sorted by count of senders asc",
                consolePage.getConnectionItems(),
                sortingComparator);

        consolePage.sortConnections(sortType, false);
        assertSorted("Console failed, items are not sorted by count of senders desc",
                consolePage.getConnectionItems(),
                true,
                sortingComparator);
    }

//
//
//      protected void doTestFilterConnectionsByEncrypted(AddressSpace addressSpace) throws Exception {
//      consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
//
//      consolePage.openWebConsolePage();
//      Address queue = new AddressBuilder()
//      .withNewMetadata()
//      .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
//      .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "queue-connection-encrypted"))
//      .endMetadata()
//      .withNewSpec()
//      .withType("queue")
//      .withAddress("queue-connection-encrypted")
//      .withPlan(getDefaultPlan(AddressType.QUEUE))
//      .endSpec()
//      .build();
//      consolePage.createAddresses(queue);
//      consolePage.openConnectionsPageWebConsole();
//
//      int receiverCount = 5;
//      clientsList = getClientUtils().attachReceivers(getSharedAddressSpace(), queue, receiverCount, -1, defaultCredentials);
//
//      consolePage.addConnectionsFilter(FilterType.ENCRYPTED, "encrypted");
//      List<ConnectionWebItem> items = consolePage.getConnectionItems(receiverCount);
//      assertThat(String.format("Console failed, does not contain %d connections", receiverCount),
//      items.size(), is(receiverCount));
//      assertConnectionUnencrypted("Console failed, does not show only Encrypted connections", items);
//
//      consolePage.clearAllFilters();
//      assertThat(consolePage.getConnectionItems(receiverCount).size(), is(receiverCount));
//
//      consolePage.addConnectionsFilter(FilterType.ENCRYPTED, "unencrypted");
//      items = consolePage.getConnectionItems();
//      assertThat(String.format("Console failed, does not contain %d connections", 0),
//      items.size(), is(0));
//      assertConnectionEncrypted("Console failed, does not show only Encrypted connections", items);
//      }
//
//      protected void doTestFilterConnectionsByUser(AddressSpace addressSpace) throws Exception {
//      consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
//
//      consolePage.openWebConsolePage();
//      Address queue = new AddressBuilder()
//      .withNewMetadata()
//      .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
//      .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "queue-connection-users"))
//      .endMetadata()
//      .withNewSpec()
//      .withType("queue")
//      .withAddress("queue-connection-users")
//      .withPlan(getDefaultPlan(AddressType.QUEUE))
//      .endSpec()
//      .build();
//      consolePage.createAddresses(queue);
//      consolePage.openConnectionsPageWebConsole();
//
//     UserCredentials pavel = new UserCredentials("pavel", "enmasse");
//      resourcesManager.createOrUpdateUser(getSharedAddressSpace(), pavel);
//      List<ExternalMessagingClient> receiversPavel = null;
//      List<ExternalMessagingClient> receiversTest = null;
//      try {
//      int receiversBatch1 = 5;
//      int receiversBatch2 = 10;
//      receiversPavel = getClientUtils().attachReceivers(getSharedAddressSpace(), queue, receiversBatch1, -1, pavel);
//      receiversTest = getClientUtils().attachReceivers(getSharedAddressSpace(), queue, receiversBatch2, -1, defaultCredentials);
//      assertThat(String.format("Console failed, does not contain %d connections", receiversBatch1 + receiversBatch2),
//      consolePage.getConnectionItems(receiversBatch1 + receiversBatch2).size(), is(receiversBatch1 + receiversBatch2));
//
//      consolePage.addConnectionsFilter(FilterType.USER, defaultCredentials.getUsername());
//      List<ConnectionWebItem> items = consolePage.getConnectionItems(receiversBatch2);
//      assertThat(String.format("Console failed, does not contain %d connections", receiversBatch2),
//      items.size(), is(receiversBatch2));
//      assertConnectionUsers(
//      String.format("Console failed, does not contain connections for user '%s'", defaultCredentials),
//      items, defaultCredentials.getUsername());
//
//      consolePage.addConnectionsFilter(FilterType.USER, pavel.getUsername());
//      assertThat(String.format("Console failed, does not contain %d connections", 0),
//      consolePage.getConnectionItems().size(), is(0));
//
//      consolePage.removeFilterByUser(defaultCredentials.getUsername());
//      items = consolePage.getConnectionItems(receiversBatch1);
//      assertThat(String.format("Console failed, does not contain %d connections", receiversBatch1),
//      items.size(), is(receiversBatch1));
//      assertConnectionUsers(
//      String.format("Console failed, does not contain connections for user '%s'", pavel),
//      items, pavel.getUsername());
//
//      consolePage.clearAllFilters();
//      assertThat(String.format("Console failed, does not contain %d connections", receiversBatch1 + receiversBatch2),
//      consolePage.getConnectionItems(receiversBatch1 + receiversBatch2).size(), is(receiversBatch1 + receiversBatch2));
//      } finally {
//      resourcesManager.removeUser(getSharedAddressSpace(), pavel.getUsername());
//      getClientUtils().stopClients(receiversTest);
//      getClientUtils().stopClients(receiversPavel);
//      }
//
//      }
//
//    //TODO tune up this test when doTestSortConnections work
//    protected void doTestFilterConnectionsByHostname(AddressSpace addressSpace) throws Exception {
//        int addressCount = 2;
//        List<Address> addresses = generateQueueTopicList(addressSpace, "via-web", IntStream.range(0, addressCount));
//
//        getResourceManager().setAddresses(addresses.toArray(new Address[0]));
//
//        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
//        consolePage.openConsolePage();
//        consolePage.openConnectionList(addressSpace);
//
//        clientsList = attachClients(addressSpace, addresses);
//
//        List<ConnectionWebItem> connectionItems = consolePage.getConnectionItems();
//        String hostname = connectionItems.get(0).getHost();
//
//        consolePage.addConnectionsFilter(FilterType.HOSTNAME, hostname);
//        assertThat(String.format("Console failed, does not contain %d connections", 1),
//                consolePage.getConnectionItems().size(), is(1));
//
//        consolePage.removeAllFilters();
//        assertThat(String.format("Console failed, does not contain %d connections", 6),
//                consolePage.getConnectionItems().size(), is(6));
//    }
//
//    protected void doTestSortConnectionsByHostname(AddressSpace addressSpace) throws Exception {
//        doTestSortConnections(addressSpace,
//                SortType.HOSTNAME,
//                this::attachClients,
//                c -> c.getHost()!=null,
//                Comparator.comparing(ConnectionWebItem::getHost));
//    }

    protected void doTestFilterConnectionsByContainerId(AddressSpace addressSpace) throws Exception {

        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-via-web"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-via-web")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        getResourceManager().setAddresses(dest);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openConnectionList(addressSpace);

        int connectionCount = 5;
        clientsList = new ArrayList<>();
        clientsList.add(getClientUtils().attachConnector(addressSpace, dest, connectionCount, 1, 1, defaultCredentials, 360));
        selenium.waitUntilPropertyPresent(60, connectionCount, () -> consolePage.getConnectionItems().size());

        String containerID = consolePage.getConnectionItems().get(0).getContainerId();

        consolePage.addConnectionsFilter(FilterType.CONTAINER, containerID);
        assertThat(String.format("Console failed, does not contain %d connections", 1),
                consolePage.getConnectionItems().size(), is(1));

        consolePage.removeAllFilters();
        assertThat(String.format("Console failed, does not contain %d connections", connectionCount),
                consolePage.getConnectionItems().size(), is(connectionCount));
    }

    protected void doTestSortConnectionsByContainerId(AddressSpace addressSpace) throws Exception {
        doTestSortConnections(addressSpace, SortType.CONTAINER_ID,
                this::attachClients,
                c -> c.getContainerId()!=null,
                Comparator.comparing(ConnectionWebItem::getContainerId));
    }

    protected void doTestMessagesMetrics(AddressSpace addressSpace) throws Exception {
        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-via-web"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-via-web")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        getResourceManager().setAddresses(dest);

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);

        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);

        assertEquals(1, consolePage.getAddressItems().size(), "Unexpected number of addresses present before attaching clients");

        int count = 5 * 60; //5 minutes to seconds
        int duration = count * 1000; // so we send 1 message per second

        clientsList = new ArrayList<>();
        //don't know why but if you try to send 300 messages rhea seems to send 1 more
        clientsList.add(getClientUtils().attachSender(addressSpace, dest, defaultCredentials, count - 1, duration));

        TestUtils.waitUntilConditionOrFail(() -> consolePage.getAddressItem(dest).getMessagesIn() == 1,
                Duration.ofSeconds(120),
                Duration.ofSeconds(3),
                ()->"Failed to wait for messagesIn/sec to reach 1");

        selenium.waitUntilPropertyPresent(15, count, () -> consolePage.getAddressItem(dest).getMessagesStored());

        clientsList.add(getClientUtils().attachReceiver(addressSpace, dest, defaultCredentials, count));

        selenium.waitUntilPropertyPresent(30, 0, () -> consolePage.getAddressItem(dest).getMessagesStored());

    }

    protected void doTestClientsMetrics(AddressSpace addressSpace) throws Exception {
        int senderCount = 5;
        int receiverCount = 10;
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);

        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressSpace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressSpace, "queue-via-web"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue-via-web")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        consolePage.createAddress(dest);

        clientsList = new ArrayList<>();
        clientsList.add(getClientUtils().attachConnector(addressSpace, dest, 1, senderCount, receiverCount, defaultCredentials, 360));
        selenium.waitUntilPropertyPresent(60, senderCount, () -> consolePage.getAddressItem(dest).getSendersCount());

        assertAll(
                () -> assertEquals(receiverCount, consolePage.getAddressItem(dest).getReceiversCount(),
                        String.format("Console failed, does not contain %d receivers", 10)),
                () -> assertEquals(senderCount, consolePage.getAddressItem(dest).getSendersCount(),
                        String.format("Console failed, does not contain %d senders", 5)));
    }

    protected void doTestCanOpenConsolePage(AddressSpace addressSpace, UserCredentials credentials, boolean userAllowed) throws Exception {
        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), credentials);
        consolePage.openConsolePage();
        log.info("User {} successfully authenticated", credentials);

        if ( userAllowed ) {
            consolePage.openAddressList(addressSpace);
        } else {
            try {
                consolePage.openAddressList(addressSpace);
                fail("Exception not thrown");
            } catch ( NullPointerException ex ) {
                // PASS
            }

            throw new IllegalAccessException();
        }
    }

    protected void doTestWithStrangeAddressNames(AddressSpace addressSpace, boolean hyphen, boolean longName, AddressType... types) throws Exception {
        String testString = null;
        if (hyphen) {
            testString = String.join("-", Collections.nCopies(2, "10charhere"));
        }
        if (longName) {
            testString = String.join("", Collections.nCopies(5, "10charhere"));
        }

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);

        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);

        for (AddressType type : types) {
            int assert_value = 1;
            Address dest;
            Address dest_topic = null;
            if (type == AddressType.SUBSCRIPTION) {
                dest_topic = new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, "topic-sub" + testString))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("topic-sub" + testString)
                        .withPlan(getDefaultPlan(AddressType.TOPIC))
                        .endSpec()
                        .build();
                log.info("Creating topic for subscription");
                consolePage.createAddress(dest_topic);
                dest = new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, testString))
                        .endMetadata()
                        .withNewSpec()
                        .withType("subscription")
                        .withAddress(testString)
                        .withTopic(dest_topic.getSpec().getAddress())
                        .withPlan(DestinationPlan.STANDARD_SMALL_SUBSCRIPTION)
                        .endSpec()
                        .build();
                assert_value = 2;
            } else {
                dest = new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressSpace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressSpace, type.toString() + "-" + testString))
                        .endMetadata()
                        .withNewSpec()
                        .withType(type.toString())
                        .withAddress(type.toString() + "-" + testString)
                        .withPlan(getDefaultPlan(type))
                        .endSpec()
                        .build();
                assert_value = 1;
            }

            consolePage.createAddress(dest);
            assertWaitForValue(assert_value, () -> consolePage.getAddressItems().size(), new TimeoutBudget(120, TimeUnit.SECONDS));

            if (type.equals(AddressType.SUBSCRIPTION)) {
                consolePage.deleteAddress(dest_topic);
            }
            consolePage.deleteAddress(dest);
            assertWaitForValue(0, () -> consolePage.getAddressItems().size(), new TimeoutBudget(20, TimeUnit.SECONDS));
        }
    }

    protected void doTestCreateAddressWithSpecialCharsShowsErrorMessage(AddressSpace addressSpace) throws Exception {

        consolePage = new ConsoleWebPage(selenium, TestUtils.getGlobalConsoleRoute(), clusterUser);
        consolePage.openConsolePage();
        consolePage.openAddressList(addressSpace);
        consolePage.openAddressCreationDialog();

        String validName = AddressUtils.generateAddressMetadataName(addressSpace, "dummy");
        String[] invalidNames = {
                        "#dummy",
                        "du#mmy",
                        ":dummy",
                        "du:mmy",
                        "$dummy",
                        ".dummy",
                        "dummy.",
                        "dummy-",
                        "-dummy",
                        "duMmy",
                        "DUM-MY"
        };

        for ( var name : invalidNames) {
            consolePage.fillAddressName(validName);
            assertFalse(consolePage.isAddressNameInvalid());

            consolePage.fillAddressName(name);
            assertTrue(consolePage.isAddressNameInvalid(), String.format("Address name %s is not marked as invalid", name));
        }

        String[] validNames = {
                        "du.mmy",
                        "du-mmy"
        };

        for (String name : validNames) {
            consolePage.fillAddressName(validName);
            assertFalse(consolePage.isAddressNameInvalid());

            consolePage.fillAddressName(name);
            assertFalse(consolePage.isAddressNameInvalid(), String.format("Address name %s is not marked as valid", name));
        }
    }

    //============================================================================================
    //============================ Help methods ==================================================
    //============================================================================================
    private List<Address> generateQueueTopicList(AddressSpace addressspace, String infix, IntStream range) {
        List<Address> addresses = new ArrayList<>();
        range.forEach(i -> {
            if (i % 2 == 0) {
                addresses.add(new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, String.format("topic-%s-%d", infix, i)))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress(String.format("topic-%s-%d", infix, i))
                        .withPlan(getDefaultPlan(AddressType.TOPIC))
                        .endSpec()
                        .build());
            } else {
                addresses.add(new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, String.format("queue-%s-%d", infix, i)))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress(String.format("queue-%s-%d", infix, i))
                        .withPlan(getDefaultPlan(AddressType.QUEUE))
                        .endSpec()
                        .build());
            }
        });
        return addresses;
    }

    private List<ExternalMessagingClient> attachClients(AddressSpace addressSpace, List<Address> destinations, UserCredentials userCredentials) throws Exception {
        return attachClients(addressSpace, destinations);
    }

    private List<ExternalMessagingClient> attachClients(AddressSpace addressSpace, List<Address> destinations) throws Exception {
        List<ExternalMessagingClient> clients = new ArrayList<>();
        for ( Address destination : destinations ) {
            clients.add(getClientUtils().attachConnector(addressSpace, destination, 1, 6, 1, defaultCredentials, 360));
            clients.add(getClientUtils().attachConnector(addressSpace, destination, 1, 4, 4, defaultCredentials, 360));
            clients.add(getClientUtils().attachConnector(addressSpace, destination, 1, 1, 6, defaultCredentials, 360));
        }
        Thread.sleep(5000);
        return clients;
    }

    private void assertAddressType(String message, List<AddressWebItem> allItems, AddressType type) {
        assertThat(message, getAddressProperty(allItems, (item -> item.getType().contains(type.toString()))).size(), is(allItems.size()));
    }

    private void assertAddressName(String message, List<AddressWebItem> allItems, String subString) {
        assertThat(message, getAddressProperty(allItems, (item -> item.getAddress().contains(subString))).size(), is(allItems.size()));
    }

    private List<ConnectionWebItem> getConnectionProperty(List<ConnectionWebItem> allItems, Predicate<ConnectionWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }

    private List<AddressWebItem> getAddressProperty(List<AddressWebItem> allItems, Predicate<AddressWebItem> f) {
        return allItems.stream().filter(f).collect(Collectors.toList());
    }

    private void waitUntilAddressSpaceActive(AddressSpace addressSpace) throws Exception {
        String name = addressSpace.getMetadata().getName();
        resourcesManager.waitForAddressSpaceReady(addressSpace);
        Boolean active = Optional.ofNullable(selenium.waitUntilItemPresent(60, () -> consolePage.getAddressSpaceItem(addressSpace)))
                .map(webItem -> webItem.getStatus().contains("Active"))
                .orElseGet(() -> {
                    log.error("AddressSpaceWebItem {} not present", name);
                    return false;
                });
        assertTrue(active, String.format("Address space %s not marked active in UI within timeout", name));
    }
}
