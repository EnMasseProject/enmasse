/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import javax.ws.rs.core.HttpHeaders;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.VertxFactory;
import io.enmasse.systemtest.utils.TestUtils;
import io.fabric8.zjsonpatch.internal.guava.Strings;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

public abstract class ApiClient implements AutoCloseable {
    protected static Logger log = CustomLogger.getLogger();
    protected WebClient client;
    protected Kubernetes kubernetes;
    protected Vertx vertx;
    protected Endpoint endpoint;
    protected Supplier<Endpoint> endpointSupplier;
    protected String authzString;
    protected String apiVersion;
    private final int initRetry = 10;

    protected ApiClient(Kubernetes kubernetes, Supplier<Endpoint> endpointSupplier, String apiVersion) {
        initializeAddressClient(kubernetes, endpointSupplier, apiVersion, "");
    }

    protected ApiClient(Kubernetes kubernetes, Supplier<Endpoint> endpointSupplier, String apiVersion, String authzString) {
        initializeAddressClient(kubernetes, endpointSupplier, apiVersion, authzString);
    }

    private void initializeAddressClient(Kubernetes kubernetes, Supplier<Endpoint> endpointSupplier, String apiVersion, String token) {
        this.vertx = VertxFactory.create();
        this.kubernetes = kubernetes;
        this.connect();
        this.authzString = String.format("Bearer %s", Strings.isNullOrEmpty(token) ? kubernetes.getApiToken() : token);
        this.endpoint = endpointSupplier.get();
        this.endpointSupplier = endpointSupplier;
        this.apiVersion = apiVersion;
    }

    protected abstract String apiClientName();

    protected void reconnect() {
        if (client != null) {
            this.client.close();
        }
        connect();
    }

    @Override
    public void close() {
        if (client != null) {
            this.client.close();
        }
        this.vertx.close();
    }

    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                // TODO: Fetch CA and use
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    protected <T> void responseHandler(AsyncResult<HttpResponse<T>> ar, CompletableFuture<T> promise, int expectedCode,
                                       String warnMessage) {
        responseHandler(ar, promise, expectedCode, warnMessage, true);
    }

    protected <T> void responseHandler(AsyncResult<HttpResponse<T>> ar, CompletableFuture<T> promise, int expectedCode,
                                       String warnMessage, boolean throwException) {
        responseHandler(ar, promise, (responseCode)-> responseCode == expectedCode, String.valueOf(expectedCode), warnMessage, throwException);
    }

    protected <T> void responseHandler(AsyncResult<HttpResponse<T>> ar, CompletableFuture<T> promise, Predicate<Integer> expectedCodePredicate,
            String warnMessage, boolean throwException) {

        responseHandler(ar, promise, expectedCodePredicate, expectedCodePredicate.toString(), warnMessage, throwException);

    }

    protected <T> void responseHandler(AsyncResult<HttpResponse<T>> ar, CompletableFuture<T> promise, Predicate<Integer> expectedCodePredicate,
                                       String expectedCodeOrCodes, String warnMessage, boolean throwException) {
        try {
            if (ar.succeeded()) {
                HttpResponse<T> response = ar.result();
                T body = response.body();
                if (expectedCodePredicate.negate().test(response.statusCode())) {
                    log.error("expected-code: {}, response-code: {}, body: {}, op: {}", expectedCodeOrCodes, response.statusCode(), response.body(), warnMessage);
                    promise.completeExceptionally(new RuntimeException("Status " + response.statusCode() + " body: " + (body != null ? body.toString() : null)));
                } else if (response.statusCode() < HTTP_OK || response.statusCode() >= HttpURLConnection.HTTP_MULT_CHOICE) {
                    if(throwException) {
                        promise.completeExceptionally(new RuntimeException(body == null ? "null" : body.toString()));
                    }else {
                        promise.complete(ar.result().body());
                    }
                } else {
                    promise.complete(ar.result().body());
                }
            } else {
                log.warn(warnMessage);
                promise.completeExceptionally(ar.cause());
            }
        } catch (io.vertx.core.json.DecodeException decEx) {
            if (ar.result().bodyAsString().toLowerCase().contains("application is not available")) {
                log.warn("'{}' is not available.", apiClientName(), ar.cause());
                throw new IllegalStateException(String.format("'%s' is not available.", apiClientName()));
            } else {
                log.warn("Unexpected object received", ar.cause());
                throw new IllegalStateException("JsonObject expected, but following object was received: " + ar.result().bodyAsString());
            }
        }
    }

    protected <T> T doRequestNTimes(int retry, Callable<T> fn, Optional<Supplier<Endpoint>> endpointFn, Optional<Runnable> reconnect) throws Exception {
        return TestUtils.doRequestNTimes(retry, () -> {
            if (endpointFn.isPresent()) {
                endpoint = endpointFn.get().get();
            }
            return fn.call();
        }, reconnect);
    }

    public String getApiVersion() {
        return apiVersion;
    }

    protected JsonObject getResource(String type, String basePath, String name) throws Exception {
        return getResource(type, basePath, name, HTTP_OK);
    }

    protected JsonObject getResource(String type, String basePath, String name, int expectedCode) throws Exception {
        String path = basePath + "/" + name;
        log.info("GET: path {}", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

        return doRequestNTimes(initRetry, () -> {
                    client.get(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: get %s %s", type, name)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(endpointSupplier),
                Optional.empty());
    }

    protected void createResource(String type, String basePath, JsonObject data) throws Exception {
        createResource(type, basePath, data, HTTP_CREATED);
    }

    protected void createResource(String type, String basePath, JsonObject data, int expectedCode) throws Exception {
        log.info("POST-{}: path {}; body {}", type, basePath, data.toString());
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), basePath)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(data, ar -> {
                                responseHandler(ar,
                                        responsePromise,
                                        expectedCode,
                                        String.format("Error: create %s '%s'", type, basePath));
                            });
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(endpointSupplier),
                Optional.empty());
    }

    protected void replaceResource(String type, String basePath, String name, JsonObject data) throws Exception {
        replaceResource(type, basePath, name, data, HTTP_OK);
    }

    protected void replaceResource(String type, String basePath, String name, JsonObject data, int expectedCode) throws Exception {

        String path = basePath + "/" + name;
        log.info("PUT-{}: path {}; body {}", type, path, data.toString());
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

        doRequestNTimes(initRetry, () -> {
                    client.put(endpoint.getPort(), endpoint.getHost(), path)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(data, ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: create %s '%s'", type, name)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(endpointSupplier),
                Optional.empty());
    }

    protected void deleteResource(String type, String basePath, String name) throws Exception {
        deleteResource(type, basePath, name, HTTP_OK);
    }

    protected void deleteResource(String type, String basePath, String name, int expectedCode) throws Exception {
        String path = basePath + "/" + name;
        log.info("DELETE-{}: path '{}'", type, path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.delete(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: delete %s '%s'", type, name)));
                    return responsePromise.get(2, TimeUnit.MINUTES);
                },
                Optional.of(endpointSupplier),
                Optional.empty());
    }
}
