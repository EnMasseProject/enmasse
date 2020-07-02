/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static io.enmasse.systemtest.iot.MessageType.EVENT;
import static io.enmasse.systemtest.iot.MessageType.TELEMETRY;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.time.Duration.ofSeconds;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.ws.rs.core.HttpHeaders;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import com.google.common.base.Throwables;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.apiclients.ApiClient;
import io.enmasse.systemtest.iot.MessageSendTester.Sender;
import io.enmasse.systemtest.logs.CustomLogger;
import io.enmasse.systemtest.utils.Predicates;
import io.enmasse.systemtest.utils.VertxUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class HttpAdapterClient extends ApiClient {

    @SuppressWarnings("serial")
    public static class ResponseException extends RuntimeException {

        private int statusCode;

        public ResponseException(final String message, final int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return this.statusCode;
        }

        public static Predicate<Throwable> instanceOf() {
            return t -> Throwables.getRootCause(t) instanceof ResponseException;
        }

        public static Predicate<Throwable> and(final Predicate<ResponseException> filter) {
            return instanceOf().and(t -> filter.test((ResponseException) Throwables.getRootCause(t)));
        }

        public static Predicate<Throwable> statusCode(int code) {
            return and(t -> t.getStatusCode() == code);
        }

    }

    protected static final Logger log = CustomLogger.getLogger();

    private static final Predicate<? super Throwable> DEFAULT_EXCEPTION_FILTER = x -> false;

    private final Set<String> tlsVersions;
    private final Buffer keyStoreBuffer;
    private final String authzString;

    private Predicate<? super Throwable> exceptionFilter = DEFAULT_EXCEPTION_FILTER;

    public HttpAdapterClient(final Vertx vertx, final Endpoint endpoint, final PrivateKey key, final X509Certificate certificate, final Set<String> tlsVersions) throws Exception {
        this(vertx, endpoint, null, null, null, key, certificate, tlsVersions);
    }

    public HttpAdapterClient(final Vertx vertx, final Endpoint endpoint, final String deviceAuthId, final String tenantId, final String password, final Set<String> tlsVersions) throws Exception {
        this(vertx, endpoint, deviceAuthId, tenantId, password, null, null, tlsVersions);
    }

    public HttpAdapterClient(final Vertx vertx, final Endpoint endpoint, final String deviceAuthId, final String tenantId, final String password) throws Exception {
        this(vertx, endpoint, deviceAuthId, tenantId, password, null, null, Collections.emptySet());
    }

    private HttpAdapterClient(
            final Vertx vertx,
            final Endpoint endpoint,
            final String deviceAuthId, final String tenantId, final String password,
            final PrivateKey key, final X509Certificate certificate,
            final Set<String> tlsVersions) throws Exception {

        super(vertx, () -> endpoint, "");

        this.tlsVersions = tlsVersions;

        if (deviceAuthId != null && tenantId != null && password != null) {
            this.authzString = getBasicAuth(deviceAuthId + "@" + tenantId, password);
        } else {
            this.authzString = null;
        }

        if (key != null && certificate != null) {
            this.keyStoreBuffer = Buffer.buffer(KeyStoreCreator.toByteArray(key, certificate));
        } else {
            this.keyStoreBuffer = null;
        }

    }

    private static String contentType(final Buffer payload) {
        return payload != null ? ContentType.APPLICATION_JSON.getMimeType() : "application/vnd.eclipse-hono-empty-notification";
    }

    @Override
    protected String apiClientName() {
        return "iot-http-adapter";
    }

    @Override
    protected WebClient createClient() {
        var options = new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false);

        VertxUtils.applyTlsVersions(options, this.tlsVersions);

        if (this.keyStoreBuffer != null) {
            options.setPfxKeyCertOptions(
                    new PfxOptions()
                            .setValue(this.keyStoreBuffer)
                            .setPassword(KeyStoreCreator.KEY_PASSWORD));
        }

        return WebClient.create(vertx, options);
    }

    public HttpResponse<?> send(MessageType messageType, Buffer payload, Predicate<Integer> expectedCodePredicate, Consumer<HttpRequest<?>> requestCustomizer,
            Duration responseTimeout) throws Exception {
        return send(messageType, null, payload, expectedCodePredicate, requestCustomizer, responseTimeout);
    }

    public HttpResponse<?> send(MessageType messageType, String pathSuffix, Buffer payload, Predicate<Integer> expectedCodePredicate,
            Consumer<HttpRequest<?>> requestCustomizer, Duration responseTimeout) throws Exception {

        final CompletableFuture<HttpResponse<?>> responsePromise = new CompletableFuture<>();
        var ms = responseTimeout.toMillis();

        log.info("POST-{}: body {}", messageType.name().toLowerCase(), payload);

        // create new request

        var path = messageType.path();
        if (pathSuffix != null) {
            path += pathSuffix;
        }
        var request = getClient().post(endpoint.getPort(), endpoint.getHost(), path)
                .putHeader(HttpHeaders.CONTENT_TYPE, contentType(payload))
                // we send with QoS 1 by default, to get some feedback
                .putHeader("QoS-Level", "1")
                .timeout(ms);

        if (this.authzString != null) {
            request = request.putHeader(AUTHORIZATION, authzString);
        }

        // allow to customize request

        if (requestCustomizer != null) {
            requestCustomizer.accept(request);
        }

        // execute request with payload

        request.sendBuffer(payload, ar -> {

            // if the request failed ...
            if (ar.failed()) {
                log.info("Request failed", ar.cause());
                // ... fail the response promise
                responsePromise.completeExceptionally(ar.cause());
                return;
            }

            log.debug("POST-{}: body {} -> {} {}", messageType.name().toLowerCase(), payload, ar.result().statusCode(), ar.result().statusMessage());

            var response = ar.result();
            var code = response.statusCode();

            log.info("POST: code {} -> {}", code, response.bodyAsString());
            if (!expectedCodePredicate.test(code)) {
                responsePromise.completeExceptionally(new ResponseException(String.format("Did not match expected status: %s - was: %s", expectedCodePredicate, code), code));
            } else {
                responsePromise.complete(ar.result());
            }

        });

        // the next line gives the timeout a bit of extra time, as the HTTP timeout should
        // kick in, we would prefer that over the timeout via the future.
        return responsePromise.get(((long) (ms * 1.1)), TimeUnit.MILLISECONDS);
    }

    /**
     * Send method suitable for using as {@link Sender}.
     */
    public boolean send(MessageSendTester.Type type, Buffer payload, Duration timeout) {
        return sendDefault(type.type(), payload, timeout);
    }

    private boolean sendDefault(MessageType type, Buffer payload, Duration timeout) {
        try {
            send(type, payload, Predicates.is(HTTP_ACCEPTED), timeout);
            return true;
        } catch (Exception e) {
            logException("Failed to send message", e);
            return false;
        }
    }

    public HttpResponse<?> send(MessageType type, Buffer payload, Predicate<Integer> expectedCodePredicate, Duration timeout) throws Exception {
        return send(type, payload, expectedCodePredicate, null, timeout);
    }

    public HttpResponse<?> send(MessageType type, Buffer payload, Predicate<Integer> expectedCodePredicate) throws Exception {
        return send(type, payload, expectedCodePredicate, null, ofSeconds(15));
    }

    public HttpResponse<?> sendTelemetry(Buffer payload, Predicate<Integer> expectedCodePredicate) throws Exception {
        return send(TELEMETRY, payload, expectedCodePredicate);
    }

    public HttpResponse<?> sendEvent(Buffer payload, Predicate<Integer> expectedCodePredicate) throws Exception {
        return send(EVENT, payload, expectedCodePredicate);
    }

    private static String getBasicAuth(final String user, final String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Set a filter to suppress certain (expected) exceptions.
     *
     * @param filter The filter to set, may be {@code null} to not suppress any exception.
     * @return This instance, for chaining calls.
     */
    public HttpAdapterClient suppressExceptions(final Predicate<Throwable> filter) {
        this.exceptionFilter = filter != null ? filter : DEFAULT_EXCEPTION_FILTER;
        return this;
    }

    private void logException(final String message, final Throwable e) {
        if (this.exceptionFilter.test(e)) {
            log.debug(message, e);
        } else {
            log.info(message, e);
        }
    }

    public static Predicate<Throwable> causedBy(final Class<? extends Throwable> clazz) {
        Objects.requireNonNull(clazz);
        return t -> clazz.isInstance(Throwables.getRootCause(t));
    }
}
