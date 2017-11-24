package io.enmasse.systemtest;


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ArtemisApiClient {

    private final WebClient client;
    private final OpenShift openshift;
    private final Vertx vertx;
    private final int initRetry = 10;
    private final String apiPath = "/console/jolokia/read/";
    private final int apiPort = 8161;


    public ArtemisApiClient(OpenShift openshift) {
        this.vertx = VertxFactory.create();
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setTrustAll(true)
                .setVerifyHost(false));
        this.openshift = openshift;
    }

    public String[] getQueueNames(Endpoint endpoint, String brokerName, String addressName) throws Exception {
        return getAddressInfo(endpoint, brokerName, addressName, "QueueNames");
    }

    public String[] getRoutingTypes(Endpoint endpoint, String brokerName, String addressName) throws Exception {
        return getAddressInfo(endpoint, brokerName, addressName, "RoutingTypes");
    }

    private String[] getAddressInfo(Endpoint endpoint, String brokerName, String addressName, String capability) throws Exception {
        Logging.log.info("get address info: capability:'{}'", capability);
        StringBuilder path = new StringBuilder(apiPath);
        path.append("org.apache.activemq.artemis:");
        path.append("address=\"").append(addressName).append("\",");
        path.append("broker=\"").append(brokerName).append("\",");
        path.append("component=addresses/");
        path.append(capability);

        Logging.log.info("request get info, capability:'{}', path:'{}'", capability, path.toString());
        CompletableFuture<String[]> responsePromise = new CompletableFuture<>();
        return TestUtils.doRequestNTimes(initRetry, () -> {
            client.get(apiPort, endpoint.getHost(), path.toString())
                    .timeout(10_000)
                    .as(BodyCodec.jsonObject())
                    .send(ar -> {
                        if (ar.succeeded()) {
                            responsePromise.complete(getValueFromResponse(ar.result().body()));
                        } else {
                            Logging.log.warn("Unexpected object received", ar.cause());
                            throw new IllegalStateException("JsonObject expected, but following object was received: "
                                    + ar.result().bodyAsString());
                        }
                    });
            return responsePromise.get(15, TimeUnit.SECONDS);
        });
    }

    public int getCountOfSubscribers(AddressSpace addressSpace, String brokerName, String addressName) throws Exception {

        Endpoint endpoint = new Endpoint(
                openshift.getRouteEndpoint(addressSpace.getName(), "messaging").getHost(), apiPort);
        String[] queueNames = getQueueNames(endpoint, brokerName, addressName);
        String[] routingTypes = getRoutingTypes(endpoint, brokerName, addressName);

        StringBuilder path = new StringBuilder(apiPath);
        path.append("org.apache.activemq.artemis:");
        path.append("address=\"").append(addressName).append("\",");
        path.append("broker=\"").append(brokerName).append("\",");
        path.append("component=addresses,");
        path.append("queue=\"").append(String.join(",", queueNames)).append("\",");
        path.append("routing-type=\"").append(String.join(",", routingTypes)).append("\",");
        path.append("subcomponent=queues/").append("ConsumerCount");

        Logging.log.info("request - count of subscribers: {}", path.toString());
        CompletableFuture<Integer> responsePromise = new CompletableFuture<>();
        return TestUtils.doRequestNTimes(initRetry, () -> {
            client.get(apiPort, endpoint.getHost(), path.toString())
                    .timeout(20_000)
                    .as(BodyCodec.jsonObject())
                    .send(ar -> {
                        if (ar.succeeded()) {
                            responsePromise.complete(ar.result().body().getInteger("value"));
                        } else {
                            Logging.log.warn("Unexpected object received", ar.cause());
                            throw new IllegalStateException("JsonObject expected, but following object was received: "
                                    + ar.result().bodyAsString());
                        }
                    });
            return responsePromise.get(30, TimeUnit.SECONDS).intValue();
        });
    }

    private String[] getValueFromResponse(JsonObject json) {
        JsonArray arrJson = json.getJsonArray("value");
        String[] arr = new String[arrJson.size()];
        for (int i = 0; i < arrJson.size(); i++) {
            arr[i] = arrJson.getString(i);
        }
        return arr;
    }
}
