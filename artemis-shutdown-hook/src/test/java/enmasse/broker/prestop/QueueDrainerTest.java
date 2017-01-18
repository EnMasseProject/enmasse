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

package enmasse.broker.prestop;

import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import org.apache.commons.lang.math.IntRange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class QueueDrainerTest {
    private QueueDrainer client;
    private Host from = TestUtil.createHost("127.0.0.1", 12345);
    private Host to = TestUtil.createHost("127.0.0.1", 12346);
    private TestBroker fromServer;
    private TestBroker toServer;

    @Before
    public void setup() throws Exception {
        fromServer = new TestBroker(from.amqpEndpoint(), "myqueue", false);
        toServer = new TestBroker(to.amqpEndpoint(), "myqueue", false);
        fromServer.start();
        toServer.start();
        client = new QueueDrainer(Vertx.vertx(), from, Optional.empty());
    }

    @After
    public void teardown() throws Exception {
        fromServer.stop();
        toServer.stop();
    }

    @Test
    public void testDrain() throws Exception {
        sendMessages(fromServer, "testfrom", 100);
        sendMessages(toServer, "testto", 100);
        System.out.println("Starting drain");
        client.drainMessages(to.amqpEndpoint(), Collections.singleton("myqueue"));
        assertThat(toServer.numMessages("myqueue"), is(200L));
        assertReceive(toServer, "testto", 100);
        assertReceive(toServer, "testfrom", 100);
        System.out.println("Checking shutdown");
        fromServer.assertShutdown(1, TimeUnit.MINUTES);
    }

    private static void sendMessages(TestBroker broker, String prefix, int numMessages) throws IOException, InterruptedException {
        List<String> messages = IntStream.range(0, numMessages)
                .mapToObj(i -> prefix + i)
                .collect(Collectors.toList());
        broker.sendMessages(messages);
    }

    private static void assertReceive(TestBroker broker, String prefix, int numMessages) throws IOException, InterruptedException {
        List<String> messages = broker.recvMessages(numMessages);
        for (int i = 0; i < numMessages; i++) {
            String actualBody = messages.get(i);
            String expectedBody = prefix + i;
            assertThat(actualBody, is(expectedBody));
        }
    }
}
