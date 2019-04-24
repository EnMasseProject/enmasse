/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import java.net.HttpURLConnection;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.apiclients.ApiClient;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

public class CredentialsRegistryClient extends ApiClient {

    private static final String CREDENTIALS_PATH = "credentials";

    public CredentialsRegistryClient(Kubernetes kubernetes, Endpoint endpoint) {
        super(kubernetes, () -> endpoint, "");
    }

    @Override
    protected String apiClientName() {
        return "iot-credentials-registry";
    }

    @Override
    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    public void addCredentials(String tenantId, String deviceId, String authId, String password) throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        var requestPath = String.format("/%s/%s", CREDENTIALS_PATH, tenantId);
        var body = createCredentialsObject(deviceId, authId, password, null);
        log.info("POST-credentials: path {}; body {}", requestPath, body.toString());
        client.post(endpoint.getPort(), endpoint.getHost(), requestPath)
            .as(BodyCodec.jsonObject())
            .timeout(120000)
            .sendJsonObject(body,
                    ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_CREATED, "Error adding credentials to device"));
        responsePromise.get(150000, TimeUnit.SECONDS);
    }

    public JsonObject getCredentials(String tenantId, String deviceId) throws Exception {
        return getCredentials(tenantId, deviceId, HttpURLConnection.HTTP_OK);
    }

    public JsonObject getCredentials(String tenantId, String deviceId, int expectedCode) throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        var requestPath = String.format("/%s/%s/%s", CREDENTIALS_PATH, tenantId, deviceId);
        log.info("GET-credentials: path {};", requestPath);
        client.get(endpoint.getPort(), endpoint.getHost(), requestPath)
            .as(BodyCodec.jsonObject())
            .send(ar -> responseHandler(ar, responsePromise, expectedCode, "Error getting credentials of device", false));
        return responsePromise.get(150000, TimeUnit.SECONDS);
    }

    public void updateCredentials(String tenantId, String deviceId, String authId, String password, Instant notAfter) throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        var requestPath = String.format("/%s/%s/%s/%s", CREDENTIALS_PATH, tenantId, authId, "hashed-password");
        JsonObject payload = createCredentialsObject(deviceId, authId, password, notAfter);
        log.info("PUT-credentials: path {}; body {}", requestPath, payload.toString());
        client.put(endpoint.getPort(), endpoint.getHost(), requestPath)
            .as(BodyCodec.jsonObject())
            .timeout(120000)
            .sendJsonObject(payload,
                    ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_NO_CONTENT, "Error updating device registration"));
        responsePromise.get(150000, TimeUnit.SECONDS);
    }

    public void deleteAllCredentials(String tenantId, String deviceId) throws Exception {
        CompletableFuture<Buffer> responsePromise = new CompletableFuture<>();
        var requestPath = String.format("/%s/%s/%s", CREDENTIALS_PATH, tenantId, deviceId);
        log.info("DELETE-credentials: path {}", requestPath);
        client.delete(endpoint.getPort(), endpoint.getHost(), requestPath)
            .send(ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_NO_CONTENT, "Error deleting all credentials of device"));
        responsePromise.get(150000, TimeUnit.SECONDS);
    }

    private JsonObject createCredentialsObject(String deviceId, String authId, String password, Instant notAfter) {
        var body = new JsonObject();
        body.put("device-id", deviceId);
        body.put("type", "hashed-password");
        body.put("auth-id", authId);

        JsonObject secret = new JsonObject(new HashMap<>(Map.of("hash-function", "sha-512", "pwd-plain", password)));
        if(notAfter != null) {
            secret.put("not-after", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(notAfter.atOffset(ZoneOffset.UTC)));
        }

        body.put("secrets", new JsonArray(List.of(secret)));
        return body;
    }
}
