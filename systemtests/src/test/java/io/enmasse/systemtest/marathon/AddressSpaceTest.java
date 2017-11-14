package io.enmasse.systemtest.marathon;

import io.enmasse.systemtest.AddressSpace;
import io.enmasse.systemtest.AddressSpaceType;
import io.enmasse.systemtest.Destination;
import io.enmasse.systemtest.Logging;
import io.enmasse.systemtest.amqp.AmqpClient;
import io.enmasse.systemtest.amqp.AmqpClientFactory;
import io.enmasse.systemtest.standard.QueueTest;
import io.enmasse.systemtest.standard.TopicTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AddressSpaceTest extends MarathonTestBase {

    @Test
    public void testCreateDeleteAddressSpaceLong() throws Exception{

        runTestInLoop(60, () -> {
            AddressSpace addressSpace = new AddressSpace("test-create-delete-addrspace-brokered",
                    "test-create-delete-addrspace-brokered",
                    AddressSpaceType.BROKERED);
            createAddressSpace(addressSpace, "none");
            Logging.log.info("Address space created");

            doAddressTest(addressSpace, "test-topic-createdelete-brokered-%d",
                    "test-queue-createdelete-brokered-%d");

            deleteAddressSpace(addressSpace);
            Logging.log.info("Address space removed");
            Thread.sleep(10000);
        });
    }

    @Test
    public void testCreateDeleteAddressesLong() throws Exception{
        AddressSpace addressSpace = new AddressSpace("test-create-delete-addresses-brokered",
                "test-create-delete-addresses-brokered",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");

        runTestInLoop(30, () -> {
            doAddressTest(addressSpace, "test-topic-createdelete-brokered-%d",
                    "test-queue-createdelete-brokered-%d");
            Thread.sleep(10000);
        });
    }

    void doAddressTest(AddressSpace addressSpace, String topicPattern,
                       String queuePattern, String username, String password) throws Exception{
        List<Destination> queueList = new ArrayList<>();
        List<Destination> topicList = new ArrayList<>();

        int destinationCount = 20;

        for(int i = 0; i < destinationCount; i++){
            queueList.add(Destination.queue(String.format(queuePattern, i)));
            topicList.add(Destination.topic(String.format(topicPattern, i)));
        }

        AmqpClientFactory amqpFactory = createAmqpClientFactory(addressSpace);
        AmqpClient queueClient;
        AmqpClient topicClient;

        setAddresses(addressSpace, queueList.toArray(new Destination[0]));
        for(Destination queue : queueList){
            queueClient = amqpFactory.createQueueClient(addressSpace);
            queueClient.getConnectOptions().setUsername("test").setPassword("test");

            QueueTest.runQueueTest(queueClient, queue, 1024);
        }

        setAddresses(addressSpace, topicList.toArray(new Destination[0]));

        for(Destination topic : topicList){
            topicClient = amqpFactory.createTopicClient(addressSpace);
            topicClient.getConnectOptions().setUsername(username).setPassword(password);

            TopicTest.runTopicTest(topicClient, topic, 1024);
        }

        deleteAddresses(addressSpace, queueList.toArray(new Destination[0]));
        deleteAddresses(addressSpace, topicList.toArray(new Destination[0]));
    }

    void doAddressTest(AddressSpace addressSpace, String topicPattern, String queuePattern) throws Exception{
        doAddressTest(addressSpace, topicPattern, queuePattern, "test", "test");
    }
}
