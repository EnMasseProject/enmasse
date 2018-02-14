/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.queue.scheduler;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class TestBrokerFactory implements BrokerFactory {

    private final Vertx vertx;
    private final String host;
    private final Map<String, Broker> brokerMap = new ConcurrentHashMap<>();
    private int schedulerPort;

    public TestBrokerFactory(Vertx vertx, String host) {
        this.vertx = vertx;
        this.host = host;
    }

    @Override
    public Future<Broker> createBroker(ProtonConnection connection) {
        Future<Broker> broker = Future.future();
        broker.complete(brokerMap.get(connection.getRemoteContainer()));
        return broker;
    }

    public TestBroker deployBroker(String id) throws InterruptedException {
        TestBroker broker = new TestBroker(id, host, schedulerPort);
        brokerMap.put(id, broker);
        CountDownLatch latch = new CountDownLatch(1);
        vertx.deployVerticle(broker, result -> {
            if (result.succeeded()) {
                broker.setDeploymentId(result.result());
                latch.countDown();
            }
        });
        latch.await(1, TimeUnit.MINUTES);
        return broker;
    }

    public void setSchedulerPort(int schedulerPort) {
        this.schedulerPort = schedulerPort;
    }
}
