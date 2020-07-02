/*
 * Copyright 2018-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import static java.net.HttpURLConnection.HTTP_OK;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.logs.CustomLogger;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public abstract class ApiClient implements AutoCloseable {
    private static final Logger log = CustomLogger.getLogger();

    protected final Vertx vertx;
    private final Vertx vertxToClose;

    protected final Endpoint endpoint;
    protected final Supplier<Endpoint> endpointSupplier;
    protected final String apiVersion;

    private WebClient client;

    protected abstract String apiClientName();

    protected ApiClient(final Supplier<Endpoint> endpointSupplier, final String apiVersion) {
        this(null, endpointSupplier, apiVersion);
    }

    protected ApiClient(final Vertx vertx, final Supplier<Endpoint> endpointSupplier, final String apiVersion) {
        // connect() may be overridden, and must not be called in the constructor
        if (vertx == null) {
            // create a new vertx instance, and track it to close it
            this.vertxToClose = Vertx.factory.vertx();
            this.vertx = this.vertxToClose;
        } else {
            // use the provided instance
            this.vertxToClose = null;
            this.vertx = vertx;
        }
        this.endpoint = endpointSupplier.get();
        this.endpointSupplier = endpointSupplier;
        this.apiVersion = apiVersion;
    }

    protected void reconnect() {
        closeClient();
        getClient();
    }

    /**
     * Get the client, create a new one if necessary.
     *
     * @return The client to use.
     */
    protected WebClient getClient() {
        if (this.client == null) {
            this.client = createClient();
        }
        return this.client;
    }

    @Override
    public void close() {
        closeClient();
        if (this.vertxToClose != null) {
            this.vertxToClose.close();
        }
    }

    private void closeClient() {
        if (client != null) {
            this.client.close();
            this.client = null;
        }
    }

    protected WebClient createClient() {
        return WebClient.create(vertx, new WebClientOptions()
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
        responseHandler(ar, promise, (responseCode) -> responseCode == expectedCode, String.valueOf(expectedCode), warnMessage, throwException);
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
                    if (throwException) {
                        promise.completeExceptionally(new RuntimeException(body == null ? "null" : body.toString()));
                    } else {
                        promise.complete(ar.result().body());
                    }
                } else {
                    promise.complete(ar.result().body());
                }
            } else {
                log.warn("Request failed: {}", warnMessage, ar.cause());
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

}
