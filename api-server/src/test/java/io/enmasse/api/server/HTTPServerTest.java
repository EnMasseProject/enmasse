/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.SubjectAccessReview;
import io.enmasse.api.auth.TokenReview;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.*;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@SuppressWarnings("deprecation")
public class HTTPServerTest {

    private Vertx vertx;
    private TestAddressSpaceApi addressSpaceApi;
    private AddressSpace addressSpace;
    private HTTPServer server;

    public static String[] apiVersions() {
        return new String[] {"v1alpha1", "v1beta1"};
    }

    @BeforeEach
    public void setup(VertxTestContext context) {
        vertx = Vertx.vertx();
        addressSpaceApi = new TestAddressSpaceApi();
        addressSpace = createAddressSpace("ns", "myinstance");
        addressSpaceApi.createAddressSpace(addressSpace);

        AuthApi authApi = mock(AuthApi.class);
        when(authApi.getNamespace()).thenReturn("controller");
        when(authApi.performTokenReview(eq("mytoken"))).thenReturn(new TokenReview("foo", "myid", true));
        when(authApi.performSubjectAccessReviewResource(eq("foo"), any(), any(), any(), anyString())).thenReturn(new SubjectAccessReview("foo", true));
        when(authApi.performSubjectAccessReviewResource(eq("foo"), any(), any(), any(), anyString())).thenReturn(new SubjectAccessReview("foo", true));

        UserApi userApi = mock(UserApi.class);
        UserList users = new UserList();
        users.getItems().add(new UserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("myinstance.user1")
                        .withNamespace("myinstance")
                        .build())
                .withSpec(new UserSpecBuilder()
                        .withUsername("user1")
                        .withAuthentication(new UserAuthenticationBuilder()
                                .withType(UserAuthenticationType.password)
                                .withPassword("admin")
                                .build())
                        .withAuthorization(Arrays.asList(new UserAuthorizationBuilder()
                                .withAddresses(Arrays.asList("queue1"))
                                .withOperations(Arrays.asList(Operation.send, Operation.recv))
                                .build()))
                        .build())
                .build());
        when(userApi.listUsers(any())).thenReturn(users);

        ApiServerOptions options = new ApiServerOptions();
        options.setVersion("1.0");
        options.setCertDir("/doesnotexist");

        this.server = new HTTPServer(addressSpaceApi, new TestSchemaProvider(), authApi, userApi, new Metrics(), options, null, null, Clock.systemUTC(), 0, 0);
        vertx.deployVerticle(this.server, context.succeeding(arg -> context.completeNow()));
    }

    @AfterEach
    public void teardown(VertxTestContext context) {
        vertx.close(context.succeeding(arg -> context.completeNow()));
    }

    private int port () {
        return this.server.getActualPort();
    }

    private AddressSpace createAddressSpace(String namespace, String name) {
        return new AddressSpace.Builder()
                .setName(name)
                .setNamespace(namespace)
                .setType("type1")
                .setPlan("myplan")
                .setStatus(new io.enmasse.address.model.AddressSpaceStatus(false))
                .appendEndpoint(new EndpointSpec.Builder()
                        .setName("foo")
                        .setService("messaging")
                        .build())
                .build();
    }

    @ParameterizedTest
    @MethodSource("apiVersions")
    public void testAddressApi(String apiVersion, VertxTestContext context) throws Throwable {
        addressSpaceApi.withAddressSpace(addressSpace).createAddress(
                new Address.Builder()
                        .setAddressSpace("myinstance")
                        .setName("myinstance.addr1")
                        .setAddress("addR1")
                        .setNamespace("ns")
                        .setType("queue")
                        .setPlan("myplan")
                        .build());

        final Checkpoint checkpoint = context.checkpoint(4);
        HttpClient client = vertx.createHttpClient();

        try {

            Future<?> f = Future.succeededFuture();

            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest r1 = client.get(port(), "localhost", "/apis/enmasse.io/" + apiVersion + "/namespaces/ns/addressspaces/myinstance/addresses", response -> {
                    next.tryComplete();

                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> {
                            assertTrue(data.containsKey("items"));
                            assertEquals(1, data.getJsonArray("items").size());
                            assertEquals("myinstance.addr1", data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"));
                        });
                        checkpoint.flag();
                    });
                });
                putAuthzToken(r1);
                r1.end();

                return next;
            });
            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest r2 = client.get(port(), "localhost", "/apis/enmasse.io/" + apiVersion + "/namespaces/ns/addresses/myinstance.addr1", response -> {
                    next.tryComplete();

                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> {
                            assertTrue(data.containsKey("metadata"));
                            assertEquals("myinstance.addr1", data.getJsonObject("metadata").getString("name"));
                        });
                        checkpoint.flag();
                    });
                });
                putAuthzToken(r2);
                r2.end();

                return next;
            });

            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest r3 = client.post(port(), "localhost", "/apis/enmasse.io/" + apiVersion + "/namespaces/ns/addressspaces/myinstance/addresses", response -> {
                    next.tryComplete();

                    response.bodyHandler(buffer -> {
                        context.verify(() -> assertEquals(201, response.statusCode()));
                        checkpoint.flag();
                    });
                });
                r3.putHeader("Content-Type", "application/json");
                putAuthzToken(r3);
                JsonObject req = new JsonObject()
                        .put("apiVersion", "enmasse.io/" + apiVersion)
                        .put("kind", "AddressList")
                        .put("items", new JsonArray()
                                .add(new JsonObject()
                                        .put("apiVersion", "enmasse.io/" + apiVersion)
                                        .put("kind", "Address")
                                        .put("metadata", new JsonObject()
                                                .put("name", "myinstance.add1"))
                                        .put("spec", new JsonObject()
                                                .put("address", "add1")
                                                .put("type", "queue")
                                                .put("plan", "plan1"))));
                r3.end(req.toBuffer());

                return next;
            });

            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest r4 = client.get(port(), "localhost", "/apis/enmasse.io/" + apiVersion + "/namespaces/ns/addressspaces/myinstance/addresses?address=addR1", response -> {
                    next.tryComplete();

                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        System.out.println(data.toString());
                        context.verify(() -> {
                            assertTrue(data.containsKey("metadata"));
                            assertEquals("addR1", data.getJsonObject("spec").getString("address"));
                        });
                        checkpoint.flag();
                    });
                });
                putAuthzToken(r4);
                r4.end();

                return next;
            });

            assertTrue(context.awaitCompletion(60, TimeUnit.SECONDS));
            if (context.failed()) {
                throw context.causeOfFailure();
            }

        } finally {
            client.close();
        }

    }
    private static HttpClientRequest putAuthzToken(HttpClientRequest request) {
        request.putHeader("Authorization", "Bearer mytoken");
        return request;
    }

    @ParameterizedTest
    @MethodSource("apiVersions")
    public void testApiResources(String apiVersion, VertxTestContext context) throws Throwable {

        final Checkpoint checkpoint = context.checkpoint(2);
        HttpClient client = vertx.createHttpClient();

        try {

            Future<?> f = Future.succeededFuture();

            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest rootReq = client.get(port(), "localhost", "/apis/enmasse.io/" + apiVersion, response -> {
                    next.tryComplete();
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> assertTrue(data.containsKey("resources")));
                        JsonArray resources = data.getJsonArray("resources");
                        context.verify(() -> assertEquals(3, resources.size()));
                        checkpoint.flag();
                    });
                });
                putAuthzToken(rootReq);
                rootReq.end();

                return next;
            });

            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest rootReq = client.get(port(), "localhost", "/apis/user.enmasse.io/" + apiVersion, response -> {
                    next.tryComplete();
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> assertTrue(data.containsKey("resources")));
                        JsonArray resources = data.getJsonArray("resources");
                        context.verify(() -> assertEquals(1, resources.size()));
                        checkpoint.flag();
                    });
                });
                putAuthzToken(rootReq);
                rootReq.end();

                return next;
            });

            assertTrue(context.awaitCompletion(60, TimeUnit.SECONDS));
            if (context.failed()) {
              throw context.causeOfFailure();
            }
        } finally {
            client.close();
        }
    }

    @ParameterizedTest
    @MethodSource("apiVersions")
    public void testSchemaApi(String apiVersion, VertxTestContext context) throws Throwable {
        HttpClient client = vertx.createHttpClient();
        try {
            {
                HttpClientRequest request = client.get(port(), "localhost", "/apis/enmasse.io/" + apiVersion + "/namespaces/myinstance/addressspaceschemas", response -> {
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        System.out.println(data.toString());
                        context.verify(() -> {
                            assertTrue(data.containsKey("items"));
                            assertEquals(1, data.getJsonArray("items").size());
                        });
                        context.completeNow();
                    });
                });
                putAuthzToken(request);
                request.end();
            }

            assertTrue(context.awaitCompletion(60, TimeUnit.SECONDS));
            if (context.failed()) {
              throw context.causeOfFailure();
            }
        } finally {
            client.close();
        }
    }

    @ParameterizedTest
    @MethodSource("apiVersions")
    public void testUserApi(String apiVersion, VertxTestContext context) throws Throwable {
        HttpClient client = vertx.createHttpClient();
        try {
            {
                HttpClientRequest r1 = client.get(port(), "localhost", "/apis/user.enmasse.io/" + apiVersion + "/namespaces/ns/messagingusers", response -> {
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> {
                            assertTrue(data.containsKey("items"));
                            assertEquals("myinstance.user1", data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"));
                        });
                        context.completeNow();
                    });
                });
                putAuthzToken(r1);
                r1.end();
            }

            assertTrue(context.awaitCompletion(60, TimeUnit.SECONDS));
            if (context.failed()) {
              throw context.causeOfFailure();
            }
        } finally {
            client.close();
        }
    }

    @ParameterizedTest
    @MethodSource("apiVersions")
    public void testAddressSpaceApi(String apiVersion, VertxTestContext context) throws Throwable {

        final Checkpoint checkpoint = context.checkpoint(4);
        HttpClient client = vertx.createHttpClient();

        try {
            Future<?> f = Future.succeededFuture();

            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest r1 = client.get(port(), "localhost", "/apis/enmasse.io/" + apiVersion + "/namespaces/ns/addressspaces", response -> {
                    next.tryComplete();
                    context.verify(() -> assertEquals(200, response.statusCode()));
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        System.out.println(buffer.toString());
                        context.verify(() -> {
                            assertTrue(data.containsKey("items"));
                            assertEquals(1, data.getJsonArray("items").size());
                            assertEquals("myinstance", data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"));
                        });
                        checkpoint.flag();
                    });
                });
                putAuthzToken(r1);
                r1.end();

                return next;
            });

            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest r2 = client.get(port(), "localhost", "/apis/enmasse.io/" + apiVersion + "/namespaces/ns/addressspaces/myinstance", response -> {
                    next.tryComplete();
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        context.verify(() -> {
                            assertTrue(data.containsKey("metadata"));
                            assertEquals("myinstance", data.getJsonObject("metadata").getString("name"));
                        });
                        checkpoint.flag();
                    });
                });
                putAuthzToken(r2);
                r2.end();

                return next;
            });

            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest r3 = client.post(port(), "localhost", "/apis/enmasse.io/" + apiVersion + "/namespaces/ns/addressspaces", response -> {
                    next.tryComplete();
                    response.bodyHandler(buffer -> {
                        context.verify(() -> assertEquals(201, response.statusCode()));
                        checkpoint.flag();
                    });
                });
                r3.putHeader("Content-Type", "application/json");
                putAuthzToken(r3);
                JsonObject req = new JsonObject()
                        .put("apiVersion", "enmasse.io/" + apiVersion)
                        .put("kind", "AddressSpace")
                        .put("metadata", new JsonObject()
                                .put("name", "a4"))
                        .put("spec", new JsonObject()
                                .put("type", "type1")
                                .put("plan", "myplan"));
                r3.end(req.toBuffer());

                return next;
            });

            f = f.compose(v -> {
                Future<?> next = Future.future();

                HttpClientRequest r4 = client.get(port(), "localhost", "/apis/enmasse.io/" + apiVersion + "/namespaces/ns/addressspaces/a4", response -> {
                    next.tryComplete();
                    response.bodyHandler(buffer -> {
                        JsonObject data = buffer.toJsonObject();
                        System.out.println(data.toString());
                        context.verify(() -> {
                            assertTrue(data.containsKey("metadata"));
                            assertEquals("myplan", data.getJsonObject("spec").getString("plan"));
                        });
                        checkpoint.flag();
                    });
                });
                putAuthzToken(r4);
                r4.end();

                return next;
            });

            assertTrue(context.awaitCompletion(60, TimeUnit.SECONDS));
            if (context.failed()) {
                throw context.causeOfFailure();
            }
        } finally {
            client.close();
        }
    }

    @Test
    public void testOpenApiSpec(VertxTestContext context) throws Throwable {
        HttpClientOptions options = new HttpClientOptions();
        HttpClient client = vertx.createHttpClient(options);
        try {
            HttpClientRequest request = client.get(port(), "localhost", "/swagger.json", response -> {
                response.bodyHandler(buffer -> {
                    JsonObject data = buffer.toJsonObject();
                    context.verify(() -> assertTrue(data.containsKey("paths")));
                    context.completeNow();
                });
            });
            putAuthzToken(request);
            request.end();

            assertTrue(context.awaitCompletion(60, TimeUnit.SECONDS));
            if (context.failed()) {
              throw context.causeOfFailure();
            }
        } finally {
            client.close();
        }
    }
}
