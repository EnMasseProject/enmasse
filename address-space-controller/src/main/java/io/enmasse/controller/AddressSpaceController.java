/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.util.*;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.AdminCrd;
import io.enmasse.api.common.CachingSchemaProvider;
import io.enmasse.controller.auth.*;
import io.enmasse.controller.common.*;
import io.enmasse.k8s.api.*;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.keycloak.KeycloakFactory;
import io.enmasse.user.keycloak.KeycloakUserApi;
import io.enmasse.user.keycloak.KubeKeycloakFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressSpaceController extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(AddressSpaceController.class.getName());
    private final NamespacedOpenShiftClient controllerClient;
    private final AddressSpaceControllerOptions options;

    static {
        try {
            AdminCrd.registerCustomCrds();
        } catch (RuntimeException t) {
            t.printStackTrace();
            throw new ExceptionInInitializerError(t);
        }
    }

    private AddressSpaceController(AddressSpaceControllerOptions options) {
        this.controllerClient = new DefaultOpenShiftClient();
        this.options = options;
    }

    private static boolean isOpenShift(NamespacedOpenShiftClient client) {
        // Need to query the full API path because Kubernetes does not allow GET on /
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);
        HttpUrl url = HttpUrl.get(client.getOpenshiftUrl()).resolve("/apis/route.openshift.io");
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            return response.code() >= 200 && response.code() < 300;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void start(Future<Void> startPromise) throws Exception {
        SchemaApi schemaApi = KubeSchemaApi.create(controllerClient, controllerClient.getNamespace());
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());
        boolean isOpenShift = isOpenShift(controllerClient);
        Kubernetes kubernetes = new KubernetesHelper(controllerClient.getNamespace(), controllerClient, controllerClient.getConfiguration().getOauthToken(), options.getTemplateDir(), isOpenShift);

        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(controllerClient);
        EventLogger eventLogger = options.isEnableEventLogger() ? new KubeEventLogger(controllerClient, controllerClient.getNamespace(), Clock.systemUTC(), "enmasse-controller")
                : new LogEventLogger();

        CertManager certManager = OpenSSLCertManager.create(controllerClient);
        AuthenticationServiceResolverFactory resolverFactory = createResolverFactory(options);
        CertProviderFactory certProviderFactory = createCertProviderFactory(options, certManager);
        AuthController authController = new AuthController(certManager, eventLogger, certProviderFactory);

        InfraResourceFactory infraResourceFactory = new TemplateInfraResourceFactory(kubernetes, resolverFactory, isOpenShift);

        KeycloakFactory keycloakFactory = new KubeKeycloakFactory(controllerClient,
                options.getStandardAuthserviceConfigName(),
                options.getStandardAuthserviceCredentialsSecretName(),
                options.getStandardAuthserviceCertSecretName());
        Clock clock = Clock.systemUTC();
        UserApi userApi = new KeycloakUserApi(keycloakFactory, clock, Duration.ZERO);

        ControllerChain controllerChain = new ControllerChain(kubernetes, addressSpaceApi, schemaProvider, eventLogger, options.getRecheckInterval(), options.getResyncInterval());
        controllerChain.addController(new CreateController(kubernetes, schemaProvider, infraResourceFactory, eventLogger, authController.getDefaultCertProvider(), options.getVersion()));
        controllerChain.addController(new StatusController(kubernetes, schemaProvider, infraResourceFactory, userApi));
        controllerChain.addController(new EndpointController(controllerClient, options.isExposeEndpointsByDefault(), isOpenShift));
        controllerChain.addController(authController);

        HTTPServer httpServer = new HTTPServer(8080);

        deployVerticles(startPromise, new Deployment(controllerChain), new Deployment(httpServer));
    }

    private CertProviderFactory createCertProviderFactory(AddressSpaceControllerOptions options, CertManager certManager) {
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
                if (options.getWildcardCertSecret() != null) {
                    return "wildcard";
                } else {
                    return "selfsigned";
                }
            }
        };
    }

    private AuthenticationServiceResolverFactory createResolverFactory(AddressSpaceControllerOptions options) {

        return type -> {
            AuthenticationServiceResolver resolver = createAuthServiceResolver(type, options);
            if (resolver == null) {
                throw new IllegalArgumentException("Unsupported resolver of type " + type);
            }
            return resolver;
        };
    }

    private AuthenticationServiceResolver createAuthServiceResolver(AuthenticationServiceType type, AddressSpaceControllerOptions options) {
        AuthenticationServiceResolver resolver = null;
        switch (type) {
            case NONE:
                resolver = options.getNoneAuthService().map(authService -> new NoneAuthenticationServiceResolver(authService.getHost(), authService.getAmqpPort())).orElse(null);
                break;
            case STANDARD:
                resolver = options.getStandardAuthService().map(authService -> {
                    ConfigMap config = controllerClient.configMaps().withName(authService.getConfigMap()).get();
                    if (config != null) {
                        return new StandardAuthenticationServiceResolver(
                                config.getData().get("hostname"),
                                Integer.parseInt(config.getData().get("port")),
                                config.getData().get("oauthUrl"),
                                config.getData().get("caSecretName"));
                    } else {
                        log.warn("Skipping standard authentication service: configmap {} not found", authService.getConfigMap());
                        return null;
                    }
                }).orElse(null);
                break;
            case EXTERNAL:
                resolver = new ExternalAuthenticationServiceResolver();
                break;
        }

        return resolver;
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
            vertx.deployVerticle(new AddressSpaceController(AddressSpaceControllerOptions.fromEnv(System.getenv())));
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting address space controller: " + e.getMessage());
            System.exit(1);
        }
    }
}
