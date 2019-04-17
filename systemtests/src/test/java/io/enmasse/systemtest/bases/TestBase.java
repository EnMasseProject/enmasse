/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases;

import com.google.common.collect.Ordering;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceSchemaList;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.ability.ITestBase;
import io.enmasse.systemtest.ability.ITestSeparator;
import io.enmasse.systemtest.ability.TestWatcher;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.amqp.ReceiverStatus;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.apiclients.AddressApiClient;
import io.enmasse.systemtest.apiclients.UserApiClient;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientConnector;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.selenium.SeleniumManagement;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.proton.sasl.SaslSystemException;
import org.apache.qpid.proton.message.Message;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.security.sasl.AuthenticationException;
import java.io.File;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TimeoutBudget.ofDuration;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.time.Duration.ofMinutes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base class for all tests
 */
@ExtendWith(TestWatcher.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestBase implements ITestBase, ITestSeparator {
    protected static final Environment environment = Environment.getInstance();
    protected static final UserCredentials clusterUser = new UserCredentials(KubeCMDClient.getOCUser());
    protected static final Kubernetes kubernetes = Kubernetes.getInstance();
    protected static final GlobalLogCollector logCollector = new GlobalLogCollector(kubernetes,
            new File(environment.testLogDir()));
    protected static AddressApiClient addressApiClient;
    private static Logger log = CustomLogger.getLogger();
    protected AmqpClientFactory amqpClientFactory;
    protected MqttClientFactory mqttClientFactory;
    protected UserCredentials managementCredentials = new UserCredentials(null, null);
    protected UserCredentials defaultCredentials = new UserCredentials(null, null);
    private List<AddressSpace> addressSpaceList = new ArrayList<>();
    private UserApiClient userApiClient;
    private boolean reuseAddressSpace;

    @BeforeEach
    public void setup() throws Exception {
        if (addressApiClient == null) {
            addressApiClient = new AddressApiClient(kubernetes);
        }
        if (!reuseAddressSpace) {
            addressSpaceList = new ArrayList<>();
        }
        amqpClientFactory = new AmqpClientFactory(kubernetes, environment, null, defaultCredentials);
        mqttClientFactory = new MqttClientFactory(kubernetes, environment, null, defaultCredentials);
    }

    @AfterEach
    public void teardown() throws Exception {
        try {
            if (mqttClientFactory != null) {
                mqttClientFactory.close();
            }
            if (amqpClientFactory != null) {
                amqpClientFactory.close();
            }

            if (!environment.skipCleanup() && !reuseAddressSpace) {
                deleteAddressspacesFromList();
            } else {
                log.warn("Remove address spaces in tear down - SKIPPED!");
            }
        } catch (Exception e) {
            log.error("Error tearing down test: {}", e.getMessage());
            throw e;
        }
    }

    protected void setReuseAddressSpace() {
        reuseAddressSpace = true;
    }

    protected void unsetReuseAddressSpace() {
        reuseAddressSpace = false;
    }


    //================================================================================================
    //==================================== AddressSpace methods ======================================
    //================================================================================================

    protected void createAddressSpace(AddressSpace addressSpace) throws Exception {
        createAddressSpace(addressSpace, addressApiClient);
    }

    protected void createAddressSpaceList(AddressSpace... addressSpaces) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_ADDRESS_SPACE);
        ArrayList<AddressSpace> spaces = new ArrayList<>();
        for (AddressSpace addressSpace : addressSpaces) {
            if (!AddressSpaceUtils.existAddressSpace(addressApiClient, addressSpace.getMetadata().getName())) {
                log.info("Address space '" + addressSpace + "' doesn't exist and will be created.");
                spaces.add(addressSpace);
                if (!addressSpace.equals(getSharedAddressSpace())) {
                    addressSpaceList.add(addressSpace);
                }
            } else {
                log.warn("Address space '" + addressSpace + "' already exists.");
                AddressSpaceUtils.getAddressSpaceObject(addressApiClient, addressSpace.getMetadata().getName());
            }
        }
        addressApiClient.createAddressSpaceList(spaces.toArray(new AddressSpace[0]));
        for (AddressSpace addressSpace : spaces) {
            AddressSpaceUtils.waitForAddressSpaceReady(addressApiClient, addressSpace);
            AddressSpaceUtils.syncAddressSpaceObject(addressSpace, addressApiClient);
            logCollector.startCollecting(addressSpace);
            log.info("Address space is ready for use - {}", addressSpace);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }

    protected void createAddressSpace(AddressSpace addressSpace, AddressApiClient apiClient) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_ADDRESS_SPACE);
        if (!AddressSpaceUtils.existAddressSpace(apiClient, addressSpace.getMetadata().getName())) {
            log.info("Address space '{}' doesn't exist and will be created.", addressSpace);
            apiClient.createAddressSpace(addressSpace);
            AddressSpaceUtils.waitForAddressSpaceReady(apiClient, addressSpace);

            if (!addressSpace.equals(getSharedAddressSpace())) {
                addressSpaceList.add(addressSpace);
            }
        } else {
            AddressSpaceUtils.waitForAddressSpaceReady(apiClient, addressSpace);
            log.info("Address space '" + addressSpace + "' already exists.");
        }
        AddressSpaceUtils.syncAddressSpaceObject(addressSpace, apiClient);
        logCollector.startCollecting(addressSpace);
        log.info("Address space is ready for use - {}", addressSpace);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    protected void deleteAddressspacesFromList() throws Exception {
        log.info("All addressspaces will be removed");
        for (AddressSpace addressSpace : addressSpaceList) {
            deleteAddressSpace(addressSpace);
        }
        addressSpaceList.clear();
    }

    protected void addToAddressSpacess(AddressSpace addressSpace) {
        this.addressSpaceList.add(addressSpace);
    }

    protected static void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        deleteAddressSpace(addressSpace, addressApiClient);
    }

    protected static void deleteAddressSpace(AddressSpace addressSpace, AddressApiClient apiClient) throws Exception {
        if (AddressSpaceUtils.existAddressSpace(apiClient, addressSpace.getMetadata().getName())) {
            AddressSpaceUtils.deleteAddressSpaceAndWait(apiClient, kubernetes, addressSpace, logCollector);
        } else {
            log.info("Address space '" + addressSpace + "' doesn't exists!");
        }
    }

    protected void deleteAllAddressSpaces() throws Exception {
        AddressSpaceUtils.deleteAllAddressSpaces(addressApiClient, logCollector);
        for (AddressSpace addressSpace : addressSpaceList) {
            AddressSpaceUtils.waitForAddressSpaceDeleted(kubernetes, addressSpace);
        }
    }

    protected AddressSpace getSharedAddressSpace() {
        return null;
    }

    protected void replaceAddressSpace(AddressSpace addressSpace) throws Exception {
        replaceAddressSpace(addressSpace, true);
    }

    protected void replaceAddressSpace(AddressSpace addressSpace, boolean waitForPlanApplied) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.UPDATE_ADDRESS_SPACE);
        if (AddressSpaceUtils.existAddressSpace(addressApiClient, addressSpace.getMetadata().getName())) {
            log.info("Address space '{}' exists and will be updated.", addressSpace);
            final String currentResourceVersion = AddressSpaceUtils.jsonToAdressSpace(addressApiClient.getAddressSpace(addressSpace.getMetadata().getName())).getMetadata().getResourceVersion();
            addressApiClient.replaceAddressSpace(addressSpace);
            TestUtils.waitForChangedResourceVersion(ofDuration(ofMinutes(5)), addressApiClient, addressSpace.getMetadata().getName(), currentResourceVersion);
            if (waitForPlanApplied) {
                AddressSpaceUtils.waitForAddressSpacePlanApplied(addressApiClient, addressSpace);
            }
            AddressSpaceUtils.waitForAddressSpaceReady(addressApiClient, addressSpace);

            if (!addressSpace.equals(getSharedAddressSpace())) {
                addressSpaceList.add(addressSpace);
            }
        } else {
            AddressSpaceUtils.getAddressSpaceObject(addressApiClient, addressSpace.getMetadata().getName());
            log.info("Address space '{}' does not exists.", addressSpace);
        }
        AddressSpaceUtils.syncAddressSpaceObject(addressSpace, addressApiClient);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    protected AddressSpace getAddressSpace(String name) throws Exception {
        return AddressSpaceUtils.getAddressSpaceObject(addressApiClient, name);
    }

    protected List<AddressSpace> getAddressSpaces() throws Exception {
        return AddressSpaceUtils.getAddressSpacesObjects(addressApiClient);
    }

    protected void waitForAddressSpaceReady(AddressSpace addressSpace) throws Exception {
        waitForAddressSpaceReady(addressSpace, addressApiClient);
    }

    protected void waitForAddressSpaceReady(AddressSpace addressSpace, AddressApiClient apiClient) throws Exception {
        AddressSpaceUtils.waitForAddressSpaceReady(apiClient, addressSpace);
    }

    protected void waitForAddressSpacePlanApplied(AddressSpace addressSpace) throws Exception {
        AddressSpaceUtils.waitForAddressSpacePlanApplied(addressApiClient, addressSpace);
    }

    protected void reloadAddressSpaceEndpoints(AddressSpace addressSpace) throws Exception {
        AddressSpaceUtils.syncAddressSpaceObject(addressSpace, addressApiClient);
    }

    //================================================================================================
    //====================================== Address methods =========================================
    //================================================================================================

    protected void deleteAddresses(AddressSpace addressSpace, Address... destinations) throws Exception {
        logCollector.collectConfigMaps();
        logCollector.collectRouterState("deleteAddresses");
        AddressUtils.delete(addressApiClient, addressSpace, destinations);
    }

    protected void appendAddresses(AddressSpace addressSpace, Address... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        appendAddresses(addressSpace, budget, destinations);
    }

    protected void appendAddresses(AddressSpace addressSpace, TimeoutBudget timeout, Address... destinations) throws Exception {
        appendAddresses(addressSpace, true, timeout, destinations);
    }

    protected void appendAddresses(AddressSpace addressSpace, boolean wait, Address... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        appendAddresses(addressSpace, wait, budget, destinations);
    }

    protected void appendAddresses(AddressSpace addressSpace, boolean wait, TimeoutBudget timeout, Address... destinations) throws Exception {
        AddressUtils.appendAddresses(addressApiClient, kubernetes, timeout, addressSpace, wait, destinations);
        logCollector.collectConfigMaps();
    }

    protected void appendAddresses(AddressSpace addressSpace, boolean wait, int batchSize, Address... destinations) throws Exception {
        TimeoutBudget timeout = new TimeoutBudget(15, TimeUnit.MINUTES);
        AddressUtils.appendAddresses(addressApiClient, kubernetes, timeout, addressSpace, wait, batchSize, destinations);
        logCollector.collectConfigMaps();
    }

    protected void setAddresses(AddressSpace addressSpace, int expectedCode, Address... addresses) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        logCollector.collectRouterState("setAddresses");

        if (expectedCode == HTTP_CONFLICT) {
            try {
                setAddresses(addressSpace, budget, expectedCode, addresses);
            } catch (ExecutionException ee) {
                log.info(ee.getMessage());
                throw new AddressAlreadyExistsException("Address cannot be created, already exists");
            }
        } else {
            setAddresses(addressSpace, budget, expectedCode, addresses);
        }
    }

    protected void setAddresses(AddressSpace addressSpace, Address... addresses) throws Exception {
        setAddresses(addressSpace, HttpURLConnection.HTTP_CREATED, addresses);
    }


    protected void setAddresses(AddressSpace addressSpace, TimeoutBudget timeout, Address... addresses) throws Exception {
        setAddresses(addressSpace, timeout, HttpURLConnection.HTTP_CREATED, addresses);
    }

    protected void setAddresses(AddressSpace addressSpace, TimeoutBudget timeout, int expectedCode, Address... addresses) throws Exception {
        AddressUtils.setAddresses(addressApiClient, kubernetes, timeout, addressSpace, true, expectedCode, addresses);
        logCollector.collectConfigMaps();
    }

    protected void setAddresses(AddressSpace addressSpace, AddressApiClient apiClient, Address... addresses) throws Exception {
        AddressUtils.setAddresses(apiClient, kubernetes, new TimeoutBudget(15, TimeUnit.MINUTES), addressSpace, true, HTTP_CREATED, addresses);
        logCollector.collectConfigMaps();
    }

    protected void replaceAddress(AddressSpace addressSpace, Address destination) throws Exception {
        AddressUtils.replaceAddress(addressApiClient, addressSpace, destination, true, new TimeoutBudget(3, TimeUnit.MINUTES));
    }

    /**
     * give you a list of names of all deployed addresses (or single deployed address)
     *
     * @param addressName name of single address
     * @return list of addresses
     * @throws Exception
     */
    protected Future<List<String>> getAddresses(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        return AddressUtils.getAddresses(addressApiClient, addressSpace, addressName, new ArrayList<>());
    }

    /**
     * give you a list of objects of all deployed addresses (or single deployed address)
     *
     * @param addressName name of single address
     * @return list of addresses
     * @throws Exception
     */
    protected Future<List<Address>> getAddressesObjects(AddressSpace addressSpace, Optional<String> addressName, Optional<HashMap<String, String>> requestParams) throws Exception {
        return AddressUtils.getAddressesObjects(addressApiClient, addressSpace, addressName, requestParams, new ArrayList<>());
    }

    protected Future<List<Address>> getAddressesObjects(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        return getAddressesObjects(addressSpace, addressName, Optional.empty());
    }

    //================================================================================================
    //======================================= User methods ===========================================
    //================================================================================================

    protected UserApiClient getUserApiClient() throws Exception {
        if (userApiClient == null) {
            userApiClient = new UserApiClient(kubernetes);
        }
        return userApiClient;
    }

    protected void setUserApiClient(UserApiClient apiClient) {
        this.userApiClient = apiClient;
    }

    protected JsonObject createUser(AddressSpace addressSpace, UserCredentials credentials) throws Exception {
        log.info("User {} in address space {} will be created", credentials, addressSpace.getMetadata().getName());
        if (!userExist(addressSpace, credentials.getUsername())) {
            return getUserApiClient().createUser(addressSpace.getMetadata().getName(), credentials);
        }
        return new JsonObject();
    }

    protected JsonObject createUser(AddressSpace addressSpace, User user) throws Exception {
        log.info("User {} in address space {} will be created", user, addressSpace.getMetadata().getName());
        if (!userExist(addressSpace, user.getSpec().getUsername())) {
            return getUserApiClient().createUser(addressSpace.getMetadata().getName(), user);
        }
        return new JsonObject();
    }

    protected JsonObject createUserServiceAccount(AddressSpace addressSpace, UserCredentials credentials, String namespace) throws Exception {
        User user = UserUtils.createUserObject(UserAuthenticationType.serviceaccount, credentials);
        return createUserServiceaccount(addressSpace, user, namespace);
    }

    protected JsonObject createUserServiceaccount(AddressSpace addressSpace, User user, String namespace) throws Exception {
        log.info("ServiceAccount user {} in address space {} will be created", user.getSpec().getUsername(), addressSpace.getMetadata().getName());
        if (!userExist(addressSpace, user.getSpec().getUsername())) {
            String serviceaccountName = kubernetes.createServiceAccount(user.getSpec().getUsername(), namespace);
            user = new DoneableUser(user).editSpec().withUsername(serviceaccountName).endSpec().done();
            return getUserApiClient().createUser(addressSpace.getMetadata().getName(), user);
        }
        return new JsonObject();
    }

    protected JsonObject updateUser(AddressSpace addressSpace, User user) throws Exception {
        log.info("User {} in address space {} will be updated", user, addressSpace.getMetadata().getName());
        return getUserApiClient().updateUser(addressSpace.getMetadata().getName(), user);
    }

    protected void removeUser(AddressSpace addressSpace, String username) throws Exception {
        log.info("User {} in address space {} will be removed", username, addressSpace.getMetadata().getName());
        getUserApiClient().deleteUser(addressSpace.getMetadata().getName(), username);
    }

    protected void createUsers(AddressSpace addressSpace, String prefixName, String prefixPswd, int from, int to)
            throws Exception {
        for (int i = from; i < to; i++) {
            createUser(addressSpace, new UserCredentials(prefixName + i, prefixPswd + i));
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

    protected boolean userExist(AddressSpace addressSpace, String username) throws Exception {
        String id = String.format("%s.%s", addressSpace.getMetadata().getName(), username);
        JsonObject response = getUserApiClient().getUserList(addressSpace.getMetadata().getName());
        log.info("User list for {}: {}", addressSpace.getMetadata().getName(), response.toString());
        JsonArray users = response.getJsonArray("items");
        for (Object user : users) {
            if (((JsonObject) user).getJsonObject("metadata").getString("name").equals(id)) {
                log.info("User {} in addressspace {} already exists", username, addressSpace.getMetadata().getName());
                return true;
            }
        }
        return false;
    }

    //================================================================================================
    //======================================= Help methods ===========================================
    //================================================================================================

    /**
     * give you a schema object
     *
     * @return schema object
     * @throws Exception
     */
    protected Future<AddressSpaceSchemaList> getSchema() throws Exception {
        return AddressSpaceUtils.getSchema(addressApiClient);
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
            TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
            TestUtils.setReplicas(kubernetes, null, deployment, numReplicas, budget);
        } else {
            throw new IllegalArgumentException("'numReplicas' must be greater than 0");
        }

    }

    protected boolean isBrokered(AddressSpace addressSpace) {
        return addressSpace.getSpec().getType().equals(AddressSpaceType.BROKERED.toString());
    }

    protected void assertCanConnect(AddressSpace addressSpace, UserCredentials credentials, List<Address> destinations) throws Exception {
        for (Address destination : destinations) {
            String message = String.format("Client failed, cannot connect to %s under user %s", destination.getSpec().getAddress(), credentials);
            AddressType addressType = AddressType.getEnum(destination.getSpec().getType());
            assertTrue(canConnectWithAmqpAddress(addressSpace, credentials, addressType, destination.getSpec().getAddress(), true), message);
        }
    }

    protected void assertCannotConnect(AddressSpace addressSpace, UserCredentials credentials, List<Address> destinations) throws Exception {
        for (Address destination : destinations) {
            String message = String.format("Client failed, can connect to %s under user %s", destination.getSpec().getAddress(), credentials);
            AddressType addressType = AddressType.getEnum(destination.getSpec().getType());
            assertFalse(canConnectWithAmqpAddress(addressSpace, credentials, addressType, destination.getSpec().getAddress(), false), message);
        }
    }

    private boolean canConnectWithAmqpAddress(AddressSpace addressSpace, UserCredentials credentials, AddressType addressType, String address, boolean defaultValue) throws Exception {
        Set<AddressType> brokeredAddressTypes = new HashSet<>(Arrays.asList(AddressType.QUEUE, AddressType.TOPIC));
        if (isBrokered(addressSpace) && !brokeredAddressTypes.contains(addressType)) {
            return defaultValue;
        }
        AmqpClient client = amqpClientFactory.createAddressClient(addressSpace, addressType);
        client.getConnectOptions().setCredentials(credentials);

        try {
            Future<List<Message>> received = client.recvMessages(address, 1);
            Future<Integer> sent = client.sendMessages(address, Collections.singletonList("msg1"));

            int numReceived = received.get(1, TimeUnit.MINUTES).size();
            int numSent = sent.get(1, TimeUnit.MINUTES);
            return (numSent == numReceived);
        } catch (ExecutionException | SecurityException | UnauthorizedAccessException ex) {
            Throwable cause = ex;
            if (ex instanceof ExecutionException) {
                cause = ex.getCause();
            }

            if (cause instanceof AuthenticationException || cause instanceof SaslSystemException || cause instanceof SecurityException || cause instanceof UnauthorizedAccessException) {
                log.info("canConnectWithAmqpAddress {} ({}): {}", address, addressType, ex.getMessage());
                return false;
            } else {
                log.warn("canConnectWithAmqpAddress {} ({}) exception", address, addressType, ex);
                throw ex;
            }
        } finally {
            client.close();
        }
    }

    protected Endpoint getMessagingRoute(AddressSpace addressSpace) throws Exception {
        Endpoint messagingEndpoint = AddressSpaceUtils.getEndpointByServiceName(addressSpace, "messaging");
        if (messagingEndpoint == null) {
            String externalEndpointName = AddressSpaceUtils.getExternalEndpointName(addressSpace, "messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
            messagingEndpoint = kubernetes.getExternalEndpoint(externalEndpointName);
        }
        if (TestUtils.resolvable(messagingEndpoint)) {
            return messagingEndpoint;
        } else {
            return kubernetes.getEndpoint("messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), "amqps");
        }
    }

    protected String getOCConsoleRoute() {
        return String.format("%s/console", environment.getApiUrl());
    }

    protected String getConsoleRoute(AddressSpace addressSpace) {
        Endpoint consoleEndpoint = getConsoleEndpoint(addressSpace);
        String consoleRoute = String.format("https://%s", consoleEndpoint.toString());
        log.info(consoleRoute);
        return consoleRoute;
    }

    protected Endpoint getConsoleEndpoint(AddressSpace addressSpace) {
        Endpoint consoleEndpoint = AddressSpaceUtils.getEndpointByServiceName(addressSpace, "console");
        if (consoleEndpoint == null) {
            String externalEndpointName = AddressSpaceUtils.getExternalEndpointName(addressSpace, "console");
            consoleEndpoint = kubernetes.getExternalEndpoint(externalEndpointName);
        }
        return consoleEndpoint;
    }

    /**
     * selenium provider with Firefox webdriver
     */
    protected SeleniumProvider getFirefoxSeleniumProvider() throws Exception {
        SeleniumProvider seleniumProvider = new SeleniumProvider();
        seleniumProvider.setupDriver(environment, kubernetes, TestUtils.getFirefoxDriver());
        return seleniumProvider;
    }

    protected void waitForSubscribersConsole(AddressSpace addressSpace, Address destination, int expectedCount) throws Exception {
        int budget = 60; //seconds
        waitForSubscribersConsole(addressSpace, destination, expectedCount, budget);
    }

    /**
     * wait for expected count of subscribers on topic (check via console)
     *
     * @param budget timeout budget in seconds
     */
    private void waitForSubscribersConsole(AddressSpace addressSpace, Address destination, int expectedCount, int budget) throws Exception {
        SeleniumProvider selenium = null;
        try {
            SeleniumManagement.deployFirefoxApp();
            selenium = getFirefoxSeleniumProvider();
            ConsoleWebPage console = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressSpace, clusterUser);
            console.openWebConsolePage();
            console.openAddressesPageWebConsole();

            selenium.waitUntilPropertyPresent(budget, expectedCount, () -> console.getAddressItem(destination).getReceiversCount());
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (selenium != null) {
                selenium.tearDownDrivers();
            }
            SeleniumManagement.removeFirefoxApp();
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
            Address replyQueue = AddressUtils.createQueueAddressObject(replyQueueName, getDefaultPlan(AddressType.QUEUE));
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

    private void waitForBrokerReplicas(AddressSpace addressSpace, Address destination, int expectedReplicas, boolean readyRequired, TimeoutBudget budget, long checkInterval) throws Exception {
        TestUtils.waitForNBrokerReplicas(addressApiClient, kubernetes, addressSpace, expectedReplicas, readyRequired, destination, budget, checkInterval);
    }

    private void waitForBrokerReplicas(AddressSpace addressSpace, Address destination,
                                       int expectedReplicas, TimeoutBudget budget) throws Exception {
        waitForBrokerReplicas(addressSpace, destination, expectedReplicas, true, budget, 5000);
    }

    protected void waitForBrokerReplicas(AddressSpace addressSpace, Address destination, int expectedReplicas) throws
            Exception {
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        waitForBrokerReplicas(addressSpace, destination, expectedReplicas, budget);
    }

    protected void waitForRouterReplicas(AddressSpace addressSpace, int expectedReplicas) throws
            Exception {
        TimeoutBudget budget = new TimeoutBudget(3, TimeUnit.MINUTES);
        Map<String, String> labels = new HashMap<>();
        labels.put("name", "qdrouterd");
        labels.put("infraUuid", AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace));
        TestUtils.waitForNReplicas(kubernetes, expectedReplicas, labels, budget);
    }

    /**
     * Wait for destinations are in isReady=true state within default timeout (10 MINUTE)
     */
    protected void waitForDestinationsReady(AddressSpace addressSpace, Address... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        AddressUtils.waitForDestinationsReady(addressApiClient, addressSpace, budget, destinations);
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
            queueClient, Address replyQueue, String topic) throws Exception {
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
    private int getSubscriberCount(BrokerManagement brokerManagement, AmqpClient queueClient, Address
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

    protected ArrayList<Address> generateTopicsList(String prefix, IntStream range) {
        ArrayList<Address> addresses = new ArrayList<>();
        range.forEach(i -> addresses.add(AddressUtils.createTopicAddressObject(prefix + i, getDefaultPlan(AddressType.QUEUE))));
        return addresses;
    }

    protected ArrayList<Address> generateQueueList(String prefix, IntStream range) {
        ArrayList<Address> addresses = new ArrayList<>();
        range.forEach(i -> addresses.add(AddressUtils.createQueueAddressObject(prefix + i, getDefaultPlan(AddressType.QUEUE))));
        return addresses;
    }

    protected ArrayList<Address> generateQueueTopicList(String infix, IntStream range) {
        ArrayList<Address> addresses = new ArrayList<>();
        range.forEach(i -> {
            if (i % 2 == 0) {
                addresses.add(AddressUtils.createTopicAddressObject(String.format("topic-%s-%d", infix, i), getDefaultPlan(AddressType.TOPIC)));
            } else {
                addresses.add(AddressUtils.createQueueAddressObject(String.format("queue-%s-%d", infix, i), getDefaultPlan(AddressType.QUEUE)));
            }
        });
        return addresses;
    }

    protected boolean sendMessage(AddressSpace addressSpace, AbstractClient client, UserCredentials
            credentials, String address, String content, int count, boolean logToOutput) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.MSG_CONTENT, content);
        arguments.put(ClientArgument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(ClientArgument.ADDRESS, address);
        arguments.put(ClientArgument.COUNT, Integer.toString(count));
        client.setArguments(arguments);

        return client.run(logToOutput);
    }

    /**
     * attach N receivers into one address with default username/password
     */
    protected List<AbstractClient> attachReceivers(AddressSpace addressSpace, Address destination,
                                                   int receiverCount) throws Exception {
        return attachReceivers(addressSpace, destination, receiverCount, defaultCredentials);
    }

    /**
     * attach N receivers into one address with own username/password
     */
    List<AbstractClient> attachReceivers(AddressSpace addressSpace, Address destination,
                                         int receiverCount, UserCredentials credentials) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(ClientArgument.TIMEOUT, "120");
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.ADDRESS, destination.getSpec().getAddress());
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property2~testValue");

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
    List<AbstractClient> attachSenders(AddressSpace addressSpace, List<Address> destinations) throws Exception {
        List<AbstractClient> senders = new ArrayList<>();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(ClientArgument.TIMEOUT, "60");
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.USERNAME, defaultCredentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, defaultCredentials.getPassword());
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.MSG_CONTENT, "msg no.%d");
        arguments.put(ClientArgument.COUNT, "30");
        arguments.put(ClientArgument.DURATION, "30");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property2~testValue");

        for (int i = 0; i < destinations.size(); i++) {
            arguments.put(ClientArgument.ADDRESS, destinations.get(i).getSpec().getAddress());
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
    List<AbstractClient> attachReceivers(AddressSpace addressSpace, List<Address> destinations) throws Exception {
        List<AbstractClient> receivers = new ArrayList<>();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(ClientArgument.TIMEOUT, "60");
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.USERNAME, defaultCredentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, defaultCredentials.getPassword());
        arguments.put(ClientArgument.LOG_MESSAGES, "json");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property2~testValue");

        for (int i = 0; i < destinations.size(); i++) {
            arguments.put(ClientArgument.ADDRESS, destinations.get(i).getSpec().getAddress());
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
    protected AbstractClient attachConnector(AddressSpace addressSpace, Address destination,
                                             int connectionCount,
                                             int senderCount, int receiverCount, UserCredentials credentials) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, getMessagingRoute(addressSpace).toString());
        arguments.put(ClientArgument.TIMEOUT, "120");
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.USERNAME, credentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, credentials.getPassword());
        arguments.put(ClientArgument.OBJECT_CONTROL, "CESR");
        arguments.put(ClientArgument.ADDRESS, destination.getSpec().getAddress());
        arguments.put(ClientArgument.COUNT, Integer.toString(connectionCount));
        arguments.put(ClientArgument.SENDER_COUNT, Integer.toString(senderCount));
        arguments.put(ClientArgument.RECEIVER_COUNT, Integer.toString(receiverCount));
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property1~50");
        arguments.put(ClientArgument.CONN_PROPERTY, "connection_property2~testValue");

        AbstractClient cli = new RheaClientConnector();
        cli.setArguments(arguments);
        cli.runAsync(false);

        return cli;
    }

    /**
     * stop all clients from list of Abstract clients
     */
    protected void stopClients(List<AbstractClient> clients) {
        if (clients != null) {
            log.info("Stopping clients...");
            clients.forEach(AbstractClient::stop);
        }
    }

    /**
     * create users and groups for wildcard authz tests
     */
    protected List<User> createUsersWildcard(AddressSpace addressSpace, Operation operation) throws
            Exception {
        List<User> users = new ArrayList<>();
        users.add(UserUtils.createUserObject("user1", "password",
                Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(operation)
                        .build())));

        users.add(UserUtils.createUserObject("user2", "password",
                Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("queue/*")
                        .withOperations(operation)
                        .build())));

        users.add(UserUtils.createUserObject("user3", "password",
                Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("topic/*")
                        .withOperations(operation)
                        .build())));

        users.add(UserUtils.createUserObject("user4", "password",
                Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("queueA*")
                        .withOperations(operation)
                        .build())));

        users.add(UserUtils.createUserObject("user5", "password",
                Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("topicA*")
                        .withOperations(operation)
                        .build())));

        for (User user : users) {
            createUser(addressSpace, user);
        }
        return users;
    }

    protected List<Address> getAddressesWildcard() {
        Address queue = AddressUtils.createQueueAddressObject("queue/1234", getDefaultPlan(AddressType.QUEUE));
        Address queue2 = AddressUtils.createQueueAddressObject("queue/ABCD", getDefaultPlan(AddressType.QUEUE));
        Address topic = AddressUtils.createTopicAddressObject("topic/2345", getDefaultPlan(AddressType.TOPIC));
        Address topic2 = AddressUtils.createTopicAddressObject("topic/ABCD", getDefaultPlan(AddressType.TOPIC));

        return Arrays.asList(queue, queue2, topic, topic2);
    }

    protected void logWithSeparator(Logger logger, String... messages) {
        logger.info("--------------------------------------------------------------------------------");
        for (String message : messages)
            logger.info(message);
    }

    /**
     * body for rest api tests
     */
    protected void runRestApiTest(AddressSpace addressSpace, Address d1, Address d2) throws Exception {
        List<String> destinationsNames = Arrays.asList(d1.getSpec().getAddress(), d2.getSpec().getAddress());
        setAddresses(addressSpace, d1);
        appendAddresses(addressSpace, d2);

        //d1, d2
        Future<List<String>> response = getAddresses(addressSpace, Optional.empty());
        assertThat("Rest api does not return all addresses", response.get(1, TimeUnit.MINUTES), is(destinationsNames));
        log.info("addresses {} successfully created", Arrays.toString(destinationsNames.toArray()));

        //get specific address d2
        response = getAddresses(addressSpace, Optional.ofNullable(AddressUtils.sanitizeAddress(d2.getMetadata().getName())));
        assertThat("Rest api does not return specific address", response.get(1, TimeUnit.MINUTES), is(destinationsNames.subList(1, 2)));

        deleteAddresses(addressSpace, d1);

        //d2
        response = getAddresses(addressSpace, Optional.ofNullable(AddressUtils.sanitizeAddress(d2.getMetadata().getName())));
        assertThat("Rest api does not return right addresses", response.get(1, TimeUnit.MINUTES), is(destinationsNames.subList(1, 2)));
        log.info("address {} successfully deleted", d1.getSpec().getAddress());

        deleteAddresses(addressSpace, d2);

        //empty
        response = getAddresses(addressSpace, Optional.empty());
        assertThat("Rest api returns addresses", response.get(1, TimeUnit.MINUTES), is(Collections.emptyList()));
        log.info("addresses {} successfully deleted", d2.getSpec().getAddress());

        setAddresses(addressSpace, d1, d2);
        deleteAddresses(addressSpace, d1, d2);

        response = getAddresses(addressSpace, Optional.empty());
        assertThat("Rest api returns addresses", response.get(1, TimeUnit.MINUTES), is(Collections.emptyList()));
        log.info("addresses {} successfully deleted", Arrays.toString(destinationsNames.toArray()));
    }

    protected void sendReceiveLargeMessage(JmsProvider jmsProvider, int sizeInMB, Address dest, int count) throws Exception {
        sendReceiveLargeMessage(jmsProvider, sizeInMB, dest, count, DeliveryMode.NON_PERSISTENT);
    }

    protected void sendReceiveLargeMessage(JmsProvider jmsProvider, int sizeInMB, Address dest, int count, int mode) throws Exception {
        int size = sizeInMB * 1024 * 1024;

        Session session = jmsProvider.getConnection().createSession(false, Session.AUTO_ACKNOWLEDGE);
        javax.jms.Queue testQueue = (javax.jms.Queue) jmsProvider.getDestination(dest.getSpec().getAddress());
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

    protected void deleteAddressSpaceCreatedBySC(AddressSpace addressSpace) throws Exception {
        TestUtils.deleteAddressSpaceCreatedBySC(kubernetes, addressSpace, logCollector);
    }

    protected List<Address> getAllStandardAddresses() {
        return Arrays.asList(
                AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.STANDARD_SMALL_QUEUE),
                AddressUtils.createTopicAddressObject("test-topic", DestinationPlan.STANDARD_SMALL_TOPIC),
                AddressUtils.createQueueAddressObject("test-queue-sharded", DestinationPlan.STANDARD_LARGE_QUEUE),
                AddressUtils.createTopicAddressObject("test-topic-sharded", DestinationPlan.STANDARD_LARGE_TOPIC),
                AddressUtils.createAnycastAddressObject("test-anycast"),
                AddressUtils.createMulticastAddressObject("test-multicast"));
    }

    protected List<Address> getAllBrokeredAddresses() {
        return Arrays.asList(
                AddressUtils.createQueueAddressObject("test-queue", DestinationPlan.BROKERED_QUEUE),
                AddressUtils.createTopicAddressObject("test-topic", DestinationPlan.BROKERED_TOPIC));
    }

    protected void sendDurableMessages(AddressSpace addressSpace, Address destination,
                                       UserCredentials credentials, int count) throws Exception {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(credentials);
        List<Message> listOfMessages = new ArrayList<>();
        IntStream.range(0, count).forEach(num -> {
            Message msg = Message.Factory.create();
            msg.setAddress(destination.getSpec().getAddress());
            msg.setDurable(true);
            listOfMessages.add(msg);
        });
        Future<Integer> sent = client.sendMessages(destination.getSpec().getAddress(), listOfMessages.toArray(new Message[0]));
        assertThat("Cannot send durable messages to " + destination, sent.get(1, TimeUnit.MINUTES), is(count));
        client.close();
    }

    protected void receiveDurableMessages(AddressSpace addressSpace, Address dest,
                                          UserCredentials credentials, int count) throws Exception {
        AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
        client.getConnectOptions().setCredentials(credentials);
        ReceiverStatus receiverStatus = client.recvMessagesWithStatus(dest.getSpec().getAddress(), count);
        assertThat("Cannot receive durable messages from " + dest + ". Got " + receiverStatus.getNumReceived(), receiverStatus.getResult().get(1, TimeUnit.MINUTES).size(), is(count));
        client.close();
    }

    //================================================================================================
    //==================================== Asserts methods ===========================================
    //================================================================================================
    protected <T extends Comparable<T>> void assertSorted(String message, Iterable<T> list) throws Exception {
        assertSorted(message, list, false);
    }

    protected <T> void assertSorted(String message, Iterable<T> list, Comparator<T> comparator) throws Exception {
        assertSorted(message, list, false, comparator);
    }

    protected <T extends Comparable<T>> void assertSorted(String message, Iterable<T> list, boolean reverse) {
        log.info("Assert sort reverse: " + reverse);
        if (!reverse)
            assertTrue(Ordering.natural().isOrdered(list), message);
        else
            assertTrue(Ordering.natural().reverse().isOrdered(list), message);
    }

    protected <T> void assertSorted(String message, Iterable<T> list, boolean reverse, Comparator<T> comparator) {
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
        assertWaitForValue(expected, fn, new TimeoutBudget(10, TimeUnit.SECONDS));
    }
}
