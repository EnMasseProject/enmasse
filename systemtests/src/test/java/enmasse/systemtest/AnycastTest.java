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
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AnycastTest extends TestBase {

    @Test
    public void testMessagesDeliveredToReceiver() throws Exception {
        Destination dest = Destination.anycast("anycast");
        setAddresses(dest);
        AmqpClient client = amqpClientFactory.createQueueClient();

        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        Future<List<Message>> recvResult = client.recvMessages(dest.getAddress(), msgs.size());
        Future<Integer> sendResult = client.sendMessages(dest.getAddress(), msgs);

        assertThat(sendResult.get(1, TimeUnit.MINUTES), is(msgs.size()));
        assertThat(recvResult.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    @Test
    public void testMultipleReceivers() throws Exception {
        Destination dest = Destination.anycast("anycastMultipleReceivers");
        setAddresses(dest);
        AmqpClient client1 = amqpClientFactory.createQueueClient();
        AmqpClient client2 = amqpClientFactory.createQueueClient();
        AmqpClient client3 = amqpClientFactory.createQueueClient();

        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        Future<List<Message>> recvResult1 = client1.recvMessages(dest.getAddress(), 1);
        Future<List<Message>> recvResult2 = client2.recvMessages(dest.getAddress(), 1);
        Future<List<Message>> recvResult3 = client3.recvMessages(dest.getAddress(), 1);
        Future<Integer> sendResult = client1.sendMessages(dest.getAddress(), msgs);

        assertThat(sendResult.get(1, TimeUnit.MINUTES), is(msgs.size()));

        assertThat(recvResult1.get(1, TimeUnit.MINUTES).size(), is(1));
        assertThat(recvResult2.get(1, TimeUnit.MINUTES).size(), is(1));
        assertThat(recvResult3.get(1, TimeUnit.MINUTES).size(), is(1));
    }
}
