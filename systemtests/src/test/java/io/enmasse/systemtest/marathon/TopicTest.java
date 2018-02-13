/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;

public class TopicTest extends MarathonTestBase {

    @Test
    public void testTopicPubSubLong() throws Exception {
        AddressSpace addressSpace = new AddressSpace("test-topic-pubsub-brokered",
                "test-topic-pubsub-brokered",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");

        int msgCount = 1000;
        int topicCount = 10;
        int senderCount = topicCount;
        int recvCount = topicCount;

        List<Destination> topicList = new ArrayList<>();

        //create queues
        for (int i = 0; i < recvCount; i++) {
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
            for (int i = 0; i < recvCount; i++) {
                recvResults.add(client.recvMessages(String.format("test-topic-pubsub-%d", i), msgCount));
            }

            //attach producers
            for (int i = 0; i < senderCount; i++) {
                collector.checkThat(client.sendMessages(topicList.get(i).getAddress(), msgBatch,
                        1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES), is(msgBatch.size()));
            }

            //check received messages
            for (int i = 0; i < recvCount; i++) {
                collector.checkThat(recvResults.get(i).get().size(), is(msgCount));
            }
            Thread.sleep(5000);
        });
    }
}
