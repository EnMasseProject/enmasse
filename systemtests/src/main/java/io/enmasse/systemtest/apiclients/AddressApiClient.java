/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import com.google.common.collect.Sets;
import io.enmasse.systemtest.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.codec.BodyCodec;
import org.slf4j.Logger;

import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AddressApiClient extends ApiClient {
    protected static Logger log = CustomLogger.getLogger();
    private final int initRetry = 10;
    private final String schemaPath = "/apis/enmasse.io/v1alpha1/schema";
    private final String addressSpacesPath;
    private final String addressPathPattern;
    private final String addressResourcePath;

    public AddressApiClient(Kubernetes kubernetes) {
        super(kubernetes, kubernetes.getRestEndpoint());
        this.addressSpacesPath = String.format("/apis/enmasse.io/v1alpha1/namespaces/%s/addressspaces", kubernetes.getNamespace());
        this.addressPathPattern = String.format("/apis/enmasse.io/v1alpha1/namespaces/%s/addressspaces", kubernetes.getNamespace()) + "/%s/addresses";
        this.addressResourcePath = String.format("/apis/enmasse.io/v1alpha1/namespaces/%s/addresses", kubernetes.getNamespace());
    }

    public void close() {
        client.close();
        vertx.close();
    }

    @Override
    protected String apiClientName() {
        return "Address-controller";
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
            JsonObject annotations = new JsonObject();
            annotations.put("enmasse.io/namespace", addressSpace.getNamespace());
            annotations.put("enmasse.io/realm-name", addressSpace.getNamespace());
            metadata.put("annotations", annotations);
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
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
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
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
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
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
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
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    /**
     * give you JsonObject with AddressesList or Address kind
     *
     * @param addressName name of address
     * @return
     * @throws Exception
     */
    public JsonObject getAddresses(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        String path = getAddressPath(addressSpace.getName()) + (addressName.map(s -> "/" + s).orElse(""));
        log.info("GET-addresses: path {}; ", path);

        return doRequestNTimes(initRetry, () -> {
                    CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
                    client.get(endpoint.getPort(), endpoint.getHost(), path)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar, responsePromise, "Error: get addresses"));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
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
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    /**
     * delete addresses via reset api
     *
     * @param addressSpace address space
     * @param destinations variable count of destinations that you can delete
     * @throws Exception
     */
    public void deleteAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        if (destinations.length == 0) {
            for (Destination destination : TestUtils.convertToListAddress(getAddresses(addressSpace, Optional.empty()), Destination.class, object -> true)) {
                deleteAddress(addressSpace.getName(), destination);
            }
        } else {
            for (Destination destination : destinations) {
                deleteAddress(addressSpace.getName(), destination);
            }
        }
    }

    private String getAddressPath(String addressSpace) {
        return String.format(addressPathPattern, addressSpace);
    }

    public void deleteAddress(String addressSpace, Destination destination) throws Exception {
        doDelete(getAddressPath(addressSpace) + "/" + destination.getName());
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
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void appendAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        JsonObject response = getAddresses(addressSpace, Optional.empty());
        Set<Destination> current = new HashSet<>(TestUtils.convertToListAddress(response, Destination.class, entries -> true));

        Set<Destination> desired = Sets.newHashSet(destinations);

        Set<Destination> toCreate = Sets.difference(desired, current);

        for (Destination destination : toCreate) {
            createAddress(addressSpace, destination);
        }
    }

    public void setAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        JsonObject response = getAddresses(addressSpace, Optional.empty());
        Set<Destination> current = new HashSet<>(TestUtils.convertToListAddress(response, Destination.class, object -> true));

        Set<Destination> desired = Sets.newHashSet(destinations);

        Set<Destination> toCreate = Sets.difference(desired, current);
        Set<Destination> toDelete = Sets.difference(current, desired);

        log.info("Creating {}", toCreate);

        for (Destination destination : toCreate) {
            createAddress(addressSpace, destination);
        }

        log.info("Deleting {}", toDelete);
        for (Destination destination : toDelete) {
            deleteAddress(addressSpace.getName(), destination);
        }
    }

    public void createAddress(Destination destination) throws Exception {
        JsonObject entry = new JsonObject();
        entry.put("apiVersion", "enmasse.io/v1alpha1");
        entry.put("kind", "Address");
        JsonObject metadata = new JsonObject();
        if (destination.getName() != null) {
            metadata.put("name", destination.getAddressSpace() + "." + destination.getName());
        }
        if (destination.getUuid() != null) {
            metadata.put("uid", destination.getUuid());
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

        log.info("post-address: path {}; body: {}", addressResourcePath, entry.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), addressResourcePath)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(entry, ar -> responseHandler(ar,
                                    responsePromise,
                                    "Error: create address"));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void createAddress(AddressSpace addressSpace, Destination destination) throws Exception {
        JsonObject entry = new JsonObject();
        entry.put("apiVersion", "enmasse.io/v1alpha1");
        entry.put("kind", "Address");
        JsonObject metadata = new JsonObject();
        if (destination.getName() != null) {
            metadata.put("name", destination.getName());
        }
        if (destination.getUuid() != null) {
            metadata.put("uid", destination.getUuid());
        }
        if (destination.getAddressSpace() != null) {
            metadata.put("addressSpace", destination.getAddressSpace());
        } else {
            metadata.put("addressSpace", addressSpace.getName());
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

        log.info("post-address: path {}; body: {}", getAddressPath(addressSpace.getName()), entry.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), getAddressPath(addressSpace.getName()))
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(entry, ar -> responseHandler(ar,
                                    responsePromise,
                                    "Error: deploy addresses"));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public JsonObject sendRequest(HttpMethod method, URL url, Optional<JsonObject> payload) throws Exception {
        log.info("{}-address: url {}; body: {}", method, url, payload.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
                    client.get("as", "s");
                    HttpRequest<JsonObject> request = client.request(method, url.getPort(), url.getHost(), url.getPath())
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject());
                    Handler<AsyncResult<HttpResponse<JsonObject>>> handleResponse = (ar) -> responseHandler(ar, responsePromise,
                            String.format("Error: send payload: '%s' with url: '%s'", payload.toString(), url));

                    if (payload.isPresent()) {
                        log.info("use payload");
                        request.sendJsonObject(payload.get(), handleResponse);
                    } else {
                        log.info("don't use payload");
                        request.send(handleResponse);
                    }
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
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


}
