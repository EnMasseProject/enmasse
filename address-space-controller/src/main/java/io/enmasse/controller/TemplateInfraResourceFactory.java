/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.*;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.admin.model.v1.AuthenticationServiceType;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.TemplateParameter;
import io.enmasse.k8s.api.AuthenticationServiceRegistry;
import io.enmasse.k8s.api.SchemaProvider;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.SecretReference;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;

import static io.enmasse.address.model.KubeUtil.applyPodTemplate;
import static io.enmasse.address.model.KubeUtil.lookupResource;

public class TemplateInfraResourceFactory implements InfraResourceFactory {
    private static final String FS_GROUP_OVERRIDE = "FS_GROUP_OVERRIDE";
    private static final Logger log = LoggerFactory.getLogger(TemplateInfraResourceFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final String WELL_KNOWN_CONSOLE_SERVICE_NAME = "console";

    private final Kubernetes kubernetes;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private final Map<String, String> env;
    private final SchemaProvider schemaProvider;
    private final Long fsGroupOverride;

    public TemplateInfraResourceFactory(Kubernetes kubernetes, AuthenticationServiceRegistry authenticationServiceRegistry, Map<String, String> env, boolean openShift, SchemaProvider schemaProvider) {
        this.kubernetes = kubernetes;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
        this.env = env;
        this.schemaProvider = schemaProvider;
        this.fsGroupOverride = getFsGroupOverride();
    }

    private void prepareParameters(InfraConfig infraConfig,
                                   AddressSpace addressSpace,
                                   Map<String, String> parameters) {

        AuthenticationService authService = authenticationServiceRegistry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService())
                .orElseThrow(() -> new IllegalArgumentException("Unable to find authentication service " + addressSpace.getSpec().getAuthenticationService()));

        if (authService.getStatus() == null) {
            throw new IllegalArgumentException("Authentication service '" + authService.getMetadata().getName() + "' is not yet deployed");
        }

        String authServiceHost = authService.getStatus().getHost();
        int authServicePort = authService.getStatus().getPort();
        String authServiceRealm = authService.getSpec().getRealm() != null ? authService.getSpec().getRealm() : addressSpace.getAnnotation(AnnotationKeys.REALM_NAME);
        SecretReference authServiceCaCertSecret = authService.getStatus().getCaCertSecret();
        SecretReference authServiceClientCertSecret = authService.getStatus().getClientCertSecret();


        if (authService.getSpec().getType().equals(AuthenticationServiceType.external) && authService.getSpec().getExternal() != null && authService.getSpec().getExternal().isAllowOverride()) {
            if (addressSpace.getSpec().getAuthenticationService().getOverrides() != null) {
                AuthenticationServiceOverrides overrides = addressSpace.getSpec().getAuthenticationService().getOverrides();
                if (overrides.getHost() != null) {
                    authServiceHost = overrides.getHost();
                }
                if (overrides.getPort() != null) {
                    authServicePort = overrides.getPort();
                }
                if (overrides.getRealm() != null) {
                    authServiceRealm = overrides.getRealm();
                }
                if (overrides.getCaCertSecret() != null) {
                    authServiceCaCertSecret = overrides.getCaCertSecret();
                }
                if (overrides.getClientCertSecret() != null) {
                    authServiceClientCertSecret = overrides.getClientCertSecret();
                }
            }
        }

        Optional<ConsoleService> console = schemaProvider.getSchema().findConsoleService(WELL_KNOWN_CONSOLE_SERVICE_NAME);
        if (console.isEmpty()) {
            log.warn("No ConsoleService found named '{}', address space console service will be unavailable", WELL_KNOWN_CONSOLE_SERVICE_NAME);
        }

        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        parameters.put(TemplateParameter.INFRA_NAMESPACE, kubernetes.getNamespace());
        parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getMetadata().getName());
        parameters.put(TemplateParameter.INFRA_UUID, infraUuid);
        parameters.put(TemplateParameter.ADDRESS_SPACE_NAMESPACE, addressSpace.getMetadata().getNamespace());
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authServiceHost);
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authServicePort));
        parameters.put(TemplateParameter.ADDRESS_SPACE_PLAN, addressSpace.getSpec().getPlan());

        String encodedCaCert = Optional.ofNullable(authServiceCaCertSecret)
                .map(secretName ->
                    kubernetes.getSecret(secretName.getName()).map(secret ->
                            secret.getData().get("tls.crt"))
                            .orElseThrow(() -> new IllegalArgumentException("Unable to decode secret " + secretName)))
                .orElseGet(() -> {
                    try {
                        return Base64.getEncoder().encodeToString(Files.readAllBytes(new File("/etc/ssl/certs/ca-bundle.crt").toPath()));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_CERT, encodedCaCert);
        if (authServiceClientCertSecret != null) {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, authServiceClientCertSecret.getName());
        }

        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, authServiceRealm);

        Map<String, CertSpec> serviceCertMapping = new HashMap<>();
        for (EndpointSpec endpoint : addressSpace.getSpec().getEndpoints()) {
            if (endpoint.getCert() != null) {
                serviceCertMapping.put(endpoint.getService(), endpoint.getCert());
            }
        }
        parameters.put(TemplateParameter.MESSAGING_SECRET, serviceCertMapping.get("messaging").getSecretName());
        parameters.put(TemplateParameter.CONSOLE_SECRET, serviceCertMapping.get("console").getSecretName());

        if (console.isPresent()) {
            ConsoleService consoleService = console.get();
            ConsoleServiceSpec spec = consoleService.getSpec();

            parameters.put(TemplateParameter.CONSOLE_OAUTH_DISCOVERY_URL, spec.getDiscoveryMetadataURL());
            parameters.put(TemplateParameter.CONSOLE_OAUTH_SCOPE, spec.getScope());

            SecretReference oauthClientSecret = spec.getOauthClientSecret();
            if (oauthClientSecret != null) {
                parameters.put(TemplateParameter.CONSOLE_OAUTH_SECRET_SECRET_NAME, oauthClientSecret.getName());
            }

            SecretReference cookieSecret = spec.getSsoCookieSecret();
            if (cookieSecret != null) {
                parameters.put(TemplateParameter.CONSOLE_SSO_COOKIE_SECRET_SECRET_NAME, cookieSecret.getName());
            }

            ConsoleServiceStatus status = consoleService.getStatus();
            if (status != null && status.getUrl() != null) {
                parameters.put(TemplateParameter.CONSOLE_LINK, status.getUrl());
            }

        }
    }

    private void prepareMqttParameters(AddressSpace addressSpace, Map<String, String> parameters) {
        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getMetadata().getName());
        parameters.put(TemplateParameter.INFRA_UUID, infraUuid);
        Map<String, CertSpec> serviceCertMapping = new HashMap<>();
        for (EndpointSpec endpoint : addressSpace.getSpec().getEndpoints()) {
            if (endpoint.getCert() != null) {
                serviceCertMapping.put(endpoint.getService(), endpoint.getCert());
            }
        }
        parameters.put(TemplateParameter.MQTT_SECRET, serviceCertMapping.get("mqtt").getSecretName());
        setIfEnvPresent(parameters, TemplateParameter.AGENT_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.MQTT_GATEWAY_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.MQTT_LWT_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.IMAGE_PULL_POLICY);
    }

    private List<HasMetadata> createStandardInfraMqtt(AddressSpace addressSpace, String templateName) {
        Map<String, String> parameters = new HashMap<>();
        prepareMqttParameters(addressSpace, parameters);
        return new ArrayList<>(kubernetes.processTemplate(templateName, parameters).getItems());
    }


    private List<HasMetadata> createStandardInfra(AddressSpace addressSpace, StandardInfraConfig standardInfraConfig) {

        Map<String, String> parameters = new HashMap<>();

        prepareParameters(standardInfraConfig, addressSpace, parameters);

        if (standardInfraConfig.getSpec().getBroker() != null) {
            if (standardInfraConfig.getSpec().getBroker().getResources() != null) {
                if (standardInfraConfig.getSpec().getBroker().getResources().getMemory() != null) {
                    parameters.put(TemplateParameter.BROKER_MEMORY_LIMIT, standardInfraConfig.getSpec().getBroker().getResources().getMemory());
                }
                if (standardInfraConfig.getSpec().getBroker().getResources().getStorage() != null) {
                    parameters.put(TemplateParameter.BROKER_STORAGE_CAPACITY, standardInfraConfig.getSpec().getBroker().getResources().getStorage());
                }
            }

            if (standardInfraConfig.getSpec().getBroker().getAddressFullPolicy() != null) {
                parameters.put(TemplateParameter.BROKER_ADDRESS_FULL_POLICY, standardInfraConfig.getSpec().getBroker().getAddressFullPolicy());
            }

            if (standardInfraConfig.getSpec().getBroker().getGlobalMaxSize() != null) {
                parameters.put(TemplateParameter.BROKER_GLOBAL_MAX_SIZE, standardInfraConfig.getSpec().getBroker().getGlobalMaxSize());
            }
        }

        if (standardInfraConfig.getSpec().getRouter() != null) {
            if (standardInfraConfig.getSpec().getRouter().getResources() != null && standardInfraConfig.getSpec().getRouter().getResources().getMemory() != null) {
                parameters.put(TemplateParameter.ROUTER_MEMORY_LIMIT, standardInfraConfig.getSpec().getRouter().getResources().getMemory());
            }

            if (standardInfraConfig.getSpec().getRouter().getLinkCapacity() != null) {
                parameters.put(TemplateParameter.ROUTER_LINK_CAPACITY, String.valueOf(standardInfraConfig.getSpec().getRouter().getLinkCapacity()));
            }

            if (standardInfraConfig.getSpec().getRouter().getHandshakeTimeout() != null) {
                parameters.put(TemplateParameter.ROUTER_HANDSHAKE_TIMEOUT, String.valueOf(standardInfraConfig.getSpec().getRouter().getHandshakeTimeout()));
            }

            if (standardInfraConfig.getSpec().getRouter().getIdleTimeout() != null) {
                parameters.put(TemplateParameter.ROUTER_IDLE_TIMEOUT, String.valueOf(standardInfraConfig.getSpec().getRouter().getIdleTimeout()));
            }

            if (standardInfraConfig.getSpec().getRouter().getWorkerThreads() != null) {
                parameters.put(TemplateParameter.ROUTER_WORKER_THREADS, String.valueOf(standardInfraConfig.getSpec().getRouter().getWorkerThreads()));
            }

            if (standardInfraConfig.getSpec().getRouter().getPolicy() != null) {
                try {
                    String vhostPolicyJson = createVhostPolicyJson(standardInfraConfig.getSpec().getRouter().getPolicy());
                    parameters.put(TemplateParameter.ROUTER_VHOST_POLICY_NAME, "public");
                    parameters.put(TemplateParameter.ROUTER_VHOST_POLICY_JSON, vhostPolicyJson);
                    parameters.put(TemplateParameter.ROUTER_ENABLE_VHOST_POLICY, "true");
                } catch (Exception e) {
                    log.warn("Error setting router policy settings, ignoring", e);
                }
            }
        }

        if (standardInfraConfig.getSpec().getAdmin() != null && standardInfraConfig.getSpec().getAdmin().getResources() != null && standardInfraConfig.getSpec().getAdmin().getResources().getMemory() != null) {
            parameters.put(TemplateParameter.ADMIN_MEMORY_LIMIT, standardInfraConfig.getSpec().getAdmin().getResources().getMemory());
        }

        parameters.put(TemplateParameter.STANDARD_INFRA_CONFIG_NAME, standardInfraConfig.getMetadata().getName());
        setIfEnvPresent(parameters, TemplateParameter.AGENT_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.STANDARD_CONTROLLER_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.ROUTER_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.BROKER_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.BROKER_PLUGIN_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.TOPIC_FORWARDER_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.IMAGE_PULL_POLICY);

        if (fsGroupOverride != null) {
            parameters.put(TemplateParameter.FS_GROUP_OVERRIDE, fsGroupOverride.toString());
        }

        Map<String, String> infraAnnotations = standardInfraConfig.getMetadata().getAnnotations();
        String templateName = getAnnotation(infraAnnotations, AnnotationKeys.TEMPLATE_NAME, "standard-space-infra");
        List<HasMetadata> items = new ArrayList<>(kubernetes.processTemplate(templateName, parameters).getItems());

        if (standardInfraConfig.getSpec().getRouter() != null && standardInfraConfig.getSpec().getRouter().getMinReplicas() != null) {
            // Workaround since parameterized integer fields cannot be loaded locally by fabric8 kubernetes-client
            for (HasMetadata item : items) {
                if (item instanceof StatefulSet && "qdrouterd".equals(item.getMetadata().getLabels().get(LabelKeys.NAME))) {
                    StatefulSet router = (StatefulSet) item;
                    router.getSpec().setReplicas(standardInfraConfig.getSpec().getRouter().getMinReplicas());
                }
            }
        }

        if (standardInfraConfig.getSpec().getAdmin() != null && standardInfraConfig.getSpec().getAdmin().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = standardInfraConfig.getSpec().getAdmin().getPodTemplate();
            Deployment adminDeployment = lookupResource(Deployment.class, "Deployment", KubeUtil.getAdminDeploymentName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = adminDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        if (standardInfraConfig.getSpec().getRouter() != null && standardInfraConfig.getSpec().getRouter().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = standardInfraConfig.getSpec().getRouter().getPodTemplate();
            StatefulSet routerSet = lookupResource(StatefulSet.class, "StatefulSet", KubeUtil.getRouterSetName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = routerSet.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        if (Boolean.parseBoolean(getAnnotation(infraAnnotations, AnnotationKeys.WITH_MQTT, "false"))) {
            String mqttTemplateName = getAnnotation(infraAnnotations, AnnotationKeys.MQTT_TEMPLATE_NAME, "standard-space-infra-mqtt");
            items.addAll(createStandardInfraMqtt(addressSpace, mqttTemplateName));
        }

        if (standardInfraConfig.getSpec().getBroker() != null) {
            return applyStorageClassName(standardInfraConfig.getSpec().getBroker().getStorageClassName(), items);
        } else {
            return items;
        }
    }

    static String createVhostPolicyJson(RouterPolicySpec policy) throws JsonProcessingException {
        // Public settings derived from infra config settings
        Map<String, Object> publicGroupPolicy = new HashMap<>();
        publicGroupPolicy.put("remoteHosts", "*");
        publicGroupPolicy.put("sources", "*");
        publicGroupPolicy.put("targets", "*");
        publicGroupPolicy.put("allowDynamicSource", true);
        publicGroupPolicy.put("allowAnonymousSender", true);

        if (policy.getMaxSessionsPerConnection() != null) {
            publicGroupPolicy.put("maxSessions", policy.getMaxSessionsPerConnection());
        }

        if (policy.getMaxSendersPerConnection() != null) {
            publicGroupPolicy.put("maxSenders", policy.getMaxSendersPerConnection());
        }

        if (policy.getMaxReceiversPerConnection() != null) {
            publicGroupPolicy.put("maxReceivers", policy.getMaxReceiversPerConnection());
        }

        Map<String, Object> publicVhostPolicy = new HashMap<>();
        publicVhostPolicy.put("hostname", "public");
        publicVhostPolicy.put("allowUnknownUser", true);

        if (policy.getMaxConnections() != null) {
            publicVhostPolicy.put("maxConnections", policy.getMaxConnections());
        }

        if (policy.getMaxConnectionsPerHost() != null) {
            publicVhostPolicy.put("maxConnectionsPerHost", policy.getMaxConnectionsPerHost());
        }

        if (policy.getMaxConnectionsPerUser() != null) {
            publicVhostPolicy.put("maxConnectionsPerUser", policy.getMaxConnectionsPerUser());
        }

        publicVhostPolicy.put("groups", Collections.singletonMap("$default", publicGroupPolicy));

        // Internal settings, used by internal components
        Map<String, Object> internalGroupPolicy = new HashMap<>();
        internalGroupPolicy.put("remoteHosts", "*");
        internalGroupPolicy.put("sources", "*");
        internalGroupPolicy.put("targets", "*");
        internalGroupPolicy.put("allowDynamicSource", true);
        internalGroupPolicy.put("allowAnonymousSender", true);

        Map<String, Object> internalVhostPolicy = new HashMap<>();
        internalVhostPolicy.put("hostname", "$default");
        internalVhostPolicy.put("allowUnknownUser", true);
        internalVhostPolicy.put("groups", Collections.singletonMap("$default", internalGroupPolicy));

        List<Object> values = new ArrayList<>();
        values.add("vhost");
        values.add(internalVhostPolicy);
        values.add(publicVhostPolicy);
        return mapper.writeValueAsString(Collections.singletonList(values));
    }

    private String getAnnotation(Map<String, String> annotations, String key, String defaultValue) {
        return Optional.ofNullable(annotations)
                .flatMap(m -> Optional.ofNullable(m.get(key)))
                .orElse(defaultValue);
    }

    private List<HasMetadata> createBrokeredInfra(AddressSpace addressSpace, BrokeredInfraConfig brokeredInfraConfig) {
        Map<String, String> parameters = new HashMap<>();

        prepareParameters(brokeredInfraConfig, addressSpace, parameters);

        if (brokeredInfraConfig.getSpec().getBroker() != null) {
            if (brokeredInfraConfig.getSpec().getBroker().getResources() != null) {
                if (brokeredInfraConfig.getSpec().getBroker().getResources().getMemory() != null) {
                    parameters.put(TemplateParameter.BROKER_MEMORY_LIMIT, brokeredInfraConfig.getSpec().getBroker().getResources().getMemory());
                }
                if (brokeredInfraConfig.getSpec().getBroker().getResources().getStorage() != null) {
                    parameters.put(TemplateParameter.BROKER_STORAGE_CAPACITY, brokeredInfraConfig.getSpec().getBroker().getResources().getStorage());
                }
            }

            if (brokeredInfraConfig.getSpec().getBroker().getAddressFullPolicy() != null) {
                parameters.put(TemplateParameter.BROKER_ADDRESS_FULL_POLICY, brokeredInfraConfig.getSpec().getBroker().getAddressFullPolicy());
            }

            if (brokeredInfraConfig.getSpec().getBroker().getGlobalMaxSize() != null) {
                parameters.put(TemplateParameter.BROKER_GLOBAL_MAX_SIZE, brokeredInfraConfig.getSpec().getBroker().getGlobalMaxSize());
            }
        }

        if (brokeredInfraConfig.getSpec().getAdmin() != null && brokeredInfraConfig.getSpec().getAdmin().getResources() != null && brokeredInfraConfig.getSpec().getAdmin().getResources().getMemory() != null) {
            parameters.put(TemplateParameter.ADMIN_MEMORY_LIMIT, brokeredInfraConfig.getSpec().getAdmin().getResources().getMemory());
        }

        setIfEnvPresent(parameters, TemplateParameter.AGENT_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.BROKER_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.BROKER_PLUGIN_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.IMAGE_PULL_POLICY);

        List<HasMetadata> items;
        String templateName = getAnnotation(brokeredInfraConfig.getMetadata().getAnnotations(), AnnotationKeys.TEMPLATE_NAME, "brokered-space-infra");
        if (brokeredInfraConfig.getSpec().getBroker() != null) {
            items = applyStorageClassName(brokeredInfraConfig.getSpec().getBroker().getStorageClassName(), kubernetes.processTemplate(templateName, parameters).getItems());
        } else {
            items = kubernetes.processTemplate(templateName, parameters).getItems();
        }

        if (brokeredInfraConfig.getSpec().getAdmin() != null && brokeredInfraConfig.getSpec().getAdmin().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = brokeredInfraConfig.getSpec().getAdmin().getPodTemplate();
            Deployment adminDeployment = lookupResource(Deployment.class, "Deployment", KubeUtil.getAgentDeploymentName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = adminDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        Deployment brokerDeployment = lookupResource(Deployment.class, "Deployment", KubeUtil.getBrokeredBrokerSetName(addressSpace), items);
        if (brokeredInfraConfig.getSpec().getBroker() != null && brokeredInfraConfig.getSpec().getBroker().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = brokeredInfraConfig.getSpec().getBroker().getPodTemplate();
            PodTemplateSpec actualPodTemplate = brokerDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        if (this.fsGroupOverride != null) {
            KubeUtil.applyFsGroupOverride(Collections.singletonList(brokerDeployment), this.fsGroupOverride);
        }

        return items;
    }

    private Long getFsGroupOverride() {
        Long fsGroupOverride = null;
        if (env.containsKey(FS_GROUP_OVERRIDE)) {
            try {
                fsGroupOverride = Long.parseLong(env.get(FS_GROUP_OVERRIDE));
            } catch (NumberFormatException ignore) {
            }
        }
        return fsGroupOverride;
    }

    private List<HasMetadata> applyStorageClassName(String storageClassName, List<HasMetadata> items) {
        if (storageClassName != null) {
            for (HasMetadata item : items) {
                if (item instanceof PersistentVolumeClaim) {
                    ((PersistentVolumeClaim) item).getSpec().setStorageClassName(storageClassName);
                }
            }
        }
        return items;
    }

    private void setIfEnvPresent(Map<String, String> parameters, String key) {
        if (env.get(key) != null) {
            parameters.put(key, env.get(key));
        }
    }


    @Override
    public List<HasMetadata> createInfraResources(AddressSpace addressSpace, InfraConfig infraConfig) {
        if ("standard".equals(addressSpace.getSpec().getType())) {
            return createStandardInfra(addressSpace, (StandardInfraConfig) infraConfig);
        } else if ("brokered".equals(addressSpace.getSpec().getType())) {
            return createBrokeredInfra(addressSpace, (BrokeredInfraConfig) infraConfig);
        } else {
            throw new IllegalArgumentException("Unknown address space type " + addressSpace.getSpec().getType());
        }
    }
}
