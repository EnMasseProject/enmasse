/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import java.io.*;
import java.time.Clock;
import java.time.Duration;
import java.util.*;

import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.controller.auth.*;
import io.enmasse.controller.common.*;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.k8s.api.*;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.keycloak.KeycloakFactory;
import io.enmasse.user.keycloak.KeycloakUserApi;
import io.enmasse.user.keycloak.KubeKeycloakFactory;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddressSpaceController {
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

    private HTTPServer metricsServer;
    private ControllerChain controllerChain;

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

    public void start() throws Exception {
        boolean isOpenShift = isOpenShift(controllerClient);
        KubeSchemaApi schemaApi = KubeSchemaApi.create(controllerClient, controllerClient.getNamespace(), isOpenShift);

        log.info("AddressSpaceController starting with options: {}", options);
        configureDefaultResources(controllerClient, options.getResourcesDir());
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());
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

        Metrics metrics = new Metrics();
        controllerChain = new ControllerChain(kubernetes, addressSpaceApi, schemaProvider, eventLogger, metrics, options.getVersion(), options.getRecheckInterval(), options.getResyncInterval());
        controllerChain.addController(new CreateController(kubernetes, schemaProvider, infraResourceFactory, eventLogger, authController.getDefaultCertProvider(), options.getVersion(), addressSpaceApi));
        controllerChain.addController(new NetworkPolicyController(controllerClient, schemaProvider));
        controllerChain.addController(new StatusController(kubernetes, schemaProvider, infraResourceFactory, userApi));
        controllerChain.addController(new EndpointController(controllerClient, options.isExposeEndpointsByDefault()));
        controllerChain.addController(authController);
        controllerChain.start();

        metricsServer = new HTTPServer(8080, metrics);
        metricsServer.start();
    }

    private void stop() {
        try {
            log.info("AddressSpaceController stopping");

            if (metricsServer != null) {
                metricsServer.stop();
            }
        } finally {
            try {
                if (controllerChain != null) {
                    try {
                        controllerChain.stop();
                    } catch (Exception ignore) {
                    }
                }
            } finally {
                controllerClient.close();
                log.info("AddressSpaceController stopped");
            }
        }
    }

    private void configureDefaultResources(NamespacedOpenShiftClient client, File resourcesDir) {
        String namespace = client.getNamespace();
        KubeResourceApplier.applyIfDifferent(new File(resourcesDir, "brokeredinfraconfigs"),
                client.customResources(AdminCrd.brokeredInfraConfigs(), BrokeredInfraConfig.class, BrokeredInfraConfigList.class, DoneableBrokeredInfraConfig.class).inNamespace(namespace),
                BrokeredInfraConfig.class,
                Comparator.comparing(BrokeredInfraConfig::getVersion));

        KubeResourceApplier.applyIfDifferent(new File(resourcesDir, "standardinfraconfigs"),
                client.customResources(AdminCrd.standardInfraConfigs(), StandardInfraConfig.class, StandardInfraConfigList.class, DoneableStandardInfraConfig.class).inNamespace(namespace),
                StandardInfraConfig.class,
                Comparator.comparing(StandardInfraConfig::getVersion));

        KubeResourceApplier.createIfNoneExists(new File(resourcesDir, "addressplans"),
                client.customResources(AdminCrd.addressPlans(), AddressPlan.class, AddressPlanList.class, DoneableAddressPlan.class).inNamespace(namespace),
                AddressPlan.class);

        KubeResourceApplier.createIfNoneExists(new File(resourcesDir, "addressspaceplans"),
                client.customResources(AdminCrd.addressSpacePlans(), AddressSpacePlan.class, AddressSpacePlanList.class, DoneableAddressSpacePlan.class).inNamespace(namespace),
                AddressSpacePlan.class);
    }

    private CertProviderFactory createCertProviderFactory(AddressSpaceControllerOptions options, CertManager certManager) {
        return new CertProviderFactory() {
            @Override
            public CertProvider createProvider(String provider) {
                if ("wildcard".equals(provider)) {
                    String secretName = options.getWildcardCertSecret();
                    return new WildcardCertProvider(controllerClient, secretName);
                } else if ("openshift".equals(provider)) {
                    return new OpenshiftCertProvider(controllerClient);
                } else if ("certBundle".equals(provider)) {
                    return new CertBundleCertProvider(controllerClient);
                } else {
                    return new SelfsignedCertProvider(controllerClient, certManager);
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

    public static void main(String args[]) {
        AddressSpaceController controller = null;
        try {
            controller = new AddressSpaceController(AddressSpaceControllerOptions.fromEnv(System.getenv()));
            controller.start();
        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting address space controller: " + e.getMessage());
            System.exit(1);
        } finally {
            if (controller != null) {
                Runtime.getRuntime().addShutdownHook(new Thread(controller::stop));
            }
        }
    }
}
