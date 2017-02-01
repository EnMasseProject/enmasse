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

package enmasse.queue.scheduler;

import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static enmasse.queue.scheduler.TestUtils.waitForPort;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class QueueSchedulerTest {

    private Vertx vertx;
    private ExecutorService executorService;
    private TestBrokerFactory brokerFactory;
    private QueueScheduler scheduler;

    @Before
    public void setup() throws Exception {
        vertx = Vertx.vertx();
        executorService = Executors.newSingleThreadScheduledExecutor();
        brokerFactory = new TestBrokerFactory(vertx, "localhost");
        scheduler = new QueueScheduler(executorService, brokerFactory, 0);
        TestUtils.deployVerticle(vertx, scheduler);
        int schedulerPort = waitForPort(() -> scheduler.getPort(), 1, TimeUnit.MINUTES);
        System.out.println("Scheduler port is " + schedulerPort);
        brokerFactory.setSchedulerPort(schedulerPort);
    }

    @After
    public void teardown() throws InterruptedException {
        vertx.close();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testAddressAddedBeforeBroker() throws InterruptedException, ExecutionException, TimeoutException {
        scheduler.addressesChanged(Collections.singletonMap("br1", Sets.newSet("queue1", "queue2")));

        TestBroker br1 = deployBroker("br1");

        waitForAddresses(br1, 2);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br1.getQueueNames(), hasItem("queue2"));
    }

    @Test
    public void testAddressAdded() throws InterruptedException {
        TestBroker br1 = deployBroker("br1");
        scheduler.addressesChanged(Collections.singletonMap("br1", Sets.newSet("queue1", "queue2")));

        waitForAddresses(br1, 2);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br1.getQueueNames(), hasItem("queue2"));

        scheduler.addressesChanged(Collections.singletonMap("br1", Sets.newSet("queue1", "queue2", "queue3")));

        waitForAddresses(br1, 3);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br1.getQueueNames(), hasItem("queue2"));
        assertThat(br1.getQueueNames(), hasItem("queue3"));
    }

    @Test
    public void testAddressRemoved() throws InterruptedException {
        TestBroker br1 = deployBroker("br1");
        scheduler.addressesChanged(Collections.singletonMap("br1", Sets.newSet("queue1", "queue2")));
        waitForAddresses(br1, 2);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br1.getQueueNames(), hasItem("queue2"));

        scheduler.addressesChanged(Collections.singletonMap("br1", Sets.newSet("queue1")));
        waitForAddresses(br1, 1);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
    }

    @Test
    public void testGroupDeleted() throws InterruptedException {
        TestBroker br1 = deployBroker("br1");
        TestBroker br2 = deployBroker("br2");

        scheduler.addressesChanged(createMap("br1", "br2", "queue1", "queue2"));

        waitForAddresses(br1, 1);
        waitForAddresses(br2, 1);

        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br2.getQueueNames(), hasItem("queue2"));

        scheduler.addressesChanged(Collections.singletonMap("br2", Sets.newSet("queue2")));
        waitForAddresses(br1, 1);
        waitForAddresses(br2, 1);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br2.getQueueNames(), hasItem("queue2"));
    }

    private Map<String, Set<String>> createMap(String key1, String key2, String value1, String value2) {
        Map<String, Set<String>> map = new HashMap<>();
        map.put(key1, Sets.newSet(value1));
        map.put(key2, Sets.newSet(value2));
        return map;
    }

                                               @Test
    public void testBrokerAdded() throws InterruptedException {
        scheduler.addressesChanged(createMap("br1", "br2", "queue1", "queue2"));

        TestBroker br1 = deployBroker("br1");
        waitForAddresses(br1, 1);
        assertThat(br1.getQueueNames(), hasItem("queue1"));

        TestBroker br2 = deployBroker("br2");
        waitForAddresses(br2, 1);
        assertThat(br2.getQueueNames(), hasItem("queue2"));
    }

    @Test
    public void testBrokerRemoved() throws InterruptedException, TimeoutException, ExecutionException {
        scheduler.addressesChanged(createMap("br1", "br2", "queue1", "queue2"));

        TestBroker br1 = deployBroker("br1");
        TestBroker br2 = deployBroker("br2");

        waitForAddresses(br1, 1);
        waitForAddresses(br2, 1);

        br2.close();
        waitTask(1, TimeUnit.MINUTES);

        br2 = deployBroker("br2");
        waitForAddresses(br2, 1);
        assertThat(br2.getQueueNames(), hasItem("queue2"));
    }

    private static void waitForAddresses(TestBroker broker, long numAddresses) throws InterruptedException {
        waitForAddresses(broker, numAddresses, 1, TimeUnit.MINUTES);
    }

    private static void waitForAddresses(TestBroker broker, long numAddresses, long timeout, TimeUnit timeUnit) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        long actualSize = broker.getNumQueues();
        while (System.currentTimeMillis() < endTime && actualSize != numAddresses) {
            actualSize = broker.getNumQueues();
            Thread.sleep(1000);
        }
        assertThat(actualSize, is(numAddresses));
    }

    private void waitTask(long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        Future f = executorService.submit(() -> { });
        f.get(timeout, timeUnit);
    }

    private TestBroker deployBroker(String id) throws InterruptedException {
        return brokerFactory.deployBroker(id);
    }
}
