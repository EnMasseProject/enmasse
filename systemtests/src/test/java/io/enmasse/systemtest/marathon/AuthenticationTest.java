package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.standard.TopicTest;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class AuthenticationTest extends MarathonTestBase {

//    @Test disabled due to issue: #520
    public void testCreateDeleteUsersLong() throws Exception {
        Logging.log.info("testCreateDeleteUsersLong start");
        AddressSpace addressSpace = new AddressSpace("test-create-delete-users-brokered",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "standard");
        Logging.log.info("Address space '{}'created", addressSpace);

        Destination queue = Destination.queue("test-create-delete-users-queue");
        Destination topic = Destination.topic("test-create-delete-users-topic");
        setAddresses(addressSpace, queue, topic);
        Logging.log.info("Addresses '{}', '{}' created", queue.getAddress(), topic.getAddress());

        final String prefixUser = "test-user";
        final String prefixPswd = "test-user";

        AtomicInteger from = new AtomicInteger(0);
        AtomicInteger to = new AtomicInteger(100);
        int iteration = 100;
        int step = 10;

        runTestInLoop(30, () -> {
            createUsers(addressSpace, prefixUser, prefixPswd, from.get(), to.get());
            Logging.log.info("Users <{};{}> successfully created", from.get(), to.get());
            for (int i = from.get(); i < to.get(); i += step) {
                doBasicAuthQueueTopicTest(addressSpace, queue, topic, prefixUser + i, prefixPswd + i);
            }
            removeUsers(addressSpace, prefixUser, from.get(), to.get() - step);
            Logging.log.info("Users <{};{}> successfully removed", from.get(), to.get() - step);
            from.set(from.get() + iteration);
            to.set(to.get() + iteration);
            Thread.sleep(5000);
        });
        Logging.log.info("testCreateDeleteUsersLong finished");
    }

    @Test
    public void testAuthSendReceiveLong() throws Exception{
        Logging.log.info("testAuthSendReceiveLong start");
        AddressSpace addressSpace = new AddressSpace("test-auth-send-receive-brokered",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "standard");
        Logging.log.info("Address space '{}'created", addressSpace);

        Destination queue = Destination.queue("test-auth-send-receive-queue");
        Destination topic = Destination.topic("test-auth-send-receive-topic");
        setAddresses(addressSpace, queue, topic);
        Logging.log.info("Addresses '{}', '{}' created", queue.getAddress(), topic.getAddress());

        final String username = "test-user";
        final String password = "test-user";

        createUser(addressSpace, username, password);

        runTestInLoop(30, () -> {
            Logging.log.info("Start test iteration");
            doBasicAuthQueueTopicTest(addressSpace, queue, topic, username, password);
            doBasicAuthQueueTopicTest(addressSpace, queue, topic, "nobody", "nobody");
        });
        Logging.log.info("testAuthSendReceiveLong finished");
    }

    protected void createUser(AddressSpace addressSpace, String username, String password) throws Exception{
        getKeycloakClient().createUser(addressSpace.getName(), username, password);
    }

    protected void removeUser(AddressSpace addressSpace, String username) throws Exception{
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

    private void doBasicAuthQueueTopicTest(AddressSpace addressSpace, Destination queue, Destination topic,
                                           String uname, String password) throws Exception {
        int messageCount = 100;
        AmqpClient queueClient = amqpClientFactory.createQueueClient(addressSpace);
        queueClient.getConnectOptions().setUsername(uname).setPassword(password);
        io.enmasse.systemtest.standard.QueueTest.runQueueTest(queueClient, queue, messageCount);
        Logging.log.info("User: '{}'; Message count:'{}'; destination:'{}' - done",
                uname, messageCount, queue.getAddress());

        AmqpClient topicClient = amqpClientFactory.createTopicClient(addressSpace);
        topicClient.getConnectOptions().setUsername(uname).setPassword(password);
        TopicTest.runTopicTest(topicClient, topic, messageCount);
        Logging.log.info("User: '{}'; Message count:'{}'; destination:'{}' - done",
                uname, messageCount, topic.getAddress());
    }
}
