/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.api.common.OpenShift;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.auth.AuthController;
import io.enmasse.controller.auth.CertBundleCertProvider;
import io.enmasse.controller.auth.CertManager;
import io.enmasse.controller.auth.CertProvider;
import io.enmasse.controller.auth.OpenSSLCertManager;
import io.enmasse.controller.auth.OpenshiftCertProvider;
import io.enmasse.controller.auth.SelfsignedCertProvider;
import io.enmasse.controller.auth.WildcardCertProvider;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.controller.common.ControllerReason;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.KubernetesHelper;
import io.enmasse.controller.keycloak.RealmController;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.k8s.api.CachingSchemaProvider;
import io.enmasse.k8s.api.ConfigMapAddressSpaceApi;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.KubeAddressSpaceApi;
import io.enmasse.k8s.api.KubeEventLogger;
import io.enmasse.k8s.api.KubeResourceApplier;
import io.enmasse.k8s.api.KubeSchemaApi;
import io.enmasse.k8s.api.LogEventLogger;
import io.enmasse.k8s.api.SchemaAuthenticationServiceRegistry;
import io.enmasse.metrics.api.Metrics;
import io.enmasse.model.CustomResourceDefinitions;
import io.enmasse.user.keycloak.KeycloakFactory;
import io.enmasse.user.keycloak.KeycloakRealmApi;
import io.enmasse.user.keycloak.KubeKeycloakFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;
import io.fabric8.kubernetes.client.utils.HttpClientUtils;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

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
    private RouterStatusCache routerStatusCache;

    private AddressSpaceController(AddressSpaceControllerOptions options) {
        Config config = new ConfigBuilder().build();
        OkHttpClient httpClient = HttpClientUtils.createHttpClient(config);
        httpClient = httpClient.newBuilder()
                .connectTimeout(options.getKubernetesApiConnectTimeout())
                .writeTimeout(options.getKubernetesApiWriteTimeout())
                .readTimeout(options.getKubernetesApiReadTimeout())
                .build();
        this.controllerClient = new DefaultKubernetesClient(httpClient, config);
        this.options = options;
    }

    public void start() throws Exception {
        boolean isOpenShift = OpenShift.isOpenShift(controllerClient);
        KubeSchemaApi schemaApi = KubeSchemaApi.create(controllerClient, controllerClient.getNamespace(), options.getVersion(), isOpenShift, true);

        log.info("AddressSpaceController starting with options: {}", options);
        if (options.isInstallDefaultResources()) {
            configureDefaultResources(controllerClient, options.getResourcesDir());
        }
        AddressSpaceSchemaUpdater addressSpaceSchemaUpdater = new AddressSpaceSchemaUpdater(controllerClient);
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider();
        schemaProvider.registerListener(addressSpaceSchemaUpdater);

        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());
        Kubernetes kubernetes = new KubernetesHelper(controllerClient.getNamespace(), controllerClient, options.getTemplateDir(), isOpenShift);

        AddressSpaceApi addressSpaceApi = KubeAddressSpaceApi.create(controllerClient, null, options.getVersion());
        EventLogger eventLogger = options.isEnableEventLogger() ? new KubeEventLogger(controllerClient, controllerClient.getNamespace(), Clock.systemUTC(), "address-space-controller")
                : new LogEventLogger();

        // Convert all address spaces from configmaps to CRD variant

        CertManager certManager = OpenSSLCertManager.create(controllerClient);
        CertProviderFactory certProviderFactory = createCertProviderFactory(options, certManager);
        AuthController authController = new AuthController(certManager, eventLogger, certProviderFactory);
        AuthenticationServiceRegistry authenticationServiceRegistry = new SchemaAuthenticationServiceRegistry(schemaProvider);
        AuthenticationServiceResolver authenticationServiceResolver = new AuthenticationServiceResolver(authenticationServiceRegistry);

        InfraResourceFactory infraResourceFactory = new TemplateInfraResourceFactory(kubernetes, System.getenv(), schemaProvider);

        Clock clock = Clock.systemUTC();
        KeycloakFactory keycloakFactory = new KubeKeycloakFactory(controllerClient);
        KeycloakRealmApi keycloakRealmApi = new KeycloakRealmApi(keycloakFactory, clock, Duration.ZERO);
        schemaProvider.registerListener(newSchema -> keycloakRealmApi.retainAuthenticationServices(newSchema.findAuthenticationServiceType(AuthenticationServiceType.standard)));

        routerStatusCache = new RouterStatusCache(eventLogger, options.getRouterStatusCheckInterval(), controllerClient, controllerClient.getNamespace(), options.getManagementConnectTimeout(), options.getManagementQueryTimeout());
        routerStatusCache.start();

        Metrics metrics = new Metrics();
        controllerChain = new ControllerChain(addressSpaceApi, schemaProvider, eventLogger, options.getRecheckInterval(), options.getResyncInterval());
        controllerChain.addController(new DefaultsController(authenticationServiceRegistry));
        controllerChain.addController(new AddressFinalizerController(addressSpaceApi));
        controllerChain.addController(new MessagingUserFinalizerController(controllerClient));
        controllerChain.addController(new ComponentFinalizerController(kubernetes));
        controllerChain.addController(new RealmFinalizerController(keycloakRealmApi, authenticationServiceRegistry));
        controllerChain.addController(new CreateController(kubernetes, schemaProvider, infraResourceFactory, eventLogger, authController.getDefaultCertProvider(), options.getVersion(), addressSpaceApi, authenticationServiceResolver));
        controllerChain.addController(new RouterConfigController(controllerClient, controllerClient.getNamespace(), authenticationServiceResolver));
        controllerChain.addController(new PodDisruptionBudgetController(controllerClient, controllerClient.getNamespace()));
        controllerChain.addController(new RealmController(keycloakRealmApi, authenticationServiceRegistry));
        controllerChain.addController(new NetworkPolicyController(controllerClient));
        controllerChain.addController(new StatusController(kubernetes, schemaProvider, infraResourceFactory, authenticationServiceRegistry, keycloakRealmApi, routerStatusCache));
        controllerChain.addController(routerStatusCache);
        controllerChain.addController(new EndpointController(controllerClient, options.isExposeEndpointsByDefault(), isOpenShift));
        controllerChain.addController(new ExportsController(controllerClient));
        controllerChain.addController(authController);
        controllerChain.addController(new MetricsReporterController(metrics, options.getVersion()));

        metricsServer = new HTTPServer(8080, metrics);
        metricsServer.start();

        convertLegacyConfigMaps(controllerClient, options, addressSpaceApi, eventLogger);

        controllerChain.start();
    }

    private static void convertLegacyConfigMaps(NamespacedKubernetesClient controllerClient, AddressSpaceControllerOptions options, AddressSpaceApi addressSpaceApi, EventLogger eventLogger) throws Exception {
        ConfigMapAddressSpaceApi legacyAddressSpaceApi = new ConfigMapAddressSpaceApi(controllerClient, options.getVersion());

        for (AddressSpace legacy : legacyAddressSpaceApi.listAllAddressSpaces()) {
            String globalName = legacy.getMetadata().getNamespace() + "." + legacy.getMetadata().getName();
            try {
                legacy.getMetadata().setResourceVersion(null);
                addressSpaceApi.createAddressSpace(legacy);

                // Should not fail, but if it does it will be handled below
                AddressSpace created = addressSpaceApi.getAddressSpaceWithName(legacy.getMetadata().getNamespace(), legacy.getMetadata().getName()).get();

                AddressApi addressApi = addressSpaceApi.withAddressSpace(created);
                AddressApi legacyAddressApi = legacyAddressSpaceApi.withAddressSpace(legacy);
                List<Address> legacyAddresses = legacyAddressApi.listAddresses(legacy.getMetadata().getNamespace()).stream().filter(address -> address.getMetadata().getName().startsWith(legacy.getMetadata().getName())).collect(Collectors.toList());
                for (Address address : legacyAddresses) {
                    address.getMetadata().setResourceVersion(null);
                    addressApi.createAddress(address);
                    log.info("Converted address {} in {} from ConfigMap to Address CRD", address.getMetadata().getName(), address.getMetadata().getNamespace());
                }
                String message = String.format("Converted address space %s in %s from ConfigMap to AddressSpace CRD. %d addresses converted", legacy.getMetadata().getName(), legacy.getMetadata().getNamespace(), legacyAddresses.size());
                eventLogger.log(ControllerReason.AddressSpaceConverted, message, EventLogger.Type.Normal, ControllerKind.AddressSpace, globalName);
            } catch (Exception e) {
                eventLogger.log(ControllerReason.AddressSpaceConversionFailed, String.format("Error converting address space %s: %s", legacy.getMetadata().getName(), e.getMessage()), EventLogger.Type.Warning, ControllerKind.AddressSpace, globalName);
                throw e;
            }
        }

        try {
            log.info("Deleting all Address ConfigMaps");
            controllerClient.configMaps().inNamespace(controllerClient.getNamespace()).withLabel(LabelKeys.TYPE, "address-config").delete();
            log.info("Deleting all AddressSpace ConfigMaps");
            controllerClient.configMaps().inNamespace(controllerClient.getNamespace()).withLabel(LabelKeys.TYPE, "address-space").delete();
            log.info("Conversion completed");
        } catch (Exception e) {
            log.warn("Error deleting legacy ConfigMaps");
            throw e;
        }
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
                try {
                    if (routerStatusCache != null) {
                        try {
                            routerStatusCache.stop();
                        } catch (Exception ignore) {
                        }
                    }
                } finally {
                    log.info("AddressSpaceController stopped");
                }
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
            final AddressSpaceControllerOptions options = AddressSpaceControllerOptions.fromEnv(System.getenv());
            log.info("AddressSpaceController starting with options: {}", options);
            controller = new AddressSpaceController(options);
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
