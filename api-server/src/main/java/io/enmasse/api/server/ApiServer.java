/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.api.server;

import io.enmasse.api.auth.AuthApi;
import io.enmasse.api.auth.KubeAuthApi;
import io.enmasse.api.common.CachingSchemaProvider;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapSchemaApi;
import io.enmasse.k8s.api.SchemaApi;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.vertx.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ApiServer extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ApiServer.class.getName());
    private final NamespacedOpenShiftClient controllerClient;
    private final ApiServerOptions options;

    private ApiServer(ApiServerOptions options) {
        this.controllerClient = new DefaultOpenShiftClient();
        this.options = options;
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        SchemaApi schemaApi = new ConfigMapSchemaApi(controllerClient, options.getNamespace());
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider(schemaApi);
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());

        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(controllerClient);

        AuthApi authApi = new KubeAuthApi(controllerClient, null, controllerClient.getConfiguration().getOauthToken());

        deployVerticles(startPromise,
                new Deployment(new HTTPServer(addressSpaceApi, schemaProvider, options.getCertDir(), options.getClientCa(), options.getRequestHeaderClientCa(), authApi, options.isEnableRbac()), new DeploymentOptions().setWorker(true)));
    }

    private void deployVerticles(Future<Void> startPromise, Deployment ... deployments) {
        List<Future> futures = new ArrayList<>();
        for (Deployment deployment : deployments) {
            Future<Void> promise = Future.future();
            futures.add(promise);
            vertx.deployVerticle(deployment.verticle, deployment.options, result -> {
                if (result.succeeded()) {
                    promise.complete();
                } else {
                    promise.fail(result.cause());
                }
            });
        }

        CompositeFuture.all(futures).setHandler(result -> {
            if (result.succeeded()) {
                startPromise.complete();
            } else {
                startPromise.fail(result.cause());
            }
        });
    }

    private static class Deployment {
        final Verticle verticle;
        final DeploymentOptions options;

        private Deployment(Verticle verticle) {
            this(verticle, new DeploymentOptions());
        }

        private Deployment(Verticle verticle, DeploymentOptions options) {
            this.verticle = verticle;
            this.options = options;
        }
    }

    public static void main(String args[]) {
        try {
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new ApiServer(ApiServerOptions.fromEnv(System.getenv())));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting API server: " + e.getMessage());
            System.exit(1);
        }
    }
}
