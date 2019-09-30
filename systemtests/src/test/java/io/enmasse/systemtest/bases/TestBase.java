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
import io.enmasse.iot.model.v1.IoTConfigBuilder;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Environment;
import io.enmasse.systemtest.TestTag;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.broker.BrokerManagement;
import io.enmasse.systemtest.cmdclients.KubeCMDClient;
import io.enmasse.systemtest.info.TestInfo;
import io.enmasse.systemtest.iot.HttpAdapterClient;
import io.enmasse.systemtest.iot.MessageType;
import io.enmasse.systemtest.logs.GlobalLogCollector;
import io.enmasse.systemtest.manager.IsolatedResourcesManager;
import io.enmasse.systemtest.manager.ResourceManager;
import io.enmasse.systemtest.manager.SharedIoTManager;
import io.enmasse.systemtest.manager.SharedResourceManager;
import io.enmasse.systemtest.messagingclients.AbstractClient;
import io.enmasse.systemtest.messagingclients.ClientArgument;
import io.enmasse.systemtest.messagingclients.ClientArgumentMap;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientConnector;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientReceiver;
import io.enmasse.systemtest.messagingclients.rhea.RheaClientSender;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.mqtt.MqttUtils;
import io.enmasse.systemtest.platform.Kubernetes;
import io.enmasse.systemtest.selenium.SeleniumManagement;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.IoTUtils;
import io.enmasse.systemtest.utils.JmsProvider;
import io.enmasse.systemtest.utils.TestUtils;
import io.enmasse.systemtest.utils.UserUtils;
import io.enmasse.systemtest.watcher.TestWatcher;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.User;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.jms.DeliveryMode;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class for all tests
 */
@ExtendWith(TestWatcher.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestBase implements ITestBase, ITestSeparator {
    protected static final UserCredentials clusterUser = new UserCredentials(KubeCMDClient.getOCUser());
    protected static final Environment environment = Environment.getInstance();
    protected static final GlobalLogCollector logCollector = new GlobalLogCollector(kubernetes,
            new File(environment.testLogDir()));
    protected ResourceManager resourcesManager;
    protected UserCredentials defaultCredentials = null;
    protected UserCredentials managementCredentials = null;

    @BeforeEach
    public void initTest() throws Exception {
        LOGGER.info("Test init");
        defaultCredentials = environment.getDefaultCredentials();
        managementCredentials = environment.getManagementCredentials();
        resourcesManager = getResourceManager();
        if (TestInfo.getInstance().isTestShared()) {

                ResourceManager.ADDRESS_SPACE_PLAN = getDefaultAddressSpacePlan();
                ResourceManager.ADDRESS_SPACE_TYPE = getAddressSpaceType().toString();
                ResourceManager.DEFAULT_ADD_SPACE_IDENTIFIER = getDefaultAddrSpaceIdentifier();

            if (resourcesManager.getSharedAddressSpace() == null) {
                if (TestInfo.getInstance().isTestIoT()) {
                    ((SharedIoTManager) resourcesManager).createSharedIoTEnv();
                } else {
                    ((SharedResourceManager) resourcesManager).setupSharedEnvironment();
                }
            }
        }
    }

    //================================================================================================
    //======================================= Help methods ===========================================
    //================================================================================================

    protected boolean userExist(AddressSpace addressSpace, String username) {
        return IsolatedResourcesManager.getInstance().getUser(addressSpace, username) != null;
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
        LOGGER.info(consoleRoute);
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
            queueClient = resourcesManager.getAmqpClientFactory().createQueueClient(addressSpace);
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
            resourcesManager.appendAddresses(replyQueue);

            boolean done = false;
            int actualSubscribers = 0;
            do {
                actualSubscribers = getSubscriberCount(brokerManagement, queueClient, replyQueue, topic);
                LOGGER.info("Have " + actualSubscribers + " subscribers. Expecting " + expectedCount);
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

    protected void waitForPodsToTerminate(List<String> uids) throws Exception {
        LOGGER.info("Waiting for following pods to be deleted {}", uids);
        assertWaitForValue(true, () -> (kubernetes.listPods(kubernetes.getInfraNamespace()).stream()
                .noneMatch(pod -> uids.contains(pod.getMetadata().getUid()))), new TimeoutBudget(2, TimeUnit.MINUTES));
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
            LOGGER.info("Stopping clients...");
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
            IsolatedResourcesManager.getInstance().createOrUpdateUser(addressSpace, user);
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
        resourcesManager.setAddresses(d1);
        resourcesManager.appendAddresses(d2);

        //d1, d2
        List<String> response = AddressUtils.getAddresses(addressSpace).stream().map(address -> address.getSpec().getAddress()).collect(Collectors.toList());
        assertThat("Rest api does not return all addresses", response, is(destinationsNames));
        LOGGER.info("addresses {} successfully created", Arrays.toString(destinationsNames.toArray()));

        //get specific address d2
        Address res = kubernetes.getAddressClient(addressSpace.getMetadata().getNamespace()).withName(d2.getMetadata().getName()).get();
        assertThat("Rest api does not return specific address", res.getSpec().getAddress(), is(d2.getSpec().getAddress()));

        resourcesManager.deleteAddresses(d1);

        //d2
        response = AddressUtils.getAddresses(addressSpace).stream().map(address -> address.getSpec().getAddress()).collect(Collectors.toList());
        assertThat("Rest api does not return right addresses", response, is(destinationsNames.subList(1, 2)));
        LOGGER.info("address {} successfully deleted", d1.getSpec().getAddress());

        resourcesManager.deleteAddresses(d2);

        //empty
        List<Address> listRes = AddressUtils.getAddresses(addressSpace);
        assertThat("Rest api returns addresses", listRes, is(Collections.emptyList()));
        LOGGER.info("addresses {} successfully deleted", d2.getSpec().getAddress());

        IsolatedResourcesManager.getInstance().setAddresses(d1, d2);
        resourcesManager.deleteAddresses(d1, d2);

        listRes = AddressUtils.getAddresses(addressSpace);
        assertThat("Rest api returns addresses", listRes, is(Collections.emptyList()));
        LOGGER.info("addresses {} successfully deleted", Arrays.toString(destinationsNames.toArray()));
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
        LOGGER.info("{}MB {} message sent", sizeInMB, mode == DeliveryMode.PERSISTENT ? "durable" : "non-durable");

        recvd = jmsProvider.receiveMessages(receiver, count, 2000);
        assertThat("Wrong count of received messages", recvd.size(), Matchers.is(count));
        LOGGER.info("{}MB {} message received", sizeInMB, mode == DeliveryMode.PERSISTENT ? "durable" : "non-durable");
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

    protected List<String> extractBodyAsString(Future<List<Message>> msgs) throws Exception {
        return msgs.get(1, TimeUnit.MINUTES).stream().map(m -> (String) ((AmqpValue) m.getBody()).getValue()).collect(Collectors.toList());
    }

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
        LOGGER.info("Assert sort reverse: " + reverse);
        if (!reverse) {
            assertTrue(Ordering.natural().isOrdered(list), message);
        } else {
            assertTrue(Ordering.natural().reverse().isOrdered(list), message);
        }
    }

    protected <T> void assertSorted(String message, Iterable<T> list, boolean reverse, Comparator<T> comparator) {
        LOGGER.info("Assert sort reverse: " + reverse);
        if (!reverse) {
            assertTrue(Ordering.from(comparator).isOrdered(list), message);
        } else {
            assertTrue(Ordering.from(comparator).reverse().isOrdered(list), message);
        }
    }

    protected <T> void assertWaitForValue(T expected, Callable<T> fn, TimeoutBudget budget) throws Exception {
        T got = null;
        LOGGER.info("waiting for expected value '{}' ...", expected);
        while (budget.timeLeft() >= 0) {
            got = fn.call();
            if (Objects.equals(expected, got)) {
                return;
            }
            Thread.sleep(100);
        }
        fail(String.format("Incorrect result value! expected: '%s', got: '%s'", expected, Objects.requireNonNull(got)));
    }

    protected <T> void assertWaitForValue(T expected, Callable<T> fn) throws Exception {
        assertWaitForValue(expected, fn, new TimeoutBudget(10, TimeUnit.SECONDS));
    }

    //================================================================================================
    //====================================== Shared test help methods =======================================
    //================================================================================================

    /**
     * Create users within groups (according to destNamePrefix and customerIndex), wait until destinations are ready to use
     * and start sending and receiving messages
     *
     * @param dest           list of all available destinations (destinations are not in ready state presumably)
     * @param users          list of users dedicated for sending messages into destinations above
     * @param destNamePrefix prefix of destinations name (due to authorization)
     * @param customerIndex  also important due to authorization (only users under this customer can send messages into dest)
     * @param messageCount   count of messages that will be send into destinations
     * @throws Exception
     */
    protected void doMessaging(List<Address> dest, List<UserCredentials> users, String destNamePrefix, int customerIndex, int messageCount) throws Exception {
        ArrayList<AmqpClient> clients = new ArrayList<>(users.size());
        String sufix = new AddressSpaceUtils().isBrokered(resourcesManager.getSharedAddressSpace()) ? "#" : "*";
        users.forEach((user) -> {
            try {
                IsolatedResourcesManager.getInstance().createOrUpdateUser(resourcesManager.getSharedAddressSpace(),
                        UserUtils.createUserResource(user)
                                .editSpec()
                                .withAuthorization(Collections.singletonList(
                                        new UserAuthorizationBuilder()
                                                .withAddresses(String.format("%s.%s.%s", destNamePrefix, customerIndex, sufix))
                                                .withOperations(Operation.send, Operation.recv).build()))
                                .endSpec()
                                .done());
                AmqpClient queueClient = resourcesManager.getAmqpClientFactory().createQueueClient();
                queueClient.getConnectOptions().setCredentials(user);
                clients.add(queueClient);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        waitForDestinationsReady(dest.toArray(new Address[0]));
        //start sending messages
        int everyN = 3;
        for (AmqpClient client : clients) {
            for (int i = 0; i < dest.size(); i++) {
                if (i % everyN == 0) {
                    Future<Integer> sent = client.sendMessages(dest.get(i).getSpec().getAddress(), TestUtils.generateMessages(messageCount));
                    //wait for messages sent
                    assertEquals(messageCount, sent.get(1, TimeUnit.MINUTES).intValue(),
                            "Incorrect count of messages send");
                }
            }
        }

        //receive messages
        for (AmqpClient client : clients) {
            for (int i = 0; i < dest.size(); i++) {
                if (i % everyN == 0) {
                    Future<List<Message>> received = client.recvMessages(dest.get(i).getSpec().getAddress(), messageCount);
                    //wait for messages received
                    assertEquals(messageCount, received.get(1, TimeUnit.MINUTES).size(),
                            "Incorrect count of messages received");
                }
            }
            client.close();
        }
    }

    //================================================================================================
    //====================================== IoT test methods  =======================================
    //================================================================================================

    protected void waitForFirstSuccessOnTelemetry(HttpAdapterClient adapterClient) throws Exception {
        IoTUtils.waitForFirstSuccess(adapterClient, MessageType.TELEMETRY);
    }

    /**
     * Test if the enabled flag is set to "enabled".
     * <br>
     * The flag is considered "enabled", in case the value is "true" or missing.
     *
     * @param enabled The object to test.
     */
    protected static void assertDefaultEnabled(final Boolean enabled) {
        if ( enabled != null && !Boolean.TRUE.equals(enabled)) {
            fail("Default value must be 'null' or 'true'");
        }
    }
}
