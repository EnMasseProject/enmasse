package io.enmasse.systemtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

public class AddressApiClient {
    private final WebClient client;
    private final Endpoint endpoint;
    private final Vertx vertx;

    public AddressApiClient(Endpoint endpoint) {
        this.vertx = VertxFactory.create();
        this.client = WebClient.create(vertx);
        this.endpoint = endpoint;
    }

    public void close() {
        client.close();
        vertx.close();
    }

    public void createAddressSpace(AddressSpace addressSpace, String authServiceType, String addrSpaceType) throws JsonProcessingException, InterruptedException, TimeoutException, ExecutionException {
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
        client.post(endpoint.getPort(), endpoint.getHost(), "/v1/addressspaces")
                .timeout(20_000)
                .as(BodyCodec.jsonObject())
                .sendJsonObject(config, ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.error("Error creating address space {}", addressSpace, ar.cause());
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });

        responsePromise.get(30, TimeUnit.SECONDS);
    }

    public void deleteAddressSpace(AddressSpace addressSpace) throws InterruptedException, TimeoutException, ExecutionException {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.delete(endpoint.getPort(), endpoint.getHost(), "/v1/addressspaces/" + addressSpace.getName())
                .as(BodyCodec.jsonObject())
                .timeout(20_000)
                .send(ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.error("Error deleting address space {}", addressSpace, ar.cause());
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        responsePromise.get(30, TimeUnit.SECONDS);
    }

    /**
     * get address space by address space name vie rest api
     *
     * @param name name of address space
     * @return
     * @throws InterruptedException
     */
    public JsonObject getAddressSpace(String name) throws InterruptedException, TimeoutException, ExecutionException {
        Logging.log.info("Following HTTP request will be used for getting address space:" +
                "/v1/addressspaces/" + name +
                " with host: " + endpoint.getHost() + " with Port: " + endpoint.getPort());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.get(endpoint.getPort(), endpoint.getHost(), "/v1/addressspaces/" + name)
                .as(BodyCodec.jsonObject())
                .send(ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error when getting address space {}", name, ar.cause());
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        return responsePromise.get(30, TimeUnit.SECONDS);
    }

    public Set<String> listAddressSpaces() throws InterruptedException, TimeoutException, ExecutionException {
        Logging.log.info("Following HTTP request will be used for getting address space:" +
                "/v1/addressspaces/" +
                " with host: " + endpoint.getHost() + " with Port: " + endpoint.getPort());

        CompletableFuture<Set<String>> list = new CompletableFuture<>();
        client.get(endpoint.getPort(), endpoint.getHost(), "/v1/addressspaces")
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
                        list.completeExceptionally(ar.cause());
                        Logging.log.warn("Failed listing address spaces", ar.cause());
                    }
                });
        return list.get(30, TimeUnit.SECONDS);
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
        String path = "/v1/addresses/" + addressSpace.getName();
        path += addressName.isPresent() ? addressName.get() : "";

        Logging.log.info("Following HTTP request will be used for getting address: " + path);

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.get(endpoint.getPort(), endpoint.getHost(), path)
                .as(BodyCodec.jsonObject())
                .timeout(20_000)
                .send(ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error when getting addresses", ar.cause());
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        return responsePromise.get(30, TimeUnit.SECONDS);
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
            doDelete("/v1/addresses/" + addressSpace.getName() + "/" + destination.getAddress());
        }

    }

    private void doDelete(String path) throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.delete(endpoint.getPort(), endpoint.getHost(), path)
                .timeout(20_000)
                .as(BodyCodec.jsonObject())
                .send(ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error during deleting addresses", ar.cause());
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        responsePromise.get(30, TimeUnit.SECONDS);
    }

    /**
     * deploying addresses via rest api
     *
     * @param addressSpace name of instance
     * @param httpMethod   PUT, POST and DELETE method are supported
     * @param destinations variable count of destinations that you can put, append or delete
     * @throws Exception
     */
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

        Logging.log.info("Following HTTP request will be used for deploy: /v1/addresses/" + addressSpace.getName() + "/");
        Logging.log.info("Following payload will be used in request: " + config.toString());

        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();

        client.request(httpMethod, endpoint.getPort(), endpoint.getHost(), "/v1/addresses/" + addressSpace.getName() + "/")
                .timeout(20_000)
                .as(BodyCodec.jsonObject())
                .sendJsonObject(config, ar -> {
                    if (ar.succeeded()) {
                        responsePromise.complete(responseHandler(ar));
                    } else {
                        Logging.log.warn("Error when deploying addresses", ar.cause());
                        responsePromise.completeExceptionally(ar.cause());
                    }
                });
        responsePromise.get(30, TimeUnit.SECONDS);
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
                throw new IllegalStateException("Address-controller is not available.");
            } else {
                throw new IllegalStateException("JsonObject expected, but following object was received: " + ar.result().bodyAsString());
            }
        }
    }
}
