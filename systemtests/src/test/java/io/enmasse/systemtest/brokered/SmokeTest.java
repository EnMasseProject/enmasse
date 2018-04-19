/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.standard.QueueTest;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SmokeTest extends TestBaseWithShared implements ITestBaseBrokered {

    /**
     * related github issue: #335
     */
    @Test
    public void testAddressTypes() throws Exception {
        Destination queueA = Destination.queue("brokeredQueueA", getDefaultPlan(AddressType.QUEUE));
        setAddresses(queueA);

        AmqpClient amqpQueueCli = amqpClientFactory.createQueueClient(sharedAddressSpace);
        QueueTest.runQueueTest(amqpQueueCli, queueA);
        amqpQueueCli.close();

        Destination topicB = Destination.topic("brokeredTopicB", getDefaultPlan(AddressType.TOPIC));
        setAddresses(topicB);

        AmqpClient amqpTopicCli = amqpClientFactory.createTopicClient(sharedAddressSpace);
        List<Future<List<Message>>> recvResults = Arrays.asList(
                amqpTopicCli.recvMessages(topicB.getAddress(), 1000),
                amqpTopicCli.recvMessages(topicB.getAddress(), 1000));

        List<String> msgsBatch = TestUtils.generateMessages(600);
        List<String> msgsBatch2 = TestUtils.generateMessages(400);

        assertAll("All senders should send all messages",
                () -> assertThat("Wrong count of messages sent: batch1",
                        amqpTopicCli.sendMessages(topicB.getAddress(), msgsBatch).get(1, TimeUnit.MINUTES), is(msgsBatch.size())),
                () -> assertThat("Wrong count of messages sent: batch2",
                        amqpTopicCli.sendMessages(topicB.getAddress(), msgsBatch2).get(1, TimeUnit.MINUTES), is(msgsBatch2.size())));

        assertAll("All receivers should receive all messages",
                () -> assertThat("Wrong count of messages received",
                        recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgsBatch.size() + msgsBatch2.size())),
                () -> assertThat("Wrong count of messages received",
                        recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgsBatch.size() + msgsBatch2.size())));
    }

    /**
     * related github issue: #334
     */
    @Test
    public void testCreateDeleteAddressSpace() throws Exception {
        AddressSpace addressSpaceA = new AddressSpace("brokered-create-delete-a", AddressSpaceType.BROKERED,
                AuthService.STANDARD);

        AddressSpace addressSpaceC = new AddressSpace("brokered-create-delete-c", AddressSpaceType.BROKERED,
                AuthService.STANDARD);
        createAddressSpaceList(addressSpaceA, addressSpaceC);

        Destination queueB = Destination.queue("brokeredQueueB", getDefaultPlan(AddressType.QUEUE));
        setAddresses(addressSpaceA, queueB);
        KeycloakCredentials user = new KeycloakCredentials("test", "test");
        createUser(addressSpaceA, user);

        AmqpClient amqpQueueCliA = amqpClientFactory.createQueueClient(addressSpaceA);
        amqpQueueCliA.getConnectOptions().setCredentials(user);
        QueueTest.runQueueTest(amqpQueueCliA, queueB);
        amqpQueueCliA.close();

        setAddresses(addressSpaceC, queueB);
        createUser(addressSpaceC, user);

        AmqpClient amqpQueueCliC = amqpClientFactory.createQueueClient(addressSpaceC);
        amqpQueueCliC.getConnectOptions().setCredentials(user);
        QueueTest.runQueueTest(amqpQueueCliC, queueB);
        amqpQueueCliC.close();

        deleteAddressSpace(addressSpaceA);

        QueueTest.runQueueTest(amqpQueueCliC, queueB);
    }

    @Test()
    @Disabled("disabled until #346 will be fixed")
    public void testCreateAlreadyExistingAddress() throws Exception {
        AddressSpace addressSpaceA = new AddressSpace("brokered-a", AddressSpaceType.BROKERED, AuthService.STANDARD);
        createAddressSpace(addressSpaceA);
        Destination queueA = Destination.queue("brokeredQueueA", getDefaultPlan(AddressType.QUEUE));
        setAddresses(addressSpaceA, queueA);

        Destination topicA = Destination.topic("brokeredTopicA", getDefaultPlan(AddressType.TOPIC));
        assertThrows(AddressAlreadyExistsException.class, () -> setAddresses(addressSpaceA, topicA),
                "setAddresses does not throw right exception");
    }
}
