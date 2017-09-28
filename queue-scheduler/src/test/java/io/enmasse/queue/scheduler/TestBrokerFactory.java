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
