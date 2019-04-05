/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import static java.net.HttpURLConnection.HTTP_OK;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.enmasse.iot.model.v1.IoTConfig;
import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Kubernetes;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;

public class IoTConfigApiClient extends ApiClient {

    private final static Logger log = CustomLogger.getLogger();
    private final int initRetry = 10;

    private final String iotConfigPath;

    public IoTConfigApiClient(Kubernetes kubernetes) {
        super(kubernetes, kubernetes::getRestEndpoint, IoTCrd.API_VERSION);
        this.iotConfigPath = String.format("/apis/%s/namespaces/%s/iotconfigs", IoTCrd.API_VERSION, kubernetes.getNamespace());
    }

    public IoTConfigApiClient(Kubernetes kubernetes, String namespace) {
        super(kubernetes, kubernetes::getRestEndpoint, IoTCrd.API_VERSION);
        this.iotConfigPath = String.format("/apis/%s/namespaces/%s/iotconfigs", IoTCrd.API_VERSION, namespace);
    }

    public IoTConfigApiClient(Kubernetes kubernetes, String namespace, String token) {
        super(kubernetes, kubernetes::getRestEndpoint, IoTCrd.API_VERSION, token);
        this.iotConfigPath = String.format("/apis/%s/namespaces/%s/iotconfigs", IoTCrd.API_VERSION, namespace);
    }

    @Override
    protected String apiClientName() {
        return "iot-config";
    }

    public void close() {
        client.close();
        vertx.close();
    }

    private void createIoTConfig(IoTConfig ioTConfig, int expectedCode) throws Exception {

        JsonObject json = new JsonObject(new ObjectMapper().writeValueAsString(ioTConfig));

        log.info("POST-iot-config: path {}; body {}", this.iotConfigPath, json.toString());
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), iotConfigPath)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(json, ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: create iot config '%s'", ioTConfig)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void createIoTConfig(IoTConfig ioTConfig) throws Exception {
        createIoTConfig(ioTConfig, HttpURLConnection.HTTP_CREATED);
    }

    public JsonObject getIoTConfig(String name, int expectedCode) throws Exception {
        String path = this.iotConfigPath + "/" + name;
        log.info("GET-iot-config: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
                    client.get(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: get iot config %s", name)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public IoTConfig getIoTConfig(String name) throws Exception {
        return new ObjectMapper().readValue(getIoTConfig(name, HttpURLConnection.HTTP_OK).encode(), IoTConfig.class);
    }

    private void deleteIoTConfig(String name, int expectedCode) throws Exception {
        String path = this.iotConfigPath + "/" + name;
        log.info("DELETE-iot-config: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.delete(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: delete iot config '%s'", name)));
                    return responsePromise.get(2, TimeUnit.MINUTES);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void deleteIoTConfig(String name) throws Exception {
        deleteIoTConfig(name, HttpURLConnection.HTTP_OK);
    }

    private JsonObject listIoTConfigsJson() throws Exception {
        log.info("GET-iot-config: path {}; endpoint {}; ", iotConfigPath, endpoint.toString());

        CompletableFuture<JsonObject> response = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
                    client.get(endpoint.getPort(), endpoint.getHost(), iotConfigPath)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar, response, HTTP_OK, "Error: get iot configs"));
                    return response.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public List<IoTConfig> listIoTConfigs() throws Exception {
        JsonArray items = listIoTConfigsJson().getJsonArray("items");
        List<IoTConfig> configs = new ArrayList<>();
        if (items != null) {
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < items.size(); i++) {
                configs.add(mapper.readValue(items.getJsonObject(i).encode(), IoTConfig.class));
            }
        }
        return configs;
    }

    public boolean existsIoTConfig(String name) throws Exception {
        JsonArray items = listIoTConfigsJson().getJsonArray("items");
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                if(items.getJsonObject(i).getJsonObject("metadata").getString("name").equals(name)){
                    return true;
                }
            }
        }
        return false;
    }

}
