/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.enmasse.address.model.*;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.SubjectAccessReview;
import io.enmasse.api.auth.TokenReview;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        AuthApi authApi = mock(AuthApi.class);
        when(authApi.getNamespace()).thenReturn("controller");
        when(authApi.performTokenReview(eq("mytoken"))).thenReturn(new TokenReview("foo", "myid", true));
        when(authApi.performSubjectAccessReview(eq("foo"), any(), any(), any())).thenReturn(new SubjectAccessReview("foo", true));
        when(authApi.performSubjectAccessReview(eq("foo"), any(), any(), any())).thenReturn(new SubjectAccessReview("foo", true));
        vertx.deployVerticle(new HTTPServer(instanceApi, new TestSchemaProvider(),"/doesnotexist", null, null), context.asyncAssertSuccess());
    }

    @After
    public void teardown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    private AddressSpace createAddressSpace(String name) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace(name)
                .setType("mytype")
                .setPlan("myplan")
                .setStatus(new io.enmasse.address.model.Status(false))
                .appendEndpoint(new Endpoint.Builder()
                        .setName("foo")
                        .setService("messaging")
                        .build())
                .build();
    }

    @Test
    public void testAddressingApi(TestContext context) throws InterruptedException {
        instanceApi.withAddressSpace(addressSpace).createAddress(
            new Address.Builder()
                    .setAddressSpace("myinstance")
                .setName("addr1")
                .setAddress("addR1")
                .setNamespace("ns")
                .setType("queue")
                .setPlan("myplan")
                .build());

        HttpClient client = vertx.createHttpClient();
        Async async = context.async(4);
        try {
            HttpClientRequest r1 = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1/namespaces/ns/addressspaces/myinstance/addresses", response -> {
                context.assertEquals(200, response.statusCode());
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    context.assertTrue(data.containsKey("items"));
                    context.assertEquals("addr1", data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"));
                    async.complete();
                });
            });
            putAuthzToken(r1);
            r1.end();

            HttpClientRequest r2 = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1/namespaces/ns/addresses/addr1", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    System.out.println(data);
                    context.assertTrue(data.containsKey("metadata"));
                    context.assertEquals("addr1", data.getJsonObject("metadata").getString("name"));
                    async.complete();
                });
            });
            putAuthzToken(r2);
            r2.end();

            HttpClientRequest r3 = client.post(8080, "localhost", "/apis/enmasse.io/v1alpha1/addresses/myinstance", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    context.assertTrue(data.containsKey("items"));
                    context.assertEquals(2, data.getJsonArray("items").size());
                    System.out.println(data.toString());
                    async.complete();
                });
            });
            putAuthzToken(r3);
            r3.end("{\"apiVersion\":\"enmasse.io/v1alpha1\",\"kind\":\"AddressList\",\"items\":[{\"metadata\":{\"name\":\"a4\"},\"spec\":{\"type\":\"queue\"}}]}");

            HttpClientRequest r4 = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1/addresses/myinstance?address=addr1", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    context.assertTrue(data.containsKey("metadata"));
                    context.assertEquals("addR1", data.getJsonObject("spec").getString("address"));
                    async.complete();
                });
            });
            putAuthzToken(r4);
            r4.end();
            async.awaitSuccess(60_000);
        } finally {
            client.close();
        }
    }

    private static HttpClientRequest putAuthzToken(HttpClientRequest request) {
        request.putHeader("Authorization", "Bearer mytoken");
        return request;
    }

    @Test
    public void testApiResources(TestContext context) throws InterruptedException {
        HttpClient client = vertx.createHttpClient();
        try {
            {
                Async async = context.async();
                HttpClientRequest rootReq = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1", response -> {
                    context.assertEquals(200, response.statusCode());
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.assertTrue(data.containsKey("resources"));
                        JsonArray resources = data.getJsonArray("resources");
                        context.assertEquals(2, resources.size());
                        async.complete();
                    });
                });
                putAuthzToken(rootReq);
                rootReq.end();
                async.awaitSuccess(60_000);
            }
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
                HttpClientRequest request = client.get(8080, "localhost", "/apis/enmasse.io/v1alpha1/schema", response -> {
                    context.assertEquals(200, response.statusCode());
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        System.out.println(data.toString());
                        context.assertTrue(data.containsKey("spec"));
                        context.assertEquals("type1", data.getJsonObject("spec").getJsonArray("addressSpaceTypes").getJsonObject(0).getString("name"));
                        async.complete();
                    });
                });
                putAuthzToken(request);
                request.end();
                async.awaitSuccess(60_000);
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
        addressSpaceApi.createAddressSpace(instance);

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
    public void testOpenApiSpec(TestContext context) throws InterruptedException {
        HttpClientOptions options = new HttpClientOptions();
        HttpClient client = vertx.createHttpClient(options);
        try {
            Async async = context.async();
            HttpClientRequest request = client.get(8080, "localhost", "/swagger.json", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    context.assertTrue(data.containsKey("paths"));
                    async.complete();
                });
            });
            putAuthzToken(request);
            request.end();
            async.awaitSuccess(60_000);
        } finally {
            client.close();
        }
    }
}
