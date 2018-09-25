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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

public class AddressApiClient extends ApiClient {
    protected static Logger log = CustomLogger.getLogger();
    private final int initRetry = 10;
    private final String schemaPath = "/apis/address.enmasse.io/v1alpha1/schema";
    private final String addressSpacesPath;
    private final String addressNestedPathPattern;
    private final String addressResourcePath;

    public AddressApiClient(Kubernetes kubernetes) throws MalformedURLException {
        super(kubernetes, kubernetes.getRestEndpoint(), "address.enmasse.io/v1alpha1");
        this.addressSpacesPath = String.format("/apis/address.enmasse.io/v1alpha1/namespaces/%s/addressspaces", kubernetes.getNamespace());
        this.addressNestedPathPattern = String.format("/apis/address.enmasse.io/v1alpha1/namespaces/%s/addressspaces", kubernetes.getNamespace()) + "/%s/addresses";
        this.addressResourcePath = String.format("/apis/address.enmasse.io/v1alpha1/namespaces/%s/addresses", kubernetes.getNamespace());
    }

    public AddressApiClient(Kubernetes kubernetes, String namespace) throws MalformedURLException {
        super(kubernetes, kubernetes.getRestEndpoint(), "address.enmasse.io/v1alpha1");
        this.addressSpacesPath = String.format("/apis/address.enmasse.io/v1alpha1/namespaces/%s/addressspaces", namespace);
        this.addressNestedPathPattern = String.format("/apis/address.enmasse.io/v1alpha1/namespaces/%s/addressspaces", namespace) + "/%s/addresses";
        this.addressResourcePath = String.format("/apis/address.enmasse.io/v1alpha1/namespaces/%s/addresses", namespace);
    }

    public AddressApiClient(Kubernetes kubernetes, String namespace, String token) throws MalformedURLException {
        super(kubernetes, kubernetes.getRestEndpoint(), "address.enmasse.io/v1alpha1", token);
        this.addressSpacesPath = String.format("/apis/address.enmasse.io/v1alpha1/namespaces/%s/addressspaces", namespace);
        this.addressNestedPathPattern = String.format("/apis/address.enmasse.io/v1alpha1/namespaces/%s/addressspaces", namespace) + "/%s/addresses";
        this.addressResourcePath = String.format("/apis/address.enmasse.io/v1alpha1/namespaces/%s/addresses", namespace);
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
            createAddressSpace(addressSpace, HTTP_CREATED);
        }
    }

    public void createAddressSpace(AddressSpace addressSpace, int expectedCode) throws Exception {
        JsonObject config = addressSpace.toJson(getApiVersion());

        log.info("POST-address-space: path {}; body {}", addressSpacesPath, config.toString());
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), addressSpacesPath)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(config, ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: create address space '%s'", addressSpace)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void createAddressSpace(AddressSpace addressSpace) throws Exception {
        createAddressSpace(addressSpace, HTTP_CREATED);
    }

    public void deleteAddressSpace(AddressSpace addressSpace, int expectedCode) throws Exception {
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
                                    expectedCode,
                                    String.format("Error: delete address space '%s'", addressSpace)));
                    return responsePromise.get(2, TimeUnit.MINUTES);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void deleteAddressSpaces(int expectedCode) throws Exception {
        String path = addressSpacesPath;
        log.info("DELETE-address-space: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.delete(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: delete address spaces")));
                    return responsePromise.get(2, TimeUnit.MINUTES);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void deleteAddressSpace(AddressSpace addressSpace) throws Exception {
        deleteAddressSpace(addressSpace, HTTP_OK);
    }

    /**
     * get address space by address space name vie rest api
     *
     * @param name name of address space
     * @return
     * @throws InterruptedException
     */
    public JsonObject getAddressSpace(String name, int expectedCode) throws Exception {
        String path = addressSpacesPath + "/" + name;
        log.info("GET-address-space: path '{}'", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
                    client.get(endpoint.getPort(), endpoint.getHost(), path)
                            .as(BodyCodec.jsonObject())
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .send(ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: get address space {}", name)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public JsonObject getAddressSpace(String name) throws Exception {
        return getAddressSpace(name, HTTP_OK);
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
                            .send(ar -> responseHandler(ar, response, HTTP_OK, "Error: get address spaces"));
                    return response.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }


    private <T> HttpRequest<T> addRequestParameters(HttpRequest<T> request, Optional<HashMap<String, String>> params) {
        if (params.isPresent()) {
            params.get().entrySet().iterator().forEachRemaining(
                    stringStringEntry -> request.addQueryParam(stringStringEntry.getKey(), stringStringEntry.getValue()));
        }

        return request;
    }

    /**
     * give you JsonObject with AddressesList or Address kind
     *
     * @param addressName name of address
     * @return
     * @throws Exception
     */
    public JsonObject getAddresses(AddressSpace addressSpace, Optional<String> addressName, int expectedCode, Optional<HashMap<String, String>> params) throws Exception {
        String path = getAddressPath(addressSpace.getName()) + (addressName.map(s -> {
            if (s.startsWith(addressSpace.getName())) {
                return "/" + s;
            } else {
                return "/" + addressSpace.getName() + "." + s;
            }
        }).orElse(""));
        log.info("GET-addresses: path {}; ", path);

        return doRequestNTimes(initRetry, () -> {
                    CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
                    HttpRequest<JsonObject> request = client.get(endpoint.getPort(), endpoint.getHost(), path)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .timeout(20_000);
                    request = addRequestParameters(request, params);
                    request.send(ar -> responseHandler(ar, responsePromise, expectedCode, "Error: get addresses"));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public JsonObject getAddresses(AddressSpace addressSpace, Optional<String> addressName, Optional<HashMap<String, String>> params) throws Exception {
        return getAddresses(addressSpace, addressName, HTTP_OK, params);
    }

    public JsonObject getAddresses(AddressSpace addressSpace, int expectedCode, Optional<String> addressName) throws Exception {
        return getAddresses(addressSpace, addressName, expectedCode, Optional.empty());
    }

    public JsonObject getAddresses(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        return getAddresses(addressSpace, HTTP_OK, addressName);
    }

    /**
     * give you JsonObject with Schema
     *
     * @return
     * @throws Exception
     */
    public JsonObject getSchema(int expectedCode) throws Exception {
        log.info("GET-schema: path {}; ", schemaPath);

        return doRequestNTimes(initRetry, () -> {
                    CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
                    client.get(endpoint.getPort(), endpoint.getHost(), schemaPath)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .timeout(20_000)
                            .send(ar -> responseHandler(ar, responsePromise, expectedCode, "Error: get addresses"));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public JsonObject getSchema() throws Exception {
        return getSchema(HTTP_OK);
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
            for (Destination destination : TestUtils.convertToListAddress(getAddresses(addressSpace, HTTP_OK, Optional.empty()),
                    Destination.class, object -> true)) {
                deleteAddress(addressSpace.getName(), destination, HTTP_OK);
            }
        } else {
            for (Destination destination : destinations) {
                deleteAddress(addressSpace.getName(), destination, HTTP_OK);
            }
        }
    }

    private String getAddressPath(String addressSpace) {
        return String.format(addressNestedPathPattern, addressSpace);
    }

    public void deleteAddress(String addressSpace, Destination destination, int expectedCode) throws Exception {
        doDelete(getAddressPath(addressSpace) + "/" + destination.getAddressName(addressSpace), expectedCode);
    }

    private void doDelete(String path, int expectedCode) throws Exception {
        log.info("DELETE-address: path {}", path);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.delete(endpoint.getPort(), endpoint.getHost(), path)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .send(ar -> responseHandler(ar, responsePromise, expectedCode, "Error: delete address"));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }


    public void appendAddresses(AddressSpace addressSpace, int batchSize, Destination... destinations) throws Exception {
        JsonObject response = getAddresses(addressSpace, HTTP_OK, Optional.empty());
        Set<Destination> current = new HashSet<>(TestUtils.convertToListAddress(response, Destination.class, entries -> true));

        Set<Destination> desired = Sets.newHashSet(destinations);

        Set<Destination> toCreate = Sets.difference(desired, current);

        log.info("Current: {}, desired: {}, toCreate: {}", current, desired, toCreate);

        destinations = toCreate.toArray(new Destination[0]);
        if (batchSize > destinations.length) {
            throw new IllegalArgumentException(String.format(
                    "Size of batches cannot be greater then count of addresses! got %s; expected <= %s",
                    batchSize, destinations.length));
        }
        int start = 0;
        int end = batchSize - 1;
        if (batchSize == -1) {
            JsonObject payload = createAddressListPayloadJson(addressSpace, destinations);
            createAddresses(addressSpace, payload, HTTP_CREATED);
        } else {
            while (end < destinations.length) {
                JsonObject payload = createAddressListPayloadJson(addressSpace, Arrays.copyOfRange(destinations, start, end));
                createAddresses(addressSpace, payload, HTTP_CREATED);
                start += batchSize;
                end += batchSize;
            }
            start -= batchSize;
            if (start < destinations.length) {
                JsonObject payload = createAddressListPayloadJson(addressSpace, Arrays.copyOfRange(destinations, start, destinations.length - 1));
                createAddresses(addressSpace, payload, HTTP_CREATED);
            }
        }
    }

    public void appendAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        JsonObject response = getAddresses(addressSpace, HTTP_OK, Optional.empty());
        Set<Destination> current = new HashSet<>(TestUtils.convertToListAddress(response, Destination.class, entries -> true));

        Set<Destination> desired = Sets.newHashSet(destinations);

        Set<Destination> toCreate = Sets.difference(desired, current);

        for (Destination destination : toCreate) {
            createAddress(addressSpace, destination, HTTP_CREATED);
        }
    }

    private JsonObject createAddressListPayloadJson(AddressSpace addressSpace, Destination... destinations) {
        JsonObject addressList = new JsonObject();
        addressList.put("apiVersion", getApiVersion());
        addressList.put("kind", "AddressList");
        JsonArray items = new JsonArray();
        for (Destination destination : destinations) {
            JsonObject item = destination.toJson(this.getApiVersion(), addressSpace.getName());
            items.add(item);
        }
        addressList.put("items", items);
        return addressList;
    }

    public void setAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        setAddresses(addressSpace, HTTP_CREATED, destinations);
    }

    public void setAddresses(AddressSpace addressSpace, int expectedCode, Destination... destinations) throws Exception {
        JsonObject response = getAddresses(addressSpace, HTTP_OK, Optional.empty());
        Set<Destination> current = new HashSet<>(TestUtils.convertToListAddress(response, Destination.class, object -> true));

        Set<Destination> desired = Sets.newHashSet(destinations);

        Set<Destination> toCreate = Sets.difference(desired, current);
        Set<Destination> toDelete = Sets.difference(current, desired);

        log.info("Creating {}", toCreate);

        for (Destination destination : toCreate) {
            createAddress(addressSpace, destination, expectedCode);
        }

        log.info("Deleting {}", toDelete);
        for (Destination destination : toDelete) {
            deleteAddress(addressSpace.getName(), destination, HTTP_OK);
        }
    }

    public void createAddress(Destination destination, int expectedCode) throws Exception {
        JsonObject addressJson = destination.toJson(apiVersion);
        log.info("POST-address: path {}; body: {}", addressResourcePath, addressJson.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), addressResourcePath)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(addressJson, ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    "Error: create address"));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void createAddress(Destination destination) throws Exception {
        createAddress(destination, HTTP_CREATED);
    }

    public void createAddress(AddressSpace addressSpace, Destination destination, int expectedCode) throws Exception {
        JsonObject entry = destination.toJson(this.getApiVersion(), addressSpace.getName());
        log.info("POST-address: path {}; body: {}", getAddressPath(addressSpace.getName()), entry.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), getAddressPath(addressSpace.getName()))
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(entry, ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    "Error: deploy addresses"));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void createAddress(AddressSpace addressSpace, Destination destination) throws Exception {
        createAddress(addressSpace, destination, HTTP_CREATED);
    }

    public void createAddresses(AddressSpace addressSpace, JsonObject payload, int expectedCode) throws Exception {
        log.info("POST-address: path {}; body: {}", getAddressPath(addressSpace.getName()), payload.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), getAddressPath(addressSpace.getName()))
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(payload, ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    "Error: deploy addresses"));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public JsonObject sendRequest(HttpMethod method, URL url, int expectedCode, Optional<JsonObject> payload) throws Exception {
        log.info("{}-address: url {}; body: {}", method, url, payload.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        return doRequestNTimes(initRetry, () -> {
                    client.get("as", "s");
                    HttpRequest<JsonObject> request = client.request(method, url.getPort(), url.getHost(), url.getPath())
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject());
                    Handler<AsyncResult<HttpResponse<JsonObject>>> handleResponse = (ar) -> responseHandler(ar, responsePromise,
                            expectedCode,
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
