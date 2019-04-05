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
import io.fabric8.kubernetes.api.model.*;
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
    private static final Logger log = LoggerFactory.getLogger(TemplateInfraResourceFactory.class);
    private static final String KC_IDP_HINT_NONE = "none";
    private static final String KC_IDP_HINT_OPENSHIFT = "openshift-v3";
    private static final ObjectMapper mapper = new ObjectMapper();

    private final Kubernetes kubernetes;
    private final AuthenticationServiceRegistry authenticationServiceRegistry;
    private final Map<String, String> env;
    private final boolean openShift;

    public TemplateInfraResourceFactory(Kubernetes kubernetes, AuthenticationServiceRegistry authenticationServiceRegistry, Map<String, String> env, boolean openShift) {
        this.kubernetes = kubernetes;
        this.authenticationServiceRegistry = authenticationServiceRegistry;
        this.env = env;
        this.openShift = openShift;
    }

    private void prepareParameters(InfraConfig infraConfig,
                                   AddressSpace addressSpace,
                                   Map<String, String> parameters) {

        AuthenticationService authService = authenticationServiceRegistry.findAuthenticationService(addressSpace.getSpec().getAuthenticationService())
                .orElseThrow(() -> new IllegalArgumentException("Unable to find authentication service " + addressSpace.getSpec().getAuthenticationService()));

        if (authService.getStatus() == null) {
            throw new IllegalArgumentException("Authentication service '" + authService.getMetadata().getName() + "' is not yet deployed");
        }


        Optional<String> kcIdpHint = getKcIdpHint(infraConfig, addressSpace, authService);

        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        parameters.put(TemplateParameter.INFRA_NAMESPACE, kubernetes.getNamespace());
        parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getMetadata().getName());
        parameters.put(TemplateParameter.INFRA_UUID, infraUuid);
        parameters.put(TemplateParameter.ADDRESS_SPACE_NAMESPACE, addressSpace.getMetadata().getNamespace());
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authService.getStatus().getHost());
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authService.getStatus().getPort()));
        parameters.put(TemplateParameter.ADDRESS_SPACE_PLAN, addressSpace.getSpec().getPlan());
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_KC_IDP_HINT, kcIdpHint.orElse(""));

        String encodedCaCert = Optional.ofNullable(authService.getStatus().getCaCertSecret())
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
        if (authService.getStatus().getClientCertSecret()  != null) {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, authService.getStatus().getClientCertSecret().getName());
        }

        if (authService.getSpec().getRealm() != null) {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, authService.getSpec().getRealm());
        } else {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, addressSpace.getAnnotation(AnnotationKeys.REALM_NAME));
        }

        if (authService.getMetadata().getAnnotations() != null &&
                authService.getMetadata().getAnnotations().get(AnnotationKeys.OAUTH_URL) != null) {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_OAUTH_URL, authService.getMetadata().getAnnotations().get(AnnotationKeys.OAUTH_URL));
        }

        Map<String, CertSpec> serviceCertMapping = new HashMap<>();
        for (EndpointSpec endpoint : addressSpace.getSpec().getEndpoints()) {
            if (endpoint.getCert() != null) {
                serviceCertMapping.put(endpoint.getService(), endpoint.getCert());
            }
        }
        parameters.put(TemplateParameter.MESSAGING_SECRET, serviceCertMapping.get("messaging").getSecretName());
        parameters.put(TemplateParameter.CONSOLE_SECRET, serviceCertMapping.get("console").getSecretName());
    }

    private Optional<String> getKcIdpHint(final InfraConfig infraConfig,
                                final AddressSpace addressSpace,
                                final AuthenticationService authenticationService) {

        String kcIdpHint = null;
        if (this.openShift && authenticationService.getSpec().getType().equals(AuthenticationServiceType.standard)) {
            kcIdpHint = KC_IDP_HINT_OPENSHIFT;
        }

        kcIdpHint = getAnnotation(infraConfig.getMetadata().getAnnotations(), AnnotationKeys.KC_IDP_HINT, kcIdpHint);

        final String hint = addressSpace.getAnnotation(AnnotationKeys.KC_IDP_HINT);
        if  ( hint != null) {
            kcIdpHint = hint;
        }
        return KC_IDP_HINT_NONE.equals(kcIdpHint) ? Optional.empty() : Optional.ofNullable(kcIdpHint);
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
                    String vhostPolicyJson = createVhostPolicyJson("$default", standardInfraConfig.getSpec().getRouter().getPolicy());
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
            Deployment adminDeployment = lookupResource("Deployment", KubeUtil.getAdminDeploymentName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = adminDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        if (standardInfraConfig.getSpec().getRouter() != null && standardInfraConfig.getSpec().getRouter().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = standardInfraConfig.getSpec().getRouter().getPodTemplate();
            StatefulSet routerSet = lookupResource("StatefulSet", KubeUtil.getRouterSetName(addressSpace), items);
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

    static String createVhostPolicyJson(String vhost, RouterPolicySpec policy) throws JsonProcessingException {
        Map<String, Object> defaultGroupPolicy = new HashMap<>();
        defaultGroupPolicy.put("remoteHosts", "*");
        if (policy.getMaxSessionsPerConnection() != null) {
            defaultGroupPolicy.put("maxSessions", policy.getMaxSessionsPerConnection());
        }

        if (policy.getMaxSendersPerConnection() != null) {
            defaultGroupPolicy.put("maxSenders", policy.getMaxSendersPerConnection());
        }

        if (policy.getMaxReceiversPerConnection() != null) {
            defaultGroupPolicy.put("maxReceivers", policy.getMaxReceiversPerConnection());
        }

        Map<String, Object> defaultVhostPolicy = new HashMap<>();
        defaultVhostPolicy.put("hostname", vhost);
        defaultVhostPolicy.put("allowUnknownUser", true);

        if (policy.getMaxConnections() != null) {
            defaultVhostPolicy.put("maxConnections", policy.getMaxConnections());
        }

        if (policy.getMaxConnectionsPerHost() != null) {
            defaultVhostPolicy.put("maxConnectionsPerHost", policy.getMaxConnectionsPerHost());
        }

        if (policy.getMaxConnectionsPerUser() != null) {
            defaultVhostPolicy.put("maxConnectionsPerUser", policy.getMaxConnectionsPerUser());
        }

        defaultVhostPolicy.put("groups", Collections.singletonMap(vhost, defaultGroupPolicy));

        List<Object> values = new ArrayList<>();
        values.add("vhost");
        values.add(defaultVhostPolicy);
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
            Deployment adminDeployment = lookupResource("Deployment", KubeUtil.getAgentDeploymentName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = adminDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        if (brokeredInfraConfig.getSpec().getBroker() != null && brokeredInfraConfig.getSpec().getBroker().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = brokeredInfraConfig.getSpec().getBroker().getPodTemplate();
            Deployment brokerDeployment = lookupResource("Deployment", KubeUtil.getBrokeredBrokerSetName(addressSpace), items);
            PodTemplateSpec actualPodTemplate = brokerDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }

        return items;
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
