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

public class QueueTest extends MarathonTestBase {

    @Test
    public void testQueueSendReceiveLong() throws Exception {
        AddressSpace addressSpace = new AddressSpace("test-queue-sendreceive-brokered",
                "test-queue-sendreceive-brokered",
                AddressSpaceType.BROKERED);
        createAddressSpace(addressSpace, "none");

        int msgCount = 1000;
        int queueCount = 10;
        int senderCount = 10;
        int recvCount = 20;

        List<Destination> queueList = new ArrayList<>();

        //create queues
        for (int i = 0; i < queueCount; i++) {
            queueList.add(Destination.queue(String.format("test-queue-sendreceive-%d", i), getDefaultPlan(AddressType.QUEUE)));
        }
        setAddresses(addressSpace, queueList.toArray(new Destination[0]));

        List<String> msgBatch = TestUtils.generateMessages(msgCount);

        runTestInLoop(30, () -> {
            //create client
            AmqpClient client = amqpClientFactory.createQueueClient(addressSpace);
            client.getConnectOptions().setUsername("test").setPassword("test");
            clients.add(client);

            //attach receivers
            List<Future<List<Message>>> recvResults = new ArrayList<>();
            for (int i = 0; i < recvCount / 2; i++) {
                recvResults.add(client.recvMessages(queueList.get(i).getAddress(), msgCount / 2));
                recvResults.add(client.recvMessages(queueList.get(i).getAddress(), msgCount / 2));
            }

            //attach senders
            for (int i = 0; i < senderCount; i++) {
                collector.checkThat(client.sendMessages(queueList.get(i).getAddress(), msgBatch,
                        1, TimeUnit.MINUTES).get(1, TimeUnit.MINUTES), is(msgBatch.size()));
            }

            //check received messages
            for (int i = 0; i < recvCount; i++) {
                collector.checkThat(recvResults.get(i).get().size(), is(msgCount / 2));
            }
            Thread.sleep(5000);
        });
    }
}
