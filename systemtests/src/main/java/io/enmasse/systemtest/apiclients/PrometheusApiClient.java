/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.apiclients;

import io.enmasse.systemtest.Endpoint;
import io.enmasse.systemtest.platform.Kubernetes;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PrometheusApiClient extends ApiClient {

    public PrometheusApiClient(Kubernetes kubernetes, Endpoint endpoint) {
        super(kubernetes, () -> endpoint);
    }

    @Override
    protected String apiClientName() {
        return "prometheus-client";
    }

    @Override
    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    public JsonObject getRules() throws Exception {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.get(endpoint.getPort(), endpoint.getHost(), "/api/v1/rules")
                .bearerTokenAuthentication(kubernetes.getApiToken())
                .as(BodyCodec.jsonObject())
                .timeout(120000)
                .send(ar -> responseHandler(ar, responsePromise, "Error getting prometheus rules"));
        return responsePromise.get(150000, TimeUnit.SECONDS);
    }

    public JsonObject doQuery(String query) throws Exception {
        Objects.requireNonNull(query);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        String uri = String.format("/api/v1/query?query=%s", query);
        client.get(endpoint.getPort(), endpoint.getHost(), uri)
                .bearerTokenAuthentication(kubernetes.getApiToken())
                .as(BodyCodec.jsonObject())
                .timeout(120000)
                .send(ar -> responseHandler(ar, responsePromise, "Error doing prometheus query " + uri));
        return responsePromise.get(150000, TimeUnit.SECONDS);
    }

    public JsonObject doRangeQuery(String query, String startTs, String endTs) throws Exception {
        Objects.requireNonNull(query);
        Objects.requireNonNull(startTs);
        Objects.requireNonNull(endTs);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        String uri = String.format("/api/v1/query_range?query=%s&start=%s&end=%s&step=14", query, startTs, endTs);
        client.get(endpoint.getPort(), endpoint.getHost(), uri)
                .bearerTokenAuthentication(kubernetes.getApiToken())
                .as(BodyCodec.jsonObject())
                .timeout(120000)
                .send(ar -> responseHandler(ar, responsePromise, "Error doing prometheus range query " + uri));
        return responsePromise.get(150000, TimeUnit.SECONDS);
    }

}
