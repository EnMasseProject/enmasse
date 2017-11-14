package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.standard.TopicTest;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class AuthenticationTest extends MarathonTestBase {
    @Test
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
        AtomicInteger to = new AtomicInteger(0);
        int iteration = 100;
        int step = 10;

        runTestInLoop(30, () -> {
            createUsers(addressSpace, prefixUser, prefixPswd, from.get(), to.get());
            Logging.log.info("Users <{};{}> successfully created", from.get(), to.get());
            for (int i = from.get(); i < to.get(); i += step) {
                doBasicAuthQueueTopicTest(addressSpace, queue, topic, prefixUser + i, prefixPswd + i);
            }
            removeUsers(addressSpace, prefixUser, from.get(), to.get() - step);
            Logging.log.info("Users <{};{}> successfully removed", from.get(), to.get());
            from.set(from.get() + iteration);
            to.set(to.get() + iteration);
        });
        Logging.log.info("testCreateDeleteUsersLong finished");
    }

    private void createUsers(AddressSpace addressSpace, String prefixName, String prefixPswd, int from, int to)
            throws Exception {
        KeycloakCredentials user;
        for (int i = from; i < to; i++) {
            user = new KeycloakCredentials(prefixName + i, prefixPswd + i);
            getKeycloakClient().createUser(addressSpace.getName(), user.getUsername(), user.getPassword());
        }
    }

    private void removeUsers(AddressSpace addressSpace, String prefixName, int from, int to) throws Exception {
        for (int i = from; i < to; i++) {
            getKeycloakClient().deleteUser(addressSpace.getName(), prefixName + i);
        }
    }

    private void doBasicAuthQueueTopicTest(AddressSpace addressSpace, Destination queue, Destination topic,
                                           String uname, String password) throws Exception {
        AmqpClient queueClient = amqpClientFactory.createQueueClient(addressSpace);
        queueClient.getConnectOptions().setUsername(uname).setPassword(password);
        io.enmasse.systemtest.standard.QueueTest.runQueueTest(queueClient, queue, 100);

        AmqpClient topicClient = amqpClientFactory.createTopicClient(addressSpace);
        topicClient.getConnectOptions().setUsername(uname).setPassword(password);
        TopicTest.runTopicTest(topicClient, topic, 100);
    }
}
