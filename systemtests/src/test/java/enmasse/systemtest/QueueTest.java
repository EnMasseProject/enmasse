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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class QueueTest extends AmqpTestBase {
    @Test
    public void testQueue() throws Exception {
        Destination dest = Destination.queue("myqueue");
        deploy(dest);
        AmqpClient client = createQueueClient();

        runQueueTest(client, dest);
    }

    public void testColocatedQueues() throws Exception {
        Destination q1 = Destination.queue("queue1", Optional.of("pooled-inmemory"));
        Destination q2 = Destination.queue("queue2", Optional.of("pooled-inmemory"));
        Destination q3 = Destination.queue("queue3", Optional.of("pooled-inmemory"));
        deploy(q1, q2, q3);

        AmqpClient client = createQueueClient();
        runQueueTest(client, q1);
        runQueueTest(client, q2);
        runQueueTest(client, q3);
    }

    public void testScaledown() throws Exception {
        Destination dest = Destination.queue("scalequeue");
        deploy(dest);
        scale(dest, 4);
        AmqpClient client = createQueueClient();
        List<Future<Integer>> sent = Arrays.asList(
                client.sendMessages(dest.getAddress(), TestUtils.generateMessages("foo", 1000)),
                client.sendMessages(dest.getAddress(), TestUtils.generateMessages("bar", 1000)),
                client.sendMessages(dest.getAddress(), TestUtils.generateMessages("baz", 1000)),
                client.sendMessages(dest.getAddress(), TestUtils.generateMessages("quux", 1000)));

        assertThat(sent.get(0).get(1, TimeUnit.MINUTES), is(1000));
        assertThat(sent.get(1).get(1, TimeUnit.MINUTES), is(1000));
        assertThat(sent.get(2).get(1, TimeUnit.MINUTES), is(1000));
        assertThat(sent.get(3).get(1, TimeUnit.MINUTES), is(1000));

        Future<List<String>> received = client.recvMessages(dest.getAddress(), 500);
        assertThat(received.get(1, TimeUnit.MINUTES).size(), is(500));

        scale(dest, 1);

        received = client.recvMessages(dest.getAddress(), 3500);

        assertThat(received.get(1, TimeUnit.MINUTES).size(), is(3500));
    }

    private static void runQueueTest(AmqpClient client, Destination dest) throws InterruptedException, TimeoutException, ExecutionException, IOException {
        List<String> msgs = TestUtils.generateMessages(1024);
        Count<Message> predicate = new Count<>(msgs.size());
        Future<Integer> numSent = client.sendMessages(dest.getAddress(), msgs, predicate);

        assertNotNull(numSent);
        int actual = 0;
        try {
            actual = numSent.get(1, TimeUnit.MINUTES);
        } catch (TimeoutException t) {
            fail("Sending messages timed out after sending " + predicate.actual());
        }
        assertThat(actual, is(msgs.size()));

        predicate = new Count<>(msgs.size());
        Future<List<String>> received = client.recvMessages(dest.getAddress(), predicate);
        actual = 0;
        try {
            actual = received.get(1, TimeUnit.MINUTES).size();
        } catch (TimeoutException t) {
            fail("Receiving messages timed out after " + predicate.actual() + " msgs received");
        }

        assertThat(actual, is(msgs.size()));

    }

    @Override
    protected String getInstanceName() {
        return this.getClass().getSimpleName();
    }

}

