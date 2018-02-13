/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.queue.scheduler;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class TestUtils {
    public static int waitForPort(Callable<Integer> portFetcher, long timeout, TimeUnit timeUnit) throws Exception {
        long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        while (System.currentTimeMillis() < endTime && portFetcher.call() == 0) {
            Thread.sleep(1000);
        }
        assertTrue(portFetcher.call() > 0);
        return portFetcher.call();
    }

    public static void deployVerticle(Vertx vertx, Verticle verticle) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.deployVerticle(verticle, r -> {
            latch.countDown();
        });
        latch.await(1, TimeUnit.MINUTES);
    }

}
