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

package io.enmasse.controller;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Endpoint;
import io.enmasse.address.model.SecretCertProvider;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.address.model.types.standard.StandardType;
import io.enmasse.controller.api.TestAddressSpaceApi;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.PasswordAuthentication;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@RunWith(VertxUnitRunner.class)
public class HTTPServerTest {

    private Vertx vertx;
    private TestAddressSpaceApi instanceApi;
    private AddressSpace addressSpace;

    @Before
    public void setup(TestContext context) throws InterruptedException {
        vertx = Vertx.vertx();
        instanceApi = new TestAddressSpaceApi();
        String addressSpaceName = "myinstance";
        addressSpace = createAddressSpace(addressSpaceName);
        instanceApi.createAddressSpace(addressSpace);
        vertx.deployVerticle(new HTTPServer(instanceApi, "/doesnotexist", Optional.of(new PasswordAuthentication("user", "pass".toCharArray()))), context.asyncAssertSuccess());
    }

    @After
    public void teardown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    private AddressSpace createAddressSpace(String name) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace(name)
                .setType(new StandardAddressSpaceType())
                .setPlan(new StandardAddressSpaceType().getPlans().get(0))
                .setStatus(new io.enmasse.address.model.Status(false))
                .appendEndpoint(new Endpoint.Builder()
                        .setName("foo")
                        .setService("messaging")
                        .setCertProvider(new SecretCertProvider("mysecret"))
                        .build())
                .build();
    }

    @Test
    public void testAddressingApi(TestContext context) throws InterruptedException {
        instanceApi.withAddressSpace(addressSpace).createAddress(
            new Address.Builder()
                    .setAddressSpace("myinstance")
                .setName("addr1")
                .setAddress("addr1")
                .setType(StandardType.QUEUE)
                .setPlan(StandardType.QUEUE.getPlans().get(0))
                .setUuid(UUID.randomUUID().toString())
                .build());

        HttpClient client = vertx.createHttpClient();
        Async async = context.async(2);
        try {
            client.getNow(8080, "localhost", "/v1/addresses/myinstance", response -> {
                context.assertEquals(200, response.statusCode());
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    context.assertTrue(data.containsKey("items"));
                    context.assertEquals("addr1", data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"));
                    async.complete();
                });
            });

            client.getNow(8080, "localhost", "/v1/addresses/myinstance/addr1", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    context.assertTrue(data.containsKey("metadata"));
                    context.assertEquals("addr1", data.getJsonObject("metadata").getString("name"));
                    async.complete();
                });
            });
        } finally {
            client.close();
        }
    }

    @Test
    public void testSchemaApi(TestContext context) throws InterruptedException {
        HttpClient client = vertx.createHttpClient();
        try {
            {
                Async async = context.async();
                client.getNow(8080, "localhost", "/v1/schema", response -> {
                    context.assertEquals(200, response.statusCode());
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.assertTrue(data.containsKey("spec"));
                        context.assertEquals("standard", data.getJsonObject("spec").getJsonArray("addressSpaceTypes").getJsonObject(0).getString("name"));
                        async.complete();
                    });
                });
            }
        } finally {
            client.close();
        }
    }

    /*
    @Test
    public void testInstanceApi() throws InterruptedException {
        Instance instance = new Instance.Builder(AddressSpaceId.withId("myinstance"))
                .messagingHost(Optional.of("messaging.example.com"))
                .build();
        instanceApi.createAddressSpace(instance);

        HttpClient client = vertx.createHttpClient();
        try {
            {
                final CountDownLatch latch = new CountDownLatch(1);
                client.getNow(8080, "localhost", "/v3/addressspace", response -> {
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
                client.getNow(8080, "localhost", "/v3/addressspace/myinstance", response -> {
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
    */

    @Test
    public void testOpenServiceBrokerAPI(TestContext context) throws InterruptedException {
        HttpClientOptions options = new HttpClientOptions();
        HttpClient client = vertx.createHttpClient(options);
        try {
            Async async = context.async();
            HttpClientRequest request = client.get(8080, "localhost", "/v2/catalog", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    context.assertTrue(data.containsKey("services"));
                    async.complete();
                });
            });
            request.headers().add(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString("user:pass".getBytes()));
            request.end();
        } finally {
            client.close();
        }
    }
}
