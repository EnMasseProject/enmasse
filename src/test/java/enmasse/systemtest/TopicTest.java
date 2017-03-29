/*
 * Copyright 2016 Red Hat Inc.
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

package enmasse.systemtest;

import enmasse.systemtest.amqp.AmqpClient;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TopicTest extends AmqpTestBase {

    public void testMultipleSubscribers() throws Exception {
        Destination dest = Destination.topic("manytopic");
        deploy(dest);
        scale(dest, 4);
        AmqpClient client = createTopicClient();
        List<String> msgs = TestUtils.generateMessages(1000);

        List<Future<List<String>>> recvResults = Arrays.asList(
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()),
                client.recvMessages(dest.getAddress(), msgs.size()));

        Thread.sleep(30_000);

        assertThat(client.sendMessages(dest.getAddress(), msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertThat(recvResults.get(0).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(1).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
        assertThat(recvResults.get(2).get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    public void testDurableLinkRoutedSubscription() throws Exception {
        Destination dest = Destination.topic("lrtopic");
        String linkName = "systest-durable";
        deploy(dest);
        scale(dest, 4);

        Thread.sleep(60_000);

        Source source = new TopicTerminusFactory().getSource("locate/" + dest.getAddress());
        source.setDurable(TerminusDurability.UNSETTLED_STATE);

        AmqpClient client = createTopicClient();
        List<String> batch1 = Arrays.asList("one", "two", "three");

        System.out.println("Receiving first batch");
        Future<List<String>> recvResults = client.recvMessages(source, linkName, batch1.size());

        // Wait for the redirect to kick in
        Thread.sleep(30_000);

        System.out.println("Sending first batch");
        assertThat(client.sendMessages(dest.getAddress(), batch1).get(1, TimeUnit.MINUTES), is(batch1.size()));
        assertThat(recvResults.get(1, TimeUnit.MINUTES), is(batch1));

        System.out.println("Sending second batch");
        List<String> batch2 = Arrays.asList("four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve");
        assertThat(client.sendMessages(dest.getAddress(), batch2).get(1, TimeUnit.MINUTES), is(batch2.size()));

        System.out.println("Done, waiting for 20 seconds");
        Thread.sleep(20_000);

        source.setAddress("locate/" + dest.getAddress());
        //at present may get one or more of the first three messages
        //redelivered due to DISPATCH-595, so use more lenient checks
        System.out.println("Receiving second batch again");
        recvResults = client.recvMessages(source, linkName, message -> {
                String body = (String) ((AmqpValue) message.getBody()).getValue();
                System.out.println("received " + body);
                return "twelve".equals(body);
            });
        assertTrue(recvResults.get(1, TimeUnit.MINUTES).containsAll(batch2));
    }

    @Test
    public void testDurableMessageRoutedSubscription() throws Exception {
        Destination dest = Destination.topic("mrtopic");
        String address = "myaddress";
        System.out.println("Deploying");
        deploy(dest);
        System.out.println("Scaling");
        scale(dest, 4);

        Thread.sleep(60_000);

        AmqpClient subClient = createQueueClient();
        AmqpClient queueClient = createQueueClient();
        AmqpClient topicClient = createTopicClient();

        Message sub = Message.Factory.create();
        sub.setAddress("$subctrl");
        sub.setCorrelationId(address);
        sub.setSubject("subscribe");
        sub.setBody(new AmqpValue(dest.getAddress()));

        System.out.println("Sending subscribe");
        subClient.sendMessages("$subctrl", sub).get(1, TimeUnit.MINUTES);

        System.out.println("Sending 122 messages");

        List<String> msgs = TestUtils.generateMessages(122);
        assertThat(topicClient.sendMessages(dest.getAddress(),msgs).get(1, TimeUnit.MINUTES), is(msgs.size()));

        System.out.println("Receiving 61 messages");
        Future<List<String>> recvResult = queueClient.recvMessages(address, 61);
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(61));

        // Do scaledown and 'reconnect' receiver and verify that we got everything

        /*
        System.out.println("Scale down brokers");
        scale(dest, 3);
        Thread.sleep(5_000);
        scale(dest, 2);
        Thread.sleep(5_000);
        scale(dest, 1);

        Thread.sleep(30_000);
        */

        System.out.println("Receiving another 61 messages");
        recvResult = queueClient.recvMessages(address, 61);
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(61));

        Message unsub = Message.Factory.create();
        unsub.setAddress("$subctrl");
        unsub.setCorrelationId(address);
        sub.setBody(new AmqpValue(dest.getAddress()));
        unsub.setSubject("unsubscribe");
        System.out.println("Sending unsubscribe");
        subClient.sendMessages("$subctrl", unsub).get(1, TimeUnit.MINUTES);
    }

    @Override
    protected String getInstanceName() {
        return this.getClass().getSimpleName();
    }
}
