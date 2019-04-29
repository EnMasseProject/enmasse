/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.SysytemTestsErrorCollector;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.selenium.SeleniumManagement;
import io.enmasse.systemtest.selenium.SeleniumProvider;
import io.enmasse.systemtest.selenium.page.ConsoleWebPage;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.standard.TopicTest;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static io.enmasse.systemtest.TestTag.marathon;
import static org.hamcrest.CoreMatchers.is;

@Tag(marathon)
abstract class MarathonTestBase extends TestBase {
    private static Logger log = CustomLogger.getLogger();
    private ArrayList<AmqpClient> clients = new ArrayList<>();
    private ConsoleWebPage consoleWebPage;
    private SysytemTestsErrorCollector collector = new SysytemTestsErrorCollector();

    @BeforeEach
    void setupMarathonTests() {
        collector.clear();
    }

    //========================================================================================================
    // Runner tests methods
    //========================================================================================================

    protected void runTestInLoop(int durationMinutes, ITestMethod test) throws Exception {
        log.info(String.format("Starting test running for %d minutes at %s",
                durationMinutes, new Date().toString()));
        int fails = 0;
        int limit = 10;
        int i = 0;
        for (long stop = System.nanoTime() + TimeUnit.MINUTES.toNanos(durationMinutes); stop > System.nanoTime(); ) {
            try {
                log.info("*********************************** Test run {} ***********************************", ++i);
                test.run();
                fails = 0;
            } catch (Exception ex) {
                log.warn("Test run {} failed with: {}", i, ex.getMessage());
                collector.addError(ex);
                if (++fails >= limit) {
                    throw new IllegalStateException(String.format("Test failed: %d times in a row: %s", fails, collector.toString()));
                }
            } finally {
                closeClients();
                log.info("***********************************************************************************");
                Thread.sleep(60_000);
            }
        }
        if (!collector.verify()) {
            throw new IllegalStateException(String.format("Test failed with these exceptions: %s", collector.toString()));
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

    void doTestCreateDeleteAddressSpaceLong(Supplier<AddressSpace> supplier) throws Exception {
        runTestInLoop(30, () -> {
            UserCredentials cred = new UserCredentials("david", "kornelius");
            AddressSpace addressSpace = supplier.get();
            createAddressSpace(addressSpace);
            createUser(addressSpace, cred);

            log.info("Address space created");

            doAddressTest(addressSpace, "test-topic-createdelete-brokered-%d",
                    "test-queue-createdelete-brokered-%d", cred);

            deleteAddressSpace(addressSpace);
            log.info("Address space removed");
        });
    }

    void doTestCreateDeleteAddressesWithAuthLong(AddressSpace addressSpace) throws Exception {
        log.info("test testCreateDeleteAddressesWithAuthLong");
        createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("test-user", "test-user");
        createUser(addressSpace, user);

        runTestInLoop(30, () -> {
            doAddressTest(addressSpace, "test-topic-createdelete-auth-brokered-%d",
                    "test-queue-createdelete-auth-brokered-%d", user);
        });
    }

    void doTestCreateHighAddressCountCheckStatusDeleteLong(AddressSpace addressSpace) throws Exception {
        String notReadyString = "\"isReady\":false";
        createAddressSpace(addressSpace);

        UserCredentials user = new UserCredentials("test-user", "test-user");
        createUser(addressSpace, user);

        List<Address> queueList = new ArrayList<>();
        int queueCount = 1500;

        IntStream.range(0, queueCount).forEach(i ->
                queueList.add(AddressUtils.createQueueAddressObject(String.format("test-queue-status-%d", i), getDefaultPlan(AddressType.QUEUE)))
        );

        runTestInLoop(60, () -> {
            //create addresses
            setAddresses(addressSpace, queueList.toArray(new Address[0]));

            //get addresses from API server request
            List<Address> response = AddressUtils.getAddresses(addressSpace);
            log.info("{}", (Object) response.toArray(new String[0]));

            //check addresses are in ready state
            assertCanConnect(addressSpace, user, queueList);

            deleteAddresses(addressSpace, queueList.toArray(new Address[0]));
        });
    }

    void doTestQueueSendReceiveLong(AddressSpace addressSpace) throws Exception {
        createAddressSpace(addressSpace);

        int msgCount = 1000;
        int queueCount = 10;
        int senderCount = 10;
        int recvCount = 20;

        List<Address> queueList = new ArrayList<>();

        //create queues
        for (int i = 0; i < queueCount; i++) {
            queueList.add(AddressUtils.createQueueAddressObject(String.format("test-queue-sendreceive-%d", i), getDefaultPlan(AddressType.QUEUE)));
        }
        setAddresses(addressSpace, queueList.toArray(new Address[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        UserCredentials credentials = new UserCredentials("test", "test");
        createUser(addressSpace, credentials);

        runTestInLoop(30, () -> {
            //create client
            AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
            client.getConnectOptions().setCredentials(credentials);
            clients.add(client);

            //attach receivers
            List<Future<List<Message>>> recvResults = new ArrayList<>();
            for (int i = 0; i < recvCount / 2; i++) {
                recvResults.add(client.recvMessages(queueList.get(i).getSpec().getAddress(), msgCount / 2));
                recvResults.add(client.recvMessages(queueList.get(i).getSpec().getAddress(), msgCount / 2));
            }

            //attach senders
            for (int i = 0; i < senderCount; i++) {
                collector.checkThat(client.sendMessages(queueList.get(i).getSpec().getAddress(), msgBatch).get(2, TimeUnit.MINUTES), is(msgBatch.size()));
            }

            //check received messages
            for (int i = 0; i < recvCount; i++) {
                collector.checkThat(recvResults.get(i).get().size(), is(msgCount / 2));
            }
        });
    }

    void doTestCreateDeleteUsersLong(AddressSpace addressSpace) throws Exception {
        log.info("testCreateDeleteUsersLong start");
        createAddressSpace(addressSpace);
        log.info("Address space '{}'created", addressSpace);

        Address queue = AddressUtils.createQueueAddressObject("test-create-delete-users-queue", getDefaultPlan(AddressType.QUEUE));
        Address topic = AddressUtils.createTopicAddressObject("test-create-delete-users-topic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressSpace, queue, topic);
        log.info("Addresses '{}', '{}' created", queue.getSpec().getAddress(), topic.getSpec().getAddress());

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
                assertCanConnect(addressSpace, new UserCredentials(prefixUser + i, prefixPswd + i), Arrays.asList(queue, topic));
            }
            removeUsers(addressSpace, prefixUser, from.get(), to.get() - step);
            log.info("Users <{};{}> successfully removed", from.get(), to.get() - step);
            from.set(from.get() + iteration);
            to.set(to.get() + iteration);
        });
        log.info("testCreateDeleteUsersLong finished");
    }

    void doTestAuthSendReceiveLong(AddressSpace addressSpace) throws Exception {
        log.info("testAuthSendReceiveLong start");
        createAddressSpace(addressSpace);
        log.info("Address space '{}'created", addressSpace);

        Address queue = AddressUtils.createQueueAddressObject("test-auth-send-receive-queue", getDefaultPlan(AddressType.QUEUE));
        Address topic = AddressUtils.createTopicAddressObject("test-auth-send-receive-topic", getDefaultPlan(AddressType.TOPIC));
        setAddresses(addressSpace, queue, topic);
        log.info("Addresses '{}', '{}' created", queue.getSpec().getAddress(), topic.getSpec().getAddress());

        UserCredentials user = new UserCredentials("test-user", "test-user");
        createUser(addressSpace, user);

        runTestInLoop(30, () -> {
            log.info("Start test loop basic auth tests");
            assertCanConnect(addressSpace, user, Arrays.asList(queue, topic));
            assertCannotConnect(addressSpace, new UserCredentials("nobody", "nobody"), Arrays.asList(queue, topic));
        });
        log.info("testAuthSendReceiveLong finished");
    }

    void doTestTopicPubSubLong(AddressSpace addressSpace) throws Exception {
        createAddressSpace(addressSpace);

        int msgCount = 1000;
        int topicCount = 10;

        List<Address> topicList = new ArrayList<>();

        //create queues
        for (int i = 0; i < topicCount; i++) {
            topicList.add(AddressUtils.createTopicAddressObject(String.format("test-topic-pubsub-%d", i), getDefaultPlan(AddressType.TOPIC)));
        }
        setAddresses(addressSpace, topicList.toArray(new Address[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        UserCredentials credentials = new UserCredentials("test", "test");
        createUser(addressSpace, credentials);
        runTestInLoop(30, () -> {
            AmqpClient client = amqpClientFactory.createTopicClient(addressSpace);
            client.getConnectOptions().setCredentials(credentials);
            clients.add(client);

            //attach subscibers
            List<Future<List<Message>>> recvResults = new ArrayList<>();
            for (int i = 0; i < topicCount; i++) {
                recvResults.add(client.recvMessages(String.format("test-topic-pubsub-%d", i), msgCount));
            }

            //attach producers
            for (int i = 0; i < topicCount; i++) {
                collector.checkThat(client.sendMessages(topicList.get(i).getSpec().getAddress(), msgBatch).get(2, TimeUnit.MINUTES), is(msgBatch.size()));
            }

            //check received messages
            for (int i = 0; i < topicCount; i++) {
                collector.checkThat(recvResults.get(i).get().size(), is(msgCount));
            }
        });
    }

    void doTestCreateDeleteAddressesViaAgentLong(AddressSpace addressSpace, String className, String testName) throws Exception {
        log.info("testCreateDeleteAddressesViaAgentLong start");
        createAddressSpace(addressSpace);
        log.info("Address space '{}'created", addressSpace);
        SeleniumManagement.deployFirefoxApp();

        UserCredentials user = new UserCredentials("test", "test");
        createUser(addressSpace, user);

        int addressCount = 5;
        ArrayList<Address> addresses = generateQueueTopicList("via-web", IntStream.range(0, addressCount));

        runTestInLoop(30, () -> {
            SeleniumProvider selenium = new SeleniumProvider();
            selenium.setupDriver(environment, kubernetes, TestUtils.getFirefoxDriver());
            consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressSpace, clusterUser);
            consoleWebPage.openWebConsolePage(user);
            try {
                consoleWebPage.createAddressesWebConsole(addresses.toArray(new Address[0]));
                consoleWebPage.deleteAddressesWebConsole(addresses.toArray(new Address[0]));
                Thread.sleep(5000);
                selenium.saveScreenShots(className, testName);
                selenium.tearDownDrivers();
            } catch (Exception ex) {
                selenium.setupDriver(environment, kubernetes, TestUtils.getFirefoxDriver());
                consoleWebPage = new ConsoleWebPage(selenium, getConsoleRoute(addressSpace), addressSpace, clusterUser);
                consoleWebPage.openWebConsolePage(user);
                throw new Exception(ex);
            }
        });
        log.info("testCreateDeleteAddressesViaAgentLong finished");
    }


    //========================================================================================================
    // Help methods
    //========================================================================================================

    private void doAddressTest(AddressSpace addressSpace, String topicPattern,
                               String queuePattern, UserCredentials credentials) throws Exception {
        List<Address> queueList = new ArrayList<>();
        List<Address> topicList = new ArrayList<>();

        int destinationCount = 20;

        for (int i = 0; i < destinationCount; i++) {
            queueList.add(AddressUtils.createQueueAddressObject(String.format(queuePattern, i), getDefaultPlan(AddressType.QUEUE)));
            topicList.add(AddressUtils.createTopicAddressObject(String.format(topicPattern, i), getDefaultPlan(AddressType.TOPIC)));
        }

        AmqpClient queueClient;
        AmqpClient topicClient;

        setAddresses(addressSpace, queueList.toArray(new Address[0]));
        appendAddresses(addressSpace, topicList.toArray(new Address[0]));

        queueClient = amqpClientFactory.createQueueClient(addressSpace);
        queueClient.getConnectOptions().setCredentials(credentials);
        clients.add(queueClient);

        topicClient = amqpClientFactory.createTopicClient(addressSpace);
        topicClient.getConnectOptions().setCredentials(credentials);
        clients.add(topicClient);

        for (Address queue : queueList) {
            QueueTest.runQueueTest(queueClient, queue, 1024);
        }

        for (Address topic : topicList) {
            TopicTest.runTopicTest(topicClient, topic, 1024);
        }

        deleteAddresses(addressSpace, queueList.toArray(new Address[0]));
        deleteAddresses(addressSpace, topicList.toArray(new Address[0]));
        Thread.sleep(15000);
    }
}
