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
package enmasse.smoketest;

import io.vertx.core.Vertx;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonSender;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static enmasse.smoketest.Environment.endpoint;

public class VertxTestBase {
    protected Vertx vertx;
    protected ProtonClient protonClient;

    @Before
    public void setup() {
        vertx = Vertx.vertx();
        protonClient = ProtonClient.create(vertx);
    }

    @After
    public void teardown() throws InterruptedException {
        vertx.close();
    }

    protected EnMasseClient createClient(boolean multicast) {
        return new EnMasseClient(protonClient, endpoint, multicast);
    }

    protected boolean waitUntilReady(String address, long timeout, TimeUnit timeUnit) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        connectToEndpoint(address, latch);
        return latch.await(timeout, timeUnit);
    }

    private void connectToEndpoint(String address, CountDownLatch latch) {
        protonClient.connect(endpoint.getHost(), endpoint.getPort(), event -> {
            if (event.succeeded()) {
                ProtonConnection connection = event.result();
                connection.openHandler(openResult -> {
                    if (openResult.succeeded()) {
                        ProtonSender sender = connection.createSender(address);
                        sender.openHandler(remoteOpenEvent -> {
                            sender.close();
                            connection.close();
                            if (remoteOpenEvent.succeeded()) {
                                latch.countDown();
                            } else {
                                scheduleReconnect(address, latch);
                            }
                        });
                        sender.open();
                    } else {
                        connection.close();
                        scheduleReconnect(address, latch);
                    }
                });
                connection.open();
            } else {
                scheduleReconnect(address, latch);
            }
        });
    }

    private void scheduleReconnect(String address, CountDownLatch latch) {
        vertx.setTimer(2000, timerId -> connectToEndpoint(address, latch));
    }
}
