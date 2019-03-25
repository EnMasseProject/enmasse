/*
 * Copyright 2018, EnMasse authors.
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

import io.enmasse.iot.model.v1.IoTCrd;
import io.enmasse.iot.model.v1.IoTProject;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Kubernetes;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.codec.BodyCodec;

public class IoTProjectApiClient extends ApiClient {

    private final static Logger log = CustomLogger.getLogger();
    private final int initRetry = 10;

    private static final String ALL_IOT_PROJECTS_PATH = String.format("/apis/%s/iotprojects", IoTCrd.API_VERSION);
    private final String iotProjectPath;

    public IoTProjectApiClient(Kubernetes kubernetes) {
        super(kubernetes, kubernetes::getRestEndpoint, IoTCrd.API_VERSION);
        this.iotProjectPath = String.format("/apis/%s/namespaces/%s/iotprojects", IoTCrd.API_VERSION, kubernetes.getNamespace());
    }

    public IoTProjectApiClient(Kubernetes kubernetes, String namespace) {
        super(kubernetes, kubernetes::getRestEndpoint, IoTCrd.API_VERSION);
        this.iotProjectPath = String.format("/apis/%s/namespaces/%s/iotprojects", IoTCrd.API_VERSION, namespace);
    }

    public IoTProjectApiClient(Kubernetes kubernetes, String namespace, String token) {
        super(kubernetes, kubernetes::getRestEndpoint, IoTCrd.API_VERSION, token);
        this.iotProjectPath = String.format("/apis/%s/namespaces/%s/iotprojects", IoTCrd.API_VERSION, namespace);
    }

	@Override
	protected String apiClientName() {
		return "iot-project";
	}

    public void close() {
        client.close();
        vertx.close();
    }

    private void createIoTProject(IoTProject iotProject, int expectedCode) throws Exception {

    	JsonObject json = new JsonObject(new ObjectMapper().writeValueAsString(iotProject));

        log.info("POST-iot-project: path {}; body {}", this.iotProjectPath, json.toString());
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), iotProjectPath)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(json, ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: create iot project '%s'", iotProject)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void createIoTProject(IoTProject iotProject) throws Exception {
    	createIoTProject(iotProject, HttpURLConnection.HTTP_CREATED);
    }

    public JsonObject getIoTProject(String name, int expectedCode) throws Exception {
        String path = this.iotProjectPath + "/" + name;
        log.info("GET-iot-project: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
                    client.get(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: get iot project %s", name)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public IoTProject getIoTProject(String name) throws Exception {
        return new ObjectMapper().readValue(getIoTProject(name, HttpURLConnection.HTTP_OK).encode(), IoTProject.class);
    }

    private void deleteIoTProject(String name, int expectedCode) throws Exception {
        String path = this.iotProjectPath + "/" + name;
        log.info("DELETE-iot-project: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.delete(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: delete iot project '%s'", name)));
                    return responsePromise.get(2, TimeUnit.MINUTES);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void deleteIoTProject(String name) throws Exception {
        deleteIoTProject(name, HttpURLConnection.HTTP_OK);
    }

    private JsonObject listIoTProjectsJson() throws Exception {
        log.info("GET-iot-project: path {}; endpoint {}; ", iotProjectPath, endpoint.toString());

        CompletableFuture<JsonObject> response = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
                    client.get(endpoint.getPort(), endpoint.getHost(), iotProjectPath)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar, response, HTTP_OK, "Error: get iot projects"));
                    return response.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public List<IoTProject> listIoTProjects() throws Exception {
        JsonArray items = listIoTProjectsJson().getJsonArray("items");
        List<IoTProject> configs = new ArrayList<>();
        if (items != null) {
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < items.size(); i++) {
                configs.add(mapper.readValue(items.getJsonObject(i).encode(), IoTProject.class));
            }
        }
        return configs;
    }

    public boolean existsIoTProject(String name) throws Exception {
        JsonArray items = listIoTProjectsJson().getJsonArray("items");
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                if(items.getJsonObject(i).getJsonObject("metadata").getString("name").equals(name)){
                    return true;
                }
            }
        }
        return false;
    }

    public JsonObject getAllIoTProjects(final int expectedCode) throws Exception {
        log.info("GET-all-iot-projects: path {}; ", ALL_IOT_PROJECTS_PATH);
        return doRequestNTimes(initRetry, () -> {
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
            HttpRequest<JsonObject> request = client.get(endpoint.getPort(), endpoint.getHost(), ALL_IOT_PROJECTS_PATH)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject())
                    .timeout(20_000);
            request.send(ar -> responseHandler(ar, responsePromise, expectedCode, "Error: get all iotprojects"));
            return responsePromise.get(30, TimeUnit.SECONDS);
        }, Optional.of(() -> kubernetes.getRestEndpoint()), Optional.empty());
    }

    public List<IoTProject> listAllIoTProjects() throws Exception {
        JsonArray items = getAllIoTProjects(HTTP_OK).getJsonArray("items");
        List<IoTProject> configs = new ArrayList<>();
        if (items != null) {
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < items.size(); i++) {
                configs.add(mapper.readValue(items.getJsonObject(i).encode(), IoTProject.class));
            }
        }
        return configs;
    }

}
