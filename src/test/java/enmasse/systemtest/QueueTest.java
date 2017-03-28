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
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class QueueTest extends AmqpTestBase {
    @Test
    public void testQueue() throws Exception {
        Destination dest = Destination.queue("myqueue");
        deploy(dest);
        AmqpClient client = createQueueClient();

        runQueueTest(client, dest);
    }

    @Test
    public void testColocatedQueues() throws Exception {
        Destination q1 = Destination.queue("group1", "queue1");
        Destination q2 = Destination.queue("group1", "queue2");
        Destination q3 = Destination.queue("group2", "queue3");
        deploy(q1, q2, q3);

        AmqpClient client = createQueueClient();
        runQueueTest(client, q1);
        runQueueTest(client, q2);
        runQueueTest(client, q3);
    }

    @Test
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

        scale(dest, 1);

        Future<List<String>> received = client.recvMessages(dest.getAddress(), 4000);

        assertThat(received.get(1, TimeUnit.MINUTES).size(), is(4000));
    }

    private static void runQueueTest(AmqpClient client, Destination dest) throws InterruptedException, TimeoutException, ExecutionException, IOException {
        List<String> msgs = TestUtils.generateMessages(1024);

        Future<Integer> numSent = null;
        TimeoutBudget timeoutBudget = new TimeoutBudget(1, TimeUnit.MINUTES);
        while (timeoutBudget.timeLeft() >= 0) {
            numSent = client.sendMessages(dest.getAddress(), msgs);
            try {
                if (numSent.get(timeoutBudget.timeLeft(), TimeUnit.MILLISECONDS) == msgs.size()) {
                    break;
                }
            } catch (Exception e) {
                Thread.sleep(2000);
            }
        }
        assertNotNull(numSent);
        assertThat(numSent.get(1, TimeUnit.SECONDS), is(msgs.size()));

        Future<List<String>> received = client.recvMessages(dest.getAddress(), msgs.size());
        assertThat(received.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));

    }

    @Override
    protected String getInstanceName() {
        return this.getClass().getSimpleName();
    }
}

