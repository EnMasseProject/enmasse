/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest;

import com.sun.jndi.toolkit.url.Uri;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AddressApiClient {
    private static Logger log = CustomLogger.getLogger();
    private final WebClient client;
    private final Kubernetes kubernetes;
    private final Vertx vertx;
    private final int initRetry = 10;
    private final String schemaPath = "/apis/enmasse.io/v1/schema";
    private final String addressSpacesPath = "/apis/enmasse.io/v1/addressspaces";
    private final String addressPath = "/apis/enmasse.io/v1/addresses";
    private final String authzString;
    private Endpoint endpoint;

    public AddressApiClient(Kubernetes kubernetes) {
        this.vertx = VertxFactory.create();
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                // TODO: Fetch CA and use
                .setTrustAll(true)
                .setVerifyHost(false));
        this.kubernetes = kubernetes;
        this.endpoint = kubernetes.getRestEndpoint();
        this.authzString = "Bearer " + kubernetes.getApiToken();
    }

    public void close() {
        client.close();
        vertx.close();
    }


    public void createAddressSpaceList(AddressSpace... addressSpaces) throws Exception {
        for (AddressSpace addressSpace : addressSpaces) {
            createAddressSpace(addressSpace);
        }
    }

    private JsonObject createAddressSpaceMetadata(AddressSpace addressSpace) {
        JsonObject metadata = new JsonObject();
        metadata.put("name", addressSpace.getName());
        if (addressSpace.getNamespace() != null) {
            metadata.put("namespace", addressSpace.getNamespace());
        }
        return metadata;
    }

    private JsonObject createAddressSpaceSpec(AddressSpace addressSpace) {
        JsonObject spec = new JsonObject();
        spec.put("type", addressSpace.getType().toString().toLowerCase());
        spec.put("plan", addressSpace.getPlan());
        JsonObject authService = new JsonObject();
        authService.put("type", addressSpace.getAuthService().toString());
        spec.put("authenticationService", authService);
        if (!addressSpace.getEndpoints().isEmpty()) {
            spec.put("endpoints", createAddressSpaceEndpoints(addressSpace));
        }
        return spec;
    }

    private JsonArray createAddressSpaceEndpoints(AddressSpace addressSpace) {
        JsonArray endpointsJson = new JsonArray();
        for (AddressSpaceEndpoint endpoint : addressSpace.getEndpoints()) {
            JsonObject endpointJson = new JsonObject();
            endpointJson.put("name", endpoint.getName());
            endpointJson.put("service", endpoint.getService());
            endpointsJson.add(endpointJson);
        }
        return endpointsJson;
    }

    public void createAddressSpace(AddressSpace addressSpace) throws Exception {
        JsonObject config = new JsonObject();
        config.put("apiVersion", "v1");
        config.put("kind", "AddressSpace");
        config.put("metadata", createAddressSpaceMetadata(addressSpace));
        config.put("spec", createAddressSpaceSpec(addressSpace));

        log.info("POST-address-space: path {}; body {}", addressSpacesPath, config.toString());
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

        doRequestNTimes(initRetry, () -> {
            client.post(endpoint.getPort(), endpoint.getHost(), addressSpacesPath)
                    .timeout(20_000)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject())
                    .sendJsonObject(config, ar -> responseHandler(ar,
                            responsePromise,
                            String.format("Error: create address space '%s'", addressSpace)));
            return responsePromise.get(30, TimeUnit.SECONDS);
        });
    }

    public void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        String path = addressSpacesPath + "/" + addressSpace.getName();
        log.info("DELETE-address-space: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
            client.delete(endpoint.getPort(), endpoint.getHost(), path)
                    .as(BodyCodec.jsonObject())
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .timeout(20_000)
                    .send(ar -> responseHandler(ar,
                            responsePromise,
                            String.format("Error: delete address space '%s'", addressSpace)));
            return responsePromise.get(2, TimeUnit.MINUTES);
        });
    }

    /**
     * get address space by address space name vie rest api
     *
     * @param name name of address space
     * @return
     * @throws InterruptedException
     */
    public JsonObject getAddressSpace(String name) throws Exception {
        String path = addressSpacesPath + "/" + name;
        log.info("GET-address-space: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
            client.get(endpoint.getPort(), endpoint.getHost(), path)
                    .as(BodyCodec.jsonObject())
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .send(ar -> responseHandler(ar,
                            responsePromise,
                            String.format("Error: get address space {}", name)));
            return responsePromise.get(30, TimeUnit.SECONDS);
        });
    }

    public Set<String> listAddressSpaces() throws Exception {
        JsonArray items = listAddressSpacesObjects().getJsonArray("items");
        Set<String> spaces = new HashSet<>();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                spaces.add(items.getJsonObject(i).getJsonObject("metadata").getString("name"));
            }
        }
        return spaces;
    }

    public JsonObject listAddressSpacesObjects() throws Exception {
        log.info("GET-address-spaces: path {}; endpoint {}; ", addressSpacesPath, endpoint.toString());

        CompletableFuture<JsonObject> response = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
            client.get(endpoint.getPort(), endpoint.getHost(), addressSpacesPath)
                    .as(BodyCodec.jsonObject())
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .timeout(20_000)
                    .send(ar -> responseHandler(ar, response, "Error: get address spaces"));
            return response.get(30, TimeUnit.SECONDS);
        });
    }

    public JsonArray getAddressesPaths() throws Exception {
        log.info("GET-addresses-paths: path {}; ", addressPath);
        return doRequestNTimes(initRetry, () -> {
            CompletableFuture<JsonArray> responsePromise = new CompletableFuture<>();
            client.get(endpoint.getPort(), endpoint.getHost(), addressPath)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonArray())
                    .timeout(20_000)
                    .send(ar -> responseHandler(ar, responsePromise, "Error: get addresses path"));
            return responsePromise.get(30, TimeUnit.SECONDS);
        });
    }

    /**
     * give you JsonObject with AddressesList or Address kind
     *
     * @param addressSpace name of instance, this is used only if isMultitenant is set to true
     * @param addressName  name of address
     * @return
     * @throws Exception
     */
    public JsonObject getAddresses(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        StringBuilder path = new StringBuilder();
        path.append(addressPath).append("/").append(addressSpace.getName());
        path.append(addressName.isPresent() ? "/" + addressName.get() : "");
        log.info("GET-addresses: path {}; ", path);

        return doRequestNTimes(initRetry, () -> {
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
            client.get(endpoint.getPort(), endpoint.getHost(), path.toString())
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject())
                    .timeout(20_000)
                    .send(ar -> responseHandler(ar, responsePromise, "Error: get addresses"));
            return responsePromise.get(30, TimeUnit.SECONDS);
        });
    }

    /**
     * give you JsonObject with Schema
     *
     * @return
     * @throws Exception
     */
    public JsonObject getSchema() throws Exception {
        log.info("GET-schema: path {}; ", schemaPath);

        return doRequestNTimes(initRetry, () -> {
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
            client.get(endpoint.getPort(), endpoint.getHost(), schemaPath)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject())
                    .timeout(20_000)
                    .send(ar -> responseHandler(ar, responsePromise, "Error: get addresses"));
            return responsePromise.get(30, TimeUnit.SECONDS);
        });
    }

    /**
     * delete addresses via reset api
     *
     * @param addressSpace address space
     * @param destinations variable count of destinations that you can delete
     * @throws Exception
     */
    public void deleteAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        StringBuilder path = new StringBuilder();
        if (destinations.length == 0) {
            path.append(addressPath).append("/").append(addressSpace.getName());
            doDelete(path.toString());
        } else {
            for (Destination destination : destinations) {
                path.append(addressPath).append("/").append(addressSpace.getName()).append("/").append(destination.getName());
                doDelete(path.toString());
                path.setLength(0);
            }
        }
    }

    private void doDelete(String path) throws Exception {
        log.info("DELETE-address: path {}", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
            client.delete(endpoint.getPort(), endpoint.getHost(), path)
                    .timeout(20_000)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject())
                    .send(ar -> responseHandler(ar, responsePromise, "Error: delete address"));
            return responsePromise.get(30, TimeUnit.SECONDS);
        });
    }

    public void deploy(AddressSpace addressSpace, HttpMethod httpMethod, Destination... destinations) throws Exception {
        JsonObject config = new JsonObject();
        config.put("apiVersion", "v1");
        config.put("kind", "AddressList");
        JsonArray items = new JsonArray();
        for (Destination destination : destinations) {
            JsonObject entry = new JsonObject();
            JsonObject metadata = new JsonObject();
            if (destination.getName() != null) {
                metadata.put("name", destination.getName());
            }
            if (destination.getUuid() != null) {
                metadata.put("uuid", destination.getUuid());
            }
            if (destination.getAddressSpace() != null) {
                metadata.put("addressSpace", destination.getAddressSpace());
            }
            entry.put("metadata", metadata);

            JsonObject spec = new JsonObject();
            if (destination.getAddress() != null) {
                spec.put("address", destination.getAddress());
            }
            if (destination.getType() != null) {
                spec.put("type", destination.getType());
            }
            if (destination.getPlan() != null) {
                spec.put("plan", destination.getPlan());
            }
            entry.put("spec", spec);

            items.add(entry);
        }
        config.put("items", items);
        deploy(addressSpace, httpMethod, config);
    }

    /**
     * deploying addresses via rest api
     *
     * @param addressSpace name of instance
     * @param httpMethod   PUT, POST and DELETE method are supported
     * @throws Exception
     */
    private void deploy(AddressSpace addressSpace, HttpMethod httpMethod, JsonObject config) throws Exception {
        StringBuilder path = new StringBuilder();
        path.append(addressPath).append("/").append(addressSpace.getName()).append("/");
        log.info("{}-address: path {}; body: {}", httpMethod, path, config.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
            client.request(httpMethod, endpoint.getPort(), endpoint.getHost(), path.toString())
                    .timeout(20_000)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject())
                    .sendJsonObject(config, ar -> responseHandler(ar,
                            responsePromise,
                            "Error: deploy addresses"));
            return responsePromise.get(30, TimeUnit.SECONDS);
        });
    }

    public JsonObject sendRequest(HttpMethod method, Uri uri, Optional<JsonObject> payload) throws Exception {
        log.info("{}-address: url {}; body: {}", method, uri, payload.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
            client.get("as", "s");
            HttpRequest<JsonObject> request = client.request(method, uri.getPort(), uri.getHost(), uri.getPath())
                    .timeout(20_000)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject());
            Handler<AsyncResult<HttpResponse<JsonObject>>> handleResponse = (ar) -> responseHandler(ar, responsePromise,
                    String.format("Error: send payload: '%s' with url: '%s'", payload.toString(), uri));

            if (payload.isPresent()) {
                log.info("use payload");
                request.sendJsonObject(payload.get(), handleResponse);
            } else {
                log.info("don't use payload");
                request.send(handleResponse);
            }
            return responsePromise.get(30, TimeUnit.SECONDS);
        });
    }

    public JsonObject responseAddressHandler(JsonObject responseData) throws AddressAlreadyExistsException {
        if (responseData != null) {
            String errMsg = responseData.getString("error");
            switch (errMsg) {
                case "Address already exists":
                    throw new AddressAlreadyExistsException(errMsg);
            }
        }
        return responseData;
    }

    private <T> void responseHandler(AsyncResult<HttpResponse<T>> ar, CompletableFuture<T> promise,
                                     String warnMessage) {
        try {
            if (ar.succeeded()) {
                HttpResponse<T> response = ar.result();
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    log.error("response status code: {}, body: {}", response.statusCode(), response.body());
                    promise.completeExceptionally(new RuntimeException(response.body().toString()));
                } else {
                    promise.complete(ar.result().body());
                }
            } else {
                log.warn(warnMessage);
                promise.completeExceptionally(ar.cause());
            }
        } catch (io.vertx.core.json.DecodeException decEx) {
            if (ar.result().bodyAsString().toLowerCase().contains("application is not available")) {
                log.warn("Address-controller is not available.", ar.cause());
                throw new IllegalStateException("Address-controller is not available.");
            } else {
                log.warn("Unexpected object received", ar.cause());
                throw new IllegalStateException("JsonObject expected, but following object was received: " + ar.result().bodyAsString());
            }
        }
    }

    public <T> T doRequestNTimes(int retry, Callable<T> fn) throws Exception {
        return TestUtils.doRequestNTimes(retry, () -> {
            endpoint = kubernetes.getRestEndpoint();
            return fn.call();
        });
    }
}
