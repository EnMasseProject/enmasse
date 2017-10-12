/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.systemtest.brokered;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.standard.QueueTest;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ElementaryAddressSpaceTest extends MultiTenantTestBase {


    /**
     * related github issue: #335
     *
     * @throws Exception
     */
    @Test
    public void testCreateDeleteAddressSpace() throws Exception {
        String brokeredA = createAddressSpace("brokered-a", "none", "brokered");
        addressSpaces.add(brokeredA);
        Destination queueA = Destination.queue("brokeredQueueA");
        setAddresses(brokeredA, queueA);

        Destination topicA = Destination.topic("brokeredTopicA");
        try {
            setAddresses(brokeredA, topicA); //address already exist exception
        } catch (Exception ex) {
            assert ex instanceof AddressAlreadyExistsException;
        }

        AmqpClient amqpQueueCli = amqpClientFactory.createQueueClient(brokeredA);
        QueueTest.runQueueTest(amqpQueueCli, queueA);

        Destination topicB = Destination.topic("brokeredTopicB");
        setAddresses(brokeredA, topicB);

        AmqpClient amqpTopicCli = amqpClientFactory.createTopicClient(brokeredA);
        List<Future<List<Message>>> recvResults = Arrays.asList(
                amqpTopicCli.recvMessages(topicB.getAddress(), 1000),
                amqpTopicCli.recvMessages(topicB.getAddress(), 1000));

        List<String> msgsBatch = TestUtils.generateMessages(600);
        List<String> msgsBatch2 = TestUtils.generateMessages(400);

        assertThat(amqpTopicCli.sendMessages(topicB.getAddress(), msgsBatch).get(1, TimeUnit.MINUTES), is(msgsBatch.size()));
        assertThat(amqpTopicCli.sendMessages(topicB.getAddress(), msgsBatch2).get(1, TimeUnit.MINUTES), is(msgsBatch2.size()));

        assertThat(recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgsBatch.size()));
        assertThat(recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgsBatch.size()));

    }

    /**
     * related github issue: #334
     *
     * @throws Exception
     */
    @Test
    public void testAddressTypes() throws Exception {
        String brokeredA = createAddressSpace("brokered-a", "none", "brokered");
        addressSpaces.add(brokeredA);
        Destination queueB = Destination.queue("brokeredQueueB");
        setAddresses(brokeredA, queueB);
        AmqpClient amqpQueueCliA = amqpClientFactory.createQueueClient(brokeredA);
        QueueTest.runQueueTest(amqpQueueCliA, queueB);

        String brokeredC = createAddressSpace("brokered-c", "none", "brokered");
        addressSpaces.add(brokeredC);
        setAddresses(brokeredC, queueB);
        AmqpClient amqpQueueCliC = amqpClientFactory.createQueueClient(brokeredC);
        QueueTest.runQueueTest(amqpQueueCliC, queueB);

        deleteAddressSpace(brokeredA);

        QueueTest.runQueueTest(amqpQueueCliC, queueB);
    }
}
