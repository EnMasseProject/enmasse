/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static io.enmasse.systemtest.iot.MessageType.EVENT;
import static io.enmasse.systemtest.iot.MessageType.TELEMETRY;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.time.Duration.ofSeconds;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.ws.rs.core.HttpHeaders;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.apiclients.ApiClient;
import io.enmasse.systemtest.apiclients.Predicates;
import io.enmasse.systemtest.iot.MessageSendTester.Sender;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class HttpAdapterClient extends ApiClient {

    protected static Logger log = CustomLogger.getLogger();

    public HttpAdapterClient(Kubernetes kubernetes, Endpoint endpoint, String deviceAuthId, String tenantId, String password) {
        super(kubernetes, () -> endpoint, "");
        this.authzString = getBasicAuth(deviceAuthId + "@" + tenantId, password);
    }

    @Override
    protected String apiClientName() {
        return "iot-http-adapter";
    }

    @Override
    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    @Override
    public void close () {
        this.client.close();
    }

    private static String contentType (final JsonObject payload) {
        return payload != null ? ContentType.APPLICATION_JSON.getMimeType() : "application/vnd.eclipse-hono-empty-notification";
    }

    public HttpResponse<?> send(MessageType messageType, JsonObject payload, Predicate<Integer> expectedCodePredicate, Consumer<HttpRequest<?>> requestCustomizer,
            Duration responseTimeout) throws Exception {
        return send(messageType, null, payload, expectedCodePredicate, requestCustomizer, responseTimeout);
    }

    public HttpResponse<?> send(MessageType messageType, String pathSuffix, JsonObject payload, Predicate<Integer> expectedCodePredicate,
            Consumer<HttpRequest<?>> requestCustomizer, Duration responseTimeout) throws Exception {

        CompletableFuture<HttpResponse<?>> responsePromise = new CompletableFuture<>();
        var ms = responseTimeout.toMillis();

        log.info("POST-{}: body {}", messageType.name().toLowerCase(), payload);

        // create new request

        var path = messageType.path();
        if (pathSuffix != null) {
            path += pathSuffix;
        }
        var request = client.post(endpoint.getPort(), endpoint.getHost(), path)
                .putHeader(HttpHeaders.AUTHORIZATION, authzString)
                .putHeader(HttpHeaders.CONTENT_TYPE, contentType(payload))
                .timeout(ms);

        // allow to customize request

        if (requestCustomizer != null) {
            requestCustomizer.accept(request);
        }

        // execute request with payload

        request.sendJsonObject(payload, ar -> {

            // if the request failed ...
            if (!ar.succeeded()) {
                // ... fail the response promise
                responsePromise.completeExceptionally(ar.cause());
                return;
            }

            final CompletableFuture<Buffer> nf = new CompletableFuture<>();
            log.debug("POST-{}: body {} -> {} {}", messageType.name().toLowerCase(), payload, ar.result().statusCode(), ar.result().statusMessage() );
            if ( ar.failed() ) {
                log.debug("Request failed", ar.cause());
                nf.completeExceptionally(ar.cause());
            } else {
                var response = ar.result();
                var code =  response.statusCode();
                log.info("POST: code {} -> {}", code, response.bodyAsString());
                if ( !expectedCodePredicate.test(code)) {
                    nf.completeExceptionally(new RuntimeException(String.format("Did not match expected status: %s - was: %s", expectedCodePredicate, code)));
                } else {
                    nf.complete(response.body());
                }
            }

            // use the result from the responseHandler
            // and map it to the responsePromise
            nf.whenComplete((res, err) -> {
                if (err != null) {
                    responsePromise.completeExceptionally(err);
                } else {
                    responsePromise.complete(ar.result());
                }
            });
        });

        // the next line gives the timeout a bit extra, as the HTTP timeout should
        // kick in, we would prefer the timeout via the future.
        return responsePromise.get(((long) (ms * 1.1)), TimeUnit.MILLISECONDS);
    }

    /**
     * Send method suitable for using as {@link Sender}.
     */
    public boolean send(MessageSendTester.Type type, JsonObject payload, Duration timeout) {
        return sendDefault(type.type(), payload, timeout);
    }

    private boolean sendDefault(MessageType type, JsonObject payload, Duration timeout) {
        try {
            send(type, payload, Predicates.is(HTTP_ACCEPTED), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public HttpResponse<?> send(MessageType type, JsonObject payload, Predicate<Integer> expectedCodePredicate, Duration timeout) throws Exception {
        return send(type, payload, expectedCodePredicate, null, timeout);
    }

    public HttpResponse<?> send(MessageType type, JsonObject payload, Predicate<Integer> expectedCodePredicate) throws Exception {
        return send(type, payload, expectedCodePredicate, null, ofSeconds(15));
    }

    public HttpResponse<?> sendTelemetry(JsonObject payload, Predicate<Integer> expectedCodePredicate) throws Exception {
        return send(TELEMETRY, payload, expectedCodePredicate);
    }

    public HttpResponse<?> sendEvent(JsonObject payload, Predicate<Integer> expectedCodePredicate) throws Exception {
        return send(EVENT, payload, expectedCodePredicate);
    }

    private String getBasicAuth(final String user, final String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

}
