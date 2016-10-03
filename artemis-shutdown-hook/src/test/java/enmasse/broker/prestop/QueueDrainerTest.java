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

import enmasse.discovery.Endpoint;
import enmasse.discovery.Host;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class QueueDrainerTest {
    private QueueDrainer client;
    private Endpoint from = new Endpoint("127.0.0.1", 12345);
    private Endpoint to = new Endpoint("127.0.0.1", 12346);
    private TestBroker fromServer;
    private TestBroker toServer;

    @Before
    public void setup() throws Exception {
        fromServer = new TestBroker(from.hostname(), from.port(), "myqueue", false);
        toServer = new TestBroker(to.hostname(), to.port(), "myqueue", false);
        Map<String, Integer> portMap = new LinkedHashMap<>();
        portMap.put("amqp", from.port());
        portMap.put("core", from.port());
        Host fromHost = new Host(from.hostname(), portMap);
        fromServer.start();
        toServer.start();
        client = new QueueDrainer(fromHost, Optional.empty());
        Thread.sleep(5000);
    }

    @Test
    public void testDrain() throws InterruptedException, IOException {
        fromServer.sendMessage("Hello drainer");
        client.drainMessages(to, "myqueue");
        String msg = toServer.recvMessage();
        assertThat(msg, is("Hello drainer"));
        assertShutdown(fromServer, 60, TimeUnit.SECONDS);
    }

    private void assertShutdown(TestBroker server, long timeout, TimeUnit timeUnit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (server.isActive() && System.currentTimeMillis() < endTime) {
            Thread.sleep(1000);
        }
        assertFalse("Server has not been shut down", server.isActive());
    }
}
