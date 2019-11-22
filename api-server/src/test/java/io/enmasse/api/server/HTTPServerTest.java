/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceBuilder;
import io.enmasse.api.auth.ApiHeaderConfig;
import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.SubjectAccessReview;
import io.enmasse.api.auth.TokenReview;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.k8s.api.TestAddressSpaceApi;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.model.v1.Operation;
import io.enmasse.user.model.v1.UserAuthenticationBuilder;
import io.enmasse.user.model.v1.UserAuthenticationType;
import io.enmasse.user.model.v1.UserAuthorizationBuilder;
import io.enmasse.user.model.v1.UserBuilder;
import io.enmasse.user.model.v1.UserList;
import io.enmasse.user.model.v1.UserSpecBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
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
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    public void setup(VertxTestContext context) throws Exception {
        vertx = Vertx.vertx();
        addressSpaceApi = new TestAddressSpaceApi();
        addressSpace = createAddressSpace("ns", "myinstance");
        addressSpaceApi.createAddressSpace(addressSpace);

        AuthApi authApi = mock(AuthApi.class);
        when(authApi.getNamespace()).thenReturn("controller");
        TokenReview tokenReview = new TokenReview("foo", "myid", null, null, true);
        when(authApi.performTokenReview(eq("mytoken"))).thenReturn(tokenReview);
        when(authApi.performSubjectAccessReviewResource(eq(tokenReview), any(), any(), any(), anyString())).thenReturn(new SubjectAccessReview("foo", true));
        when(authApi.performSubjectAccessReviewResource(eq(tokenReview), any(), any(), any(), anyString())).thenReturn(new SubjectAccessReview("foo", true));

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
        when(userApi.listUsers(any(), any())).thenReturn(users);
        when(userApi.listAllUsers(any())).thenReturn(users);

        ApiServerOptions options = new ApiServerOptions();
        options.setVersion("1.0");
        options.setCertDir("/doesnotexist");

        AuthenticationServiceRegistry authenticationServiceRegistry = mock(AuthenticationServiceRegistry.class);
        AuthenticationService authenticationService = new AuthenticationServiceBuilder()
                .withNewMetadata()
                .withName("standard")
                .endMetadata()
                .withNewStatus()
                .withHost("example")
                .withPort(5671)
                .endStatus()
                .build();
        when(authenticationServiceRegistry.findAuthenticationService(any())).thenReturn(Optional.of(authenticationService));
        when(authenticationServiceRegistry.resolveDefaultAuthenticationService()).thenReturn(Optional.of(authenticationService));
        when(authenticationServiceRegistry.listAuthenticationServices()).thenReturn(Collections.singletonList(authenticationService));

        ResteasyDeploymentFactory resteasyDeploymentFactory = new ResteasyDeploymentFactory(addressSpaceApi, new TestSchemaProvider(), authApi, userApi, Clock.systemUTC(), authenticationServiceRegistry, ApiHeaderConfig.DEFAULT_HEADERS_CONFIG, new Metrics(), false);
        this.server = new HTTPServer(options, resteasyDeploymentFactory, null, null, 0);
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
        return new AddressSpaceBuilder()

                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()

                .withNewSpec()
                .withType("type1")
                .withPlan("myplan")

                .addNewEndpoint()
                        .withName("foo")
                        .withService("messaging")
                        .endEndpoint()

                .endSpec()

                .withNewStatus(false)

                .build();
    }

    private static HttpClientRequest putAuthzToken(HttpClientRequest request) {
        request.putHeader("Authorization", "Bearer mytoken");
        return request;
    }

    private void runSingleRequest(final VertxTestContext context, final String apiVersion, final HttpMethod method, final String uri, final JsonObject payload, final BiConsumer<HttpClientResponse, Buffer> responseConsumer) throws Throwable {
        final HttpClient client = vertx.createHttpClient();

        try {

            HttpClientRequest req = client.request(method, port(), "localhost", uri, response -> {
                response.bodyHandler(buffer -> {
                    responseConsumer.accept(response, buffer);
                    if(!context.completed()) {
                        context.completeNow();
                    }
                });
            });
            req.putHeader("Content-Type", "application/json");
            putAuthzToken(req);

            if ( payload != null ) {
                req.end(payload.toBuffer());
            } else {
                req.end();
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
    public void testApiResources(String apiVersion, VertxTestContext context) throws Throwable {

        final Checkpoint checkpoint = context.checkpoint(1);
        HttpClient client = vertx.createHttpClient();

        try {

            Future<?> f = Future.succeededFuture();

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
    public void testUserApi(String apiVersion, VertxTestContext context) throws Throwable {

        runSingleRequest(context, apiVersion, HttpMethod.GET,  "/apis/user.enmasse.io/" + apiVersion + "/namespaces/ns/messagingusers", null, (response, buffer) -> {
            context.verify(() -> {
                assertEquals(200, response.statusCode());
                JsonObject data = buffer.toJsonObject();
                assertTrue(data.containsKey("items"));
                assertEquals(1, data.getJsonArray("items").size());
                assertEquals("myinstance.user1", data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"));
            });
        });

    }

    @ParameterizedTest
    @MethodSource("apiVersions")
    public void testUserApiNonNamespaces(String apiVersion, VertxTestContext context) throws Throwable {

        runSingleRequest(context, apiVersion, HttpMethod.GET,  "/apis/user.enmasse.io/" + apiVersion + "/messagingusers", null, (response, buffer) -> {
            context.verify(() -> {
                assertEquals(200, response.statusCode());
                JsonObject data = buffer.toJsonObject();
                assertTrue(data.containsKey("items"));
                assertEquals(1, data.getJsonArray("items").size());
                assertEquals("myinstance.user1", data.getJsonArray("items").getJsonObject(0).getJsonObject("metadata").getString("name"));
            });
        });

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
