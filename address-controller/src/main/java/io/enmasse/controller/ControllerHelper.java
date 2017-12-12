/*
 * Copyright 2017 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.enmasse.controller;

import io.enmasse.controller.common.*;
import io.enmasse.address.model.*;
import io.enmasse.address.model.types.Plan;
import io.enmasse.address.model.types.TemplateConfig;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.k8s.api.EventLogger;
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

    public ControllerHelper(Kubernetes kubernetes, AuthenticationServiceResolverFactory authResolverFactory, EventLogger eventLogger) {
        this.kubernetes = kubernetes;
        this.namespace = kubernetes.getNamespace();
        this.authResolverFactory = authResolverFactory;
        this.eventLogger = eventLogger;
    }

    public void create(AddressSpace addressSpace) {
        Kubernetes instanceClient = kubernetes.withNamespace(addressSpace.getNamespace());
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
        Plan plan = addressSpace.getPlan();
        StandardResources returnVal = new StandardResources();
        returnVal.resourceList = new KubernetesList();
        returnVal.routeEndpoints = new ArrayList<>();


        if (plan.getTemplateConfig().isPresent()) {
            List<ParameterValue> parameterValues = new ArrayList<>();
            AuthenticationService authService = addressSpace.getAuthenticationService();
            AuthenticationServiceResolver authResolver = authResolverFactory.getResolver(authService.getType());

            parameterValues.add(new ParameterValue(TemplateParameter.ADDRESS_SPACE, addressSpace.getName()));
            parameterValues.add(new ParameterValue(TemplateParameter.ADDRESS_SPACE_SERVICE_HOST, getApiServer()));
            parameterValues.add(new ParameterValue(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authResolver.getHost(authService)));
            parameterValues.add(new ParameterValue(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authResolver.getPort(authService))));
            authResolver.getCaSecretName(authService).ifPresent(secretName -> kubernetes.getSecret(secretName).ifPresent(secret -> parameterValues.add(new ParameterValue(TemplateParameter.AUTHENTICATION_SERVICE_CA_CERT, secret.getData().get("tls.crt")))));
            kubernetes.getSecret("address-controller-cert").ifPresent(secret -> parameterValues.add(new ParameterValue(TemplateParameter.ADDRESS_CONTROLLER_CA_CERT, secret.getData().get("tls.crt"))));
            authResolver.getClientSecretName(authService).ifPresent(secret -> parameterValues.add(new ParameterValue(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, secret)));
            authResolver.getSaslInitHost(addressSpace.getName(), authService).ifPresent(saslInitHost -> parameterValues.add(new ParameterValue(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, saslInitHost)));

            // Step 1: Validate endpoints and remove unknown
            List<String> availableServices = addressSpace.getType().getServiceNames();
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
                parameterValues.add(new ParameterValue(TemplateParameter.MESSAGING_SECRET, serviceCertProviders.get("messaging").getSecretName()));
            }
            if (availableServices.contains("console")) {
                parameterValues.add(new ParameterValue(TemplateParameter.CONSOLE_SECRET, serviceCertProviders.get("console").getSecretName()));
            }
            if (availableServices.contains("mqtt")) {
                parameterValues.add(new ParameterValue(TemplateParameter.MQTT_SECRET, serviceCertProviders.get("mqtt").getSecretName()));
            }

            // Step 5: Create infrastructure
            TemplateConfig templateConfig = plan.getTemplateConfig().get();
            returnVal.resourceList = kubernetes.processTemplate(templateConfig.getName(), parameterValues.toArray(new ParameterValue[0]));
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
                .map(space -> new NamespaceInfo(space.getName(), space.getNamespace(), space.getCreatedBy()))
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
