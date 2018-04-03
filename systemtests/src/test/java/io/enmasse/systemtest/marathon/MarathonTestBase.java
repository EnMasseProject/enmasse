/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.resolvers.ExtensionContextParameterResolver;
import io.enmasse.systemtest.selenium.ConsoleWebPage;
import io.enmasse.systemtest.selenium.ISeleniumProvider;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.standard.TopicTest;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ErrorCollector;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;

@Tag("marathon")
@ExtendWith(ExtensionContextParameterResolver.class)
public abstract class MarathonTestBase extends TestBase implements ISeleniumProvider {
    private static Logger log = CustomLogger.getLogger();
    private ArrayList<AmqpClient> clients = new ArrayList<>();
    private SeleniumProvider selenium = new SeleniumProvider();
    private ConsoleWebPage consoleWebPage;
    private ErrorCollector collector = new ErrorCollector();

    @Override
    public WebDriver buildDriver() {
        return getFirefoxDriver();
    }

    @AfterEach
    public void after(ExtensionContext context) {
        if (context.getExecutionException().isPresent()) { //test failed
            selenium.onFailed(context);
            selenium.tearDownDrivers();
        }
    }

    //========================================================================================================
    // Runner tests methods
    //========================================================================================================

    private void runTestInLoop(int durationMinutes, ITestMethod test) {
        log.info(String.format("Starting test running for %d minutes at %s",
                durationMinutes, new Date().toString()));
        int fails = 0;
        int limit = 10;
        for (long stop = System.nanoTime() + TimeUnit.MINUTES.toNanos(durationMinutes); stop > System.nanoTime(); ) {
            try {
                test.run();
                fails = 0;
            } catch (Exception ex) {
                collector.addError(ex);
                if (++fails >= limit) {
                    throw new IllegalStateException(String.format("Test failed: %d times in a row", fails));
                }
            } finally {
                closeClients();
            }
        }
    }

    private void closeClients() {
        for (AmqpClient client : clients) {
            try {
                client.close();
                log.info("Client is closed.");
            } catch (Exception ex) {
                collector.addError(ex);
            }
        }
        clients.clear();
    }

    //========================================================================================================
    // Tests methods
    //========================================================================================================

    void doTestCreateDeleteAddressSpaceLong(AddressSpace addressSpace) throws Exception {
        runTestInLoop(30, () -> {
            createAddressSpace(addressSpace);
            log.info("Address space created");

            doAddressTest(addressSpace, "test-topic-createdelete-brokered-%d",
                    "test-queue-createdelete-brokered-%d");

            deleteAddressSpace(addressSpace);
            log.info("Address space removed");
            Thread.sleep(10000);
        });
    }

    void doTestCreateDeleteAddressesLong(AddressSpace addressSpace) throws Exception {
        createAddressSpace(addressSpace);

        runTestInLoop(30, () -> {
            doAddressTest(addressSpace, "test-topic-createdelete-brokered-%d",
                    "test-queue-createdelete-brokered-%d");
            Thread.sleep(30000);
        });
    }

    void doTestCreateDeleteAddressesWithAuthLong(AddressSpace addressSpace) throws Exception {
        log.info("test testCreateDeleteAddressesWithAuthLong");
        createAddressSpace(addressSpace);

        String username = "test-user";
        String password = "test-user";

        createUser(addressSpace, username, password);

        runTestInLoop(30, () -> {
            doAddressTest(addressSpace, "test-topic-createdelete-auth-brokered-%d",
                    "test-queue-createdelete-auth-brokered-%d", username, password);
            Thread.sleep(30000);
        });
    }

    void doTestQueueSendReceiveLong(AddressSpace addressSpace) throws Exception {
        createAddressSpace(addressSpace);

        int msgCount = 1000;
        int queueCount = 10;
        int senderCount = 10;
        int recvCount = 20;

        List<Destination> queueList = new ArrayList<>();

        //create queues
        for (int i = 0; i < queueCount; i++) {
            queueList.add(Destination.queue(String.format("test-queue-sendreceive-%d", i), getDefaultPlan(AddressType.QUEUE)));
        }
        setAddresses(addressSpace, queueList.toArray(new Destination[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        runTestInLoop(30, () -> {
            //create client
            AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
            client.getConnectOptions().setUsername("test").setPassword("test");
            clients.add(client);

            //attach receivers
            List<Future<List<Message>>> recvResults = new ArrayList<>();
            for (int i = 0; i < recvCount / 2; i++) {
                recvResults.add(client.recvMessages(queueList.get(i).getAddress(), msgCount / 2));
                recvResults.add(client.recvMessages(queueList.get(i).getAddress(), msgCount / 2));
            }

            //attach senders
            for (int i = 0; i < senderCount; i++) {
                collector.checkThat(client.sendMessages(queueList.get(i).getAddress(), msgBatch,
                        1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES), is(msgBatch.size()));
            }

            //check received messages
            for (int i = 0; i < recvCount; i++) {
                collector.checkThat(recvResults.get(i).get().size(), is(msgCount / 2));
            }
            Thread.sleep(5000);
        });
    }

    void doTestCreateDeleteUsersLong(AddressSpace addressSpace) throws Exception {
        log.info("testCreateDeleteUsersLong start");
        createAddressSpace(addressSpace);
        log.info("Address space '{}'created", addressSpace);

        Destination queue = Destination.queue("test-create-delete-users-queue", getDefaultPlan(AddressType.QUEUE));
        Destination topic = Destination.topic("test-create-delete-users-topic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressSpace, queue, topic);
        log.info("Addresses '{}', '{}' created", queue.getAddress(), topic.getAddress());

        final String prefixUser = "test-user";
        final String prefixPswd = "test-user";

        AtomicInteger from = new AtomicInteger(0);
        AtomicInteger to = new AtomicInteger(100);
        int iteration = 100;
        int step = 10;

        runTestInLoop(30, () -> {
            createUsers(addressSpace, prefixUser, prefixPswd, from.get(), to.get());
            log.info("Users <{};{}> successfully created", from.get(), to.get());
            for (int i = from.get(); i < to.get(); i += step) {
                doBasicAuthQueueTopicTest(addressSpace, queue, topic, prefixUser + i, prefixPswd + i);
            }
            removeUsers(addressSpace, prefixUser, from.get(), to.get() - step);
            log.info("Users <{};{}> successfully removed", from.get(), to.get() - step);
            from.set(from.get() + iteration);
            to.set(to.get() + iteration);
            Thread.sleep(5000);
        });
        log.info("testCreateDeleteUsersLong finished");
    }

    void doTestAuthSendReceiveLong(AddressSpace addressSpace) throws Exception {
        log.info("testAuthSendReceiveLong start");
        createAddressSpace(addressSpace);
        log.info("Address space '{}'created", addressSpace);

        Destination queue = Destination.queue("test-auth-send-receive-queue", getDefaultPlan(AddressType.QUEUE));
        Destination topic = Destination.topic("test-auth-send-receive-topic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressSpace, queue, topic);
        log.info("Addresses '{}', '{}' created", queue.getAddress(), topic.getAddress());

        final String username = "test-user";
        final String password = "test-user";

        createUser(addressSpace, username, password);

        runTestInLoop(30, () -> {
            log.info("Start test loop basic auth tests");
            doBasicAuthQueueTopicTest(addressSpace, queue, topic, username, password);
            assertCannotConnect(addressSpace, "nobody", "nobody", Arrays.asList(queue, topic));
            Thread.sleep(5000);
        });
        log.info("testAuthSendReceiveLong finished");
    }

    void doTestCreateDeleteUsersRestartKeyCloakLong(AddressSpace addressSpace) throws Exception {
        log.info("testCreateDeleteUsersRestartKeyCloakLong start");
        createAddressSpace(addressSpace);
        log.info("Address space '{}'created", addressSpace);

        final Destination queue = Destination.queue("test-create-delete-users-restart-queue", getDefaultPlan(AddressType.QUEUE));
        final Destination topic = Destination.topic("test-create-delete-users-restart-topic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressSpace, queue, topic);
        log.info("Addresses '{}', '{}' created", queue.getAddress(), topic.getAddress());

        final String username = "test-user";
        final String password = "test-user";

        runTestInLoop(30, () -> {
            log.info("Start test iteration");
            createUser(addressSpace, username, password);
            assertCanConnect(addressSpace, username, password, Arrays.asList(queue, topic));
            log.info("Restart keycloak");
            scaleKeycloak(0);
            scaleKeycloak(1);
            Thread.sleep(160000);
            assertCanConnect(addressSpace, username, password, Arrays.asList(queue, topic));
            removeUser(addressSpace, username);
        });
        log.info("testCreateDeleteUsersRestartKeyCloakLong finished");
    }

    void doTestTopicPubSubLong(AddressSpace addressSpace) throws Exception {
        createAddressSpace(addressSpace);

        int msgCount = 1000;
        int topicCount = 10;

        List<Destination> topicList = new ArrayList<>();

        //create queues
        for (int i = 0; i < topicCount; i++) {
            topicList.add(Destination.topic(String.format("test-topic-pubsub-%d", i), getDefaultPlan(AddressType.TOPIC)));
        }
        setAddresses(addressSpace, topicList.toArray(new Destination[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        runTestInLoop(30, () -> {
            AmqpClient client = amqpClientFactory.createTopicClient(addressSpace);
            client.getConnectOptions().setUsername("test").setPassword("test");
            clients.add(client);

            //attach subscibers
            List<Future<List<Message>>> recvResults = new ArrayList<>();
            for (int i = 0; i < topicCount; i++) {
                recvResults.add(client.recvMessages(String.format("test-topic-pubsub-%d", i), msgCount));
            }

            //attach producers
            for (int i = 0; i < topicCount; i++) {
                collector.checkThat(client.sendMessages(topicList.get(i).getAddress(), msgBatch,
                        1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES), is(msgBatch.size()));
            }

            //check received messages
            for (int i = 0; i < topicCount; i++) {
                collector.checkThat(recvResults.get(i).get().size(), is(msgCount));
            }
            Thread.sleep(5000);
        });
    }

    void doTestCreateDeleteAddressesViaAgentLong(AddressSpace addressSpace) throws Exception {
        log.info("testCreateDeleteUsersLong start");
        createAddressSpace(addressSpace);
        log.info("Address space '{}'created", addressSpace);

        username = "test";
        password = "test";
        createUser(addressSpace, username, username);

        selenium.setupDriver(environment, kubernetes, buildDriver());
        consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressApiClient, addressSpace, username, password);
        consoleWebPage.openWebConsolePage(username, password);

        int addressCount = 5;
        ArrayList<Destination> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        runTestInLoop(30, () -> {
            consoleWebPage.createAddressesWebConsole(addresses.toArray(new Destination[0]));
            consoleWebPage.deleteAddressesWebConsole(addresses.toArray(new Destination[0]));
            Thread.sleep(5000);
        });
        log.info("testCreateDeleteAddressesViaAgentLong finished");
    }

    //========================================================================================================
    // Help methods
    //========================================================================================================

    private void doAddressTest(AddressSpace addressSpace, String topicPattern,
                               String queuePattern, String username, String password) throws Exception {
        List<Destination> queueList = new ArrayList<>();
        List<Destination> topicList = new ArrayList<>();

        int destinationCount = 20;

        for (int i = 0; i < destinationCount; i++) {
            queueList.add(Destination.queue(String.format(queuePattern, i), getDefaultPlan(AddressType.QUEUE)));
            topicList.add(Destination.topic(String.format(topicPattern, i), getDefaultPlan(AddressType.TOPIC)));
        }

        AmqpClient queueClient;
        AmqpClient topicClient;

        setAddresses(addressSpace, queueList.toArray(new Destination[0]));
        for (Destination queue : queueList) {
            queueClient = amqpClientFactory.createQueueClient(addressSpace);
            queueClient.getConnectOptions().setUsername(username).setPassword(password);
            clients.add(queueClient);

            QueueTest.runQueueTest(queueClient, queue, 1024);
        }

        setAddresses(addressSpace, topicList.toArray(new Destination[0]));

        for (Destination topic : topicList) {
            topicClient = amqpClientFactory.createTopicClient(addressSpace);
            topicClient.getConnectOptions().setUsername(username).setPassword(password);
            clients.add(topicClient);

            TopicTest.runTopicTest(topicClient, topic, 1024);
        }

        deleteAddresses(addressSpace, queueList.toArray(new Destination[0]));
        deleteAddresses(addressSpace, topicList.toArray(new Destination[0]));
        Thread.sleep(15000);
    }

    private void doAddressTest(AddressSpace addressSpace, String topicPattern, String queuePattern) throws Exception {
        doAddressTest(addressSpace, topicPattern, queuePattern, "test", "test");
    }

    private void doBasicAuthQueueTopicTest(AddressSpace addressSpace, Destination queue, Destination topic,
                                           String uname, String password) throws Exception {
        int messageCount = 100;
        AmqpClient queueClient = amqpClientFactory.createQueueClient(addressSpace);
        queueClient.getConnectOptions().setUsername(uname).setPassword(password);
        clients.add(queueClient);
        io.enmasse.systemtest.standard.QueueTest.runQueueTest(queueClient, queue, messageCount);
        log.info("User: '{}'; Message count:'{}'; destination:'{}' - done",
                uname, messageCount, queue.getAddress());

        AmqpClient topicClient = amqpClientFactory.createTopicClient(addressSpace);
        topicClient.getConnectOptions().setUsername(uname).setPassword(password);
        clients.add(topicClient);
        TopicTest.runTopicTest(topicClient, topic, messageCount);
        log.info("User: '{}'; Message count:'{}'; destination:'{}' - done",
                uname, messageCount, topic.getAddress());
    }
}
