/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases;

import com.google.common.collect.Ordering;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.ability.ITestBase;
import io.enmasse.systemtest.ability.ITestSeparator;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.apiclients.OSBApiClient;
import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.Argument;
import io.enmasse.systemtest.clients.ArgumentMap;
import io.enmasse.systemtest.clients.rhea.RheaClientConnector;
import io.enmasse.systemtest.clients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.clients.rhea.RheaClientSender;
import io.enmasse.systemtest.mqtt.MqttClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.resources.SchemaData;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;

import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for all tests
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestBase implements ITestBase, ITestSeparator {
    protected static final Environment environment = new Environment();
    protected static final Kubernetes kubernetes = Kubernetes.create(environment);
    private static final GlobalLogCollector logCollector = new GlobalLogCollector(kubernetes,
            new File(environment.testLogDir()));
    protected static final AddressApiClient addressApiClient = new AddressApiClient(kubernetes);
    private static Logger log = CustomLogger.getLogger();
    protected AmqpClientFactory amqpClientFactory;
    protected MqttClientFactory mqttClientFactory;
    protected KeycloakCredentials managementCredentials = new KeycloakCredentials(null, null);
    protected KeycloakCredentials defaultCredentials = new KeycloakCredentials(null, null);
    private List<AddressSpace> addressSpaceList = new ArrayList<>();
    private KeycloakClient keycloakApiClient;

    protected static void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        if (TestUtils.existAddressSpace(addressApiClient, addressSpace.getName())) {
            TestUtils.deleteAddressSpace(addressApiClient, addressSpace, logCollector);
            TestUtils.waitForAddressSpaceDeleted(kubernetes, addressSpace);
        } else {
            log.info("Address space '" + addressSpace + "' doesn't exists!");
        }
    }

    AddressSpace getSharedAddressSpace() {
        return null;
    }

    @BeforeEach
    public void setup() {
        addressSpaceList = new ArrayList<>();
        amqpClientFactory = new AmqpClientFactory(kubernetes, environment, null, defaultCredentials);
        mqttClientFactory = new MqttClientFactory(kubernetes, environment, null, defaultCredentials);
    }

    @AfterEach
    public void teardown() throws Exception {
        try {
            mqttClientFactory.close();
            amqpClientFactory.close();

            if (!environment.skipCleanup()) {
                for (AddressSpace addressSpace : addressSpaceList) {
                    deleteAddressSpace(addressSpace);
                }
                addressSpaceList.clear();
            } else {
                log.warn("Remove address spaces in tear down - SKIPPED!");
            }
        } catch (Exception e) {
            log.error("Error tearing down test: {}", e.getMessage());
            throw e;
        }
    }




    //================================================================================================
    //==================================== AddressSpace methods ======================================
    //================================================================================================

    protected void addToAddressSpaceList(AddressSpace... addressSpaces) {
        addressSpaceList.addAll(Arrays.asList(addressSpaces));
    }

    protected List<AddressSpace> getAddressSpaceList() {
        return addressSpaceList;
    }

    protected void createAddressSpace(AddressSpace addressSpace) throws Exception {
        createAddressSpace(addressSpace, !isBrokered(addressSpace));
    }


    protected void createAddressSpaceList(AddressSpace... addressSpaces) throws Exception {
        List<AddressSpace> addrSpacesResponse = new ArrayList<>();
        ArrayList<AddressSpace> spaces = new ArrayList<>();
        for (AddressSpace addressSpace : addressSpaces) {
            if (!TestUtils.existAddressSpace(addressApiClient, addressSpace.getName())) {
                log.info("Address space '" + addressSpace + "' doesn't exist and will be created.");
                spaces.add(addressSpace);
            } else {
                log.warn("Address space '" + addressSpace + "' already exists.");
                addrSpacesResponse.add(TestUtils.getAddressSpaceObject(addressApiClient, addressSpace.getName()));
            }
        }
        addressApiClient.createAddressSpaceList(spaces.toArray(new AddressSpace[0]));
        boolean extraWait = false;
        for (AddressSpace addressSpace : spaces) {
            logCollector.startCollecting(addressSpace.getNamespace());
            addrSpacesResponse.add(TestUtils.waitForAddressSpaceReady(addressApiClient, addressSpace.getName()));
            if (!addressSpace.equals(getSharedAddressSpace())) {
                addressSpaceList.add(addressSpace);
            }
            extraWait = extraWait || !isBrokered(addressSpace);
        }
        if (extraWait) {
            log.info("One of requested address-spaces is 'standard' type - Waiting for 2 minutes before starting tests");
            Thread.sleep(120_000);
        }
        Arrays.stream(addressSpaces).forEach(originalAddrSpace -> {
            if (originalAddrSpace.getEndpoints().isEmpty()) {
                originalAddrSpace.setEndpoints(addrSpacesResponse.stream().filter(
                        resposeAddrSpace -> resposeAddrSpace.getName().equals(originalAddrSpace.getName())).findFirst().get().getEndpoints());
                log.info(String.format("Address-space '%s' endpoints successfully set", originalAddrSpace.getName()));
            }
            log.info(String.format("Address-space successfully created: %s", originalAddrSpace));
        });
    }

    protected void createAddressSpace(AddressSpace addressSpace, boolean extraWait) throws Exception {
        AddressSpace addrSpaceResponse;
        if (!TestUtils.existAddressSpace(addressApiClient, addressSpace.getName())) {
            log.info("Address space '" + addressSpace + "' doesn't exist and will be created.");
            addressApiClient.createAddressSpace(addressSpace);
            addrSpaceResponse = TestUtils.waitForAddressSpaceReady(addressApiClient, addressSpace.getName());

            if (!addressSpace.equals(getSharedAddressSpace())) {
                addressSpaceList.add(addressSpace);
            }

            if (extraWait) {
                log.info("Waiting for 2 minutes before starting tests");
                Thread.sleep(120_000);
            }
        } else {
            addrSpaceResponse = TestUtils.getAddressSpaceObject(addressApiClient, addressSpace.getName());
            log.info("Address space '" + addressSpace + "' already exists.");
        }
        if (addressSpace.getEndpoints().isEmpty()) {
            addressSpace.setEndpoints(addrSpaceResponse.getEndpoints());
            log.info("Address-space '{}' endpoints successfully set.", addressSpace.getName());
        }
        log.info("Address-space successfully created: '{}'", addressSpace);
    }

    //!TODO: protected void appendAddressSpace(...)

    protected AddressSpace getAddressSpace(String name) throws Exception {
        return TestUtils.getAddressSpaceObject(addressApiClient, name);
    }

    protected List<AddressSpace> getAddressSpaces() throws Exception {
        return TestUtils.getAddressSpacesObjects(addressApiClient);
    }

    private KeycloakClient getKeycloakClient() throws Exception {
        if (keycloakApiClient == null) {
            KeycloakCredentials creds = environment.keycloakCredentials();
            if (creds == null) {
                creds = kubernetes.getKeycloakCredentials();
            }
            keycloakApiClient = new KeycloakClient(kubernetes.getKeycloakEndpoint(), creds, kubernetes.getKeycloakCA());
        }
        return keycloakApiClient;
    }

    protected void deleteAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        logCollector.collectConfigMaps(addressSpace.getNamespace());
        TestUtils.delete(addressApiClient, addressSpace, destinations);
    }

    protected void appendAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        appendAddresses(addressSpace, budget, destinations);
    }

    protected void appendAddresses(AddressSpace addressSpace, TimeoutBudget timeout, Destination... destinations) throws Exception {
        appendAddresses(addressSpace, true, timeout, destinations);
    }

    protected void appendAddresses(AddressSpace addressSpace, boolean wait, Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        appendAddresses(addressSpace, wait, budget, destinations);
    }

    protected void appendAddresses(AddressSpace addressSpace, boolean wait, TimeoutBudget timeout, Destination... destinations) throws Exception {
        TestUtils.appendAddresses(addressApiClient, kubernetes, timeout, addressSpace, wait, destinations);
        logCollector.collectConfigMaps(addressSpace.getNamespace());
    }


    protected void setAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        setAddresses(addressSpace, budget, destinations);
    }


    protected void setAddresses(AddressSpace addressSpace, TimeoutBudget timeout, Destination... destinations) throws Exception {
        TestUtils.setAddresses(addressApiClient, kubernetes, timeout, addressSpace, true, destinations);
        logCollector.collectConfigMaps(addressSpace.getNamespace());
    }

    protected List<URL> getAddressesPaths() throws Exception {
        return TestUtils.getAddressesPaths(addressApiClient);
    }

    protected JsonObject sendRestApiRequest(HttpMethod method, URL url, Optional<JsonObject> payload) throws Exception {
        return TestUtils.sendRestApiRequest(addressApiClient, method, url, payload);
    }

    /**
     * give you a list of names of all deployed addresses (or single deployed address)
     *
     * @param addressName name of single address
     * @return list of addresses
     * @throws Exception
     */
    protected Future<List<String>> getAddresses(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        return TestUtils.getAddresses(addressApiClient, addressSpace, addressName, new ArrayList<>());
    }

    /**
     * give you a list of objects of all deployed addresses (or single deployed address)
     *
     * @param addressName name of single address
     * @return list of addresses
     * @throws Exception
     */
    protected Future<List<Address>> getAddressesObjects(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        return TestUtils.getAddressesObjects(addressApiClient, addressSpace, addressName, new ArrayList<>());
    }

    /**
     * give you a schema object
     *
     * @return schema object
     * @throws Exception
     */
    protected Future<SchemaData> getSchema() throws Exception {
        return TestUtils.getSchema(addressApiClient);
    }

    /**
     * give you a list of objects of all deployed addresses (or single deployed address)
     *
     * @param addressName name of single address
     * @return list of Destinations
     * @throws Exception
     */
    protected Future<List<Destination>> getDestinationsObjects(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        return TestUtils.getDestinationsObjects(addressApiClient, addressSpace, addressName, new ArrayList<>());
    }

    /**
     * scale up/down destination (StatefulSet) to count of replicas, includes waiting for expected replicas
     */
    private void scale(AddressSpace addressSpace, Destination destination, int numReplicas, long checkInterval) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.setReplicas(kubernetes, addressSpace, destination, numReplicas, budget, checkInterval);
    }

    private void scaleWithoutWait(AddressSpace addressSpace, Destination destination, int numReplicas) throws Exception {
        TestUtils.setReplicas(kubernetes, addressSpace, destination, numReplicas);
    }

    void scale(AddressSpace addressSpace, Destination destination, int numReplicas) throws Exception {
        scale(addressSpace, destination, numReplicas, 5000);
    }

    protected void scaleKeycloak(int numReplicas) throws Exception {
        scaleInGlobal("keycloak", numReplicas);
    }

    /**
     * scale up/down deployment to count of replicas, includes waiting for expected replicas
     *
     * @param deployment  name of deployment
     * @param numReplicas count of replicas
     * @throws InterruptedException
     */
    private void scaleInGlobal(String deployment, int numReplicas) throws InterruptedException {
        if (numReplicas >= 0) {
            TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
            TestUtils.setReplicas(kubernetes, environment.namespace(), deployment, numReplicas, budget);
        } else {
            throw new IllegalArgumentException("'numReplicas' must be greater than 0");
        }

    }

    protected void createGroup(AddressSpace addressSpace, String groupName) throws Exception {
        getKeycloakClient().createGroup(getRealmName(addressSpace), groupName);
    }

    protected void joinGroup(AddressSpace addressSpace, String groupName, String username) throws Exception {
        getKeycloakClient().joinGroup(getRealmName(addressSpace), groupName, username);
    }

    protected void leaveGroup(AddressSpace addressSpace, String groupName, String username) throws Exception {
        getKeycloakClient().leaveGroup(getRealmName(addressSpace), groupName, username);
    }


    protected void createUser(AddressSpace addressSpace, KeycloakCredentials credentials, String... groups) throws Exception {
        log.info("User {} will be created", credentials);
        if (groups != null && groups.length > 0) {
            getKeycloakClient().createUser(getRealmName(addressSpace), credentials, groups);
        } else {
            getKeycloakClient().createUser(getRealmName(addressSpace), credentials);
        }
    }

    protected void removeUser(AddressSpace addressSpace, String username) throws Exception {
        getKeycloakClient().deleteUser(getRealmName(addressSpace), username);
    }

    private static String getRealmName(AddressSpace addressSpace) {
        return addressSpace.getName();
    }

    protected void createUsers(AddressSpace addressSpace, String prefixName, String prefixPswd, int from, int to)
            throws Exception {
        for (int i = from; i < to; i++) {
            createUser(addressSpace, new KeycloakCredentials(prefixName + i, prefixPswd + i));
        }
    }

    protected void removeUsers(AddressSpace addressSpace, List<String> users) throws Exception {
        for (String user : users) {
            removeUser(addressSpace, user);
        }
    }

    protected void removeUsers(AddressSpace addressSpace, String prefixName, int from, int to) throws Exception {
        for (int i = from; i < to; i++) {
            removeUser(addressSpace, prefixName + i);
        }
    }

    protected boolean isBrokered(AddressSpace addressSpace) {
        return addressSpace.getType().equals(AddressSpaceType.BROKERED);
    }

    protected void assertCanConnect(AddressSpace addressSpace, KeycloakCredentials credentials, List<Destination> destinations) throws Exception {
        assertTrue(canConnectWithAmqp(addressSpace, credentials, destinations),
                "Client failed, cannot connect under user " + credentials);
        // TODO: Enable this when mqtt is stable enough
        // assertTrue(canConnectWithMqtt(addressSpace, username, password));
    }

    protected void assertCannotConnect(AddressSpace addressSpace, KeycloakCredentials credentials, List<Destination> destinations) throws Exception {
        try {
            assertFalse(canConnectWithAmqp(addressSpace, credentials, destinations),
                    "Client failed, can connect under user " + credentials);
            fail("Expected connection to timeout");
        } catch (ConnectTimeoutException ignored) {
        }

        // TODO: Enable this when mqtt is stable enough
        // assertFalse(canConnectWithMqtt(addressSpace, username, password));
    }


    private boolean canConnectWithAmqp(AddressSpace addressSpace, KeycloakCredentials credentials, List<Destination> destinations) throws Exception {
        for (Destination destination : destinations) {
            String message = String.format("Client failed, cannot connect to %s under user %s", destination.getType(), credentials);
            switch (destination.getType()) {
                case "queue":
                    assertTrue(canConnectWithAmqpToQueue(addressSpace, credentials, destination.getAddress()), message);
                    break;
                case "topic":
                    assertTrue(canConnectWithAmqpToTopic(addressSpace, credentials, destination.getAddress()), message);
                    break;
                case "multicast":
                    if (!isBrokered(addressSpace))
                        assertTrue(canConnectWithAmqpToMulticast(addressSpace, credentials, destination.getAddress()), message);
                    break;
                case "anycast":
                    if (!isBrokered(addressSpace))
                        assertTrue(canConnectWithAmqpToAnycast(addressSpace, credentials, destination.getAddress()), message);
                    break;
            }
        }
        return true;
    }

    private boolean canConnectWithMqtt(String name, KeycloakCredentials credentials) throws Exception {
        AddressSpace addressSpace = new AddressSpace(name);
        MqttClient client = mqttClientFactory.createClient(addressSpace);
        client.setCredentials(credentials);

        Future<List<MqttMessage>> received = client.recvMessages("t1", 1);
        Future<Integer> sent = client.sendMessages("t1", Collections.singletonList("msgt1"));

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    private boolean canConnectWithAmqpToQueue(AddressSpace addressSpace, KeycloakCredentials credentials, String queueAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(credentials);

        Future<Integer> sent = client.sendMessages(queueAddress, Collections.singletonList("msg1"), 10, TimeUnit.SECONDS);
        Future<List<Message>> received = client.recvMessages(queueAddress, 1, 10, TimeUnit.SECONDS);

        return (sent.get(10, TimeUnit.SECONDS) == received.get(10, TimeUnit.SECONDS).size());
    }

    private boolean canConnectWithAmqpToAnycast(AddressSpace addressSpace, KeycloakCredentials credentials, String anycastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(credentials);

        Future<List<Message>> received = client.recvMessages(anycastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(anycastAddress, Collections.singletonList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(10, TimeUnit.SECONDS) == received.get(10, TimeUnit.SECONDS).size());
    }

    private boolean canConnectWithAmqpToMulticast(AddressSpace addressSpace, KeycloakCredentials credentials, String multicastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createBroadcastClient(addressSpace);
        client.getConnectOptions().setCredentials(credentials);

        Future<List<Message>> received = client.recvMessages(multicastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(multicastAddress, Collections.singletonList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(10, TimeUnit.SECONDS) == received.get(10, TimeUnit.SECONDS).size());
    }

    private boolean canConnectWithAmqpToTopic(AddressSpace addressSpace, KeycloakCredentials credentials, String topicAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createTopicClient(addressSpace);
        client.getConnectOptions().setCredentials(credentials);

        Future<List<Message>> received = client.recvMessages(topicAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(topicAddress, Collections.singletonList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(10, TimeUnit.SECONDS) == received.get(10, TimeUnit.SECONDS).size());
    }

    protected Endpoint getMessagingRoute(AddressSpace addressSpace) {
        Endpoint messagingEndpoint = addressSpace.getEndpoint("messaging");
        if (messagingEndpoint == null) {
            String externalEndpointName = TestUtils.getExternalEndpointName(addressSpace, "messaging");
            messagingEndpoint = kubernetes.getExternalEndpoint(addressSpace.getNamespace(), externalEndpointName);
        }
        if (TestUtils.resolvable(messagingEndpoint)) {
            return messagingEndpoint;
        } else {
            return kubernetes.getEndpoint(addressSpace.getNamespace(), "messaging", "amqps");
        }
    }

    protected String getOCConsoleRoute() {
        return String.format("%s/console", environment.openShiftUrl());
    }

    protected String getConsoleRoute(AddressSpace addressSpace) {
        Endpoint consoleEndpoint = addressSpace.getEndpoint("console");
        if (consoleEndpoint == null) {
            String externalEndpointName = TestUtils.getExternalEndpointName(addressSpace, "console");
            consoleEndpoint = kubernetes.getExternalEndpoint(addressSpace.getNamespace(), externalEndpointName);
        }
        String consoleRoute = String.format("https://%s", consoleEndpoint.toString());
        log.info(consoleRoute);
        return consoleRoute;
    }

    /**
     * selenium provider with Firefox webdriver
     */
    protected SeleniumProvider getFirefoxSeleniumProvider() throws Exception {
        SeleniumProvider seleniumProvider = new SeleniumProvider();
        seleniumProvider.setupDriver(environment, kubernetes, TestUtils.getFirefoxDriver());
        return seleniumProvider;
    }

    protected void waitForSubscribersConsole(AddressSpace addressSpace, Destination destination, int expectedCount) {
        int budget = 60; //seconds
        waitForSubscribersConsole(addressSpace, destination, expectedCount, budget);
    }

    /**
     * wait for expected count of subscribers on topic (check via console)
     *
     * @param budget timeout budget in seconds
     */
    private void waitForSubscribersConsole(AddressSpace addressSpace, Destination destination, int expectedCount, int budget) {
        SeleniumProvider selenium = null;
        try {
            selenium = getFirefoxSeleniumProvider();
            ConsoleWebPage console = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressApiClient, addressSpace, defaultCredentials);
            console.openWebConsolePage();
            console.openAddressesPageWebConsole();
            selenium.waitUntilPropertyPresent(budget, expectedCount, () -> console.getAddressItem(destination).getReceiversCount());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (selenium != null) {
                selenium.tearDownDrivers();
            }
        }
    }

    /**
     * wait for expected count of subscribers on topic
     *
     * @param addressSpace
     * @param topic         name of topic
     * @param expectedCount count of expected subscribers
     * @throws Exception
     */
    protected void waitForSubscribers(BrokerManagement brokerManagement, AddressSpace addressSpace, String topic, int expectedCount) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        waitForSubscribers(brokerManagement, addressSpace, topic, expectedCount, budget);
    }

    private void waitForSubscribers(BrokerManagement brokerManagement, AddressSpace addressSpace, String topic, int expectedCount, TimeoutBudget budget) throws Exception {
        AmqpClient queueClient = null;
        try {
            queueClient = amqpClientFactory.createQueueClient(addressSpace);
            queueClient.setConnectOptions(queueClient.getConnectOptions().setCredentials(managementCredentials));
            String replyQueueName = "reply-queue";
            Destination replyQueue = Destination.queue(replyQueueName, getDefaultPlan(AddressType.QUEUE));
            appendAddresses(addressSpace, replyQueue);

            boolean done = false;
            int actualSubscribers = 0;
            do {
                actualSubscribers = getSubscriberCount(brokerManagement, queueClient, replyQueue, topic);
                log.info("Have " + actualSubscribers + " subscribers. Expecting " + expectedCount);
                if (actualSubscribers != expectedCount) {
                    Thread.sleep(1000);
                } else {
                    done = true;
                }
            } while (budget.timeLeft() >= 0 && !done);
            if (!done) {
                throw new RuntimeException("Only " + actualSubscribers + " out of " + expectedCount + " subscribed before timeout");
            }
        } finally {
            Objects.requireNonNull(queueClient).close();
        }
    }

    private void waitForBrokerReplicas(AddressSpace addressSpace, Destination destination, int expectedReplicas, boolean readyRequired, TimeoutBudget budget, long checkInterval) throws InterruptedException {
        TestUtils.waitForNBrokerReplicas(kubernetes, addressSpace.getNamespace(), expectedReplicas, readyRequired, destination, budget, checkInterval);
    }

    protected void waitForBrokerReplicas(AddressSpace addressSpace, Destination destination, int expectedReplicas, boolean readyRequired, TimeoutBudget budget) throws InterruptedException {
        waitForBrokerReplicas(addressSpace, destination, expectedReplicas, readyRequired, budget, 5000);
    }

    private void waitForBrokerReplicas(AddressSpace addressSpace, Destination destination,
                                       int expectedReplicas, TimeoutBudget budget) throws InterruptedException {
        waitForBrokerReplicas(addressSpace, destination, expectedReplicas, true, budget, 5000);
    }

    protected void waitForBrokerReplicas(AddressSpace addressSpace, Destination destination, int expectedReplicas) throws
            InterruptedException {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        waitForBrokerReplicas(addressSpace, destination, expectedReplicas, budget);
    }

    protected void waitForRouterReplicas(AddressSpace addressSpace, int expectedReplicas) throws
            InterruptedException {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        TestUtils.waitForNReplicas(kubernetes, addressSpace.getNamespace(), expectedReplicas, Collections.singletonMap("name", "qdrouterd"), budget);
    }

    protected void waitForAutoScale(AddressSpace addressSpace, Destination dest, int setValue, int expectedValue) throws Exception {
        log.info("Set '{}' replicas and wait for autoscale to '{}'", setValue, expectedValue);
        CompletableFuture<Void> scaleCheckerDown = CompletableFuture.runAsync(() -> {
            try {
                waitForBrokerReplicas(addressSpace, dest, setValue, false, new TimeoutBudget(2, TimeUnit.MINUTES), 1);
                log.info("Waiting for expected replicas {} finished!", setValue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        scaleWithoutWait(addressSpace, dest, setValue);
        scaleCheckerDown.get(3, TimeUnit.MINUTES);
        waitForBrokerReplicas(addressSpace, dest, expectedValue, new TimeoutBudget(2, TimeUnit.MINUTES));
    }

    /**
     * Wait for destinations are in isReady=true state within default timeout (5 MINUTE)
     */
    protected void waitForDestinationsReady(AddressSpace addressSpace, Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.waitForDestinationsReady(addressApiClient, addressSpace, budget, destinations);
    }

    /**
     * return list of queue names created for subscribers
     *
     * @param queueClient
     * @param replyQueue  queue for answer is required
     * @param topic       topic name
     * @return
     * @throws Exception
     */
    private List<String> getBrokerQueueNames(BrokerManagement brokerManagement, AmqpClient
            queueClient, Destination replyQueue, String topic) throws Exception {
        return brokerManagement.getQueueNames(queueClient, replyQueue, topic);
    }

    /**
     * get count of subscribers subscribed to 'topic'
     *
     * @param queueClient queue client with admin permissions
     * @param replyQueue  queue for answer is required
     * @param topic       topic name
     * @return
     * @throws Exception
     */
    private int getSubscriberCount(BrokerManagement brokerManagement, AmqpClient queueClient, Destination
            replyQueue, String topic) throws Exception {
        List<String> queueNames = getBrokerQueueNames(brokerManagement, queueClient, replyQueue, topic);

        AtomicInteger subscriberCount = new AtomicInteger(0);
        queueNames.forEach((String queue) -> {
            try {
                subscriberCount.addAndGet(brokerManagement.getSubscriberCount(queueClient, replyQueue, queue));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return subscriberCount.get();
    }

    protected ArrayList<Destination> generateTopicsList(String prefix, IntStream range) {
        ArrayList<Destination> addresses = new ArrayList<>();
        range.forEach(i -> addresses.add(Destination.topic(prefix + i, getDefaultPlan(AddressType.QUEUE))));
        return addresses;
    }

    protected ArrayList<Destination> generateQueueList(String prefix, IntStream range) {
        ArrayList<Destination> addresses = new ArrayList<>();
        range.forEach(i -> addresses.add(Destination.queue(prefix + i, getDefaultPlan(AddressType.QUEUE))));
        return addresses;
    }

    protected ArrayList<Destination> generateQueueTopicList(String infix, IntStream range) {
        ArrayList<Destination> addresses = new ArrayList<>();
        range.forEach(i -> {
            if (i % 2 == 0) {
                addresses.add(Destination.topic(String.format("topic-%s-%d", infix, i), getDefaultPlan(AddressType.TOPIC)));
            } else {
                addresses.add(Destination.queue(String.format("queue-%s-%d", infix, i), getDefaultPlan(AddressType.QUEUE)));
            }
        });
        return addresses;
    }

    protected boolean sendMessage(AddressSpace addressSpace, AbstractClient client, KeycloakCredentials
            credentials, String address, String content, int count, boolean logToOutput) {
        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.USERNAME, credentials.getUsername());
        arguments.put(Argument.PASSWORD, credentials.getPassword());
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.MSG_CONTENT, content);
        arguments.put(Argument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(Argument.ADDRESS, address);
        arguments.put(Argument.COUNT, Integer.toString(count));
        client.setArguments(arguments);

        return client.run(logToOutput);
    }

    /**
     * attach N receivers into one address with default username/password
     */
    protected List<AbstractClient> attachReceivers(AddressSpace addressSpace, Destination destination,
                                                   int receiverCount) throws Exception {
        return attachReceivers(addressSpace, destination, receiverCount, defaultCredentials);
    }

    /**
     * attach N receivers into one address with own username/password
     */
    List<AbstractClient> attachReceivers(AddressSpace addressSpace, Destination destination,
                                         int receiverCount, KeycloakCredentials credentials) throws Exception {
        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(Argument.TIMEOUT, "120");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, credentials.getUsername());
        arguments.put(Argument.PASSWORD, credentials.getPassword());
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.ADDRESS, destination.getAddress());
        arguments.put(Argument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(Argument.CONN_PROPERTY, "connection_property2~testValue");

        List<AbstractClient> receivers = new ArrayList<>();
        for (int i = 0; i < receiverCount; i++) {
            RheaClientReceiver rec = new RheaClientReceiver();
            rec.setArguments(arguments);
            rec.runAsync(false);
            receivers.add(rec);
        }

        Thread.sleep(15000); //wait for attached
        return receivers;
    }

    /**
     * attach senders to destinations (for N-th destination is attached N+1 senders)
     */
    List<AbstractClient> attachSenders(AddressSpace addressSpace, List<Destination> destinations) {
        List<AbstractClient> senders = new ArrayList<>();

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(Argument.TIMEOUT, "60");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, defaultCredentials.getUsername());
        arguments.put(Argument.PASSWORD, defaultCredentials.getPassword());
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.MSG_CONTENT, "msg no.%d");
        arguments.put(Argument.COUNT, "30");
        arguments.put(Argument.DURATION, "30");
        arguments.put(Argument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(Argument.CONN_PROPERTY, "connection_property2~testValue");

        for (int i = 0; i < destinations.size(); i++) {
            arguments.put(Argument.ADDRESS, destinations.get(i).getAddress());
            for (int j = 0; j < i + 1; j++) {
                AbstractClient send = new RheaClientSender();
                send.setArguments(arguments);
                send.runAsync(false);
                senders.add(send);
            }
        }

        return senders;
    }

    /**
     * attach receivers to destinations (for N-th destination is attached N+1 senders)
     */
    List<AbstractClient> attachReceivers(AddressSpace addressSpace, List<Destination> destinations) {
        List<AbstractClient> receivers = new ArrayList<>();

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(Argument.TIMEOUT, "60");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, defaultCredentials.getUsername());
        arguments.put(Argument.PASSWORD, defaultCredentials.getPassword());
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(Argument.CONN_PROPERTY, "connection_property2~testValue");

        for (int i = 0; i < destinations.size(); i++) {
            arguments.put(Argument.ADDRESS, destinations.get(i).getAddress());
            for (int j = 0; j < i + 1; j++) {
                AbstractClient rec = new RheaClientReceiver();
                rec.setArguments(arguments);
                rec.runAsync(false);
                receivers.add(rec);
            }
        }

        return receivers;
    }

    /**
     * create M connections with N receivers and K senders
     */
    protected AbstractClient attachConnector(AddressSpace addressSpace, Destination destination,
                                             int connectionCount,
                                             int senderCount, int receiverCount, KeycloakCredentials credentials) {
        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(Argument.TIMEOUT, "120");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, credentials.getUsername());
        arguments.put(Argument.PASSWORD, credentials.getPassword());
        arguments.put(Argument.OBJECT_CONTROL, "CESR");
        arguments.put(Argument.ADDRESS, destination.getAddress());
        arguments.put(Argument.COUNT, Integer.toString(connectionCount));
        arguments.put(Argument.SENDER_COUNT, Integer.toString(senderCount));
        arguments.put(Argument.RECEIVER_COUNT, Integer.toString(receiverCount));
        arguments.put(Argument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(Argument.CONN_PROPERTY, "connection_property2~testValue");

        AbstractClient cli = new RheaClientConnector();
        cli.setArguments(arguments);
        cli.runAsync(false);

        return cli;
    }

    /**
     * stop all clients from list of Abstract clients
     */
    protected void stopClients(List<AbstractClient> clients) {
        log.info("Stopping clients...");
        clients.forEach(AbstractClient::stop);
    }

    /**
     * create users and groups for wildcard authz tests
     */
    protected List<KeycloakCredentials> createUsersWildcard(AddressSpace addressSpace, String groupPrefix) throws
            Exception {
        List<KeycloakCredentials> users = new ArrayList<>();
        if (addressSpace.getType() == AddressSpaceType.BROKERED) {
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_#", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_queue.#", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_topic.#", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_queue.*", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_topic.*", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_queueA*", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_topicA*", "password"));
        } else {
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_*", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_queue*", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_topic*", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_queue.*", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_topic.*", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_queue.A*", "password"));
            users.add(new KeycloakCredentials("user_" + groupPrefix + "_topic.A*", "password"));
        }

        for (KeycloakCredentials cred : users) {
            createUser(addressSpace, cred, cred.getUsername().replace("user_", ""));
        }
        return users;
    }

    protected List<Destination> getAddressesWildcard() {
        Destination queue = Destination.queue("queue.1234", getDefaultPlan(AddressType.QUEUE));
        Destination queue2 = Destination.queue("queue.ABCD", getDefaultPlan(AddressType.QUEUE));
        Destination topic = Destination.topic("topic.2345", getDefaultPlan(AddressType.TOPIC));
        Destination topic2 = Destination.topic("topic.ABCD", getDefaultPlan(AddressType.TOPIC));

        return Arrays.asList(queue, queue2, topic, topic2);
    }

    protected void logWithSeparator(Logger logger, String... messages) {
        logger.info("--------------------------------------------------------------------------------");
        for (String message : messages)
            logger.info(message);
    }

    //================================================================================================
    //==================================== Asserts methods ===========================================
    //================================================================================================
    protected void assertSorted(String message, Iterable list) throws Exception {
        assertSorted(message, list, false);
    }

    protected void assertSorted(String message, Iterable list, Comparator comparator) throws Exception {
        assertSorted(message, list, false, comparator);
    }

    protected void assertSorted(String message, Iterable list, boolean reverse) {
        log.info("Assert sort reverse: " + reverse);
        if (!reverse)
            assertTrue(Ordering.natural().isOrdered(list), message);
        else
            assertTrue(Ordering.natural().reverse().isOrdered(list), message);
    }

    protected void assertSorted(String message, Iterable list, boolean reverse, Comparator comparator) {
        log.info("Assert sort reverse: " + reverse);
        if (!reverse)
            assertTrue(Ordering.from(comparator).isOrdered(list), message);
        else
            assertTrue(Ordering.from(comparator).reverse().isOrdered(list), message);
    }

    protected void assertWaitForValue(int expected, Callable<Integer> fn, TimeoutBudget budget) throws Exception {
        Integer got = null;
        log.info("waiting for expected value '{}' ...", expected);
        while (budget.timeLeft() >= 0) {
            got = fn.call();
            if (got != null && expected == got.intValue()) {
                return;
            }
            Thread.sleep(100);
        }
        fail(String.format("Incorrect results value! expected: '%s', got: '%s'", expected, Objects.requireNonNull(got).intValue()));
    }

    protected void assertWaitForValue(int expected, Callable<Integer> fn) throws Exception {
        assertWaitForValue(expected, fn, new TimeoutBudget(2, TimeUnit.SECONDS));
    }

    /**
     * body for rest api tests
     */
    protected void runRestApiTest(AddressSpace addressSpace, Destination d1, Destination d2) throws Exception {
        List<String> destinationsNames = Arrays.asList(d1.getAddress(), d2.getAddress());
        setAddresses(addressSpace, d1);
        appendAddresses(addressSpace, d2);

        //d1, d2
        Future<List<String>> response = getAddresses(addressSpace, Optional.empty());
        assertThat("Rest api does not return all addresses", response.get(1, TimeUnit.MINUTES), is(destinationsNames));
        log.info("addresses {} successfully created", Arrays.toString(destinationsNames.toArray()));

        //get specific address d2
        response = getAddresses(addressSpace, Optional.ofNullable(TestUtils.sanitizeAddress(d2.getName())));
        assertThat("Rest api does not return specific address", response.get(1, TimeUnit.MINUTES), is(destinationsNames.subList(1, 2)));

        deleteAddresses(addressSpace, d1);

        //d2
        response = getAddresses(addressSpace, Optional.ofNullable(TestUtils.sanitizeAddress(d2.getName())));
        assertThat("Rest api does not return right addresses", response.get(1, TimeUnit.MINUTES), is(destinationsNames.subList(1, 2)));
        log.info("address {} successfully deleted", d1.getAddress());

        deleteAddresses(addressSpace, d2);

        //empty
        response = getAddresses(addressSpace, Optional.empty());
        assertThat("Rest api returns addresses", response.get(1, TimeUnit.MINUTES), is(Collections.emptyList()));
        log.info("addresses {} successfully deleted", d2.getAddress());

        setAddresses(addressSpace, d1, d2);
        deleteAddresses(addressSpace, d1, d2);

        response = getAddresses(addressSpace, Optional.empty());
        assertThat("Rest api returns addresses", response.get(1, TimeUnit.MINUTES), is(Collections.emptyList()));
        log.info("addresses {} successfully deleted", Arrays.toString(destinationsNames.toArray()));
    }

    protected void sendReceiveLargeMessage(JmsProvider jmsProvider, int sizeInMB, Destination dest, int count) throws Exception {
        sendReceiveLargeMessage(jmsProvider, sizeInMB, dest, count, DeliveryMode.NON_PERSISTENT);
    }

    protected void sendReceiveLargeMessage(JmsProvider jmsProvider, int sizeInMB, Destination dest, int count, int mode) throws Exception {
        int size = sizeInMB * 1024 * 1024;

        Session session = jmsProvider.getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        javax.jms.Queue testQueue = (javax.jms.Queue) jmsProvider.getDestination(dest.getAddress());
        List<javax.jms.Message> messages = jmsProvider.generateMessages(session, count, size);

        MessageProducer sender = session.createProducer(testQueue);
        MessageConsumer receiver = session.createConsumer(testQueue);
        List<javax.jms.Message> recvd;

        jmsProvider.sendMessages(sender, messages, mode, javax.jms.Message.DEFAULT_PRIORITY, javax.jms.Message.DEFAULT_TIME_TO_LIVE);
        log.info("{}MB {} message sent", sizeInMB, mode == DeliveryMode.PERSISTENT ? "durable" : "non-durable");

        recvd = jmsProvider.receiveMessages(receiver, count, 2000);
        assertThat("Wrong count of received messages", recvd.size(), Matchers.is(count));
        log.info("{}MB {} message received", sizeInMB, mode == DeliveryMode.PERSISTENT ? "durable" : "non-durable");
    }

    protected void deleteAddressSpaceCreatedBySC(String namespace, AddressSpace addressSpace) throws Exception {
        TestUtils.deleteNamespace(kubernetes, addressSpace, namespace, logCollector);
    }
}
