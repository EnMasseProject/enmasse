/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.standard;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.ability.ITestBaseStandard;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.resolvers.JmsProviderParameterResolver;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(JmsProviderParameterResolver.class)
public class QueueTest extends TestBaseWithShared implements ITestBaseStandard {
    private static Logger log = CustomLogger.getLogger();
    private Connection connection;

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    public static void runQueueTest(AmqpClient client, Destination dest) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        runQueueTest(client, dest, 1024);
    }

    public static void runQueueTest(AmqpClient client, Destination dest, int countMessages) throws InterruptedException, TimeoutException, ExecutionException, IOException {
        List<String> msgs = TestUtils.generateMessages(countMessages);
        Count<Message> predicate = new Count<>(msgs.size());
        Future<Integer> numSent = client.sendMessages(dest.getAddress(), msgs, predicate);

        assertNotNull(numSent, "Sending messages didn't start");
        int actual = 0;
        try {
            actual = numSent.get(1, TimeUnit.MINUTES);
        } catch (TimeoutException t) {
            fail("Sending messages timed out after sending " + predicate.actual());
        }
        assertThat("Wrong count of messages sent", actual, is(msgs.size()));

        predicate = new Count<>(msgs.size());
        Future<List<Message>> received = client.recvMessages(dest.getAddress(), predicate);
        actual = 0;
        try {
            actual = received.get(1, TimeUnit.MINUTES).size();
        } catch (TimeoutException t) {
            fail("Receiving messages timed out after " + predicate.actual() + " msgs received");
        }

        assertThat("Wrong count of messages received", actual, is(msgs.size()));
    }

    @Test
    void testColocatedQueues() throws Exception {
        Destination q1 = Destination.queue("queue1", "pooled-queue");
        Destination q2 = Destination.queue("queue2", "pooled-queue");
        Destination q3 = Destination.queue("queue3", "pooled-queue");
        setAddresses(q1, q2, q3);

        AmqpClient client = amqpClientFactory.createQueueClient();
        runQueueTest(client, q1);
        runQueueTest(client, q2);
        runQueueTest(client, q3);
    }

    @Test
    void testShardedQueues() throws Exception {
        Destination q1 = Destination.queue("persistedQueue1", "sharded-queue");
        Destination q2 = Destination.queue("persistedQueue2", "sharded-queue");

        setAddresses(q1, q2);

        AmqpClient client = amqpClientFactory.createQueueClient();
        runQueueTest(client, q1);
        runQueueTest(client, q2);
    }

    @Test
    void testRestApi() throws Exception {
        Destination q1 = Destination.queue("queue1", getDefaultPlan(AddressType.QUEUE));
        Destination q2 = Destination.queue("queue2", getDefaultPlan(AddressType.QUEUE));

        runRestApiTest(sharedAddressSpace, q1, q2);
    }

    @Test
    void testCreateDeleteQueue() throws Exception {
        List<String> queues = IntStream.range(0, 16).mapToObj(i -> "queue-create-delete-" + i).collect(Collectors.toList());
        Destination destExtra = Destination.queue("ext-queue", "pooled-queue");

        List<Destination> addresses = new ArrayList<>();
        queues.forEach(queue -> addresses.add(Destination.queue(queue, "pooled-queue")));

        AmqpClient client = amqpClientFactory.createQueueClient();
        for (Destination address : addresses) {
            setAddresses(address, destExtra);
            Thread.sleep(20_000);

            //runQueueTest(client, address, 1); //TODO! commented due to issue #429

            deleteAddresses(address);
            Future<List<String>> response = getAddresses(Optional.empty());
            assertThat("Extra destination was not created ",
                    response.get(20, TimeUnit.SECONDS), is(Collections.singletonList(destExtra.getAddress())));
            deleteAddresses(destExtra);
            response = getAddresses(Optional.empty());
            assertThat("No destinations are expected",
                    response.get(20, TimeUnit.SECONDS), is(java.util.Collections.emptyList()));
            Thread.sleep(20_000);
        }
    }

    @Test
    void testMessagePriorities() throws Exception {
        Destination dest = Destination.queue("messagePrioritiesQueue", getDefaultPlan(AddressType.QUEUE));
        setAddresses(dest);

        AmqpClient client = amqpClientFactory.createQueueClient();
        Thread.sleep(30_000);

        int msgsCount = 1024;
        List<Message> listOfMessages = new ArrayList<>();
        for (int i = 0; i < msgsCount; i++) {
            Message msg = Message.Factory.create();
            msg.setAddress(dest.getAddress());
            msg.setBody(new AmqpValue(dest.getAddress()));
            msg.setSubject("subject");
            msg.setPriority((short) (i % 10));
            listOfMessages.add(msg);
        }

        Future<Integer> sent = client.sendMessages(dest.getAddress(),
                listOfMessages.toArray(new Message[0]));
        assertThat("Wrong count of messages sent", sent.get(1, TimeUnit.MINUTES), is(msgsCount));

        Future<List<Message>> received = client.recvMessages(dest.getAddress(), msgsCount);
        assertThat("Wrong count of messages received", received.get(1, TimeUnit.MINUTES).size(), is(msgsCount));

        int sub = 1;
        for (Message m : received.get()) {
            for (Message mSub : received.get().subList(sub, received.get().size())) {
                assertTrue(m.getPriority() >= mSub.getPriority(), "Wrong order of messages");
            }
            sub++;
        }
    }

    @Test
    @Disabled("disabled due to issue #851")
    void testScaledown() throws Exception {
        Destination dest = Destination.queue("scalequeue", "sharded-queue");
        setAddresses(dest);
        scale(dest, 4);

        Thread.sleep(30000);
        AmqpClient client = amqpClientFactory.createQueueClient();
        List<Future<Integer>> sent = Arrays.asList(
                client.sendMessages(dest.getAddress(), TestUtils.generateMessages("foo", 1000)),
                client.sendMessages(dest.getAddress(), TestUtils.generateMessages("bar", 1000)),
                client.sendMessages(dest.getAddress(), TestUtils.generateMessages("baz", 1000)),
                client.sendMessages(dest.getAddress(), TestUtils.generateMessages("quux", 1000)));

        assertAll("All sender should send all messages",
                () -> assertThat("Wrong count of messages sent: sender0",
                        sent.get(0).get(1, TimeUnit.MINUTES), is(1000)),
                () -> assertThat("Wrong count of messages sent: sender1",
                        sent.get(1).get(1, TimeUnit.MINUTES), is(1000)),
                () -> assertThat("Wrong count of messages sent: sender2",
                        sent.get(2).get(1, TimeUnit.MINUTES), is(1000)),
                () -> assertThat("Wrong count of messages sent: sender3",
                        sent.get(3).get(1, TimeUnit.MINUTES), is(1000))
        );

        Future<List<Message>> received = client.recvMessages(dest.getAddress(), 500);
        assertThat("Wrong count of messages received",
                received.get(1, TimeUnit.MINUTES).size(), is(500));

        scale(dest, 1);

        Thread.sleep(30000);

        received = client.recvMessages(dest.getAddress(), 3500);

        assertThat("Wrong count of messages received",
                received.get(1, TimeUnit.MINUTES).size(), is(3500));
    }

    @Test
    public void concurrentOperationsTest() throws Exception {
        HashMap<CompletableFuture<Void>, List<KeycloakCredentials>> company = new HashMap<>();
        int customersCount = 10;
        int usersCount = 5;
        int destinationCount = 10;
        String destNamePrefix = "queue";

        for (int i = 0; i < customersCount; i++) {
            //define users
            ArrayList<KeycloakCredentials> users = new ArrayList<>(usersCount);
            for (int j = 0; j < usersCount; j++) {
                users.add(new KeycloakCredentials(
                        String.format("uname.%s.%s", i, j),
                        String.format("p$$wd.%s.%s", i, j)));
            }

            //define destinations
            Destination[] destinations = new Destination[destinationCount];
            for (int destI = 0; destI < destinationCount; destI++) {
                destinations[destI] = Destination.queue(String.format("%s.%s.%s", destNamePrefix, i, destI), getDefaultPlan(AddressType.QUEUE));
            }

            //run async: append addresses; create users; send/receive messages
            final int customerIndex = i;
            company.put(CompletableFuture.runAsync(() ->
            {
                try {
                    int messageCount = 43;
                    appendAddresses(false, destinations); //without wait
                    doMessaging(Arrays.asList(destinations), users, destNamePrefix, customerIndex, messageCount);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }), users);
        }

        //once one of the doMessaging method is finished  then remove appropriate users
        for (Map.Entry<CompletableFuture<Void>, List<KeycloakCredentials>> customer : company.entrySet()) {
            customer.getKey().get();
            removeUsers(sharedAddressSpace, customer.getValue().stream().map(KeycloakCredentials::getUsername).collect(Collectors.toList()));
        }
    }

    @Test
    void testLargeMessages(JmsProvider jmsProvider) throws Exception {
        Destination addressQueue = Destination.queue("jmsQueue", getDefaultPlan(AddressType.QUEUE));
        setAddresses(addressQueue);

        connection = jmsProvider.createConnection(getMessagingRoute(sharedAddressSpace).toString(), defaultCredentials,
                "jmsCliId", addressQueue);
        connection.start();

        sendReceiveLargeMessage(jmsProvider, 90, addressQueue, 1);
        sendReceiveLargeMessage(jmsProvider, 50, addressQueue, 1);
        sendReceiveLargeMessage(jmsProvider, 10, addressQueue, 1);
        sendReceiveLargeMessage(jmsProvider, 1, addressQueue, 1);
        sendReceiveLargeMessage(jmsProvider, 90, addressQueue, 1, DeliveryMode.PERSISTENT);
        sendReceiveLargeMessage(jmsProvider, 50, addressQueue, 1, DeliveryMode.PERSISTENT);
        sendReceiveLargeMessage(jmsProvider, 10, addressQueue, 1, DeliveryMode.PERSISTENT);
        sendReceiveLargeMessage(jmsProvider, 1, addressQueue, 1, DeliveryMode.PERSISTENT);
    }
}

