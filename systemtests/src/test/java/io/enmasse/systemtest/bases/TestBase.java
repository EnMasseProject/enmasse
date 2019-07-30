/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.bases;

import com.google.common.collect.Ordering;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceSchemaList;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.AdminResourcesManager;
import io.enmasse.systemtest.BrokerManagement;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.DestinationPlan;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.GlobalLogCollector;
import io.enmasse.systemtest.JmsProvider;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.TimeoutBudget;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.ability.ITestBase;
import io.enmasse.systemtest.ability.ITestSeparator;
import io.enmasse.systemtest.ability.TestWatcher;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.amqp.ReceiverStatus;
import io.enmasse.systemtest.amqp.UnauthorizedAccessException;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientConnector;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.mqtt.MqttClientFactory;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.selenium.SeleniumManagement;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.timemeasuring.SystemtestsOperation;
import io.enmasse.systemtest.timemeasuring.TimeMeasuringSystem;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.enmasse.user.model.v1.UserBuilder;
import io.vertx.proton.ProtonClientOptions;
import io.vertx.proton.sasl.MechanismMismatchException;
import io.vertx.proton.sasl.SaslSystemException;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TimeoutBudget.ofDuration;
import static java.time.Duration.ofMinutes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
    private static Logger log = CustomLogger.getLogger();
    protected AmqpClientFactory amqpClientFactory;
    protected MqttClientFactory mqttClientFactory;
    protected UserCredentials managementCredentials = new UserCredentials(null, null);
    protected UserCredentials defaultCredentials = new UserCredentials(null, null);
    private List<AddressSpace> addressSpaceList = new ArrayList<>();
    private boolean reuseAddressSpace;

    protected static void simpleMQTTSendReceive(Address dest, IMqttClient client, int msgCount) throws Exception {
        List<MqttMessage> messages = IntStream.range(0, msgCount).boxed().map(i -> {
            MqttMessage m = new MqttMessage();
            m.setPayload(String.format("mqtt-simple-send-receive-%s", i).getBytes(StandardCharsets.UTF_8));
            m.setQos(1);
            return m;
        }).collect(Collectors.toList());

        List<CompletableFuture<MqttMessage>> receiveFutures = MqttUtils.subscribeAndReceiveMessages(client, dest.getSpec().getAddress(), messages.size(), 1);
        List<CompletableFuture<Void>> publishFutures = MqttUtils.publish(client, dest.getSpec().getAddress(), messages);

        int publishCount = MqttUtils.awaitAndReturnCode(publishFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages published",
                publishCount, is(messages.size()));

        int receivedCount = MqttUtils.awaitAndReturnCode(receiveFutures, 1, TimeUnit.MINUTES);
        assertThat("Incorrect count of messages received",
                receivedCount, is(messages.size()));
    }

    @BeforeEach
    public void setup() throws Exception {
        if (!reuseAddressSpace) {
            addressSpaceList = new ArrayList<>();
        }
        amqpClientFactory = new AmqpClientFactory(null, defaultCredentials);
        mqttClientFactory = new MqttClientFactory(null, defaultCredentials);
    }

    @AfterEach
    public void teardown() throws Exception {
        try {
            if (!environment.skipCleanup() && !reuseAddressSpace) {
                deleteAddressspacesFromList();
                AdminResourcesManager.getInstance().tearDown();
            } else {
                log.warn("Remove address spaces in tear down - SKIPPED!");
            }
        } catch (Exception e) {
            log.error("Error tearing down test", e);

            throw e;
        }
    }

    @AfterEach
    public void closeMqttClient() {
        if (mqttClientFactory != null) {
            mqttClientFactory.close();
            mqttClientFactory = null;
        }
    }

    @AfterEach
    public void closeAmqpClient() throws Exception {
        if (amqpClientFactory != null) {
            amqpClientFactory.close();
            amqpClientFactory = null;
        }

    }

    protected void setReuseAddressSpace() {
        reuseAddressSpace = true;
    }


    //================================================================================================
    //==================================== AddressSpace methods ======================================
    //================================================================================================

    protected void unsetReuseAddressSpace() {
        reuseAddressSpace = false;
    }

    protected void createAddressSpaceList(AddressSpace... addressSpaces) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_ADDRESS_SPACE);
        ArrayList<AddressSpace> spaces = new ArrayList<>();
        for (AddressSpace addressSpace : addressSpaces) {
            if (!AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
                log.info("Address space '" + addressSpace + "' doesn't exist and will be created.");
                spaces.add(addressSpace);
                if (!addressSpace.equals(getSharedAddressSpace())) {
                    addressSpaceList.add(addressSpace);
                }
            } else {
                log.warn("Address space '" + addressSpace + "' already exists.");
            }
        }
        spaces.forEach(addressSpace ->
                kubernetes.getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).createOrReplace(addressSpace));
        for (AddressSpace addressSpace : spaces) {
            AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
            AddressSpaceUtils.syncAddressSpaceObject(addressSpace);
            logCollector.startCollecting(addressSpace);
        }
        TimeMeasuringSystem.stopOperation(operationID);
    }

    protected void createAddressSpace(AddressSpace addressSpace) throws Exception {
        String operationID = TimeMeasuringSystem.startOperation(SystemtestsOperation.CREATE_ADDRESS_SPACE);
        if (!AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
            log.info("Address space '{}' doesn't exist and will be created.", addressSpace);
            kubernetes.getAddressSpaceClient(addressSpace.getMetadata().getNamespace()).createOrReplace(addressSpace);
            AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);

            if (!addressSpace.equals(getSharedAddressSpace())) {
                addressSpaceList.add(addressSpace);
            }
        } else {
            AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
            log.info("Address space '" + addressSpace + "' already exists.");
        }
        AddressSpaceUtils.syncAddressSpaceObject(addressSpace);
        logCollector.startCollecting(addressSpace);
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

    protected void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        if (AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
            AddressSpaceUtils.deleteAddressSpaceAndWait(addressSpace, logCollector);
        } else {
            log.info("Address space '" + addressSpace.getMetadata().getName() + "' doesn't exists!");
        }
    }

    protected void deleteAllAddressSpaces() throws Exception {
        AddressSpaceUtils.deleteAllAddressSpaces(logCollector);
        for (AddressSpace addressSpace : addressSpaceList) {
            AddressSpaceUtils.waitForAddressSpaceDeleted(addressSpace);
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
        var client = kubernetes.getAddressSpaceClient(addressSpace.getMetadata().getNamespace());
        if (AddressSpaceUtils.existAddressSpace(addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName())) {
            log.info("Address space '{}' exists and will be updated.", addressSpace);
            final String currentResourceVersion = client.withName(addressSpace.getMetadata().getName()).get().getMetadata().getResourceVersion();
            client.createOrReplace(addressSpace);
            Thread.sleep(10_000);
            TestUtils.waitForChangedResourceVersion(ofDuration(ofMinutes(5)), addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), currentResourceVersion);
            if (waitForPlanApplied) {
                AddressSpaceUtils.waitForAddressSpacePlanApplied(addressSpace);
            }
            AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
            AddressSpaceUtils.syncAddressSpaceObject(addressSpace);

            if (!addressSpace.equals(getSharedAddressSpace())) {
                addressSpaceList.add(addressSpace);
            }
        } else {
            log.info("Address space '{}' does not exists.", addressSpace.getMetadata().getName());
        }
        log.info("Address space updated: {}", addressSpace);
        TimeMeasuringSystem.stopOperation(operationID);
    }

    protected void waitForAddressSpaceReady(AddressSpace addressSpace) throws Exception {
        AddressSpaceUtils.waitForAddressSpaceReady(addressSpace);
    }

    protected void waitForAddressSpacePlanApplied(AddressSpace addressSpace) throws Exception {
        AddressSpaceUtils.waitForAddressSpacePlanApplied(addressSpace);
    }

    protected AddressSpace getAddressSpace(String addressSpaceName) {
        return kubernetes.getAddressSpaceClient().withName(addressSpaceName).get();
    }

    //================================================================================================
    //====================================== Address methods =========================================
    //================================================================================================

    protected AddressSpace getAddressSpace(String namespace, String addressSpaceName) {
        return kubernetes.getAddressSpaceClient(namespace).withName(addressSpaceName).get();
    }

    protected void deleteAddresses(Address... destinations) throws Exception {
        logCollector.collectConfigMaps();
        logCollector.collectRouterState("deleteAddresses");
        AddressUtils.delete(destinations);
    }

    protected void deleteAddresses(AddressSpace addressSpace) throws Exception {
        logCollector.collectConfigMaps();
        logCollector.collectRouterState("deleteAddresses");
        AddressUtils.delete(addressSpace);
    }

    protected void appendAddresses(Address... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        appendAddresses(budget, destinations);
    }

    protected void appendAddresses(TimeoutBudget timeout, Address... destinations) throws Exception {
        appendAddresses(true, timeout, destinations);
    }

    protected void appendAddresses(boolean wait, Address... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        appendAddresses(wait, budget, destinations);
    }

    protected void appendAddresses(boolean wait, TimeoutBudget timeout, Address... destinations) throws Exception {
        AddressUtils.appendAddresses(timeout, wait, destinations);
        logCollector.collectConfigMaps();
    }

    protected void setAddresses(Address... addresses) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(15, TimeUnit.MINUTES);
        logCollector.collectRouterState("setAddresses");
        AddressUtils.setAddresses(budget, true, addresses);
    }

    //================================================================================================
    //======================================= User methods ===========================================
    //================================================================================================

    protected void replaceAddress(Address destination) throws Exception {
        AddressUtils.replaceAddress(destination, true, new TimeoutBudget(3, TimeUnit.MINUTES));
    }

    protected User createOrUpdateUser(AddressSpace addressSpace, UserCredentials credentials) {
        User user = new UserBuilder()
                .withNewMetadata()
                .withName(addressSpace.getMetadata().getName() + "." + credentials.getUsername())
                .endMetadata()
                .withNewSpec()
                .withUsername(credentials.getUsername())
                .withNewAuthentication()
                .withType(UserAuthenticationType.password)
                .withNewPassword(UserUtils.passwordToBase64(credentials.getPassword()))
                .endAuthentication()
                .addNewAuthorization()
                .withAddresses("*")
                .addToOperations(Operation.send)
                .addToOperations(Operation.recv)
                .endAuthorization()
                .addNewAuthorization()
                .addToOperations(Operation.manage)
                .endAuthorization()
                .endSpec()
                .build();
        return createOrUpdateUser(addressSpace, user);
    }

    protected User createOrUpdateUser(AddressSpace addressSpace, User user) {
        log.info("User {} in address space {} will be created", user, addressSpace.getMetadata().getName());
        if (user.getMetadata().getName() == null || !user.getMetadata().getName().contains(addressSpace.getMetadata().getName())) {
            user.getMetadata().setName(addressSpace.getMetadata().getName() + "." + user.getSpec().getUsername());
        }
        return kubernetes.getUserClient(addressSpace.getMetadata().getNamespace()).createOrReplace(user);
    }

    protected User createUserServiceaccount(AddressSpace addressSpace, UserCredentials cred) {
        log.info("ServiceAccount user {} in address space {} will be created", cred.getUsername(), addressSpace.getMetadata().getName());
        String serviceaccountName = kubernetes.createServiceAccount(cred.getUsername(), addressSpace.getMetadata().getNamespace());
        User user = new UserBuilder()
                .withNewMetadata()
                .withName(String.format("%s.%s", addressSpace.getMetadata().getName(),
                        Pattern.compile(".*:").matcher(serviceaccountName).replaceAll("")))
                .endMetadata()
                .withNewSpec()
                .withUsername(serviceaccountName)
                .withNewAuthentication()
                .withType(UserAuthenticationType.serviceaccount)
                .endAuthentication()
                .addNewAuthorization()
                .withAddresses("*")
                .addToOperations(Operation.send)
                .addToOperations(Operation.recv)
                .endAuthorization()
                .endSpec()
                .build();
        return createOrUpdateUser(addressSpace, user);
    }

    protected void removeUser(AddressSpace addressSpace, User user) {
        log.info("User {} in address space {} will be removed", user.getMetadata().getName(), addressSpace.getMetadata().getName());
        kubernetes.getUserClient(addressSpace.getMetadata().getNamespace()).withName(user.getMetadata().getName()).cascading(true).delete();
    }

    protected void removeUser(AddressSpace addressSpace, String userName) {
        log.info("User {} in address space {} will be removed", userName, addressSpace.getMetadata().getName());
        kubernetes.getUserClient(addressSpace.getMetadata().getNamespace()).withName(String.format("%s.%s", addressSpace.getMetadata().getName(), userName)).cascading(true).delete();
    }

    protected User getUser(AddressSpace addressSpace, String username) {
        String id = String.format("%s.%s", addressSpace.getMetadata().getName(), username);
        List<User> response = kubernetes.getUserClient(addressSpace.getMetadata().getNamespace()).list().getItems();
        log.info("User list for {}: {}", addressSpace.getMetadata().getName(), response);
        for (User user : response) {
            if (user.getMetadata().getName().equals(id)) {
                log.info("User {} in addressspace {} already exists", username, addressSpace.getMetadata().getName());
                return user;
            }
        }
        return null;
    }

    //================================================================================================
    //======================================= Help methods ===========================================
    //================================================================================================

    protected boolean userExist(AddressSpace addressSpace, String username) {
        return getUser(addressSpace, username) != null;
    }

    /**
     * give you a schema object
     *
     * @return schema object
     * @throws Exception
     */
    protected AddressSpaceSchemaList getSchema() throws Exception {
        return kubernetes.getSchemaClient().list();
    }

    /**
     * scale up/down deployment to count of replicas, includes waiting for expected replicas
     *
     * @param deployment  name of deployment
     * @param numReplicas count of replicas
     * @throws InterruptedException
     */
    protected void scaleDeployment(String deployment, int numReplicas) throws InterruptedException {
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
        try (AmqpClient client = amqpClientFactory.createAddressClient(addressSpace, addressType)) {
            client.getConnectOptions().setCredentials(credentials);
            ProtonClientOptions protonClientOptions = client.getConnectOptions().getProtonClientOptions();
            protonClientOptions.setLogActivity(true);
            client.getConnectOptions().setProtonClientOptions(protonClientOptions);


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

                if (cause instanceof AuthenticationException || cause instanceof SaslSystemException || cause instanceof SecurityException || cause instanceof UnauthorizedAccessException || cause instanceof MechanismMismatchException) {
                    log.info("canConnectWithAmqpAddress {} ({}): {}", address, addressType, ex.getMessage());
                    return false;
                } else {
                    log.warn("canConnectWithAmqpAddress {} ({}) exception", address, addressType, ex);
                    throw ex;
                }
            }
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
            return kubernetes.getEndpoint("messaging-" + AddressSpaceUtils.getAddressSpaceInfraUuid(addressSpace), addressSpace.getMetadata().getNamespace(), "amqps");
        }
    }

    protected String getOCConsoleRoute() {
        if (environment.isOcp4()) {
            return String.format("https://console-openshift-console.%s", environment.kubernetesDomain());
        } else {
            return String.format("%s/console", environment.getApiUrl());
        }
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
        SeleniumProvider seleniumProvider = SeleniumProvider.getInstance();
        seleniumProvider.setupDriver(TestUtils.getFirefoxDriver());
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
            Address replyQueue = new AddressBuilder()
                    .withNewMetadata()
                    .withNamespace(addressSpace.getMetadata().getNamespace())
                    .withName(AddressUtils.generateAddressMetadataName(addressSpace, replyQueueName))
                    .endMetadata()
                    .withNewSpec()
                    .withType("queue")
                    .withAddress(replyQueueName)
                    .withPlan(getDefaultPlan(AddressType.QUEUE))
                    .endSpec()
                    .build();
            appendAddresses(replyQueue);

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
        TestUtils.waitForNBrokerReplicas(addressSpace, expectedReplicas, readyRequired, destination, budget, checkInterval);
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
        TestUtils.waitForNReplicas(expectedReplicas, labels, budget);
    }

    /**
     * Wait for destinations are in isReady=true state within default timeout (10 MINUTE)
     */
    protected void waitForDestinationsReady(Address... destinations) throws Exception {
        TimeoutBudget budget = new TimeoutBudget(10, TimeUnit.MINUTES);
        AddressUtils.waitForDestinationsReady(budget, destinations);
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

    protected ArrayList<Address> generateQueueTopicList(AddressSpace addressspace, String infix, IntStream range) {
        ArrayList<Address> addresses = new ArrayList<>();
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

    /**
     * attach N receivers into one address with own username/password
     */
    public List<AbstractClient> attachReceivers(AddressSpace addressSpace, Address destination,
                                                int receiverCount, int timeout, UserCredentials credentials) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, getMessagingRoute(addressSpace).toString());
        if (timeout > 0) {
            arguments.put(ClientArgument.TIMEOUT, Integer.toString(timeout));
        }
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
    public List<AbstractClient> attachSenders(AddressSpace addressSpace, List<Address> destinations, int timeout, UserCredentials defaultCredentials) throws Exception {
        List<AbstractClient> senders = new ArrayList<>();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, getMessagingRoute(addressSpace).toString());
        if (timeout > 0) {
            arguments.put(ClientArgument.TIMEOUT, Integer.toString(timeout));
        }
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
    public List<AbstractClient> attachReceivers(AddressSpace addressSpace, List<Address> destinations, int timeout, UserCredentials userCredentials) throws Exception {
        List<AbstractClient> receivers = new ArrayList<>();

        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, getMessagingRoute(addressSpace).toString());
        if (timeout > 0) {
            arguments.put(ClientArgument.TIMEOUT, Integer.toString(timeout));
        }
        arguments.put(ClientArgument.CONN_SSL, "true");
        arguments.put(ClientArgument.USERNAME, userCredentials.getUsername());
        arguments.put(ClientArgument.PASSWORD, userCredentials.getPassword());
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
                                             int senderCount, int receiverCount, UserCredentials credentials, int timeout) throws Exception {
        ClientArgumentMap arguments = new ClientArgumentMap();
        arguments.put(ClientArgument.BROKER, getMessagingRoute(addressSpace).toString());
        if (timeout > 0) {
            arguments.put(ClientArgument.TIMEOUT, Integer.toString(timeout));
        }
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
        users.add(UserUtils.createUserResource(new UserCredentials("user1", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        users.add(UserUtils.createUserResource(new UserCredentials("user2", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("queue/*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        users.add(UserUtils.createUserResource(new UserCredentials("user3", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("topic/*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        users.add(UserUtils.createUserResource(new UserCredentials("user4", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("queueA*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        users.add(UserUtils.createUserResource(new UserCredentials("user5", "password"))
                .editSpec()
                .withAuthorization(Collections.singletonList(new UserAuthorizationBuilder()
                        .withAddresses("topicA*")
                        .withOperations(operation)
                        .build()))
                .endSpec()
                .done());

        for (User user : users) {
            createOrUpdateUser(addressSpace, user);
        }
        return users;
    }

    protected List<Address> getAddressesWildcard(AddressSpace addressspace) {
        Address queue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressspace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressspace, "queue/1234"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue/1234")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        Address queue2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressspace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressspace, "queue/ABCD"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue/ABCD")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        Address topic = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressspace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressspace, "topic/2345"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("topic/2345")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();

        Address topic2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(addressspace.getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(addressspace, "topic/ABCD"))
                .endMetadata()
                .withNewSpec()
                .withType("topic")
                .withAddress("topic/ABCD")
                .withPlan(getDefaultPlan(AddressType.TOPIC))
                .endSpec()
                .build();

        return Arrays.asList(queue, queue2, topic, topic2);
    }

    protected void logWithSeparator(Logger logger, String... messages) {
        logger.info("--------------------------------------------------------------------------------");
        for (String message : messages) {
            logger.info(message);
        }
    }

    /**
     * body for rest api tests
     */
    protected void runRestApiTest(AddressSpace addressSpace, Address d1, Address d2) throws Exception {
        List<String> destinationsNames = Arrays.asList(d1.getSpec().getAddress(), d2.getSpec().getAddress());
        setAddresses(d1);
        appendAddresses(d2);

        //d1, d2
        List<String> response = AddressUtils.getAddresses(addressSpace).stream().map(address -> address.getSpec().getAddress()).collect(Collectors.toList());
        assertThat("Rest api does not return all addresses", response, is(destinationsNames));
        log.info("addresses {} successfully created", Arrays.toString(destinationsNames.toArray()));

        //get specific address d2
        Address res = kubernetes.getAddressClient(addressSpace.getMetadata().getNamespace()).withName(d2.getMetadata().getName()).get();
        assertThat("Rest api does not return specific address", res.getSpec().getAddress(), is(d2.getSpec().getAddress()));

        deleteAddresses(d1);

        //d2
        response = AddressUtils.getAddresses(addressSpace).stream().map(address -> address.getSpec().getAddress()).collect(Collectors.toList());
        assertThat("Rest api does not return right addresses", response, is(destinationsNames.subList(1, 2)));
        log.info("address {} successfully deleted", d1.getSpec().getAddress());

        deleteAddresses(d2);

        //empty
        List<Address> listRes = AddressUtils.getAddresses(addressSpace);
        assertThat("Rest api returns addresses", listRes, is(Collections.emptyList()));
        log.info("addresses {} successfully deleted", d2.getSpec().getAddress());

        setAddresses(d1, d2);
        deleteAddresses(d1, d2);

        listRes = AddressUtils.getAddresses(addressSpace);
        assertThat("Rest api returns addresses", listRes, is(Collections.emptyList()));
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

    protected List<Address> getAllStandardAddresses(AddressSpace addressspace) {
        return Arrays.asList(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-queue"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue")
                        .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.STANDARD_SMALL_TOPIC)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-queue-sharded"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue-sharded")
                        .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-topic-sharded"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic-sharded")
                        .withPlan(DestinationPlan.STANDARD_LARGE_TOPIC)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-anycast"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("anycast")
                        .withAddress("test-anycast")
                        .withPlan(DestinationPlan.STANDARD_SMALL_ANYCAST)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-multicast"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("multicast")
                        .withAddress("test-multicast")
                        .withPlan(DestinationPlan.STANDARD_SMALL_MULTICAST)
                        .endSpec()
                        .build());
    }

    protected List<Address> getAllBrokeredAddresses(AddressSpace addressspace) {
        return Arrays.asList(
                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-queue"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress("test-queue")
                        .withPlan(DestinationPlan.BROKERED_QUEUE)
                        .endSpec()
                        .build(),

                new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(addressspace.getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(addressspace, "test-topic"))
                        .endMetadata()
                        .withNewSpec()
                        .withType("topic")
                        .withAddress("test-topic")
                        .withPlan(DestinationPlan.BROKERED_TOPIC)
                        .endSpec()
                        .build());
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

    protected void connectAddressSpace(AddressSpace addressSpace, UserCredentials credentials) throws Exception {
        try (AmqpClient client = amqpClientFactory.createQueueClient(addressSpace)) {
            client.getConnectOptions().setCredentials(credentials);
            CompletableFuture<Void> connect = client.connect();
            try {
                connect.get(5, TimeUnit.MINUTES);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Exception) {
                    throw ((Exception) e.getCause());
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }
    }

    protected List<String> extractBodyAsString(Future<List<Message>> msgs) throws Exception {
        return msgs.get(1, TimeUnit.MINUTES).stream().map(m -> (String) ((AmqpValue) m.getBody()).getValue()).collect(Collectors.toList());
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
        if (!reverse) {
            assertTrue(Ordering.natural().isOrdered(list), message);
        } else {
            assertTrue(Ordering.natural().reverse().isOrdered(list), message);
        }
    }

    protected <T> void assertSorted(String message, Iterable<T> list, boolean reverse, Comparator<T> comparator) {
        log.info("Assert sort reverse: " + reverse);
        if (!reverse) {
            assertTrue(Ordering.from(comparator).isOrdered(list), message);
        } else {
            assertTrue(Ordering.from(comparator).reverse().isOrdered(list), message);
        }
    }

    protected void assertWaitForValue(int expected, Callable<Integer> fn, TimeoutBudget budget) throws Exception {
        Integer got = null;
        log.info("waiting for expected value '{}' ...", expected);
        while (budget.timeLeft() >= 0) {
            got = fn.call();
            if (got != null && expected == got) {
                return;
            }
            Thread.sleep(100);
        }
        fail(String.format("Incorrect results value! expected: '%s', got: '%s'", expected, Objects.requireNonNull(got)));
    }

    protected void assertWaitForValue(int expected, Callable<Integer> fn) throws Exception {
        assertWaitForValue(expected, fn, new TimeoutBudget(10, TimeUnit.SECONDS));
    }
}
