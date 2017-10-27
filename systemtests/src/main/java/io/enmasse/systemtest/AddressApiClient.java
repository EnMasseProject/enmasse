package io.enmasse.systemtest;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

public class AddressApiClient {
    private final WebClient client;
    private final OpenShift openshift;
    private final Endpoint endpoint;
    private final Vertx vertx;
    private final int initRetry = 10;

    public AddressApiClient(OpenShift openshift) throws InterruptedException {
        this.vertx = VertxFactory.create();
        this.client = WebClient.create(vertx);
        this.openshift = openshift;
        this.endpoint = openshift.getRestEndpoint();
    }

    public void close() {
        client.close();
        vertx.close();
    }

    public void createAddressSpace(AddressSpace addressSpace, String authServiceType, String addrSpaceType) throws Exception {
        createAddressSpace(addressSpace, authServiceType, addrSpaceType, endpoint, initRetry);
    }

    private void createAddressSpace(AddressSpace addressSpace, String authServiceType, String addrSpaceType, Endpoint locEndpoint, int retry) throws Exception {
        JsonObject config = new JsonObject();
        config.put("apiVersion", "v1");
        config.put("kind", "AddressSpace");

        JsonObject metadata = new JsonObject();
        metadata.put("name", addressSpace.getName());
        metadata.put("namespace", addressSpace.getNamespace());
        config.put("metadata", metadata);

        JsonObject spec = new JsonObject();
        spec.put("type", addrSpaceType);
        config.put("spec", spec);

        JsonObject authService = new JsonObject();
        authService.put("type", authServiceType);
        spec.put("authenticationService", authService);

        Logging.log.info("Following payload will be used in POST request: " + config.toString());
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.post(locEndpoint.getPort(), locEndpoint.getHost(), "/v1/addressspaces")
                .timeout(20_000)
                .as(BodyCodec.jsonObject())
                .sendJsonObject(config, ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error creating address space {}", addressSpace);
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });

        try {
            responsePromise.get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                try {
                    Logging.log.info("Trying to reload endpoint, remaining iterations: " + retry);
                    createAddressSpace(addressSpace, authServiceType, addrSpaceType, openshift.getRestEndpoint(), retry - 1);
                } catch (Exception ex2) {
                    throw ex2;
                }
            } else {
                ex.getCause().printStackTrace();
                throw ex;
            }
        }
    }

    public void deleteAddressSpace(AddressSpace addressSpace) throws InterruptedException, TimeoutException, ExecutionException, UnknownHostException {
        deleteAddressSpace(addressSpace, endpoint, initRetry);
    }

    private void deleteAddressSpace(AddressSpace addressSpace, Endpoint locEndpoint, int retry) throws InterruptedException, TimeoutException, ExecutionException, UnknownHostException {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.delete(locEndpoint.getPort(), locEndpoint.getHost(), "/v1/addressspaces/" + addressSpace.getName())
                .as(BodyCodec.jsonObject())
                .timeout(20_000)
                .send(ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error deleting address space {}", addressSpace);
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        try {
            responsePromise.get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                try {
                    Logging.log.info("Trying to reload endpoint, remaining iterations: " + retry);
                    deleteAddressSpace(addressSpace, openshift.getRestEndpoint(), retry - 1);
                } catch (Exception ex2) {
                    throw ex2;
                }
            } else {
                ex.getCause().printStackTrace();
                throw ex;
            }
        }
    }

    public JsonObject getAddressSpace(String name) throws InterruptedException, TimeoutException, ExecutionException {
        return getAddressSpace(name, endpoint, initRetry);
    }

    /**
     * get address space by address space name vie rest api
     *
     * @param name name of address space
     * @return
     * @throws InterruptedException
     */
    private JsonObject getAddressSpace(String name, Endpoint locEndpoint, int retry) throws InterruptedException, TimeoutException, ExecutionException {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        Logging.log.info("Following HTTP request will be used for getting address space:" +
                "/v1/addressspaces/" + name +
                " with host: " + locEndpoint.getHost() + " with Port: " + locEndpoint.getPort());
        client.get(locEndpoint.getPort(), locEndpoint.getHost(), "/v1/addressspaces/" + name)
                .as(BodyCodec.jsonObject())
                .send(ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error when getting address space {}", name);
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        try {
            return responsePromise.get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                try {
                    Logging.log.info("Trying to reload endpoint, remaining iterations: " + retry);
                    return getAddressSpace(name, openshift.getRestEndpoint(), retry - 1);
                } catch (Exception ex2) {
                    throw ex2;
                }
            } else {
                ex.getCause().printStackTrace();
                throw ex;
            }
        }
    }

    public Set<String> listAddressSpaces() throws InterruptedException, TimeoutException, ExecutionException {
        return listAddressSpaces(endpoint, initRetry);
    }

    private Set<String> listAddressSpaces(Endpoint locEndpoint, int retry) throws InterruptedException, TimeoutException, ExecutionException {
        CompletableFuture<Set<String>> list = new CompletableFuture<>();
        Logging.log.info("Following HTTP request will be used for getting address space:" +
                "/v1/addressspaces/" +
                " with host: " + locEndpoint.getHost() + " with Port: " + locEndpoint.getPort());
        client.get(locEndpoint.getPort(), locEndpoint.getHost(), "/v1/addressspaces")
                .as(BodyCodec.jsonObject())
                .timeout(20_000)
                .send(ar -> {
                    if (ar.succeeded()) {
                        Set<String> spaces = new HashSet<>();
                        JsonObject object = responseHandler(ar);
                        JsonArray items = object.getJsonArray("items");
                        Logging.log.info("Following list of address spaces received" + object.toString());
                        if (items != null) {
                            for (int i = 0; i < items.size(); i++) {
                                spaces.add(items.getJsonObject(i).getJsonObject("metadata").getString("name"));
                            }
                        }
                        list.complete(spaces);
                    } else {
                        Logging.log.warn("Failed listing address spaces", ar.cause());
                        list.completeExceptionally(ar.cause());
                    }
                });
        try {
            return list.get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                try {
                    Logging.log.info("Trying to reload endpoint, remaining iterations: " + retry);
                    return listAddressSpaces(openshift.getRestEndpoint(), retry - 1);
                } catch (Exception ex2) {
                    throw ex2;
                }
            } else {
                ex.getCause().printStackTrace();
                throw ex;
            }
        }
    }


    public JsonObject getAddresses(AddressSpace addressSpace, Optional<String> addressName) throws Exception {
        return getAddresses(addressSpace, addressName, endpoint, initRetry);
    }

    /**
     * give you JsonObject with AddressesList or Address kind
     *
     * @param addressSpace name of instance, this is used only if isMultitenant is set to true
     * @param addressName  name of address
     * @return
     * @throws Exception
     */
    private JsonObject getAddresses(AddressSpace addressSpace, Optional<String> addressName, Endpoint locEndpoint, int retry) throws Exception {
        String path = "/v1/addresses/" + addressSpace.getName();
        path += addressName.isPresent() ? addressName.get() : "";

        Logging.log.info("Following HTTP request will be used for getting address: " + path);

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.get(locEndpoint.getPort(), locEndpoint.getHost(), path)
                .as(BodyCodec.jsonObject())
                .timeout(20_000)
                .send(ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error when getting addresses");
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        try {
            return responsePromise.get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                try {
                    Logging.log.info("Trying to reload endpoint, remaining iterations: " + retry);
                    return getAddresses(addressSpace, addressName, openshift.getRestEndpoint(), retry - 1);
                } catch (Exception ex2) {
                    throw ex2;
                }
            } else {
                ex.getCause().printStackTrace();
                throw ex;
            }
        }
    }

    /**
     * delete addresses via reset api
     *
     * @param addressSpace address space
     * @param destinations variable count of destinations that you can delete
     * @throws Exception
     */
    public void deleteAddresses(AddressSpace addressSpace, Destination... destinations) throws Exception {
        for (Destination destination : destinations) {
            doDelete("/v1/addresses/" + addressSpace.getName() + "/" + destination.getAddress(), endpoint, initRetry);
        }
    }

    private void doDelete(String path, Endpoint locEndpoint, int retry) throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.delete(locEndpoint.getPort(), locEndpoint.getHost(), path)
                .timeout(20_000)
                .as(BodyCodec.jsonObject())
                .send(ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error during deleting addresses");
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        try {
            responsePromise.get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                try {
                    Logging.log.info("Trying to reload endpoint, remaining iterations: " + retry);
                    doDelete(path, openshift.getRestEndpoint(), retry - 1);
                } catch (Exception ex2) {
                    throw ex2;
                }
            } else {
                ex.getCause().printStackTrace();
                throw ex;
            }
        }
    }

    public void deploy(AddressSpace addressSpace, HttpMethod httpMethod, Destination... destinations) throws Exception {
        JsonObject config = new JsonObject();
        config.put("apiVersion", "v1");
        config.put("kind", "AddressList");
        JsonArray items = new JsonArray();
        for (Destination destination : destinations) {
            JsonObject entry = new JsonObject();
            JsonObject metadata = new JsonObject();
            metadata.put("name", destination.getAddress());
            metadata.put("addressSpace", addressSpace.getName());
            entry.put("metadata", metadata);

            JsonObject spec = new JsonObject();
            spec.put("address", destination.getAddress());
            spec.put("type", destination.getType());
            destination.getPlan().ifPresent(e -> spec.put("plan", e));
            entry.put("spec", spec);

            items.add(entry);
        }
        config.put("items", items);
        deploy(addressSpace, httpMethod, endpoint, config, initRetry);
    }

    /**
     * deploying addresses via rest api
     *
     * @param addressSpace name of instance
     * @param httpMethod   PUT, POST and DELETE method are supported
     * @throws Exception
     */
    private void deploy(AddressSpace addressSpace, HttpMethod httpMethod, Endpoint locEndpoint, JsonObject config, int retry) throws Exception {
        Logging.log.info("Following HTTP request will be used for deploy: /v1/addresses/" + addressSpace.getName() + "/");
        Logging.log.info("Following payload will be used in request: " + config.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.request(httpMethod, locEndpoint.getPort(), locEndpoint.getHost(), "/v1/addresses/" + addressSpace.getName() + "/")
                .timeout(20_000)
                .as(BodyCodec.jsonObject())
                .sendJsonObject(config, ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error when deploying addresses");
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        try {
            responsePromise.get(30, TimeUnit.SECONDS);
        } catch (Exception ex) {
            if (ex.getCause() instanceof UnknownHostException && retry > 0) {
                try {
                    Logging.log.info("Trying to reload endpoint, remaining iterations: " + retry);
                    deploy(addressSpace, httpMethod, openshift.getRestEndpoint(), config, retry - 1);
                } catch (Exception ex2) {
                    throw ex2;
                }
            } else {
                ex.getCause().printStackTrace();
                throw ex;
            }
        }
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

    private JsonObject responseHandler(AsyncResult<HttpResponse<JsonObject>> ar) {
        try {
            return ar.result().body();
        } catch (io.vertx.core.json.DecodeException decEx) {
            if (ar.result().bodyAsString().toLowerCase().contains("application is not available")) {
                Logging.log.warn("Address-controller is not available.", ar.cause());
                throw new IllegalStateException("Address-controller is not available.");
            } else {
                Logging.log.warn("Unexpected object received", ar.cause());
                throw new IllegalStateException("JsonObject expected, but following object was received: " + ar.result().bodyAsString());
            }
        }
    }
}
