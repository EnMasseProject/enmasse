/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package enmasse.broker.prestop;

import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClientOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class QueueDrainerTest {
    private QueueDrainer client;
    private Host from = TestUtil.createHost("127.0.0.1", 11111);
    private Host to = TestUtil.createHost("127.0.0.1", 22222);
    private TestBroker fromServer;
    private TestBroker toServer;

    @Before
    public void setup() throws Exception {
        fromServer = new TestBroker(from.amqpEndpoint(), Arrays.asList("myqueue", "queue2"), false);
        toServer = new TestBroker(to.amqpEndpoint(), Arrays.asList("myqueue", "queue2"), false);
        fromServer.start();
        toServer.start();
        client = new QueueDrainer(Vertx.vertx(), from, new ArtemisBrokerFactory(20_000), new ProtonClientOptions(), Optional.empty());
    }

    @After
    public void teardown() throws Exception {
        fromServer.stop();
        toServer.stop();
    }

    @Test
    @Ignore
    public void testDrain() throws Exception {
        sendMessages(fromServer, "myqueue", "testfrom", 100);
        sendMessages(fromServer, "queue2", "q2from", 10);
        sendMessages(toServer, "myqueue","testto", 100);
        sendMessages(toServer, "queue2", "q2to", 1);

        System.out.println("Starting drain");
        client.drainMessages(to.amqpEndpoint(), "");
        assertThat(toServer.numMessages("myqueue"), is(200L));
        assertThat(toServer.numMessages("queue2"), is(11L));

        assertReceive(toServer, "myqueue", "testto", 100);
        assertReceive(toServer, "myqueue", "testfrom", 100);
        assertReceive(toServer, "queue2", "q2to", 1);
        assertReceive(toServer, "queue2", "q2from", 10);

        System.out.println("Checking shutdown");
        fromServer.assertShutdown(1, TimeUnit.MINUTES);
    }

    private static void sendMessages(TestBroker broker, String address, String prefix, int numMessages) throws IOException, InterruptedException {
        List<String> messages = IntStream.range(0, numMessages)
                .mapToObj(i -> prefix + i)
                .collect(Collectors.toList());
        broker.sendMessages(address, messages);
    }

    private static void assertReceive(TestBroker broker, String address, String prefix, int numMessages) throws IOException, InterruptedException {
        List<String> messages = broker.recvMessages(address, numMessages);
        for (int i = 0; i < numMessages; i++) {
            String actualBody = messages.get(i);
            String expectedBody = prefix + i;
            assertThat(actualBody, is(expectedBody));
        }
    }
}
