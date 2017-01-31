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

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class QueueSchedulerTest {

    private Vertx vertx;
    private ExecutorService executorService;
    private TestBrokerFactory brokerFactory;
    private QueueScheduler scheduler;
    private TestConfigServ testConfigServ;

    @Before
    public void setup() throws Exception {
        vertx = Vertx.vertx();
        testConfigServ = new TestConfigServ(0);
        deployVerticle(testConfigServ);
        int configPort = waitForPort(() -> testConfigServ.getPort(), 1, TimeUnit.MINUTES);
        System.out.println("Config port is " + configPort);

        executorService = Executors.newSingleThreadScheduledExecutor();
        brokerFactory = new TestBrokerFactory(vertx, "localhost");
        scheduler = new QueueScheduler("localhost", configPort, executorService, brokerFactory, 0);
        deployVerticle(scheduler);
        int schedulerPort = waitForPort(() -> scheduler.getPort(), 1, TimeUnit.MINUTES);
        System.out.println("Scheduler port is " + schedulerPort);
        brokerFactory.setSchedulerPort(schedulerPort);
    }

    private int waitForPort(Callable<Integer> portFetcher, long timeout, TimeUnit timeUnit) throws Exception {
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (System.currentTimeMillis() < endTime && portFetcher.call() == 0) {
            Thread.sleep(1000);
        }
        assertTrue(portFetcher.call() > 0);
        return portFetcher.call();
    }

    private void deployVerticle(Verticle verticle) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.deployVerticle(verticle, r -> {
            latch.countDown();
        });
        latch.await(1, TimeUnit.MINUTES);
    }

    @After
    public void teardown() throws InterruptedException {
        vertx.close();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);
    }

    @Test
    public void testAddressAddedBeforeBroker() throws InterruptedException, ExecutionException, TimeoutException {
        testConfigServ.deployConfig("{\"br1\":{\"queue1\":null,\"queue2\":null}}");

        TestBroker br1 = deployBroker("br1");

        waitForAddresses(br1, 2);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br1.getQueueNames(), hasItem("queue2"));
    }

    @Test
    public void testAddressAdded() throws InterruptedException {
        TestBroker br1 = deployBroker("br1");
        testConfigServ.deployConfig("{\"br1\":{\"queue1\":null,\"queue2\":null}}");

        waitForAddresses(br1, 2);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br1.getQueueNames(), hasItem("queue2"));

        testConfigServ.deployConfig("{\"br1\":{\"queue1\":null,\"queue2\":null,\"queue3\":null}}");

        waitForAddresses(br1, 3);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br1.getQueueNames(), hasItem("queue2"));
        assertThat(br1.getQueueNames(), hasItem("queue3"));
    }

    @Test
    public void testAddressRemoved() throws InterruptedException {
        TestBroker br1 = deployBroker("br1");
        testConfigServ.deployConfig("{\"br1\":{\"queue1\":null,\"queue2\":null}}");
        waitForAddresses(br1, 2);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br1.getQueueNames(), hasItem("queue2"));

        testConfigServ.deployConfig("{\"br1\":{\"queue1\":null}}");
        waitForAddresses(br1, 1);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
    }

    @Test
    public void testGroupDeleted() throws InterruptedException {
        TestBroker br1 = deployBroker("br1");
        TestBroker br2 = deployBroker("br2");

        testConfigServ.deployConfig("{\"br1\":{\"queue1\":null}, \"br2\": {\"queue2\":null}}");

        waitForAddresses(br1, 1);
        waitForAddresses(br2, 1);

        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br2.getQueueNames(), hasItem("queue2"));

        testConfigServ.deployConfig("{\"br2\": {\"queue2\":null}}");
        waitForAddresses(br1, 1);
        waitForAddresses(br2, 1);
        assertThat(br1.getQueueNames(), hasItem("queue1"));
        assertThat(br2.getQueueNames(), hasItem("queue2"));
    }

    @Test
    public void testBrokerAdded() throws InterruptedException {
        testConfigServ.deployConfig("{\"br1\":{\"queue1\":null}, \"br2\": {\"queue2\":null}}");

        TestBroker br1 = deployBroker("br1");
        waitForAddresses(br1, 1);
        assertThat(br1.getQueueNames(), hasItem("queue1"));

        TestBroker br2 = deployBroker("br2");
        waitForAddresses(br2, 1);
        assertThat(br2.getQueueNames(), hasItem("queue2"));
    }

    @Test
    public void testBrokerRemoved() throws InterruptedException, TimeoutException, ExecutionException {
        testConfigServ.deployConfig("{\"br1\":{\"queue1\":null}, \"br2\": {\"queue2\":null}}");

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
