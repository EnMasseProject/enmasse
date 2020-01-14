/*
 * Copyright 2018, EnMasse authors.
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class OpenshiftCertValidatorApiClient extends ApiClient {


    public OpenshiftCertValidatorApiClient(Endpoint endpoint) {
        super(() -> endpoint, "");
    }

    @Override
    protected String apiClientName() {
        return "openshift-cert-validator";
    }

    @Override
    protected void connect() {
        this.client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(false)
                .setTrustAll(true)
                .setVerifyHost(false));
    }

    public JsonObject test(JsonObject request) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<JsonObject> responsePromise = new CompletableFuture<>();
        client.get(endpoint.getPort(), endpoint.getHost(), "")
                .as(BodyCodec.jsonObject())
                .timeout(120000)
                .sendJson(request,
                        ar -> responseHandler(ar, responsePromise, HttpURLConnection.HTTP_OK, "Error testing openshift-cert-validator"));
        return responsePromise.get(150000, TimeUnit.SECONDS);

    }

}
