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

package enmasse.controller;

import enmasse.controller.model.*;
import io.enmasse.address.model.impl.Address;
import io.enmasse.address.model.impl.AddressStatus;
import io.enmasse.address.model.impl.types.standard.StandardType;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class HTTPServerTest {

    private Vertx vertx;
    private TestInstanceApi instanceApi;

    @Before
    public void setup() throws InterruptedException {
        vertx = Vertx.vertx();
        instanceApi = new TestInstanceApi();
        AddressSpaceId addressSpaceId = AddressSpaceId.withId("myinstance");
        instanceApi.createInstance(new Instance.Builder(addressSpaceId).build());
        CountDownLatch latch = new CountDownLatch(1);
        vertx.deployVerticle(new HTTPServer(instanceApi, "/doesnotexist"), c -> {
            latch.countDown();
        });
        latch.await(1, TimeUnit.MINUTES);
    }

    @After
    public void teardown() {
        vertx.close();
    }

    @Test
    public void testAddressingApi() throws InterruptedException {
        instanceApi.withInstance(AddressSpaceId.withId("myinstance")).createAddress(
            new Address.Builder()
                    .setAddressSpace("myinstance")
                .setName("addr1")
                .setAddress("addr1")
                .setType(StandardType.QUEUE)
                .setPlan(StandardType.QUEUE.getPlans().get(0))
                .setUuid(UUID.randomUUID().toString())
                .build());

        HttpClient client = vertx.createHttpClient();
        try {
            CountDownLatch latch = new CountDownLatch(2);
            client.getNow(8080, "localhost", "/v1/addresses/myinstance", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    assertTrue(data.containsKey("items"));
                    assertThat(data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"), is("addr1"));
                    latch.countDown();
                });
            });

            client.getNow(8080, "localhost", "/v1/addresses/myinstance/addr1", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    assertTrue(data.containsKey("metadata"));
                    assertThat(data.getJsonObject("metadata").getString("name"), is("addr1"));
                    latch.countDown();
                });
            });
            assertTrue(latch.await(1, TimeUnit.MINUTES));
        } finally {
            client.close();
        }
    }

    @Test
    public void testSchemaApi() throws InterruptedException {
        HttpClient client = vertx.createHttpClient();
        try {
            {
                CountDownLatch latch = new CountDownLatch(1);
                client.getNow(8080, "localhost", "/v1/schema", response -> {
                    assertThat(response.statusCode(), is(200));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        assertTrue(data.containsKey("spec"));
                        assertThat(data.getJsonObject("spec").getJsonArray("addressSpaceTypes").getJsonObject(0).getString("name"), is("standard"));
                        latch.countDown();
                    });
                });
                assertTrue(latch.await(1, TimeUnit.MINUTES));
            }
        } finally {
            client.close();
        }
    }

    @Test
    public void testInstanceApi() throws InterruptedException {
        Instance instance = new Instance.Builder(AddressSpaceId.withId("myinstance"))
                .messagingHost(Optional.of("messaging.example.com"))
                .build();
        instanceApi.createInstance(instance);

        HttpClient client = vertx.createHttpClient();
        try {
            {
                final CountDownLatch latch = new CountDownLatch(1);
                client.getNow(8080, "localhost", "/v3/instance", response -> {
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        assertTrue(data.containsKey("kind"));
                        assertThat(data.getString("kind"), is("InstanceList"));
                        assertTrue(data.containsKey("items"));
                        JsonArray items = data.getJsonArray("items");
                        assertThat(items.size(), is(1));
                        assertThat(items.getJsonObject(0).getJsonObject("spec").getString("messagingHost"), is("messaging.example.com"));
                        latch.countDown();
                    });
                });
                assertTrue(latch.await(1, TimeUnit.MINUTES));
            }

            {
                final CountDownLatch latch = new CountDownLatch(1);
                client.getNow(8080, "localhost", "/v3/instance/myinstance", response -> {
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        assertTrue(data.containsKey("metadata"));
                        assertThat(data.getJsonObject("metadata").getString("name"), is("myinstance"));
                        assertThat(data.getString("kind"), is("Instance"));
                        assertThat(data.getJsonObject("spec").getString("messagingHost"), is("messaging.example.com"));
                        latch.countDown();
                    });
                });
                assertTrue(latch.await(1, TimeUnit.MINUTES));
            }
        } finally {
            client.close();
        }
    }
}
