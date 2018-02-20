/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.controller.common.*;
import io.enmasse.address.model.*;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.SchemaApi;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.client.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static io.enmasse.controller.common.ControllerReason.AddressSpaceCreated;
import static io.enmasse.controller.common.ControllerReason.AddressSpaceDeleteFailed;
import static io.enmasse.controller.common.ControllerReason.AddressSpaceDeleted;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;
import static io.enmasse.k8s.api.EventLogger.Type.Warning;

/**
 * Helper class for managing a standard address space.
 */
public class ControllerHelper {
    private static final Logger log = LoggerFactory.getLogger(ControllerHelper.class.getName());
    private final Kubernetes kubernetes;
    private final String namespace;
    private final AuthenticationServiceResolverFactory authResolverFactory;
    private final EventLogger eventLogger;
    private final SchemaApi schemaApi;

    public ControllerHelper(Kubernetes kubernetes, AuthenticationServiceResolverFactory authResolverFactory, EventLogger eventLogger, SchemaApi schemaApi) {
        this.kubernetes = kubernetes;
        this.namespace = kubernetes.getNamespace();
        this.authResolverFactory = authResolverFactory;
        this.eventLogger = eventLogger;
        this.schemaApi = schemaApi;
    }

    public void create(AddressSpace addressSpace) {
        Kubernetes instanceClient = kubernetes.withNamespace(addressSpace.getNamespace());

        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaApi.getSchema());
        addressSpaceResolver.validate(addressSpace);

        if (namespace.equals(addressSpace.getNamespace())) {
            if (instanceClient.hasService("messaging")) {
                return;
            }
        } else {
            if (kubernetes.existsNamespace(addressSpace.getNamespace())) {
                return;
            }
            log.info("Creating address space {}", addressSpace);
            kubernetes.createNamespace(addressSpace);
            kubernetes.addAddressSpaceAdminRoleBinding(addressSpace);
            kubernetes.addSystemImagePullerPolicy(namespace, addressSpace);
            kubernetes.addAddressSpaceRoleBindings(addressSpace);
            kubernetes.createServiceAccount(addressSpace.getNamespace(), kubernetes.getAddressSpaceAdminSa());
            schemaApi.copyIntoNamespace(addressSpaceResolver.getPlan(addressSpaceResolver.getType(addressSpace), addressSpace), addressSpace.getNamespace());
        }

        StandardResources resourceList = createResourceList(addressSpace);

        for (Endpoint endpoint : resourceList.routeEndpoints) {
            Service service = null;
            for (HasMetadata resource : resourceList.resourceList.getItems()) {
                if (resource.getKind().equals("Service") && resource.getMetadata().getName().equals(endpoint.getService())) {
                    service = (Service) resource;
                    break;
                }
            }
            kubernetes.createEndpoint(endpoint, service, addressSpace.getName(), addressSpace.getNamespace());
        }

        kubernetes.create(resourceList.resourceList, addressSpace.getNamespace());
        eventLogger.log(AddressSpaceCreated, "Created address space", Normal, ControllerKind.AddressSpace, addressSpace.getName());
    }

    private static class StandardResources {
        public KubernetesList resourceList;
        public List<Endpoint> routeEndpoints;
    }

    private StandardResources createResourceList(AddressSpace addressSpace) {
        StandardResources returnVal = new StandardResources();
        returnVal.resourceList = new KubernetesList();
        returnVal.routeEndpoints = new ArrayList<>();

        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaApi.getSchema());
        AddressSpaceType addressSpaceType = addressSpaceResolver.getType(addressSpace);
        AddressSpacePlan plan = addressSpaceResolver.getPlan(addressSpaceType, addressSpace);
        ResourceDefinition resourceDefinition = addressSpaceResolver.getResourceDefinition(plan);

        if (resourceDefinition != null && resourceDefinition.getTemplateName().isPresent()) {
            Map<String, String> parameters = new HashMap<>();
            AuthenticationService authService = addressSpace.getAuthenticationService();
            AuthenticationServiceResolver authResolver = authResolverFactory.getResolver(authService.getType());

            parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getName());
            parameters.put(TemplateParameter.ADDRESS_SPACE_SERVICE_HOST, getApiServer());
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authResolver.getHost(authService));
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authResolver.getPort(authService)));

            authResolver.getCaSecretName(authService).ifPresent(secretName -> kubernetes.getSecret(secretName).ifPresent(secret -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_CERT, secret.getData().get("tls.crt"))));
            kubernetes.getSecret("address-controller-cert").ifPresent(secret -> parameters.put(TemplateParameter.ADDRESS_CONTROLLER_CA_CERT, secret.getData().get("tls.crt")));
            authResolver.getClientSecretName(authService).ifPresent(secret -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, secret));
            authResolver.getSaslInitHost(addressSpace.getName(), authService).ifPresent(saslInitHost -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, saslInitHost));

            // Step 1: Validate endpoints and remove unknown
            List<String> availableServices = addressSpaceType.getServiceNames();
            Map<String, CertProvider> serviceCertProviders = new HashMap<>();

            List<Endpoint> endpoints = null;
            if (addressSpace.getEndpoints() != null) {
                endpoints = new ArrayList<>(addressSpace.getEndpoints());
                Iterator<Endpoint> it = endpoints.iterator();
                while (it.hasNext()) {
                    Endpoint endpoint = it.next();
                    if (!availableServices.contains(endpoint.getService())) {
                        log.info("Unknown service {} for endpoint {}, removing", endpoint.getService(), endpoint.getName());
                        it.remove();
                    } else {
                        endpoint.getCertProvider().ifPresent(certProvider -> serviceCertProviders.put(endpoint.getService(), certProvider));
                    }
                }
            } else {
            // Step 2: Create endpoints if the user didnt supply any
                endpoints = availableServices.stream()
                        .map(service -> new Endpoint.Builder().setName(service).setService(service).build())
                        .collect(Collectors.toList());
            }

            // Step 3: Create missing secrets if not specified
            for (String service : availableServices) {
                String secretName = getSecretName(service);

                if (!serviceCertProviders.containsKey(service)) {
                    CertProvider certProvider = new SecretCertProvider(secretName);
                    serviceCertProviders.put(service, certProvider);
                }
            }

            // Step 3: Ensure all endpoints have their certProviders set
            returnVal.routeEndpoints = endpoints.stream()
                    .map(endpoint -> {
                        if (!endpoint.getCertProvider().isPresent()) {
                            return new Endpoint.Builder(endpoint)
                                    .setCertProvider(serviceCertProviders.get(endpoint.getService()))
                                    .build();
                        } else {
                            return endpoint;
                        }
                    }).collect(Collectors.toList());

            if (availableServices.contains("messaging")) {
                parameters.put(TemplateParameter.MESSAGING_SECRET, serviceCertProviders.get("messaging").getSecretName());
            }
            if (availableServices.contains("console")) {
                parameters.put(TemplateParameter.CONSOLE_SECRET, serviceCertProviders.get("console").getSecretName());
            }
            if (availableServices.contains("mqtt")) {
                parameters.put(TemplateParameter.MQTT_SECRET, serviceCertProviders.get("mqtt").getSecretName());
            }

            parameters.putAll(resourceDefinition.getTemplateParameters());

            List<ParameterValue> parameterValues = new ArrayList<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                parameterValues.add(new ParameterValue(entry.getKey(), entry.getValue()));
            }

            // Step 5: Create infrastructure
            returnVal.resourceList = kubernetes.processTemplate(resourceDefinition.getTemplateName().get(), parameterValues.toArray(new ParameterValue[0]));
        }
        return returnVal;
    }

    private static String getSecretName(String serviceName) {
        return "external-certs-" + serviceName;
    }

    private String getApiServer() {
        return "address-controller." + namespace + ".svc.cluster.local";
    }

    public boolean isReady(AddressSpace addressSpace) {
        Set<String> readyDeployments = kubernetes.withNamespace(addressSpace.getNamespace()).getReadyDeployments().stream()
                .map(deployment -> deployment.getMetadata().getName())
                .collect(Collectors.toSet());

        Set<String> requiredDeployments = createResourceList(addressSpace).resourceList.getItems().stream()
                .filter(KubernetesHelper::isDeployment)
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toSet());

        return readyDeployments.containsAll(requiredDeployments);
    }

    public void retainAddressSpaces(Set<AddressSpace> desiredAddressSpaces) {
        if (desiredAddressSpaces.size() == 1 && desiredAddressSpaces.iterator().next().getNamespace().equals(namespace)) {
            return;
        }
        Set<NamespaceInfo> actual = kubernetes.listAddressSpaces();
        Set<NamespaceInfo> desired = desiredAddressSpaces.stream()
                .map(space -> new NamespaceInfo(space.getUid(), space.getName(), space.getNamespace(), space.getCreatedBy()))
                .collect(Collectors.toSet());

        actual.removeAll(desired);

        for (NamespaceInfo toRemove : actual) {
            try {
                log.info("Deleting address space {}", toRemove);
                kubernetes.deleteNamespace(toRemove);
                eventLogger.log(AddressSpaceDeleted, "Deleted address space", Normal, ControllerKind.AddressSpace, toRemove.getAddressSpace());
            } catch (KubernetesClientException e) {
                eventLogger.log(AddressSpaceDeleteFailed, e.getMessage(), Warning, ControllerKind.AddressSpace, toRemove.getAddressSpace());
                log.info("Exception when deleting namespace (may already be in progress): " + e.getMessage());
            }
        }
    }
}
