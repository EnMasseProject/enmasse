package io.enmasse.systemtest.brokered;

import io.enmasse.systemtest.*;
import io.enmasse.systemtest.amqp.AmqpClient;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;


import static org.hamcrest.CoreMatchers.is;

public class TopicTest extends BrokeredTestBase {

    //disable due to authorization exception with create queue on topic address with wildcards
    //@Test
    public void testTopicPubSubWildcards() throws Exception{

        int msgCount = 1000;
        int topicCount = 10;
        int senderCount = topicCount;
        int recvCount = topicCount / 2;

        List<Destination> topicList = new ArrayList<>();

        //create queues
        for(int i = 0; i < recvCount; i++){
            topicList.add(Destination.topic(String.format("test-topic-pubsub%d.%d", i, i + 1)));
            topicList.add(Destination.topic(String.format("test-topic-pubsub%d.%d", i, i + 2)));
        }
        setAddresses(defaultAddressSpace, topicList.toArray(new Destination[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        AmqpClient client = amqpClientFactory.createTopicClient(defaultAddressSpace);
        client.getConnectOptions().setUsername("test").setPassword("test");

        //attach subscibers
        List<Future<List<Message>>> recvResults = new ArrayList<>();
        for (int i = 0; i < recvCount; i++) {
            recvResults.add(client.recvMessages(String.format("test-topic-pubsub%d.*", i), msgCount * 2));
        }

        //attach producers
        for(int i = 0; i < senderCount; i++ ) {
            assertThat(client.sendMessages(topicList.get(i).getAddress(), msgBatch,
                    1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES), is(msgBatch.size()));
        }

        //check received messages
        for (int i = 0; i < recvCount; i++) {
            assertThat(recvResults.get(i).get().size(), is(msgCount * 2));
        }

        client.close();
    }
}
