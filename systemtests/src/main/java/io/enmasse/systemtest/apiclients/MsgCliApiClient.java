/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.Kubernetes;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MsgCliApiClient extends ApiClient {


    public MsgCliApiClient(Kubernetes kubernetes, Endpoint endpoint, String apiVersion) {
        super(kubernetes, endpoint, apiVersion);
    }

    @Override
    protected String apiClientName() {
        return "Docker Messaging Clients";
    }

    @Override
    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(false)
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    /**
     * Start new messaging client(s)
     *
     * @param clientArguments list of arguments for client (together with client name!)
     * @param count           count of clients that will be started
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public JsonObject startClients(List<String> clientArguments, int count) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        JsonObject request = new JsonObject();
        request.put("command", new JsonArray(clientArguments));
        request.put("count", count);

        client.post(endpoint.getPort(), endpoint.getHost(), "")
                .as(BodyCodec.jsonObject())
                .timeout(120_000)
                .sendJson(request,
                        ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_OK, "Error starting messaging clients"));
        return responsePromise.get(150_000, TimeUnit.SECONDS);

    }

    /**
     * Get all info about messaging client (stdOut, stdErr, code, isRunning)
     *
     * @param uuid client id
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public JsonObject getClientInfo(String uuid) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        JsonObject request = new JsonObject();
        request.put("id", uuid);

        client.get(endpoint.getPort(), endpoint.getHost(), "")
                .as(BodyCodec.jsonObject())
                .timeout(10_000)
                .sendJson(request,
                        ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_OK, "Error getting messaging clients info"));
        return responsePromise.get(20, TimeUnit.SECONDS);

    }

    /**
     * Stop messaging client and get all informations about them (stdOut, stdErr, code, isRunning)
     *
     * @param uuid client id
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public JsonObject stopClient(String uuid) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        JsonObject request = new JsonObject();
        request.put("id", uuid);

        client.delete(endpoint.getPort(), endpoint.getHost(), "")
                .as(BodyCodec.jsonObject())
                .timeout(10_000)
                .sendJson(request,
                        ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_OK, "Error removing messaging clients"));
        return responsePromise.get(20, TimeUnit.SECONDS);
    }
}
