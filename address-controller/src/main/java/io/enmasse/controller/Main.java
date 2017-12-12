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

import java.time.Clock;
import java.util.*;

import io.enmasse.address.model.AuthenticationServiceResolver;
import io.enmasse.address.model.AuthenticationServiceType;
import io.enmasse.address.model.CertProvider;
import io.enmasse.address.model.Endpoint;
import io.enmasse.controller.auth.*;
import io.enmasse.controller.common.*;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.KubeEventLogger;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

public class Main extends AbstractVerticle {
    private final OpenShiftClient controllerClient;
    private final ControllerOptions options;
    private final Kubernetes kubernetes;

    private Main(ControllerOptions options) throws Exception {
        this.controllerClient = new DefaultOpenShiftClient(new ConfigBuilder()
                .withMasterUrl(options.getMasterUrl())
                .withOauthToken(options.getToken())
                .withNamespace(options.getNamespace())
                .build());
        this.options = options;
        this.kubernetes = new KubernetesHelper(options.getNamespace(), controllerClient, options.getToken(), options.getEnvironment(), options.getTemplateDir(), options.getAddressControllerSa(), options.getAddressSpaceAdminSa(), options.isEnableRbac());
    }

    @Override
    public void start(Future<Void> startPromise) {
        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(controllerClient);
        EventLogger eventLogger = new KubeEventLogger(controllerClient, controllerClient.getNamespace(), Clock.systemUTC(), "enmasse-controller");

        CertManager certManager = OpenSSLCertManager.create(controllerClient, options.getNamespace());
        AuthenticationServiceResolverFactory resolverFactory = createResolverFactory(options);
        EventLogger authEventLogger = new KubeEventLogger(controllerClient, controllerClient.getNamespace(), Clock.systemUTC(), "auth-controller");
        AuthController authController = new AuthController(certManager, authEventLogger);

        deployVerticles(startPromise,
                new Deployment(new Controller(controllerClient, addressSpaceApi, kubernetes, resolverFactory, eventLogger, authController)),
//                new Deployment(new AMQPServer(kubernetes.getNamespace(), addressSpaceApi, options.port())),
                new Deployment(new HTTPServer(addressSpaceApi, options.getCertDir(), kubernetes, kubernetes.isRBACSupported()), new DeploymentOptions().setWorker(true)));
    }

    private AuthenticationServiceResolverFactory createResolverFactory(ControllerOptions options) {
        Map<AuthenticationServiceType, AuthenticationServiceResolver> resolverMap = new HashMap<>();
        options.getNoneAuthService().ifPresent(authService -> {
            resolverMap.put(AuthenticationServiceType.NONE, new NoneAuthenticationServiceResolver(authService.getHost(), authService.getAmqpPort()));
        });

        options.getStandardAuthService().ifPresent(authService -> {
            resolverMap.put(AuthenticationServiceType.STANDARD, new StandardAuthenticationServiceResolver(authService.getHost(), authService.getAmqpPort()));
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

    private Endpoint appendEndpoint(CertProvider certProvider, String name, String service, String host) {
        return new Endpoint.Builder()
                .setCertProvider(certProvider)
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
            vertx.deployVerticle(new Main(ControllerOptions.fromEnv(System.getenv())));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting address controller: " + e.getMessage());
            System.exit(1);
        }
    }
}
