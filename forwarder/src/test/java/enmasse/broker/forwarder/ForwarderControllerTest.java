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

package enmasse.broker.forwarder;

import enmasse.discovery.Host;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.proton.ProtonClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ForwarderControllerTest {
    private static final Logger log = LoggerFactory.getLogger(ForwarderControllerTest.class.getName());
    private Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(10));
    private String localHost = "127.0.0.1";
    private final String address = "mytopic";
    ProtonClient client = ProtonClient.create(vertx);
    private TestBroker serverA = new TestBroker(client, localHost, 5672, address);
    private TestBroker serverB = new TestBroker(client, localHost, 5673, address);
    private TestBroker serverC = new TestBroker(client, localHost, 5674, address);

    @Before
    public void setup() throws Exception {
        serverA.start();
        serverB.start();
        serverC.start();
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testBrokerReplicator() throws InterruptedException, TimeoutException, ExecutionException {
        Host hostA = new Host(localHost, Collections.singletonMap("amqp", 5672));
        Host hostB = new Host(localHost, Collections.singletonMap("amqp", 5673));
        Host hostC = new Host(localHost, Collections.singletonMap("amqp", 5674));

        ForwarderController replicator = new ForwarderController(hostA, address);

        Set<Host> hosts = new LinkedHashSet<>();
        hosts.add(hostB);
        replicator.hostsChanged(hosts);
        Thread.sleep(5000);
        hosts.add(hostC);
        replicator.hostsChanged(hosts);

        long timeout = 60_000;
        waitForConnections(serverA, 2, timeout);
        waitForConnections(serverB, 1, timeout);
        waitForConnections(serverB, 1, timeout);

        CompletableFuture<List<String>> resultB = serverB.recvMessages(2, 60, TimeUnit.SECONDS);
        CompletableFuture<List<String>> resultC = serverC.recvMessages(2, 60, TimeUnit.SECONDS);

        serverA.sendMessage("Hello 1", 60, TimeUnit.SECONDS);
        serverA.sendMessage("Hello 2", 60, TimeUnit.SECONDS);

        assertMessages(resultB.get(120, TimeUnit.SECONDS), "Hello 1", "Hello 2");
        assertMessages(resultC.get(120, TimeUnit.SECONDS), "Hello 1", "Hello 2");
        vertx.close();
    }

    private void assertMessages(List<String> result, String...messages) {
        assertThat(messages.length, is(2));
        for (String message : messages) {
            assertThat(result, hasItem(message));
        }
    }

    private static void waitForConnections(TestBroker server, int num, long timeout) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < endTime) {
            log.info("Num connected is : " + server.numConnected());
            if (server.numConnected() >= num) {
                break;
            }
            Thread.sleep(1000);
        }
        assertTrue(server.numConnected() >= num);
    }
}
