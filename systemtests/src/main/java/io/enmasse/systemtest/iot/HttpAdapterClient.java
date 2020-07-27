/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.systemtest.iot;

import com.google.common.base.Throwables;
import io.enmasse.iot.utils.MoreFutures;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.apiclients.ApiClient;
import io.enmasse.systemtest.framework.LoggerUtils;
import io.enmasse.systemtest.utils.Predicates;
import io.enmasse.systemtest.utils.VertxUtils;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import javax.ws.rs.core.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static io.vertx.core.Future.failedFuture;
import static io.vertx.core.Future.succeededFuture;
import static java.lang.System.lineSeparator;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.time.Duration.ofMillis;

public class HttpAdapterClient extends ApiClient {

    @SuppressWarnings("serial")
    public static class ResponseException extends RuntimeException {

        private final int statusCode;

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

    protected static final Logger log = LoggerUtils.getLogger();

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

    public Future<HttpResponse<?>> sendAsync(
            final MessageType messageType,
            final Buffer payload,
            final Duration timeout,
            final Consumer<HttpRequest<?>> requestCustomizer
    ) {
        return sendAsync(messageType, null, payload, timeout, requestCustomizer);
    }

    public Future<HttpResponse<?>> sendAsync(
            final MessageType messageType,
            final String pathSuffix,
            final Buffer payload,
            final Duration timeout,
            final Consumer<HttpRequest<?>> requestCustomizer
    ) {

        // create new request

        final String path;
        if (pathSuffix != null) {
            path = messageType.path() + pathSuffix;
        } else {
            path = messageType.path();
        }

        log.info("POST(pre) - path: {}, body '{}'", path, payload);

        var request = getClient().post(this.endpoint.getPort(), this.endpoint.getHost(), path)
                .putHeader(HttpHeaders.CONTENT_TYPE, contentType(payload))
                // we send with QoS 1 by default, to get some feedback
                .putHeader("QoS-Level", "1")
                .timeout(timeout.toMillis());

        if (this.authzString != null) {
            request = request.putHeader(AUTHORIZATION, authzString);
        }

        // allow to customize request

        if (requestCustomizer != null) {
            requestCustomizer.accept(request);
        }

        log.info("POST(pre) - path: {}, headers: {}{}", path, lineSeparator(), request.headers());

        // result promise

        final Promise<HttpResponse<Buffer>> result = Promise.promise();

        // execute request with payload

        request.sendBuffer(payload, result);

        // return result

        return result.future()
                .onFailure(cause -> {
                    log.info("Request failed", cause);
                })
                .onSuccess(r -> {
                    log.info("POST(completed): path: {}, body '{}' -> {} {}", path, payload, r.statusCode(), r.statusMessage());
                })
                .map(x -> x);

    }

    public HttpResponse<?> send(MessageType messageType, String pathSuffix, Buffer payload, Predicate<Integer> expectedCodePredicate,
                                Consumer<HttpRequest<?>> requestCustomizer, Duration responseTimeout) throws Exception {

        var f = sendAsync(messageType, pathSuffix, payload, responseTimeout, requestCustomizer)
                .flatMap(response -> {
                    var code = response.statusCode();

                    log.info("POST(assert): code {} -> {}", code, response.bodyAsString());
                    if (expectedCodePredicate.test(code)) {
                        return succeededFuture(response);
                    } else {
                        return failedFuture(new ResponseException(String.format("Did not match expected status: %s - was: %s", expectedCodePredicate, code), code));
                    }
                });

        // the next line gives the timeout a bit of extra time, as the HTTP timeout should
        // kick in, we would prefer that over the timeout via the future.
        return MoreFutures.await(f, ofMillis((long) (responseTimeout.toMillis() * 1.1)));
    }

    /**
     * Send method suitable for using as {@link MessageSendTester.Sender}.
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
