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

package enmasse.smoketest;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class QueueTest extends VertxTestBase {

    @Test
    public void testQueue() throws Exception {
        String dest = "myqueue";
        waitForAddress(dest, 5, TimeUnit.MINUTES);
        waitUntilReady(dest, 5, TimeUnit.MINUTES);
        EnMasseClient client = createQueueClient();
        List<String> msgs = Arrays.asList("foo", "bar", "baz");

        Future<Integer> numSent = client.sendMessages(dest, msgs);
        assertThat(numSent.get(1, TimeUnit.MINUTES), is(msgs.size()));

        Future<List<String>> received = client.recvMessages(dest, msgs.size());
        assertThat(received.get(1, TimeUnit.MINUTES).size(), is(msgs.size()));
    }

    public void testScaledown() throws Exception {
        String dest = "myqueue";
        TestUtils.setReplicas(dest, dest, 3, 5, TimeUnit.MINUTES);
        waitForAddress(dest, 5, TimeUnit.MINUTES);
        waitUntilReady(dest, 5, TimeUnit.MINUTES);
        EnMasseClient client = createQueueClient();
        List<Future<Integer>> sent = Arrays.asList(
                client.sendMessages(dest, Arrays.asList("foo")),
                client.sendMessages(dest, Arrays.asList("bar")),
                client.sendMessages(dest, Arrays.asList("baz")),
                client.sendMessages(dest, Arrays.asList("quux")));

        assertThat(sent.get(0).get(1, TimeUnit.MINUTES), is(1));
        assertThat(sent.get(1).get(1, TimeUnit.MINUTES), is(1));
        assertThat(sent.get(2).get(1, TimeUnit.MINUTES), is(1));
        assertThat(sent.get(3).get(1, TimeUnit.MINUTES), is(1));

        TestUtils.setReplicas(dest, dest, 1, 5, TimeUnit.MINUTES);
        waitUntilReady(dest, 5, TimeUnit.MINUTES);

        Future<List<String>> received = client.recvMessages("myqueue", 4);

        assertThat(received.get(1, TimeUnit.MINUTES).size(), is(4));
    }
}

