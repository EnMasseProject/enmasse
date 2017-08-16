/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.enmasse.controller;

import io.enmasse.controller.auth.AuthController;
import io.enmasse.controller.auth.CertManager;
import io.enmasse.controller.auth.SelfSignedCertManager;
import io.enmasse.controller.common.*;
import io.enmasse.controller.standard.StandardController;
import io.enmasse.address.model.*;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.standard.StandardAddressSpaceType;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.*;

import java.util.*;

public class Controller extends AbstractVerticle {
    private final OpenShiftClient controllerClient;
    private final ControllerOptions options;
    private final Kubernetes kubernetes;

    public Controller(ControllerOptions options) throws Exception {
        this.controllerClient = new DefaultOpenShiftClient(new ConfigBuilder()
                .withMasterUrl(options.masterUrl())
                .withOauthToken(options.token())
                .withNamespace(options.namespace())
                .build());
        this.options = options;
        this.kubernetes = new KubernetesHelper(options.namespace(), controllerClient, options.templateDir());
    }

    @Override
    public void start(Future<Void> startPromise) {
        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(vertx, controllerClient);

        if (!options.isMultiinstance() && !kubernetes.hasService("messaging")) {
            AddressSpaceType type = new StandardAddressSpaceType();
            AddressSpace.Builder builder = new AddressSpace.Builder()
                    .setName("default")
                    .setNamespace(kubernetes.getNamespace())
                    .setType(type)
                    .setAuthenticationService(new AuthenticationService.Builder()
                            .setType(AuthenticationServiceType.NONE)
                            .build())
                    .setPlan(type.getDefaultPlan());

            Optional<CertProvider> certProvider = options.certSecret().map(SecretCertProvider::new);

            options.messagingHost().ifPresent(host ->
                    appendEndpoint(certProvider, "messaging", "messaging", host));
            options.mqttHost().ifPresent(host ->
                    appendEndpoint(certProvider, "mqtt", "mqtt", host));
            options.consoleHost().ifPresent(host ->
                    appendEndpoint(certProvider, "console", "console", host));
            addressSpaceApi.createAddressSpace(builder.build());
        }

        CertManager certManager = SelfSignedCertManager.create(controllerClient);

        deployVerticles(startPromise,
                new Deployment(new AuthController(certManager, addressSpaceApi)),
                new Deployment(new StandardController(controllerClient, addressSpaceApi, kubernetes, createResolverFactory(options), options.isMultiinstance())),
//                new Deployment(new AMQPServer(kubernetes.getNamespace(), addressSpaceApi, options.port())),
                new Deployment(new HTTPServer(addressSpaceApi, options.certDir(), options.osbAuth()), new DeploymentOptions().setWorker(true)));
    }

    private AuthenticationServiceResolverFactory createResolverFactory(ControllerOptions options) {
        Map<AuthenticationServiceType, AuthenticationServiceResolver> resolverMap = new HashMap<>();
        options.getNoneAuthService().ifPresent(authService -> {
            resolverMap.put(AuthenticationServiceType.NONE, new NoneAuthenticationServiceResolver(authService.getHost(), authService.getPort()));
        });

        options.getStandardAuthService().ifPresent(authService -> {
            resolverMap.put(AuthenticationServiceType.STANDARD, new NoneAuthenticationServiceResolver(authService.getHost(), authService.getPort()));
        });

        resolverMap.put(AuthenticationServiceType.EXTERNAL, new ExternalAuthenticationServiceResolver());


        return type -> {
            AuthenticationServiceResolver resolver = resolverMap.get(type);
            if (resolver == null) {
                throw new IllegalArgumentException("Unsupported resolver of type " + type);
            }
            return resolver;
        };
    }

    private Endpoint appendEndpoint(Optional<CertProvider> certProvider, String name, String service, String host) {
        return new Endpoint.Builder()
                .setCertProvider(certProvider.orElse(null))
                .setName(name)
                .setService(service)
                .setHost(host)
                .build();
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
            vertx.deployVerticle(new Controller(ControllerOptions.fromEnv(System.getenv())));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting address controller: " + e.getMessage());
            System.exit(1);
        }
    }
}
