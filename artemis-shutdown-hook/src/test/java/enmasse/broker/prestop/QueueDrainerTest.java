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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    }

    @After
    public void teardown() throws Exception {
        fromServer.stop();
        toServer.stop();
    }

    @Test
    public void testDrain() throws Exception {
        System.out.println("Sending message");
        fromServer.sendMessage("Hello drainer");
        System.out.println("Starting drain");
        client.drainMessages(to, "myqueue");
        System.out.println("Receiving message");
        String msg = toServer.recvMessage();
        System.out.println("Checking message");
        assertThat(msg, is("Hello drainer"));
        System.out.println("Checking shutdown");
        fromServer.assertShutdown(1, TimeUnit.MINUTES);
        System.out.println("DONE");
    }

}
