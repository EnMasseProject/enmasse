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

package enmasse.address.controller;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class HTTPServerTest {

    private Vertx vertx;
    private TestManager testManager;

    @Before
    public void setup() throws InterruptedException {
        vertx = Vertx.vertx();
        testManager = new TestManager();
        CountDownLatch latch = new CountDownLatch(1);
        vertx.deployVerticle(new HTTPServer(testManager), c -> {
            latch.countDown();
        });
        latch.await(1, TimeUnit.MINUTES);
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testApi() throws InterruptedException {
        testManager.destinationList.add(new DestinationGroup("group0", Sets.newSet(new Destination("addr1", false, false, Optional.empty()))));
        HttpClient client = vertx.createHttpClient();
        try {
            CountDownLatch latch = new CountDownLatch(2);
            client.getNow(8080, "localhost", "/v1/enmasse/addresses", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    assertTrue(data.containsKey("addr1"));
                    latch.countDown();
                });
            });
            client.getNow(8080, "localhost", "/v1/enmasse/addresses", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    assertTrue(data.containsKey("addr1"));
                    latch.countDown();
                });
            });
            assertTrue(latch.await(1, TimeUnit.MINUTES));
        } finally {
            client.close();
        }
    }

    private static class TestManager implements AddressManager {
        Set<DestinationGroup> destinationList = new LinkedHashSet<>();
        boolean throwException = false;

        @Override
        public void destinationsUpdated(Set<DestinationGroup> destinationList) {
            if (throwException) {
                throw new RuntimeException();
            }
            this.destinationList = new LinkedHashSet<>(destinationList);
        }

        @Override
        public Set<DestinationGroup> listDestinationGroups() {
            if (throwException) {
                throw new RuntimeException();
            }
            return this.destinationList;
        }
    }
}
