/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases;

import com.google.common.collect.Ordering;
import com.sun.jndi.toolkit.url.Uri;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.clients.AbstractClient;
import io.enmasse.systemtest.clients.Argument;
import io.enmasse.systemtest.clients.ArgumentMap;
import io.enmasse.systemtest.clients.rhea.RheaClientConnector;
import io.enmasse.systemtest.clients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.clients.rhea.RheaClientSender;
import io.enmasse.systemtest.mqtt.MqttClient;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.resources.AddressPlan;
import io.enmasse.systemtest.resources.AddressSpacePlan;
import io.enmasse.systemtest.resources.ResourceDefinition;
import io.enmasse.systemtest.resources.SchemaData;
import io.enmasse.systemtest.selenium.ConsoleWebPage;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * Base class for all tests
 */
public abstract class TestBase extends SystemTestRunListener {
    private static Logger log = CustomLogger.getLogger();

    protected static final Environment environment = new Environment();

    protected static final Kubernetes kubernetes = Kubernetes.create(environment);
    private static final GlobalLogCollector logCollector = new GlobalLogCollector(kubernetes,
            new File(environment.testLogDir()));
    protected static final AddressApiClient addressApiClient = new AddressApiClient(kubernetes);
    protected static final PlansProvider plansProvider = new PlansProvider(kubernetes);

    protected String username;
    protected String password;
    protected AmqpClientFactory amqpClientFactory;
    protected MqttClientFactory mqttClientFactory;
    private List<AddressSpace> addressSpaceList = new ArrayList<>();
    protected KeycloakCredentials managementCredentials = new KeycloakCredentials(null, null);
    private BrokerManagement brokerManagement = new ArtemisManagement();
    private KeycloakClient keycloakApiClient;

    protected static void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        if (TestUtils.existAddressSpace(addressApiClient, addressSpace.getName())) {
            logCollector.collectEvents(addressSpace.getNamespace());
            logCollector.collectLogsTerminatedPods(addressSpace.getNamespace());
            logCollector.collectConfigMaps(addressSpace.getNamespace());
            addressApiClient.deleteAddressSpace(addressSpace);
            TestUtils.waitForAddressSpaceDeleted(kubernetes, addressSpace);
            logCollector.stopCollecting(addressSpace.getNamespace());
        } else {
            log.info("Address space '" + addressSpace + "' doesn't exists!");
        }
    }

    protected AddressSpace getSharedAddressSpace() {
        return null;
    }

    protected abstract String getDefaultPlan(AddressType addressType);


    @Before
    public void setup() throws Exception {
        addressSpaceList = new ArrayList<>();
        amqpClientFactory = new AmqpClientFactory(kubernetes, environment, null, username, password);
        mqttClientFactory = new MqttClientFactory(kubernetes, environment, null, username, password);
    }

    @After
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
            extraWait = extraWait ? extraWait : !isBrokered(addressSpace);
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
        AddressSpace addrSpaceResponse = null;
        if (!TestUtils.existAddressSpace(addressApiClient, addressSpace.getName())) {
            log.info("Address space '" + addressSpace + "' doesn't exist and will be created.");
            addressApiClient.createAddressSpace(addressSpace);
            logCollector.startCollecting(addressSpace.getNamespace());
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

    protected KeycloakClient getKeycloakClient() throws Exception {
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
        TestUtils.deploy(addressApiClient, kubernetes, budget, addressSpace, HttpMethod.POST, destinations);
        logCollector.collectConfigMaps(addressSpace.getNamespace());
    }


    protected void setAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.deploy(addressApiClient, kubernetes, budget, addressSpace, HttpMethod.PUT, destinations);
        logCollector.collectConfigMaps(addressSpace.getNamespace());
    }

    protected void appendAddresses(AddressSpace addressSpace, TimeoutBudget timeout, Destination... destinations) throws Exception {
        TestUtils.deploy(addressApiClient, kubernetes, timeout, addressSpace, HttpMethod.POST, destinations);
    }


    protected void setAddresses(AddressSpace addressSpace, TimeoutBudget timeout, Destination... destinations) throws Exception {
        TestUtils.deploy(addressApiClient, kubernetes, timeout, addressSpace, HttpMethod.PUT, destinations);
    }

    protected List<Uri> getAddressesPaths() throws Exception {
        return TestUtils.getAddressesPaths(addressApiClient);
    }

    protected JsonObject sendRestApiRequest(HttpMethod method, Uri uri, Optional<JsonObject> payload) throws Exception {
        return TestUtils.sendRestApiRequest(addressApiClient, method, uri, payload);
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
    protected void scale(AddressSpace addressSpace, Destination destination, int numReplicas, long checkInterval) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(5, TimeUnit.MINUTES);
        TestUtils.setReplicas(kubernetes, addressSpace, destination, numReplicas, budget, checkInterval);
    }

    protected void scale(AddressSpace addressSpace, Destination destination, int numReplicas) throws Exception {
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
        getKeycloakClient().createGroup(addressSpace.getName(), groupName);
    }

    protected void joinGroup(AddressSpace addressSpace, String groupName, String username) throws Exception {
        getKeycloakClient().joinGroup(addressSpace.getName(), groupName, username);
    }

    protected void leaveGroup(AddressSpace addressSpace, String groupName, String username) throws Exception {
        getKeycloakClient().leaveGroup(addressSpace.getName(), groupName, username);
    }

    protected void createUser(AddressSpace addressSpace, String username, String password) throws Exception {
        getKeycloakClient().createUser(addressSpace.getName(), username, password);
    }

    protected void removeUser(AddressSpace addressSpace, String username) throws Exception {
        getKeycloakClient().deleteUser(addressSpace.getName(), username);
    }

    protected void createUsers(AddressSpace addressSpace, String prefixName, String prefixPswd, int from, int to)
            throws Exception {
        for (int i = from; i < to; i++) {
            createUser(addressSpace, prefixName + i, prefixPswd + i);
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

    protected void assertCanConnect(AddressSpace addressSpace, String username, String password, List<Destination> destinations) throws Exception {
        assertTrue("Client failed, cannot connect under user " + username,
                canConnectWithAmqp(addressSpace, username, password, destinations));
        // TODO: Enable this when mqtt is stable enough
        // assertTrue(canConnectWithMqtt(addressSpace, username, password));
    }

    protected void assertCannotConnect(AddressSpace addressSpace, String username, String password, List<Destination> destinations) throws Exception {
        try {
            assertFalse("Client failed, can connect under user " + username,
                    canConnectWithAmqp(addressSpace, username, password, destinations));
            fail("Expected connection to timeout");
        } catch (ConnectTimeoutException e) {
        }

        // TODO: Enable this when mqtt is stable enough
        // assertFalse(canConnectWithMqtt(addressSpace, username, password));
    }


    private boolean canConnectWithAmqp(AddressSpace addressSpace, String username, String password, List<Destination> destinations) throws Exception {
        for (Destination destination : destinations) {
            String message = String.format("Client failed, cannot connect to %s under user %s", destination.getType(), username);
            switch (destination.getType()) {
                case "queue":
                    assertTrue(message, canConnectWithAmqpToQueue(addressSpace, username, password, destination.getAddress()));
                    break;
                case "topic":
                    assertTrue(message, canConnectWithAmqpToTopic(addressSpace, username, password, destination.getAddress()));
                    break;
                case "multicast":
                    if (!isBrokered(addressSpace))
                        assertTrue(message, canConnectWithAmqpToMulticast(addressSpace, username, password, destination.getAddress()));
                    break;
                case "anycast":
                    if (!isBrokered(addressSpace))
                        assertTrue(message, canConnectWithAmqpToAnycast(addressSpace, username, password, destination.getAddress()));
                    break;
            }
        }
        return true;
    }

    private boolean canConnectWithMqtt(String name, String username, String password) throws Exception {
        AddressSpace addressSpace = new AddressSpace(name);
        MqttClient client = mqttClientFactory.createClient(addressSpace);
        MqttConnectOptions options = client.getMqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());

        Future<List<MqttMessage>> received = client.recvMessages("t1", 1);
        Future<Integer> sent = client.sendMessages("t1", Arrays.asList("msgt1"));

        return (sent.get(1, TimeUnit.MINUTES) == received.get(1, TimeUnit.MINUTES).size());
    }

    protected boolean canConnectWithAmqpToQueue(AddressSpace addressSpace, String username, String password, String queueAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<Integer> sent = client.sendMessages(queueAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);
        Future<List<Message>> received = client.recvMessages(queueAddress, 1, 10, TimeUnit.SECONDS);

        return (sent.get(10, TimeUnit.SECONDS) == received.get(10, TimeUnit.SECONDS).size());
    }

    protected boolean canConnectWithAmqpToAnycast(AddressSpace addressSpace, String username, String password, String anycastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(anycastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(anycastAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(10, TimeUnit.SECONDS) == received.get(10, TimeUnit.SECONDS).size());
    }

    protected boolean canConnectWithAmqpToMulticast(AddressSpace addressSpace, String username, String password, String multicastAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createBroadcastClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(multicastAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(multicastAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

        return (sent.get(10, TimeUnit.SECONDS) == received.get(10, TimeUnit.SECONDS).size());
    }

    protected boolean canConnectWithAmqpToTopic(AddressSpace addressSpace, String username, String password, String topicAddress) throws InterruptedException, IOException, TimeoutException, ExecutionException {
        AmqpClient client = amqpClientFactory.createTopicClient(addressSpace);
        client.getConnectOptions().setUsername(username).setPassword(password);

        Future<List<Message>> received = client.recvMessages(topicAddress, 1, 10, TimeUnit.SECONDS);
        Future<Integer> sent = client.sendMessages(topicAddress, Arrays.asList("msg1"), 10, TimeUnit.SECONDS);

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

    protected FirefoxDriver getFirefoxDriver() {
        FirefoxOptions opts = new FirefoxOptions();
        opts.setHeadless(true);
        return new FirefoxDriver(opts);
    }

    protected ChromeDriver getChromeDriver() {
        ChromeOptions opts = new ChromeOptions();
        opts.setHeadless(true);
        opts.addArguments("--no-sandbox");
        return new ChromeDriver(opts);
    }

    /**
     * selenium provider with Firefox webdriver
     */
    protected SeleniumProvider getFirefoxSeleniumProvider() throws Exception {
        SeleniumProvider seleniumProvider = new SeleniumProvider();
        seleniumProvider.setupDriver(environment, kubernetes, getFirefoxDriver());
        return seleniumProvider;
    }

    protected void waitForSubscribersConsole(AddressSpace addressSpace, Destination destination, int expectedCount) throws Exception {
        int budget = 60; //seconds
        waitForSubscribersConsole(addressSpace, destination, expectedCount, budget);
    }

    /**
     * wait for expected count of subscribers on topic (check via console)
     *
     * @param budget timeout budget in seconds
     */
    protected void waitForSubscribersConsole(AddressSpace addressSpace, Destination destination, int expectedCount, int budget) {
        SeleniumProvider selenium = null;
        try {
            selenium = getFirefoxSeleniumProvider();
            ConsoleWebPage console = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressApiClient, addressSpace, username, password);
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
    protected void waitForSubscribers(AddressSpace addressSpace, String topic, int expectedCount) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        waitForSubscribers(addressSpace, topic, expectedCount, budget);
    }

    protected void waitForSubscribers(AddressSpace addressSpace, String topic, int expectedCount, TimeoutBudget budget) throws Exception {
        AmqpClient queueClient = null;
        try {
            queueClient = amqpClientFactory.createQueueClient(addressSpace);
            queueClient.setConnectOptions(queueClient.getConnectOptions()
                    .setUsername(managementCredentials.getUsername())
                    .setPassword(managementCredentials.getPassword()));
            String replyQueueName = "reply-queue";
            Destination replyQueue = Destination.queue(replyQueueName, getDefaultPlan(AddressType.QUEUE));
            appendAddresses(addressSpace, replyQueue);

            boolean done = false;
            int actualSubscribers = 0;
            do {
                actualSubscribers = getSubscriberCount(queueClient, replyQueue, topic);
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
        } catch (Exception ex) {
            throw ex;
        } finally {
            queueClient.close();
        }
    }

    protected void waitForBrokerReplicas(AddressSpace addressSpace, Destination destination, int expectedReplicas) throws InterruptedException {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        TestUtils.waitForNBrokerReplicas(kubernetes, addressSpace.getNamespace(), expectedReplicas, destination, budget);
    }

    protected void waitForRouterReplicas(AddressSpace addressSpace, int expectedReplicas) throws InterruptedException {
        TimeoutBudget budget = new TimeoutBudget(1, TimeUnit.MINUTES);
        TestUtils.waitForNReplicas(kubernetes, addressSpace.getNamespace(), expectedReplicas, Collections.singletonMap("name", "qdrouterd"), budget);
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
    protected List<String> getBrokerQueueNames(AmqpClient queueClient, Destination replyQueue, String topic) throws Exception {
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
    protected int getSubscriberCount(AmqpClient queueClient, Destination replyQueue, String topic) throws Exception {
        List<String> queueNames = getBrokerQueueNames(queueClient, replyQueue, topic);

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

    protected boolean sendMessage(AddressSpace addressSpace, AbstractClient client, String username, String password,
                                  String address, String content, int count, boolean logToOutput) throws Exception {
        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
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
    protected List<AbstractClient> attachReceivers(AddressSpace addressSpace, Destination destination, int receiverCount) throws Exception {
        return attachReceivers(addressSpace, destination, receiverCount, username, password);
    }

    /**
     * attach N receivers into one address with own username/password
     */
    protected List<AbstractClient> attachReceivers(AddressSpace addressSpace, Destination destination, int receiverCount, String username, String password) throws Exception {
        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(Argument.TIMEOUT, "120");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.ADDRESS, destination.getAddress());
        arguments.put(Argument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(Argument.CONN_PROPERTY, "connection_property2~testValue");

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

    /**
     * attach senders to destinations (for N-th destination is attached N+1 senders)
     */
    protected List<AbstractClient> attachSenders(AddressSpace addressSpace, List<Destination> destinations) throws Exception {
        List<AbstractClient> senders = new ArrayList<>();

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(Argument.TIMEOUT, "60");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
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
                send.runAsync();
                senders.add(send);
            }
        }

        return senders;
    }

    /**
     * attach receivers to destinations (for N-th destination is attached N+1 senders)
     */
    protected List<AbstractClient> attachReceivers(AddressSpace addressSpace, List<Destination> destinations) throws Exception {
        List<AbstractClient> receivers = new ArrayList<>();

        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(Argument.TIMEOUT, "60");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.LOG_MESSAGES, "json");
        arguments.put(Argument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(Argument.CONN_PROPERTY, "connection_property2~testValue");

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

    /**
     * create M connections with N receivers and K senders
     */
    protected AbstractClient attachConnector(AddressSpace addressSpace, Destination destination, int connectionCount,
                                             int senderCount, int receiverCount, String username, String password) throws Exception {
        ArgumentMap arguments = new ArgumentMap();
        arguments.put(Argument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(Argument.TIMEOUT, "120");
        arguments.put(Argument.CONN_SSL, "true");
        arguments.put(Argument.USERNAME, username);
        arguments.put(Argument.PASSWORD, password);
        arguments.put(Argument.OBJECT_CONTROL, "CESR");
        arguments.put(Argument.ADDRESS, destination.getAddress());
        arguments.put(Argument.COUNT, Integer.toString(connectionCount));
        arguments.put(Argument.SENDER_COUNT, Integer.toString(senderCount));
        arguments.put(Argument.RECEIVER_COUNT, Integer.toString(receiverCount));
        arguments.put(Argument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(Argument.CONN_PROPERTY, "connection_property2~testValue");

        AbstractClient cli = new RheaClientConnector();
        cli.setArguments(arguments);
        cli.runAsync();

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
    protected List<KeycloakCredentials> createUsersWildcard(AddressSpace addressSpace, String groupPrefix) throws Exception {
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
            getKeycloakClient().createUser(addressSpace.getName(), cred.getUsername(), cred.getPassword(),
                    cred.getUsername().replace("user_", ""));
        }

        return users;
    }

    protected List<Destination> getAddressesWildcard() throws Exception {
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
    //==================================== Config maps operations ====================================
    //================================================================================================

    //===================
    //Address config-maps
    //===================
    protected void createAddressPlanConfig(AddressPlan addressPlan) {
        createAddressPlanConfig(addressPlan, false);
    }

    protected void createAddressPlanConfig(AddressPlan addressPlan, boolean replaceExisting) {
        TestUtils.createAddressPlanConfig(kubernetes, addressPlan, replaceExisting);
    }

    protected AddressPlan getAddressPlanConfig(String configName) throws NotImplementedException {
        return TestUtils.getAddressPlanConfig(configName);
    }

    protected boolean removeAddressPlanConfig(AddressPlan addressPlan) throws NotImplementedException {
        return TestUtils.removeAddressPlanConfig(kubernetes, addressPlan);
    }

    protected void appendAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        TestUtils.appendAddressPlan(kubernetes, addressPlan, addressSpacePlan);
    }

    protected boolean removeAddressPlan(AddressPlan addressPlan, AddressSpacePlan addressSpacePlan) {
        return TestUtils.removeAddressPlan(kubernetes, addressPlan, addressSpacePlan);
    }

    //=========================
    //Address space config-maps
    //=========================

    protected void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        createAddressSpacePlanConfig(addressSpacePlan, false);
    }

    protected void createAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan, boolean replaceExisting) {
        TestUtils.createAddressSpacePlanConfig(kubernetes, addressSpacePlan, replaceExisting);
    }

    protected AddressSpacePlan getAddressSpacePlanConfig(String config) {
        return TestUtils.getAddressSpacePlanConfig(kubernetes, config);
    }

    protected boolean removeAddressSpacePlanConfig(AddressSpacePlan addressSpacePlan) {
        return TestUtils.removeAddressSpacePlanConfig(kubernetes, addressSpacePlan);
    }

    //=========================
    //Resource definition config-maps
    //=========================

    protected void createResourceDefinitionConfig(ResourceDefinition resourceDefinition) {
        createResourceDefinitionConfig(resourceDefinition, false);
    }

    protected void createResourceDefinitionConfig(ResourceDefinition resourceDefinition, boolean replaceExisting) {
        TestUtils.createResourceDefinitionConfig(kubernetes, resourceDefinition, replaceExisting);
    }

    protected ResourceDefinition getResourceDefinitionConfig(String config) {
        return TestUtils.getResourceDefinitionConfig(kubernetes, config);
    }

    protected boolean removeResourceDefinitionConfig(ResourceDefinition resourceDefinition) {
        return TestUtils.removeResourceDefinitionConfig(kubernetes, resourceDefinition);
    }


    //================================================================================================
    //==================================== Asserts methods ===========================================
    //================================================================================================
    public void assertSorted(String message, Iterable list) throws Exception {
        assertSorted(message, list, false);
    }

    public void assertSorted(String message, Iterable list, Comparator comparator) throws Exception {
        assertSorted(message, list, false, comparator);
    }

    public void assertSorted(String message, Iterable list, boolean reverse) throws Exception {
        log.info("Assert sort reverse: " + reverse);
        if (!reverse)
            assertTrue(message, Ordering.natural().isOrdered(list));
        else
            assertTrue(message, Ordering.natural().reverse().isOrdered(list));
    }

    public void assertSorted(String message, Iterable list, boolean reverse, Comparator comparator) throws Exception {
        log.info("Assert sort reverse: " + reverse);
        if (!reverse)
            assertTrue(message, Ordering.from(comparator).isOrdered(list));
        else
            assertTrue(message, Ordering.from(comparator).reverse().isOrdered(list));
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
        fail(String.format("Incorrect results value! expected: '%s', got: '%s'", expected, got.intValue()));
    }

    protected void assertWaitForValue(int expected, Callable<Integer> fn) throws Exception {
        assertWaitForValue(expected, fn, new TimeoutBudget(2, TimeUnit.SECONDS));
    }
}
