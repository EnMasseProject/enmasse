/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AuthenticationServiceSettings;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.admin.model.v1.BrokeredInfraConfig;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.config.LabelKeys;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.TemplateParameter;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.enmasse.address.model.KubeUtil.applyCpuMemory;
import static io.enmasse.address.model.KubeUtil.applyPodTemplate;
import static io.enmasse.address.model.KubeUtil.lookupResource;
import static io.enmasse.address.model.KubeUtil.overrideFsGroup;
import static io.enmasse.config.Apps.setConnectsTo;
import static io.enmasse.config.Apps.setPartOf;

public class TemplateInfraResourceFactory implements InfraResourceFactory {
    private final Kubernetes kubernetes;
    private final Map<String, String> env;

    public TemplateInfraResourceFactory(Kubernetes kubernetes, Map<String, String> env) {
        this.kubernetes = kubernetes;
        this.env = env;
    }

    private void prepareParameters(AddressSpace addressSpace,
                                   AuthenticationServiceSettings authServiceSettings,
                                   Map<String, String> parameters) {

        String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        parameters.put(TemplateParameter.INFRA_NAMESPACE, kubernetes.getNamespace());
        parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getMetadata().getName());
        parameters.put(TemplateParameter.INFRA_UUID, infraUuid);
        parameters.put(TemplateParameter.ADDRESS_SPACE_NAMESPACE, addressSpace.getMetadata().getNamespace());
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authServiceSettings.getHost());
        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authServiceSettings.getPort()));
        parameters.put(TemplateParameter.ADDRESS_SPACE_PLAN, addressSpace.getSpec().getPlan());

        String encodedCaCert = Optional.ofNullable(authServiceSettings.getCaCertSecret())
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
        if (authServiceSettings.getClientCertSecret() != null) {
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, authServiceSettings.getClientCertSecret().getName());
        }

        parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, authServiceSettings.getRealm());

        Map<String, CertSpec> serviceCertMapping = new HashMap<>();
        for (EndpointSpec endpoint : addressSpace.getSpec().getEndpoints()) {
            if (endpoint.getCert() != null) {
                serviceCertMapping.put(endpoint.getService(), endpoint.getCert());
            }
        }
        parameters.put(TemplateParameter.MESSAGING_SECRET, serviceCertMapping.get("messaging").getSecretName());

        Optional<Secret> secret = kubernetes.getSecret("broker-support-" + infraUuid);
        if (secret.isPresent()) {
            Map<String, String> secretData  = secret.get().getData();
            if (secretData.containsKey("username") && secretData.containsKey("password")) {
                parameters.put(TemplateParameter.BROKER_SUPPORT_USER, secret.get().getData().get("username"));
                parameters.put(TemplateParameter.BROKER_SUPPORT_PWD, secret.get().getData().get("password"));
            } else {
                createSupportCredentials(parameters);
            }
        } else {
            createSupportCredentials(parameters);
        }
    }

    private void createSupportCredentials(Map<String, String> parameters) {
        String brokerSupportUser =  String.format("broker-support-%s", UUID.randomUUID());
        String brokerSupportPwd = UUID.randomUUID().toString();
        parameters.put(TemplateParameter.BROKER_SUPPORT_USER, Base64.getEncoder().encodeToString(brokerSupportUser.getBytes()));
        parameters.put(TemplateParameter.BROKER_SUPPORT_PWD, Base64.getEncoder().encodeToString(brokerSupportPwd.getBytes()));
    }

    private List<HasMetadata> createStandardInfra(AddressSpace addressSpace, StandardInfraConfig standardInfraConfig, AuthenticationServiceSettings authenticationServiceSettings) {

        Map<String, String> parameters = new HashMap<>();

        prepareParameters(addressSpace, authenticationServiceSettings, parameters);

        if (standardInfraConfig.getSpec().getBroker() != null) {
            if (standardInfraConfig.getSpec().getBroker().getResources() != null) {
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

        parameters.put(TemplateParameter.STANDARD_INFRA_CONFIG_NAME, standardInfraConfig.getMetadata().getName());
        setIfEnvPresent(parameters, TemplateParameter.AGENT_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.STANDARD_CONTROLLER_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.ROUTER_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.BROKER_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.BROKER_PLUGIN_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.TOPIC_FORWARDER_IMAGE);
        setIfEnvPresent(parameters, TemplateParameter.IMAGE_PULL_POLICY);
        setIfEnvPresent(parameters, TemplateParameter.FS_GROUP_FALLBACK_MAP);

        Map<String, String> infraAnnotations = standardInfraConfig.getMetadata().getAnnotations();
        String templateName = getAnnotation(infraAnnotations, AnnotationKeys.TEMPLATE_NAME, "standard-space-infra");
        List<HasMetadata> items = new ArrayList<>(kubernetes.processTemplate(templateName, parameters).getItems());

        if (standardInfraConfig.getSpec().getRouter() != null) {
            // Workaround since parameterized integer fields cannot be loaded locally by fabric8 kubernetes-client
            for (HasMetadata item : items) {
                if (item instanceof StatefulSet && item.getMetadata().getLabels() != null && "qdrouterd".equals(item.getMetadata().getLabels().get(LabelKeys.NAME))) {
                    StatefulSet router = (StatefulSet) item;
                    if (standardInfraConfig.getSpec().getRouter().getMinReplicas() != null) {
                        router.getSpec().setReplicas(standardInfraConfig.getSpec().getRouter().getMinReplicas());
                    }
                    if (standardInfraConfig.getSpec().getRouter().getResources() != null ) {
                        applyCpuMemory(router.getSpec().getTemplate(), standardInfraConfig.getSpec().getRouter().getResources().getCpu(), standardInfraConfig.getSpec().getRouter().getResources().getMemory());
                    }
                }
            }
        }

        Deployment adminDeployment = lookupResource(Deployment.class, "Deployment", KubeUtil.getAdminDeploymentName(addressSpace), items);
        if (standardInfraConfig.getSpec().getAdmin() != null && standardInfraConfig.getSpec().getAdmin().getResources() != null) {
            applyCpuMemory(adminDeployment.getSpec().getTemplate(), standardInfraConfig.getSpec().getAdmin().getResources().getCpu(), standardInfraConfig.getSpec().getAdmin().getResources().getMemory());
        }
        if (standardInfraConfig.getSpec().getAdmin() != null && standardInfraConfig.getSpec().getAdmin().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = standardInfraConfig.getSpec().getAdmin().getPodTemplate();
            PodTemplateSpec actualPodTemplate = adminDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }
        setPartOf(adminDeployment, addressSpace);

        StatefulSet routerSet = lookupResource(StatefulSet.class, "StatefulSet", KubeUtil.getRouterSetName(addressSpace), items);
        if (standardInfraConfig.getSpec().getRouter() != null && standardInfraConfig.getSpec().getRouter().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = standardInfraConfig.getSpec().getRouter().getPodTemplate();
            PodTemplateSpec actualPodTemplate = routerSet.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }
        setPartOf(routerSet, addressSpace);
        setConnectsTo(adminDeployment, routerSet.getMetadata().getName());

        if (standardInfraConfig.getSpec().getBroker() != null) {
            return applyStorageClassName(standardInfraConfig.getSpec().getBroker().getStorageClassName(), items);
        } else {
            return items;
        }
    }


    private String getAnnotation(Map<String, String> annotations, String key, String defaultValue) {
        return Optional.ofNullable(annotations)
                .flatMap(m -> Optional.ofNullable(m.get(key)))
                .orElse(defaultValue);
    }

    private List<HasMetadata> createBrokeredInfra(AddressSpace addressSpace, BrokeredInfraConfig brokeredInfraConfig, AuthenticationServiceSettings authenticationServiceSettings) {
        Map<String, String> parameters = new HashMap<>();

        prepareParameters(addressSpace, authenticationServiceSettings, parameters);

        if (brokeredInfraConfig.getSpec().getBroker() != null) {
            if (brokeredInfraConfig.getSpec().getBroker().getResources() != null) {
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

            if (brokeredInfraConfig.getSpec().getBroker().getJavaOpts() != null) {
                parameters.put(TemplateParameter.BROKER_JAVA_OPTS, brokeredInfraConfig.getSpec().getBroker().getJavaOpts());
            }
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

        Deployment adminDeployment = lookupResource(Deployment.class, "Deployment", KubeUtil.getAgentDeploymentName(addressSpace), items);
        if (brokeredInfraConfig.getSpec().getAdmin() != null && brokeredInfraConfig.getSpec().getAdmin().getResources() != null) {
            applyCpuMemory(adminDeployment.getSpec().getTemplate(), brokeredInfraConfig.getSpec().getAdmin().getResources().getCpu(), brokeredInfraConfig.getSpec().getAdmin().getResources().getMemory());
        }
        if (brokeredInfraConfig.getSpec().getAdmin() != null && brokeredInfraConfig.getSpec().getAdmin().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = brokeredInfraConfig.getSpec().getAdmin().getPodTemplate();
            PodTemplateSpec actualPodTemplate = adminDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }
        setPartOf(adminDeployment, addressSpace);

        Deployment brokerDeployment = lookupResource(Deployment.class, "Deployment", KubeUtil.getBrokeredBrokerSetName(addressSpace), items);
        if (brokeredInfraConfig.getSpec().getBroker() != null && brokeredInfraConfig.getSpec().getBroker().getResources() != null) {
            applyCpuMemory(brokerDeployment.getSpec().getTemplate(), brokeredInfraConfig.getSpec().getBroker().getResources().getCpu(), brokeredInfraConfig.getSpec().getBroker().getResources().getMemory());
        }
        if (brokeredInfraConfig.getSpec().getBroker() != null && brokeredInfraConfig.getSpec().getBroker().getPodTemplate() != null) {
            PodTemplateSpec podTemplate = brokeredInfraConfig.getSpec().getBroker().getPodTemplate();
            PodTemplateSpec actualPodTemplate = brokerDeployment.getSpec().getTemplate();
            applyPodTemplate(actualPodTemplate, podTemplate);
        }
        overrideFsGroup(brokerDeployment.getSpec().getTemplate(), "broker", kubernetes.getNamespace());
        setPartOf(brokerDeployment, addressSpace);

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
    public List<HasMetadata> createInfraResources(AddressSpace addressSpace, InfraConfig infraConfig, AuthenticationServiceSettings authenticationServiceSettings) {
        if ("standard".equals(addressSpace.getSpec().getType())) {
            return createStandardInfra(addressSpace, (StandardInfraConfig) infraConfig, authenticationServiceSettings);
        } else if ("brokered".equals(addressSpace.getSpec().getType())) {
            return createBrokeredInfra(addressSpace, (BrokeredInfraConfig) infraConfig, authenticationServiceSettings);
        } else {
            throw new IllegalArgumentException("Unknown address space type " + addressSpace.getSpec().getType());
        }
    }

}
