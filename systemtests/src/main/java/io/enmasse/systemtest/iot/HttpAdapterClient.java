/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.ws.rs.core.HttpHeaders;

import org.apache.http.entity.ContentType;
import org.slf4j.Logger;

import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.apiclients.ApiClient;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class HttpAdapterClient extends ApiClient {

    protected static Logger log = CustomLogger.getLogger();

    private static final String TELEMETRY_PATH = "/telemetry";
    private static final String EVENT_PATH = "/event";

    public HttpAdapterClient(Kubernetes kubernetes, Endpoint endpoint, String username, String password) {
        super(kubernetes, () -> endpoint, "");
        this.authzString = getBasicAuth(username, password);
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

    public void sendTelemetry(JsonObject payload, Predicate<Integer> expectedCodePredicate, String expectedCodeOrCodes) throws Exception {
        CompletableFuture<Buffer> responsePromise = new CompletableFuture<>();
        log.info("POST-telemetry: body {}", payload.toString());
        client.post(endpoint.getPort(), endpoint.getHost(), TELEMETRY_PATH)
            .putHeader(HttpHeaders.AUTHORIZATION, authzString)
            .putHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
            .timeout(120000)
            .sendJsonObject(payload,
                    ar -> responseHandler(ar, responsePromise, expectedCodePredicate, expectedCodeOrCodes, "Error sending telemetry data", false));
        responsePromise.get(150000, TimeUnit.SECONDS);
    }

    public void sendEvent(JsonObject payload, Predicate<Integer> expectedCodePredicate, String expectedCodeOrCodes) throws Exception {
        CompletableFuture<Buffer> responsePromise = new CompletableFuture<>();
        log.info("POST-event: body {}", payload.toString());
        client.post(endpoint.getPort(), endpoint.getHost(), EVENT_PATH)
            .putHeader(HttpHeaders.AUTHORIZATION, authzString)
            .putHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
            .timeout(120000)
            .sendJsonObject(payload,
                    ar -> responseHandler(ar, responsePromise, expectedCodePredicate, expectedCodeOrCodes, "Error sending event data", false));
        responsePromise.get(150000, TimeUnit.SECONDS);
    }

    private String getBasicAuth(final String user, final String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

}
