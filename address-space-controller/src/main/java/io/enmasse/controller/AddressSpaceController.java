/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import io.enmasse.address.model.Schema;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.controller.auth.*;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.KubernetesHelper;
import io.enmasse.controller.keycloak.RealmController;
import io.enmasse.k8s.api.*;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.model.CustomResourceDefinitions;
import io.enmasse.user.api.DelegateUserApi;
import io.enmasse.user.api.NullUserApi;
import io.enmasse.user.api.UserApi;
import io.enmasse.user.keycloak.KeycloakFactory;
import io.enmasse.user.keycloak.KeycloakUserApi;
import io.enmasse.user.keycloak.KubeKeycloakFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

public class AddressSpaceController {
    private static final Logger log = LoggerFactory.getLogger(AddressSpaceController.class.getName());
    private final NamespacedKubernetesClient controllerClient;
    private final AddressSpaceControllerOptions options;

    static {
        try {
            CustomResourceDefinitions.registerAll();
        } catch (RuntimeException t) {
            t.printStackTrace();
            throw new ExceptionInInitializerError(t);
        }
    }

    private HTTPServer metricsServer;
    private ControllerChain controllerChain;

    private AddressSpaceController(AddressSpaceControllerOptions options) {
        this.controllerClient = new DefaultKubernetesClient();
        this.options = options;
    }

    private static boolean isOpenShift(NamespacedKubernetesClient client) {
        // Need to query the full API path because Kubernetes does not allow GET on /
        OkHttpClient httpClient = client.adapt(OkHttpClient.class);
        HttpUrl url = HttpUrl.get(client.getMasterUrl()).resolve("/apis/route.openshift.io");
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
        KubeSchemaApi schemaApi = KubeSchemaApi.create(controllerClient, controllerClient.getNamespace(), options.getVersion(), isOpenShift);

        log.info("AddressSpaceController starting with options: {}", options);
        if (options.isInstallDefaultResources()) {
            configureDefaultResources(controllerClient, options.getResourcesDir());
        }
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());
        Kubernetes kubernetes = new KubernetesHelper(controllerClient.getNamespace(), controllerClient, options.getTemplateDir(), isOpenShift);

        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(controllerClient);
        EventLogger eventLogger = options.isEnableEventLogger() ? new KubeEventLogger(controllerClient, controllerClient.getNamespace(), Clock.systemUTC(), "address-space-controller")
                : new LogEventLogger();

        CertManager certManager = OpenSSLCertManager.create(controllerClient);
        CertProviderFactory certProviderFactory = createCertProviderFactory(options, certManager);
        AuthController authController = new AuthController(certManager, eventLogger, certProviderFactory);
        AuthenticationServiceRegistry authenticationServiceRegistry = new SchemaAuthenticationServiceRegistry(schemaProvider);

        InfraResourceFactory infraResourceFactory = new TemplateInfraResourceFactory(kubernetes, authenticationServiceRegistry, System.getenv(), isOpenShift, schemaProvider);

        Clock clock = Clock.systemUTC();
        KeycloakFactory keycloakFactory = new KubeKeycloakFactory(controllerClient);
        KeycloakUserApi keycloakUserApi = new KeycloakUserApi(keycloakFactory, clock, Duration.ZERO);
        schemaProvider.registerListener(newSchema -> keycloakUserApi.pruneAuthenticationServices(newSchema.findAuthenticationServiceType(AuthenticationServiceType.standard)));
        UserApi userApi = new DelegateUserApi(Map.of(AuthenticationServiceType.none, new NullUserApi(),
                AuthenticationServiceType.external, new NullUserApi(),
                AuthenticationServiceType.standard, keycloakUserApi));

        Metrics metrics = new Metrics();
        controllerChain = new ControllerChain(kubernetes, addressSpaceApi, schemaProvider, eventLogger, metrics, options.getVersion(), options.getRecheckInterval(), options.getResyncInterval());
        controllerChain.addController(new CreateController(kubernetes, schemaProvider, infraResourceFactory, eventLogger, authController.getDefaultCertProvider(), options.getVersion(), addressSpaceApi));
        controllerChain.addController(new RealmController(keycloakUserApi, authenticationServiceRegistry));
        controllerChain.addController(new NetworkPolicyController(controllerClient, schemaProvider));
        controllerChain.addController(new StatusController(kubernetes, schemaProvider, infraResourceFactory, authenticationServiceRegistry, userApi));
        controllerChain.addController(new EndpointController(controllerClient, options.isExposeEndpointsByDefault(), isOpenShift));
        controllerChain.addController(new ExportsController(controllerClient));
        controllerChain.addController(authController);
        controllerChain.addController(new DeleteController(kubernetes));
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

    private void configureDefaultResources(NamespacedKubernetesClient client, File resourcesDir) {
        String namespace = client.getNamespace();

        KubeResourceApplier.applyIfDifferent(new File(resourcesDir, "configmaps"),
                client.configMaps().inNamespace(namespace),
                ConfigMap.class,
                (c1, c2) -> c1.getData().equals(c2.getData()) ? 0 : -1);
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
