/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.shared.standard;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressBuilder;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBase;
import io.enmasse.systemtest.bases.shared.ITestSharedStandard;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.model.address.AddressType;
import io.enmasse.systemtest.model.addressplan.DestinationPlan;
import io.enmasse.systemtest.resolvers.JmsProviderParameterResolver;
import io.enmasse.systemtest.time.TimeoutBudget;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.utils.Count;
import io.enmasse.systemtest.utils.JmsProvider;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.enmasse.systemtest.TestTag.NON_PR;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(JmsProviderParameterResolver.class)
public class QueueTest extends TestBase implements ITestSharedStandard {
    private static Logger log = CustomLogger.getLogger();
    private Connection connection;

    public static void runQueueTest(AmqpClient client, Address dest) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        runQueueTest(client, dest, 512);
    }

    public static void runQueueTest(AmqpClient client, Address dest, int countMessages) throws InterruptedException, TimeoutException, ExecutionException, IOException {
        List<String> msgs = TestUtils.generateMessages(countMessages);
        Count<Message> predicate = new Count<>(msgs.size());
        long timeoutMs = countMessages * 200; //estimate in worst case it takes at most 200ms to send one message
        log.info("Start sending with " + timeoutMs + " ms timeout");
        Future<Integer> numSent = client.sendMessages(dest.getSpec().getAddress(), msgs, predicate);

        assertNotNull(numSent, "Sending messages didn't start");
        int actual = 0;
        try {
            actual = numSent.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException t) {
            logCollector.collectRouterState("runQueueTestSend");
            fail("Sending messages timed out after sending " + predicate.actual());
        }
        assertThat("Wrong count of messages sent", actual, is(msgs.size()));

        predicate = new Count<>(msgs.size());
        log.info("Start receiving with " + timeoutMs + " ms timeout");
        Future<List<Message>> received = client.recvMessages(dest.getSpec().getAddress(), predicate);
        actual = 0;
        try {
            actual = received.get(timeoutMs, TimeUnit.MILLISECONDS).size();
        } catch (TimeoutException t) {
            logCollector.collectRouterState("runQueueTestRecv");
            fail("Receiving messages timed out after " + predicate.actual() + " msgs received");
        }

        assertThat("Wrong count of messages received", actual, is(msgs.size()));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    @Test
    @Tag(NON_PR)
    void testColocatedQueues() throws Exception {
        Address q1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "queue1"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue1")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        Address q2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "queue2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue2")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        Address q3 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "queue3"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue3")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();
        resourcesManager.setAddresses(q1, q2, q3);

        AmqpClient client = getAmqpClientFactory().createQueueClient();
        runQueueTest(client, q1);
        runQueueTest(client, q2);
        runQueueTest(client, q3);
    }

    @Test
    void testShardedQueues() throws Exception {
        Address q1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "sharded-queue-1"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("sharded-queue-1")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();
        Address q2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "sharded-queue-2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("sharded_Queue_2")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();
        kubernetes.getAddressClient().create(q2);

        resourcesManager.appendAddresses(q1);
        waitForDestinationsReady(q2);

        AmqpClient client = getAmqpClientFactory().createQueueClient();
        runQueueTest(client, q1);
        runQueueTest(client, q2);
    }

    @Test
    @Tag(NON_PR)
    void testRestApi() throws Exception {
        Address q1 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "queue1"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue1")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        Address q2 = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "queue2"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue2")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();

        runRestApiTest(getSharedAddressSpace(), q1, q2);
    }

    @Test
    void testMessagePriorities() throws Exception {
        Address dest = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "queuepriorities"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("queue_priorities")
                .withPlan(getDefaultPlan(AddressType.QUEUE))
                .endSpec()
                .build();
        resourcesManager.setAddresses(dest);

        AmqpClient client = getAmqpClientFactory().createQueueClient();
        Thread.sleep(30_000);

        int msgsCount = 1024;
        List<Message> listOfMessages = new ArrayList<>();
        for (int i = 0; i < msgsCount; i++) {
            Message msg = Message.Factory.create();
            msg.setAddress(dest.getSpec().getAddress());
            msg.setBody(new AmqpValue(dest.getSpec().getAddress()));
            msg.setSubject("subject");
            msg.setPriority((short) (i % 10));
            listOfMessages.add(msg);
        }

        Future<Integer> sent = client.sendMessages(dest.getSpec().getAddress(),
                listOfMessages.toArray(new Message[0]));
        assertThat("Wrong count of messages sent", sent.get(150, TimeUnit.SECONDS), is(msgsCount));

        Future<List<Message>> received = client.recvMessages(dest.getSpec().getAddress(), msgsCount);
        assertThat("Wrong count of messages received", received.get(150, TimeUnit.SECONDS).size(), is(msgsCount));

        int sub = 1;
        for (Message m : received.get()) {
            for (Message mSub : received.get().subList(sub, received.get().size())) {
                assertTrue(m.getPriority() >= mSub.getPriority(), "Wrong order of messages");
            }
            sub++;
        }
    }

    @Test
    void testScaledown() throws Exception {
        Address xlarge = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "scalequeue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("scalequeue")
                .withPlan(DestinationPlan.STANDARD_XLARGE_QUEUE)
                .endSpec()
                .build();
        Address large = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "scalequeue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("scalequeue")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();
        Address small = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "scalequeue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("scalequeue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        testScale(xlarge, large, true);
        testScale(large, small, false);
    }

    @Test
    void testScaleup() throws Exception {
        Address xlarge = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "scalequeue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("scalequeue")
                .withPlan(DestinationPlan.STANDARD_XLARGE_QUEUE)
                .endSpec()
                .build();
        Address large = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "scalequeue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("scalequeue")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();
        Address small = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "scalequeue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("scalequeue")
                .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                .endSpec()
                .build();

        testScale(small, large, true);
        testScale(large, xlarge, false);
    }

    private void testScale(Address before, Address after, boolean createInitial) throws Exception {
        assertEquals(before.getSpec().getAddress(), after.getSpec().getAddress());
        assertEquals(before.getMetadata().getName(), after.getMetadata().getName());
        assertEquals(before.getSpec().getType(), after.getSpec().getType());

        if (createInitial) {
            resourcesManager.setAddresses(before);
        }

        AmqpClient client = getAmqpClientFactory().createQueueClient();
        final List<String> prefixes = Arrays.asList("foo", "bar", "baz", "quux");
        final int numMessages = 500;
        final int totalNumMessages = numMessages * prefixes.size();
        final int numReceiveBeforeDraining = numMessages / 2;
        final int numReceivedAfterScaled = totalNumMessages - numReceiveBeforeDraining;
        final int numReceivedAfterScaledPhase1 = numReceivedAfterScaled / 2;
        final int numReceivedAfterScaledPhase2 = numReceivedAfterScaled - numReceivedAfterScaledPhase1;

        List<Future<Integer>> sent = prefixes.stream().map(prefix -> client.sendMessages(before.getSpec().getAddress(), TestUtils.generateMessages(prefix, numMessages))).collect(Collectors.toList());

        assertAll("All sender should send all messages",
                () -> assertThat("Wrong count of messages sent: sender0",
                        sent.get(0).get(150, TimeUnit.SECONDS), is(numMessages)),
                () -> assertThat("Wrong count of messages sent: sender1",
                        sent.get(1).get(150, TimeUnit.SECONDS), is(numMessages)),
                () -> assertThat("Wrong count of messages sent: sender2",
                        sent.get(2).get(150, TimeUnit.SECONDS), is(numMessages)),
                () -> assertThat("Wrong count of messages sent: sender3",
                        sent.get(3).get(150, TimeUnit.SECONDS), is(numMessages))
        );

        assertReceive("before replace", numReceiveBeforeDraining, before, client);


        resourcesManager.replaceAddress(after);
        CustomLogger.getLogger().info(String.format("Now scaling from '%s' to '%s'", before.getSpec().getPlan(), after.getSpec().getPlan()));
        logCollector.collectLogsOfPodsByLabels(kubernetes.getInfraNamespace(), before.getSpec().getPlan(), Collections.singletonMap("role", "broker"));
        resourcesManager.replaceAddress(after);
        // Receive messages sent before address was replaced
        assertReceive("after replace", numReceivedAfterScaledPhase1, after, client);

        Thread.sleep(30_000);

        // Give system a chance to do something stupid
        assertReceive("after replace, second batch", numReceivedAfterScaledPhase2, after, client);

        // Ensure send and receive works after address was replaced
        assertThat("Wrong count of messages sent before awaiting drain", client.sendMessages(after.getSpec().getAddress(), TestUtils.generateMessages(prefixes.get(0), numMessages)).get(150, TimeUnit.SECONDS), is(numMessages));
        assertReceive("before awaiting drain", numMessages, after, client);

        // Ensure there are no brokers in Draining state
        AddressUtils.waitForBrokersDrained(new TimeoutBudget(4, TimeUnit.MINUTES), after);

        // Ensure send and receive works after all brokers are drained
        assertThat("Wrong count of messages sent after awaiting drain", client.sendMessages(after.getSpec().getAddress(), TestUtils.generateMessages(prefixes.get(1), numMessages)).get(150, TimeUnit.SECONDS), is(numMessages));
        assertReceive("after awaiting drain", numMessages, after, client);
    }

    private void assertReceive(String phase, int expected, Address addr, AmqpClient client) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            Future<List<Message>> listFuture = client.recvMessages(addr.getSpec().getAddress(), expected);
            int actual = listFuture.get(150, TimeUnit.SECONDS).size();
            assertThat("Wrong count of messages received " + phase, actual, is(expected));
        } catch (TimeoutException e) {
            CustomLogger.getLogger().error("Timed out " + phase);
            throw e;
        }
    }

    @Test
    public void testConcurrentOperations() throws Exception {
        HashMap<CompletableFuture<Void>, List<UserCredentials>> company = new HashMap<>();
        int customersCount = 10;
        int usersCount = 5;
        int destinationCount = 10;
        String destNamePrefix = "queue";

        for (int i = 0; i < customersCount; i++) {
            //define users
            ArrayList<UserCredentials> users = new ArrayList<>(usersCount);
            for (int j = 0; j < usersCount; j++) {
                users.add(new UserCredentials(
                        String.format("uname-%d-%d", i, j),
                        String.format("p$$wd-%d-%d", i, j)));
            }

            //define destinations
            Address[] destinations = new Address[destinationCount];
            for (int destI = 0; destI < destinationCount; destI++) {
                destinations[destI] = new AddressBuilder()
                        .withNewMetadata()
                        .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                        .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), String.format("%s.%s.%s", destNamePrefix, i, destI)))
                        .endMetadata()
                        .withNewSpec()
                        .withType("queue")
                        .withAddress(String.format("%s.%s.%s", destNamePrefix, i, destI))
                        .withPlan(DestinationPlan.STANDARD_SMALL_QUEUE)
                        .endSpec()
                        .build();
            }

            //run async: append addresses; create users; send/receive messages
            final int customerIndex = i;
            company.put(CompletableFuture.runAsync(() ->
            {
                try {
                    int messageCount = 43;
                    resourcesManager.appendAddresses(false, destinations);
                    doMessaging(Arrays.asList(destinations), users, destNamePrefix, customerIndex, messageCount);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }, runnable -> new Thread(runnable).start()), users);
        }

        //once one of the doMessaging method is finished  then remove appropriate users
        for (Map.Entry<CompletableFuture<Void>, List<UserCredentials>> customer : company.entrySet()) {
            customer.getKey().get();
            customer.getValue().stream().forEach(user -> resourcesManager.removeUser(getSharedAddressSpace(), user.getUsername()));
        }
    }

    @Test
    @Disabled("due to issue #1330")
    void testLargeMessages(JmsProvider jmsProvider) throws Exception {
        Address addressQueue = new AddressBuilder()
                .withNewMetadata()
                .withNamespace(getSharedAddressSpace().getMetadata().getNamespace())
                .withName(AddressUtils.generateAddressMetadataName(getSharedAddressSpace(), "jms-queue"))
                .endMetadata()
                .withNewSpec()
                .withType("queue")
                .withAddress("jmsQueue")
                .withPlan(DestinationPlan.STANDARD_LARGE_QUEUE)
                .endSpec()
                .build();
        resourcesManager.setAddresses(addressQueue);

        connection = jmsProvider.createConnection(getMessagingRoute(getSharedAddressSpace()).toString(), defaultCredentials,
                "jmsCliId", addressQueue);
        connection.start();

        sendReceiveLargeMessage(jmsProvider, 50, addressQueue, 1);
        sendReceiveLargeMessage(jmsProvider, 10, addressQueue, 1);
        sendReceiveLargeMessage(jmsProvider, 1, addressQueue, 1);
        sendReceiveLargeMessage(jmsProvider, 50, addressQueue, 1, DeliveryMode.PERSISTENT);
        sendReceiveLargeMessage(jmsProvider, 10, addressQueue, 1, DeliveryMode.PERSISTENT);
        sendReceiveLargeMessage(jmsProvider, 1, addressQueue, 1, DeliveryMode.PERSISTENT);
    }
}

