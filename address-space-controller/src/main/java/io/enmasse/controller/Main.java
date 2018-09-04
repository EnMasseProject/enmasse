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
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.NamespacedOpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class.getName());

    public static void run(ControllerOptions options, NamespacedOpenShiftClient controllerClient) throws Exception {
        SchemaApi schemaApi = new ConfigMapSchemaApi(controllerClient, controllerClient.getNamespace());
        CachingSchemaProvider schemaProvider = new CachingSchemaProvider(schemaApi);
        schemaApi.watchSchema(schemaProvider, options.getResyncInterval());
        Kubernetes kubernetes = new KubernetesHelper(controllerClient.getNamespace(), controllerClient, controllerClient.getConfiguration().getOauthToken(), options.getEnvironment(), options.getTemplateDir(), options.getAddressControllerSa(), options.getAddressSpaceAdminSa(), options.isEnableRbac(), options.getImpersonateUser());

        AddressSpaceApi addressSpaceApi = new ConfigMapAddressSpaceApi(controllerClient);
        EventLogger eventLogger = options.isEnableEventLogger() ? new KubeEventLogger(controllerClient, controllerClient.getNamespace(), Clock.systemUTC(), "enmasse-controller")
                : new LogEventLogger();

        CertManager certManager = OpenSSLCertManager.create(controllerClient);
        AuthenticationServiceResolverFactory resolverFactory = createResolverFactory(options, controllerClient);
        CertProviderFactory certProviderFactory = createCertProviderFactory(options, controllerClient, certManager);
        AuthController authController = new AuthController(certManager, eventLogger, certProviderFactory);

        InfraResourceFactory infraResourceFactory = new TemplateInfraResourceFactory(kubernetes, schemaProvider, resolverFactory);

        ControllerChain controllerChain = new ControllerChain(kubernetes, addressSpaceApi, schemaProvider, eventLogger, options.getRecheckInterval(), options.getResyncInterval());
        controllerChain.addController(new CreateController(kubernetes, schemaProvider, infraResourceFactory, kubernetes.getNamespace(), eventLogger, authController.getDefaultCertProvider()));
        controllerChain.addController(new StatusController(kubernetes, infraResourceFactory));
        controllerChain.addController(new EndpointController(controllerClient, options.isExposeEndpointsByDefault()));
        controllerChain.addController(authController);

        controllerChain.start();

        HTTPServer httpServer = new HTTPServer(8080);
        httpServer.start();
    }

    private static CertProviderFactory createCertProviderFactory(ControllerOptions options, NamespacedOpenShiftClient controllerClient, CertManager certManager) {
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

    private static AuthenticationServiceResolverFactory createResolverFactory(ControllerOptions options, NamespacedOpenShiftClient controllerClient) {
        Map<AuthenticationServiceType, AuthenticationServiceResolver> resolverMap = new HashMap<>();
        options.getNoneAuthService().ifPresent(authService -> {
            resolverMap.put(AuthenticationServiceType.NONE, new NoneAuthenticationServiceResolver(authService.getHost(), authService.getAmqpPort()));
        });

        options.getStandardAuthService().ifPresent(authService -> {
            ConfigMap config = controllerClient.configMaps().withName(authService.getConfigMap()).get();
            if (config != null) {
                resolverMap.put(AuthenticationServiceType.STANDARD, new StandardAuthenticationServiceResolver(
                        config.getData().get("hostname"),
                        Integer.parseInt(config.getData().get("port")),
                        Boolean.valueOf(config.getData().get("oauthDisabled")) ? null: config.getData().get("httpUrl"),
                        config.getData().get("caSecretName")));
            } else {
                log.warn("Skipping standard authentication service: configmap {} not found", authService.getConfigMap());
            }
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

    public static void main(String args[]) {
        try {
            ControllerOptions options = ControllerOptions.fromEnv(System.getenv());
            NamespacedOpenShiftClient client = new DefaultOpenShiftClient();

            run(options, client);

        } catch (IllegalArgumentException e) {
            System.out.println(String.format("Unable to parse arguments: %s", e.getMessage()));
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Error starting address space controller: " + e.getMessage());
            System.exit(1);
        }
    }
}
