/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.apiclients.ApiClient;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

public class DeviceRegistryClient extends ApiClient {

    private static final String REGISTRATION_PATH = "registration";

    public DeviceRegistryClient(Kubernetes kubernetes, Endpoint endpoint) {
        super(kubernetes, () -> endpoint, "");
    }

    @Override
    protected String apiClientName() {
        return "iot-device-registry";
    }

    @Override
    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    public void registerDevice(String tenantId, String deviceId) throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        var requestPath = String.format("/%s/%s", REGISTRATION_PATH, tenantId);
        var body = new JsonObject();
        body.put("device-id", deviceId);
        log.info("POST-device: path {}; body {}", requestPath, body.toString());
        client.post(endpoint.getPort(), endpoint.getHost(), requestPath)
            .as(BodyCodec.jsonObject())
            .timeout(120000)
            .sendJsonObject(body,
                    ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_CREATED, "Error registering a device"));
        responsePromise.get(150000, TimeUnit.SECONDS);
    }

    public JsonObject getDeviceRegistration(String tenantId, String deviceId) throws Exception {
        return getDeviceRegistration(tenantId, deviceId, HttpURLConnection.HTTP_OK);
    }

    public JsonObject getDeviceRegistration(String tenantId, String deviceId, int expectedCode) throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        var requestPath = String.format("/%s/%s/%s", REGISTRATION_PATH, tenantId, deviceId);
        log.info("GET-device: path {};", requestPath);
        client.get(endpoint.getPort(), endpoint.getHost(), requestPath)
            .as(BodyCodec.jsonObject())
            .send(ar -> responseHandler(ar, responsePromise, expectedCode, "Error getting device registration", false));
        return responsePromise.get(150000, TimeUnit.SECONDS);
    }

    public void updateDeviceRegistration(String tenantId, String deviceId, JsonObject payload) throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        var requestPath = String.format("/%s/%s/%s", REGISTRATION_PATH, tenantId, deviceId);
        log.info("PUT-device: path {}; body {}", requestPath, payload.toString());
        client.put(endpoint.getPort(), endpoint.getHost(), requestPath)
            .as(BodyCodec.jsonObject())
            .timeout(120000)
            .sendJsonObject(payload,
                    ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_NO_CONTENT, "Error updating device registration"));
        responsePromise.get(150000, TimeUnit.SECONDS);
    }

    public void deleteDeviceRegistration(String tenantId, String deviceId) throws Exception {
        CompletableFuture<Buffer> responsePromise = new CompletableFuture<>();
        var requestPath = String.format("/%s/%s/%s", REGISTRATION_PATH, tenantId, deviceId);
        log.info("DELETE-device: path {};", requestPath);
        client.delete(endpoint.getPort(), endpoint.getHost(), requestPath)
            .send(ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_NO_CONTENT, "Error deleting device registration"));
        responsePromise.get(150000, TimeUnit.SECONDS);
    }

}
