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

import java.net.HttpURLConnection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ConsoleApiClient extends ApiClient {

    public ConsoleApiClient(Endpoint endpoint) {
        super(() -> endpoint, "");
    }

    @Override
    protected String apiClientName() {
        return "console-client";
    }

    @Override
    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    public JsonObject doQuery(String query) throws Exception {
        Objects.requireNonNull(query);
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        String uri = String.format("/api/v1/query?query=%s", query);
        client.get(endpoint.getPort(), endpoint.getHost(), uri)
                .bearerTokenAuthentication(authzString)
                .as(BodyCodec.jsonObject())
                .timeout(120000)
                .send(ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_OK, "Error doing console query " + uri));
        return responsePromise.get(150000, TimeUnit.SECONDS);
    }
}
