/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import java.time.Clock;
import java.util.*;

import io.enmasse.address.model.*;
import io.enmasse.controller.auth.*;
import io.enmasse.controller.common.*;
import io.enmasse.k8s.api.*;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

public class Main extends AbstractVerticle {
    private final NamespacedOpenShiftClient controllerClient;
    private final ControllerOptions options;
    private final Kubernetes kubernetes;

    private Main(ControllerOptions options) {
        this.controllerClient = new DefaultOpenShiftClient(new ConfigBuilder()
                .withMasterUrl(options.getMasterUrl())
                .withOauthToken(options.getToken())
                .withNamespace(options.getNamespace())
                .build());
        this.options = options;
        this.kubernetes = new KubernetesHelper(options.getNamespace(), controllerClient, options.getToken(), options.getEnvironment(), options.getTemplateDir(), options.getAddressControllerSa(), options.getAddressSpaceAdminSa(), options.isEnableRbac());
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        SchemaApi schemaApi = new ConfigMapSchemaApi(controllerClient, options.getNamespace());
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider(schemaApi);
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());

        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(controllerClient);
        EventLogger eventLogger = options.isEnableEventLogger() ? new KubeEventLogger(controllerClient, controllerClient.getNamespace(), Clock.systemUTC(), "enmasse-controller")
                : new LogEventLogger();

        CertManager certManager = OpenSSLCertManager.create(controllerClient);
        AuthenticationServiceResolverFactory resolverFactory = createResolverFactory(options);
        CertProviderFactory certProviderFactory = createCertProviderFactory(options, certManager);
        AuthController authController = new AuthController(certManager, eventLogger, certProviderFactory);

        InfraResourceFactory infraResourceFactory = new TemplateInfraResourceFactory(kubernetes, schemaProvider, resolverFactory, authController.getDefaultCertProvider());

        ControllerChain controllerChain = new ControllerChain(kubernetes, addressSpaceApi, schemaProvider, eventLogger, options.getRecheckInterval(), options.getResyncInterval());
        controllerChain.addController(new CreateController(kubernetes, schemaProvider, infraResourceFactory, kubernetes.getNamespace(), eventLogger));
        controllerChain.addController(new StatusController(kubernetes, infraResourceFactory));
        controllerChain.addController(new EndpointController(controllerClient));
        controllerChain.addController(authController);

        deployVerticles(startPromise,
                new Deployment(controllerChain),
                new Deployment(new HTTPServer(addressSpaceApi, schemaProvider, options.getCertDir(), kubernetes, kubernetes.isRBACSupported()), new DeploymentOptions().setWorker(true)));
    }

    private CertProviderFactory createCertProviderFactory(ControllerOptions options, CertManager certManager) {
        return new CertProviderFactory() {
            @Override
            public CertProvider createProvider(CertSpec certSpec) {
                if ("wildcard".equals(certSpec.getProvider())) {
                    String secretName = options.getWildcardCertSecret();
                    return new WildcardCertProvider(controllerClient, certSpec, secretName);
                } else {
                    return new SelfsignedCertProvider(controllerClient, certSpec, certManager);
                }
            }

            @Override
            public String getDefaultProviderName() {
                return "selfsigned";
            }
        };
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
