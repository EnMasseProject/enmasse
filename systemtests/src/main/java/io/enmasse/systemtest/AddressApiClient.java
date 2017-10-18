package io.enmasse.systemtest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

import static io.vertx.core.json.Json.mapper;

public class AddressApiClient {
    private final HttpClient httpClient;
    private final Endpoint endpoint;
    private final Vertx vertx;

    public AddressApiClient(Endpoint endpoint) {
        this.vertx = VertxFactory.create();
        this.httpClient = vertx.createHttpClient();
        this.endpoint = endpoint;
    }

    public void close() {
        httpClient.close();
        vertx.close();
    }

    public void createAddressSpace(AddressSpace addressSpace, String authServiceType) throws JsonProcessingException, InterruptedException {
        this.createAddressSpace(addressSpace, authServiceType, "standard");
    }

    public void createAddressSpace(AddressSpace addressSpace, String authServiceType, String addrSpaceType) throws JsonProcessingException, InterruptedException {
        ObjectNode config = mapper.createObjectNode();
        config.put("apiVersion", "v1");
        config.put("kind", "AddressSpace");

        ObjectNode metadata = config.putObject("metadata");
        metadata.put("name", addressSpace.getName());
        metadata.put("namespace", addressSpace.getNamespace());

        ObjectNode spec = config.putObject("spec");
        spec.put("type", addrSpaceType);

        ObjectNode authService = spec.putObject("authenticationService");
        authService.put("type", authServiceType);

        CountDownLatch latch = new CountDownLatch(1);
        HttpClientRequest request;

        Logging.log.info("Following payload will be used in POST request: " + config.toString());
        request = httpClient.post(endpoint.getPort(), endpoint.getHost(), "/v1/addressspaces");
        request.putHeader("content-type", "application/json");
        request.handler(event -> {
            if (event.statusCode() >= 200 && event.statusCode() < 300) {
                latch.countDown();
            }
        });
        request.end(Buffer.buffer(mapper.writeValueAsBytes(config)));
        latch.await(30, TimeUnit.SECONDS);
    }

    public void deleteAddressSpace(AddressSpace name) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClientRequest request;
        request = httpClient.delete(endpoint.getPort(), endpoint.getHost(), "/v1/addressspaces/" + name.getName());
        request.putHeader("content-type", "application/json");
        request.handler(event -> {
            if (event.statusCode() >= 200 && event.statusCode() < 300) {
                latch.countDown();
            }
        });
        request.end();
        latch.await(30, TimeUnit.SECONDS);
    }

    /**
     * get address space by address space name vie rest api
     *
     * @param name name of address space
     * @return
     * @throws InterruptedException
     */
    public JsonObject getAddressSpace(String name) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        Logging.log.info("Following HTTP request will be used for getting address space:" +
                "/v1/addressspaces/" + name +
                " with host: " + endpoint.getHost() + " with Port: " + endpoint.getPort());
        HttpClientRequest request = httpClient.get(endpoint.getPort(), endpoint.getHost(), "/v1/addressspaces/" + name);
        request.putHeader("content-type", "application/json");

        final JsonObject[] responseArray = new JsonObject[1];
        request.handler(event -> {
            event.bodyHandler(responseData -> {
                responseArray[0] = responseData.toJsonObject();
                latch.countDown();
            });
            if (event.statusCode() >= 200 && event.statusCode() < 300) {
                latch.countDown();
            } else {
                Logging.log.warn("Error when getting address space: " + event.statusCode() + ": " + event.statusMessage());
            }
        });
        request.end();
        latch.await(30, TimeUnit.SECONDS);
        return responseArray[0];
    }

    public Set<String> listAddressSpaces() throws InterruptedException, TimeoutException, ExecutionException {
        Logging.log.info("Following HTTP request will be used for getting address space:" +
                "/v1/addressspaces/" +
                " with host: " + endpoint.getHost() + " with Port: " + endpoint.getPort());
        HttpClientRequest request = httpClient.get(endpoint.getPort(), endpoint.getHost(), "/v1/addressspaces");
        request.putHeader("content-type", "application/json");

        CompletableFuture<Set<String>> list = new CompletableFuture<>();
        request.handler(event -> {
            event.bodyHandler(responseData -> {
                Set<String> spaces = new HashSet<>();
                JsonObject object = responseData.toJsonObject();
                JsonArray items = object.getJsonArray("items");
                Logging.log.info("Following list of address spaces received" + object.toString());
                if (items != null) {
                    for (int i = 0; i < items.size(); i++) {
                        spaces.add(items.getJsonObject(i).getJsonObject("metadata").getString("name"));
                    }
                }
                list.complete(spaces);
            });
            if (event.statusCode() < 200 || event.statusCode() >= 300) {
                list.completeExceptionally(new RuntimeException("Error when getting address space: " + event.statusCode() + ": " + event.statusMessage()));
            }
        });
        request.end();
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
        HttpClientRequest request;
        String path = "/v1/addresses/" + addressSpace.getName();
        path += addressName.isPresent() ? addressName.get() : "";

        CountDownLatch latch = new CountDownLatch(2);

        Logging.log.info("Following HTTP request will be used for getting address: " + path);

        request = httpClient.request(HttpMethod.GET, endpoint.getPort(), endpoint.getHost(), path);
        request.setTimeout(10_000);
        request.exceptionHandler(event -> {
            Logging.log.warn("Exception while performing request", event.getCause());
        });

        final JsonObject[] responseArray = new JsonObject[1];
        request.handler(event -> {
            event.bodyHandler(responseData -> {
                responseArray[0] = responseData.toJsonObject();
                latch.countDown();
            });
            if (event.statusCode() >= 200 && event.statusCode() < 300) {
                latch.countDown();
            } else {
                Logging.log.warn("Error when getting addresses: " + event.statusCode() + ": " + event.statusMessage());
            }
        });
        request.end();
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout getting address config");
        }
        return responseArray[0];
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
        CountDownLatch latch = new CountDownLatch(2);
        HttpClientRequest request = httpClient.request(HttpMethod.DELETE, endpoint.getPort(), endpoint.getHost(), path);
        request.setTimeout(10_000);
        request.exceptionHandler(event -> {
            Logging.log.warn("Exception while performing request", event.getCause());
        });
        request.handler(event -> {
            event.bodyHandler(responseData -> {
                latch.countDown();
            });
            if (event.statusCode() >= 200 && event.statusCode() < 300) {
                latch.countDown();
            } else {
                Logging.log.warn("Error during deleting addresses: " + event.statusCode() + ": " + event.statusMessage());
            }
        });
        request.end();
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout deleting addresses");
        }
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

        ObjectNode config = mapper.createObjectNode();
        config.put("apiVersion", "v1");
        config.put("kind", "AddressList");
        ArrayNode items = config.putArray("items");
        for (Destination destination : destinations) {
            ObjectNode entry = items.addObject();
            ObjectNode metadata = entry.putObject("metadata");
            metadata.put("name", destination.getAddress());
            metadata.put("addressSpace", addressSpace.getName());
            ObjectNode spec = entry.putObject("spec");
            spec.put("address", destination.getAddress());
            spec.put("type", destination.getType());
            destination.getPlan().ifPresent(e -> spec.put("plan", e));
        }

        CountDownLatch latch = new CountDownLatch(1);

        Logging.log.info("Following HTTP request will be used for deploy: /v1/addresses/" + addressSpace + "/");
        Logging.log.info("Following payload will be used in request: " + config.toString());

        HttpClientRequest request = httpClient.request(httpMethod, endpoint.getPort(), endpoint.getHost(), "/v1/addresses/" + addressSpace + "/");
        request.setTimeout(30_000);
        request.putHeader("content-type", "application/json");
        request.exceptionHandler(event -> {
            Logging.log.warn("Exception while performing request", event.getCause());
        });
        request.handler(event -> {
            if (event.statusCode() >= 200 && event.statusCode() < 300) {
                latch.countDown();
            } else {
                Logging.log.warn("Error when deploying addresses: " + event.statusCode() + ": " + event.statusMessage());
            }
        });
        request.end(Buffer.buffer(mapper.writeValueAsBytes(config)));
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout deploying address config");
        }
    }
}
