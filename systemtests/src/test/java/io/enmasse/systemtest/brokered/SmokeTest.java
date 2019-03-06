/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered;

import io.enmasse.address.model.Address;
import io.enmasse.systemtest.*;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.utils.AddressUtils;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.utils.TestUtils;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.enmasse.systemtest.TestTag.nonPR;
import static io.enmasse.systemtest.TestTag.smoke;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag(nonPR)
@Tag(smoke)
class SmokeTest extends TestBaseWithShared implements ITestBaseBrokered {

    /**
     * related github issue: #335
     */
    @Test
    void testAddressTypes() throws Exception {
        Address queueA = AddressUtils.createQueue("brokeredQueueA", getDefaultPlan(AddressType.QUEUE));
        setAddresses(queueA);

        AmqpClient amqpQueueCli = amqpClientFactory.createQueueClient(sharedAddressSpace);
        QueueTest.runQueueTest(amqpQueueCli, queueA);
        amqpQueueCli.close();

        Address topicB = AddressUtils.createTopic("brokeredTopicB", getDefaultPlan(AddressType.TOPIC));
        setAddresses(topicB);

        AmqpClient amqpTopicCli = amqpClientFactory.createTopicClient(sharedAddressSpace);
        List<Future<List<Message>>> recvResults = Arrays.asList(
                amqpTopicCli.recvMessages(topicB.getSpec().getAddress(), 1000),
                amqpTopicCli.recvMessages(topicB.getSpec().getAddress(), 1000));

        List<String> msgsBatch = TestUtils.generateMessages(600);
        List<String> msgsBatch2 = TestUtils.generateMessages(400);

        assertAll("All senders should send all messages",
                () -> assertThat("Wrong count of messages sent: batch1",
                        amqpTopicCli.sendMessages(topicB.getSpec().getAddress(), msgsBatch).get(1, TimeUnit.MINUTES), is(msgsBatch.size())),
                () -> assertThat("Wrong count of messages sent: batch2",
                        amqpTopicCli.sendMessages(topicB.getSpec().getAddress(), msgsBatch2).get(1, TimeUnit.MINUTES), is(msgsBatch2.size())));

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
    void testCreateDeleteAddressSpace() throws Exception {
        AddressSpace addressSpaceA = new AddressSpace("brokered-create-delete-a", AddressSpaceType.BROKERED,
                AuthService.STANDARD);

        AddressSpace addressSpaceC = new AddressSpace("brokered-create-delete-c", AddressSpaceType.BROKERED,
                AuthService.STANDARD);
        createAddressSpaceList(addressSpaceA, addressSpaceC);

        Address queueB = AddressUtils.createQueue("brokeredQueueB", getDefaultPlan(AddressType.QUEUE));
        setAddresses(addressSpaceA, queueB);
        UserCredentials user = new UserCredentials("test", "test");
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

    @Test
    void testCreateAlreadyExistingAddress() throws Exception {
        String addr_name = "brokeredAddrA";
        Address queueA = AddressUtils.createQueue(addr_name, getDefaultPlan(AddressType.QUEUE));
        setAddresses(queueA);

        Address topicA = AddressUtils.createTopic(addr_name, getDefaultPlan(AddressType.TOPIC));
        assertThrows(AddressAlreadyExistsException.class, () -> setAddresses(HTTP_CONFLICT, topicA),
                "setAddresses does not throw right exception");
    }
}
