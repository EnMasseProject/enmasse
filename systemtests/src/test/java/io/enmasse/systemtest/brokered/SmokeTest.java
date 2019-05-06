/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.systemtest.AddressSpacePlans;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.UserCredentials;
import io.enmasse.systemtest.ability.ITestBaseBrokered;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.TestBaseWithShared;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.utils.AddressUtils;
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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Tag(nonPR)
@Tag(smoke)
class SmokeTest extends TestBaseWithShared implements ITestBaseBrokered {

    @Test
    void testAddressTypes() throws Exception {
        Address queueA = AddressUtils.createQueueAddressObject("brokeredQueueA", getDefaultPlan(AddressType.QUEUE));
        setAddresses(queueA);

        AmqpClient amqpQueueCli = amqpClientFactory.createQueueClient(sharedAddressSpace);
        QueueTest.runQueueTest(amqpQueueCli, queueA);
        amqpQueueCli.close();

        Address topicB = AddressUtils.createTopicAddressObject("brokeredTopicB", getDefaultPlan(AddressType.TOPIC));
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

    @Test
    void testCreateDeleteAddressSpace() throws Exception {
        AddressSpace addressSpaceA = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("add-space-a")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();

        AddressSpace addressSpaceC = new AddressSpaceBuilder()
                .withNewMetadata()
                .withName("addr-space-c")
                .withNamespace(kubernetes.getInfraNamespace())
                .endMetadata()
                .withNewSpec()
                .withType(AddressSpaceType.BROKERED.toString())
                .withPlan(AddressSpacePlans.BROKERED)
                .withNewAuthenticationService()
                .withName("standard-authservice")
                .endAuthenticationService()
                .endSpec()
                .build();
        createAddressSpaceList(addressSpaceA, addressSpaceC);

        Address queueB = AddressUtils.createQueueAddressObject("brokeredQueueB", getDefaultPlan(AddressType.QUEUE));
        setAddresses(addressSpaceA, queueB);
        UserCredentials user = new UserCredentials("test", "test");
        createOrUpdateUser(addressSpaceA, user);

        AmqpClient amqpQueueCliA = amqpClientFactory.createQueueClient(addressSpaceA);
        amqpQueueCliA.getConnectOptions().setCredentials(user);
        QueueTest.runQueueTest(amqpQueueCliA, queueB);
        amqpQueueCliA.close();

        setAddresses(addressSpaceC, queueB);
        createOrUpdateUser(addressSpaceC, user);

        AmqpClient amqpQueueCliC = amqpClientFactory.createQueueClient(addressSpaceC);
        amqpQueueCliC.getConnectOptions().setCredentials(user);
        QueueTest.runQueueTest(amqpQueueCliC, queueB);
        amqpQueueCliC.close();

        deleteAddressSpace(addressSpaceA);

        QueueTest.runQueueTest(amqpQueueCliC, queueB);
    }
}
