package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.TestUtils;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;

public class TopicTest extends MarathonTestBase {

    @Test
    public void testTopicPubSubLong() throws Exception{
        AddressSpace addressSpace = new AddressSpace("test-topic-pubsub-brokered",
                "test-topic-pubsub-brokered",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");

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
        setAddresses(addressSpace, topicList.toArray(new Destination[0]));

        AmqpClientFactory amqpFactory = createAmqpClientFactory(addressSpace);

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        runTestInLoop(30, () -> {
            //create client
            AmqpClient client = amqpFactory.createTopicClient(addressSpace);
            client.getConnectOptions().setUsername("test").setPassword("test");

            //attach subscibers
            List<Future<List<Message>>> recvResults = new ArrayList<>();
            for (int i = 0; i < recvCount; i++) {
                recvResults.add(client.recvMessages(String.format("test-topic-pubsub%d.*", i), msgCount * 2));
            }

            //attach producers
            for(int i = 0; i < senderCount; i++ ) {
                collector.checkThat(client.sendMessages(topicList.get(i).getAddress(), msgBatch,
                        10, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES), is(msgBatch.size()));
            }

            //check received messages
            for (int i = 0; i < recvCount; i++) {
                collector.checkThat(recvResults.get(i).get().size(), is(msgCount * 2));
            }

            client.close();
            Thread.sleep(2000);
        });
    }
}
