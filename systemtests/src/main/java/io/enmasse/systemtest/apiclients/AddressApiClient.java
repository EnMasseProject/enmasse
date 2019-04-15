/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.CoreCrd;
import io.enmasse.systemtest.AddressAlreadyExistsException;
import io.enmasse.systemtest.CustomLogger;
import io.enmasse.systemtest.Kubernetes;
import io.enmasse.systemtest.utils.AddressSpaceUtils;
import io.enmasse.systemtest.utils.AddressUtils;
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
    private final static Logger log = CustomLogger.getLogger();
    private final int initRetry = 10;

    private final static String ADDRESS_PATH = "/apis/enmasse.io/" + CoreCrd.VERSION + "/addresses";
    private final static String ADDRESS_SPACE_PATH = "/apis/enmasse.io/" + CoreCrd.VERSION + "/addressspaces";

    private final String schemaPathPattern = "/apis/enmasse.io/" + CoreCrd.VERSION + "/namespaces/%s/addressspaceschemas";
    private final String addressSpacesPathPattern = "/apis/enmasse.io/" + CoreCrd.VERSION + "/namespaces/%s/addressspaces";
    private final String addressNestedPathPattern = "/apis/enmasse.io/" + CoreCrd.VERSION + "/namespaces/%s/addressspaces/%s/addresses";
    private final String addressResourcePathPattern = "/apis/enmasse.io/" + CoreCrd.VERSION + "/namespaces/%s/addresses";
    private final String defaultNamespace;

    public AddressApiClient(Kubernetes kubernetes) throws MalformedURLException {
        super(kubernetes, kubernetes::getRestEndpoint, "enmasse.io/" + CoreCrd.VERSION);
        this.defaultNamespace = kubernetes.getNamespace();
    }

    public AddressApiClient(Kubernetes kubernetes, String namespace) throws MalformedURLException {
        super(kubernetes, kubernetes::getRestEndpoint, "enmasse.io/" + CoreCrd.VERSION);
        this.defaultNamespace = namespace;
    }

    public AddressApiClient(Kubernetes kubernetes, String namespace, String token) throws MalformedURLException {
        super(kubernetes, kubernetes::getRestEndpoint, "enmasse.io/" + CoreCrd.VERSION, token);
        this.defaultNamespace = namespace;
    }

    @Override
    public void close() {
        client.close();
        vertx.close();
    }

    @Override
    protected String apiClientName() {
        return "Address-Api";
    }

    public void createAddressSpaceList(AddressSpace... addressSpaces) throws Exception {
        for (AddressSpace addressSpace : addressSpaces) {
            createAddressSpace(addressSpace, HTTP_CREATED);
        }
    }

    public void createAddressSpace(io.enmasse.address.model.AddressSpace addressSpace, int expectedCode) throws Exception {
        JsonObject config = AddressSpaceUtils.addressSpaceToJson(addressSpace);

        String addressSpacesPath = String.format(addressSpacesPathPattern, defaultNamespace);

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

    public void replaceAddressSpace(AddressSpace addressSpace) throws Exception {
        replaceAddressSpace(addressSpace, HTTP_OK);
    }

    public void replaceAddressSpace(AddressSpace addressSpace, int expectedCode) throws Exception {
        String addressSpacesPath = String.format(addressSpacesPathPattern, defaultNamespace);
        String path = addressSpacesPath + "/" + addressSpace.getMetadata().getName();
        JsonObject config = AddressSpaceUtils.addressSpaceToJson(addressSpace);

        log.info("UPDATE-address-space: path {}; body {}", addressSpacesPath, config.toString());
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

        doRequestNTimes(initRetry, () -> {
                    client.put(endpoint.getPort(), endpoint.getHost(), path)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(config, ar -> responseHandler(ar,
                                    responsePromise,
                                    expectedCode,
                                    String.format("Error: replacing address space '%s'", addressSpace)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public void deleteAddressSpace(AddressSpace addressSpace, int expectedCode) throws Exception {
        String addressSpacesPath = String.format(addressSpacesPathPattern, defaultNamespace);
        String path = addressSpacesPath + "/" + addressSpace.getMetadata().getName();
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
        String path = String.format(addressSpacesPathPattern, defaultNamespace);
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
        return getAddressSpace(name, defaultNamespace, expectedCode);
    }

    public JsonObject getAddressSpace(String name, String namespace, int expectedCode) throws Exception {
        String addressSpacesPath = String.format(addressSpacesPathPattern, namespace);
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
                                    String.format("Error: get address space %s", name)));
                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
    }

    public JsonObject getAddressSpace(String name) throws Exception {
        return getAddressSpace(name, HTTP_OK);
    }

    public JsonObject getAddressSpace(String name, String namespace) throws Exception {
        return getAddressSpace(name, namespace, HTTP_OK);
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
        String addressSpacesPath = String.format(addressSpacesPathPattern, defaultNamespace);
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

    /**
     * Get all addressesspaces (non-namespaced)
     *
     * @param expectedCode the expected return code
     * @return the result
     * @throws Exception if anything goes wrong
     */
    public JsonObject getAllAddressSpaces(final int expectedCode) throws Exception {
        log.info("GET-all-address-spaces: path {}; ", ADDRESS_SPACE_PATH);
        return doRequestNTimes(initRetry, () -> {
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
            HttpRequest<JsonObject> request = client.get(endpoint.getPort(), endpoint.getHost(), ADDRESS_SPACE_PATH)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject())
                    .timeout(20_000);
            request.send(ar -> responseHandler(ar, responsePromise, expectedCode, "Error: get addressesspaces"));
            return responsePromise.get(30, TimeUnit.SECONDS);
        }, Optional.of(() -> kubernetes.getRestEndpoint()), Optional.empty());
    }

    /**
     * Get all addresses (non-namespaced)
     *
     * @return the result
     * @throws Exception if anything goes wrong
     */
    public JsonObject getAllAddresseSpaces() throws Exception {
        return getAllAddressSpaces(HTTP_OK);
    }


    private <T> HttpRequest<T> addRequestParameters(HttpRequest<T> request, Optional<HashMap<String, String>> params) {
        if (params.isPresent()) {
            params.get().entrySet().iterator().forEachRemaining(
                    stringStringEntry -> request.addQueryParam(stringStringEntry.getKey(), stringStringEntry.getValue()));
        }

        return request;
    }

    /**
     * Get all addresses (non-namespaced)
     *
     * @param expectedCode the expected return code
     * @return the result
     * @throws Exception if anything goes wrong
     */
    public JsonObject getAllAddresses(final int expectedCode) throws Exception {
        log.info("GET-all-addresses: path {}; ", ADDRESS_PATH);
        return doRequestNTimes(initRetry, () -> {
            CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
            HttpRequest<JsonObject> request = client.get(endpoint.getPort(), endpoint.getHost(), ADDRESS_PATH)
                    .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                    .as(BodyCodec.jsonObject())
                    .timeout(20_000);
            request.send(ar -> responseHandler(ar, responsePromise, expectedCode, "Error: get addresses"));
            return responsePromise.get(30, TimeUnit.SECONDS);
        }, Optional.of(() -> kubernetes.getRestEndpoint()), Optional.empty());
    }

    /**
     * Get all addresses (non-namespaced)
     *
     * @return the result
     * @throws Exception if anything goes wrong
     */
    public JsonObject getAllAddresses() throws Exception {
        return getAllAddresses(HTTP_OK);
    }

    /**
     * give you JsonObject with AddressesList or Address kind
     *
     * @param addressName name of address
     * @return
     * @throws Exception
     */
    public JsonObject getAddresses(AddressSpace addressSpace, Optional<String> addressName, int expectedCode, Optional<HashMap<String, String>> params) throws Exception {
        String path = getAddressPath(addressSpace.getMetadata().getName()) + (addressName.map(s -> {
            if (s.startsWith(addressSpace.getMetadata().getName())) {
                return "/" + s;
            } else {
                return "/" + addressSpace.getMetadata().getName() + "." + s;
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
        String schemaPath = String.format(schemaPathPattern, defaultNamespace);
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
    public void deleteAddresses(AddressSpace addressSpace, Address... destinations) throws Exception {
        if (destinations.length == 0) {
            for (Address destination : AddressUtils.convertToListAddress(getAddresses(addressSpace, HTTP_OK, Optional.empty()),
                    Address.class, object -> true)) {
                deleteAddress(addressSpace.getMetadata().getName(), destination, HTTP_OK);
            }
        } else {
            for (Address destination : destinations) {
                deleteAddress(addressSpace.getMetadata().getName(), destination, HTTP_OK);
            }
        }
    }

    private String getAddressPath(String addressSpace) {
        return String.format(addressNestedPathPattern, defaultNamespace, addressSpace);
    }

    public void deleteAddress(String addressSpace, Address destination, int expectedCode) throws Exception {
        doDelete(getAddressPath(addressSpace) + "/" + AddressUtils.generateAddressMetadataName(addressSpace, destination), expectedCode);
    }

    public void replaceAddress(String addressSpace, Address destination, int expectedCode) throws Exception {
        String path = getAddressPath(addressSpace) + "/" + AddressUtils.generateAddressMetadataName(addressSpace, destination);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        JsonObject payload = AddressUtils.addressToJson(addressSpace, destination);
        log.info("UPDATE-address: path {}: {}", path, payload.toString());
        doRequestNTimes(initRetry, () -> {
                    client.put(endpoint.getPort(), endpoint.getHost(), path)
                            .timeout(20_000)
                            .putHeader(HttpHeaders.AUTHORIZATION.toString(), authzString)
                            .as(BodyCodec.jsonObject())
                            .sendJsonObject(payload, ar -> responseHandler(ar, responsePromise, expectedCode, "Error: delete address"));

                    return responsePromise.get(30, TimeUnit.SECONDS);
                },
                Optional.of(() -> kubernetes.getRestEndpoint()),
                Optional.empty());
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


    public void appendAddresses(AddressSpace addressSpace, int batchSize, Address... destinations) throws Exception {
        JsonObject response = getAddresses(addressSpace, HTTP_OK, Optional.empty());
        List<Address> current = new ArrayList<>(AddressUtils.convertToListAddress(response, io.enmasse.address.model.Address.class, object -> true));

        List<Address> toCreate = new ArrayList<>(Arrays.asList(destinations));

        toCreate.removeAll(current);

        log.info("Current: {}, desired: {}, toCreate: {}", current, destinations, toCreate);

        destinations = toCreate.toArray(new Address[0]);
        if (batchSize > destinations.length) {
            throw new IllegalArgumentException(String.format(
                    "Size of batches cannot be greater then count of addresses! got %s; expected <= %s",
                    batchSize, destinations.length));
        }
        if (batchSize == -1) {
            JsonObject payload = createAddressListPayloadJson(addressSpace, destinations);
            createAddresses(addressSpace, payload, HTTP_CREATED);
        } else {
            int start = 0;
            try {
                while (start < destinations.length) {
                    Address[] splice = Arrays.copyOfRange(destinations, start, Math.min(start + batchSize, destinations.length));
                    JsonObject payload = createAddressListPayloadJson(addressSpace, splice);
                    createAddresses(addressSpace, payload, HTTP_CREATED);
                    start += splice.length;
                }
            } finally {
                if (start < destinations.length) {
                    log.error("Create addresses failed processing destinations at index {}  (size {})", start, destinations.length);
                }
            }
        }
    }

    public void appendAddresses(AddressSpace addressSpace, Address... destinations) throws Exception {
        JsonObject response = getAddresses(addressSpace, HTTP_OK, Optional.empty());

        List<Address> current = new ArrayList<>(AddressUtils.convertToListAddress(response, Address.class, object -> true));

        List<Address> toCreate = new ArrayList<>(Arrays.asList(destinations));

        toCreate.removeAll(current);

        for (Address destination : toCreate) {
            createAddress(addressSpace, destination, HTTP_CREATED);
        }
    }

    private JsonObject createAddressListPayloadJson(AddressSpace addressSpace, Address... destinations) throws Exception {
        JsonObject addressList = new JsonObject();
        addressList.put("apiVersion", getApiVersion());
        addressList.put("kind", "AddressList");
        JsonArray items = new JsonArray();
        for (Address destination : destinations) {
            JsonObject item = AddressUtils.addressToJson(addressSpace.getMetadata().getName(), destination);
            items.add(item);
        }
        addressList.put("items", items);
        return addressList;
    }

    public void setAddresses(AddressSpace addressSpace, Address... addresses) throws Exception {
        setAddresses(addressSpace, HTTP_CREATED, addresses);
    }

    public void setAddresses(AddressSpace addressSpace, int expectedCode, Address... addresses) throws Exception {
        JsonObject response = getAddresses(addressSpace, HTTP_OK, Optional.empty());

        List<Address> current = new ArrayList<>(AddressUtils.convertToListAddress(response, Address.class, object -> true));

        List<Address> toCreate = new ArrayList<>(Arrays.asList(addresses));
        List<Address> toDelete = new ArrayList<>(current);

        toDelete.removeAll(toCreate);
        toCreate.removeAll(current);

        log.info("Creating {}", toCreate);

        for (Address destination : toCreate) {
            createAddress(addressSpace, destination, expectedCode);
        }

        log.info("Deleting {}", toDelete);
        for (Address destination : toDelete) {
            deleteAddress(addressSpace.getMetadata().getName(), destination, HTTP_OK);
        }
    }

    public void createAddress(Address destination, int expectedCode) throws Exception {
        String addressResourcePath = String.format(addressResourcePathPattern, defaultNamespace);
        JsonObject addressJson = AddressUtils.addressToJson(destination);
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

    public void createAddress(Address destination) throws Exception {
        createAddress(destination, HTTP_CREATED);
    }

    public void createAddress(AddressSpace addressSpace, Address destination, int expectedCode) throws Exception {
        JsonObject entry = AddressUtils.addressToJson(addressSpace.getMetadata().getName(), destination);
        log.info("POST-address: path {}; body: {}", getAddressPath(addressSpace.getMetadata().getName()), entry.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), getAddressPath(addressSpace.getMetadata().getName()))
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

    public void createAddress(AddressSpace addressSpace, Address destination) throws Exception {
        createAddress(addressSpace, destination, HTTP_CREATED);
    }

    public void createAddresses(AddressSpace addressSpace, JsonObject payload, int expectedCode) throws Exception {
        log.info("POST-address: path {}; body: {}", getAddressPath(addressSpace.getMetadata().getName()), payload.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        doRequestNTimes(initRetry, () -> {
                    client.post(endpoint.getPort(), endpoint.getHost(), getAddressPath(addressSpace.getMetadata().getName()))
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
