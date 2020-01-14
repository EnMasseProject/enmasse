/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import com.google.common.net.HttpHeaders;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.apiclients.ApiClient;
import io.enmasse.systemtest.logs.CustomLogger;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class HonoApiClient extends ApiClient {

    private static final Logger log = CustomLogger.getLogger();

    protected HonoApiClient(final Supplier<Endpoint> endpointSupplier) {
        super(endpointSupplier, "");
    }

    @Override
    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    protected HttpResponse<Buffer> execute (final HttpMethod method, final String requestPath, final String body) throws Exception {
        final CompletableFuture<HttpResponse<Buffer>> responsePromise = new CompletableFuture<>();
        log.info("{}-{}: path {}; body {}", method, apiClientName(), requestPath, body);
        client.request(method, endpoint.getPort(), endpoint.getHost(), requestPath)
            .as(BodyCodec.buffer())
            .timeout(120000)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .putHeader(HttpHeaders.AUTHORIZATION, authzString)
            .sendBuffer(Optional.ofNullable(body).map(Buffer::buffer).orElse(null),
                    ar -> {
                        if ( ar.succeeded() ) {
                            logResult(ar.result());
                            responsePromise.complete(ar.result());
                        } else {
                            responsePromise.completeExceptionally(ar.cause());
                        }
                    });
        return responsePromise.get(150000, TimeUnit.MILLISECONDS);
    }

    private void logResult(final HttpResponse<Buffer> result) {
        log.info("result - code: {}, headers: {}, body: {}",
                result.statusCode(),
                result
                    .headers().entries().stream()
                    .map(e -> String.format("'%s' -> '%s'", e.getKey(), e.getValue()))
                    .collect(Collectors.joining(", ")),
                result.bodyAsString());
    }

    protected Buffer execute (final HttpMethod method, final String requestPath, final String body, int expectedStatusCode, String failureMessage) throws Exception {
        final CompletableFuture<Buffer> responsePromise = new CompletableFuture<>();
        log.info("{}-{}: path {}; body {}", method, apiClientName(), requestPath, body);
        client.request(method, endpoint.getPort(), endpoint.getHost(), requestPath)
            .as(BodyCodec.buffer())
            .timeout(120000)
            .putHeader(HttpHeaders.AUTHORIZATION, authzString)
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .sendBuffer(Optional.ofNullable(body).map(Buffer::buffer).orElse(null),
                    ar -> responseHandler(ar, responsePromise, expectedStatusCode, failureMessage, false));
        return responsePromise.get(150000, TimeUnit.SECONDS);
    }

}
