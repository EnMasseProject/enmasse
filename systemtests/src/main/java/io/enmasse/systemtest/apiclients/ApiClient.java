/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.VertxFactory;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.platform.Kubernetes;
import io.fabric8.zjsonpatch.internal.guava.Strings;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.net.HttpURLConnection.HTTP_OK;

public abstract class ApiClient implements AutoCloseable {
    private static final Logger LOGGER = CustomLogger.getLogger();
    protected WebClient client;
    protected Kubernetes kubernetes;
    protected Vertx vertx;
    protected Endpoint endpoint;
    protected String authzString;

    protected ApiClient(Kubernetes kubernetes, Supplier<Endpoint> endpointSupplier) {
        initializeAddressClient(kubernetes, endpointSupplier);
    }

    private void initializeAddressClient(Kubernetes kubernetes, Supplier<Endpoint> endpointSupplier) {
        this.vertx = VertxFactory.create();
        this.kubernetes = kubernetes;
        this.connect();
        this.authzString = String.format("Bearer %s", Strings.isNullOrEmpty("") ? kubernetes.getApiToken() : "");
        this.endpoint = endpointSupplier.get();
    }

    protected abstract String apiClientName();

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

    <T> void responseHandler(AsyncResult<HttpResponse<T>> ar, CompletableFuture<T> promise,
                             String warnMessage) {
        responseHandler(ar, promise, HttpURLConnection.HTTP_OK, warnMessage, true);
    }

    protected <T> void responseHandler(AsyncResult<HttpResponse<T>> ar, CompletableFuture<T> promise, int expectedCode,
                                       String warnMessage, boolean throwException) {
        responseHandler(ar, promise, (responseCode) -> responseCode == expectedCode, String.valueOf(expectedCode), warnMessage, throwException);
    }

    private <T> void responseHandler(AsyncResult<HttpResponse<T>> ar, CompletableFuture<T> promise, Predicate<Integer> expectedCodePredicate,
                                     String expectedCodeOrCodes, String warnMessage, boolean throwException) {
        try {
            if (ar.succeeded()) {
                HttpResponse<T> response = ar.result();
                T body = response.body();
                if (expectedCodePredicate.negate().test(response.statusCode())) {
                    LOGGER.error("expected-code: {}, response-code: {}, body: {}, op: {}",
                            expectedCodeOrCodes, response.statusCode(), response.body(), warnMessage);
                    promise.completeExceptionally(new RuntimeException("Status " +
                            response.statusCode() + " body: " + (body != null ? body.toString() : null)));
                } else if (response.statusCode() < HTTP_OK || response.statusCode() >= HttpURLConnection.HTTP_MULT_CHOICE) {
                    if (throwException) {
                        promise.completeExceptionally(new RuntimeException(body == null ? "null" : body.toString()));
                    } else {
                        promise.complete(ar.result().body());
                    }
                } else {
                    promise.complete(ar.result().body());
                }
            } else {
                LOGGER.warn("Request failed: {}", warnMessage, ar.cause());
                promise.completeExceptionally(ar.cause());
            }
        } catch (io.vertx.core.json.DecodeException decEx) {
            if (ar.result().bodyAsString().toLowerCase().contains("application is not available")) {
                LOGGER.warn("'{}' is not available.", apiClientName(), ar.cause());
                throw new IllegalStateException(String.format("'%s' is not available.", apiClientName()));
            } else {
                LOGGER.warn("Unexpected object received", ar.cause());
                throw new IllegalStateException("JsonObject expected, but following object was received: " + ar.result().bodyAsString());
            }
        }
    }
}
