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

package io.enmasse.queue.scheduler;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ArtemisTest {

    private Vertx vertx;
    private TestArtemis server;

    @Before
    public void setup() throws Exception {
        vertx = Vertx.vertx();
        server = new TestArtemis("localhost", 12346);
        server.start();
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testManagement() throws InterruptedException, ExecutionException, TimeoutException {
        ProtonClient client = ProtonClient.create(vertx);
        CompletableFuture<Future<Broker>> promise = new CompletableFuture<>();
        client.connect("localhost", 12346, h -> {
            assertTrue(h.succeeded());
            promise.complete(Artemis.create(vertx, h.result().open()));
        });

        Artemis artemis = (Artemis)promise.get().get(30, TimeUnit.SECONDS);

        artemis.deployQueue("queue1");
        artemis.deployQueue("queue2");

        long numQueues = artemis.getNumQueues();
        long endTime = System.currentTimeMillis() + 60_000;
        while (numQueues != 2L && System.currentTimeMillis() < endTime) {
            Thread.sleep(2000);
            numQueues = artemis.getNumQueues();
        }
        assertThat(numQueues, is(2L));
    }
}
