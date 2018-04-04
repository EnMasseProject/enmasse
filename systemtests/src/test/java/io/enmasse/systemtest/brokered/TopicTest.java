/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.brokered;

import io.enmasse.systemtest.AddressType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.TestUtils;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.bases.BrokeredTestBase;
import org.apache.qpid.proton.message.Message;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TopicTest extends BrokeredTestBase {

    //disable due to authorization exception with create queue on topic address with wildcards
    //@Test
    public void testTopicPubSubWildcards() throws Exception {

        int msgCount = 1000;
        int topicCount = 10;
        int senderCount = topicCount;
        int recvCount = topicCount / 2;

        List<Destination> topicList = new ArrayList<>();

        //create queues
        for (int i = 0; i < recvCount; i++) {
            topicList.add(Destination.topic(String.format("test-topic-pubsub%d.%d", i, i + 1), getDefaultPlan(AddressType.TOPIC)));
            topicList.add(Destination.topic(String.format("test-topic-pubsub%d.%d", i, i + 2), getDefaultPlan(AddressType.TOPIC)));
        }
        setAddresses(topicList.toArray(new Destination[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        AmqpClient client = amqpClientFactory.createTopicClient(sharedAddressSpace);
        client.getConnectOptions().setUsername("test").setPassword("test");

        //attach subscibers
        List<Future<List<Message>>> recvResults = new ArrayList<>();
        for (int i = 0; i < recvCount; i++) {
            recvResults.add(client.recvMessages(String.format("test-topic-pubsub%d.*", i), msgCount * 2));
        }

        //attach producers
        for (int i = 0; i < senderCount; i++) {
            assertThat("Wrong count of messages sent: sender" + i,
                    client.sendMessages(topicList.get(i).getAddress(), msgBatch,
                            1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES), is(msgBatch.size()));
        }

        //check received messages
        for (int i = 0; i < recvCount; i++) {
            assertThat("Wrong count of messages received: receiver" + i,
                    recvResults.get(i).get().size(), is(msgCount * 2));
        }

        client.close();
    }

    @Test
    public void testRestApi() throws Exception {
        Destination t1 = Destination.topic("topic1", getDefaultPlan(AddressType.TOPIC));
        Destination t2 = Destination.topic("topic2", getDefaultPlan(AddressType.TOPIC));

        runRestApiTest(t1, t2);
    }
}
