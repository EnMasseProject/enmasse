/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.enmasse.controller.common.AuthenticationServiceResolverFactory;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.TemplateParameter;
import io.enmasse.k8s.api.SchemaApi;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.client.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class TemplateInfraResourceFactory implements InfraResourceFactory {
    private static final Logger log = LoggerFactory.getLogger(TemplateInfraResourceFactory.class.getName());

    private final Kubernetes kubernetes;
    private final SchemaProvider schemaProvider;
    private final AuthenticationServiceResolverFactory authResolverFactory;
    private final String defaultCertProvider;

    public TemplateInfraResourceFactory(Kubernetes kubernetes, SchemaProvider schemaProvider, AuthenticationServiceResolverFactory authResolverFactory, String defaultCertProvider) {
        this.kubernetes = kubernetes;
        this.schemaProvider = schemaProvider;
        this.authResolverFactory = authResolverFactory;
        this.defaultCertProvider = defaultCertProvider;
    }

    @Override
    public List<HasMetadata> createResourceList(AddressSpace addressSpace) {
        List<HasMetadata> resourceList = new ArrayList<>();

        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
        AddressSpaceType addressSpaceType = addressSpaceResolver.getType(addressSpace);
        AddressSpacePlan plan = addressSpaceResolver.getPlan(addressSpaceType, addressSpace);
        ResourceDefinition resourceDefinition = addressSpaceResolver.getResourceDefinition(plan);

        if (resourceDefinition != null && resourceDefinition.getTemplateName().isPresent()) {
            Map<String, String> parameters = new HashMap<>();
            AuthenticationService authService = addressSpace.getAuthenticationService();
            AuthenticationServiceResolver authResolver = authResolverFactory.getResolver(authService.getType());

            parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getName());
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authResolver.getHost(authService));
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authResolver.getPort(authService)));

            authResolver.getCaSecretName(authService).ifPresent(secretName -> kubernetes.getSecret(secretName).ifPresent(secret -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_CERT, secret.getData().get("tls.crt"))));
            authResolver.getClientSecretName(authService).ifPresent(secret -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, secret));
            authResolver.getSaslInitHost(addressSpace.getName(), authService).ifPresent(saslInitHost -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, saslInitHost));

            // Step 1: Validate endpoints and remove unknown
            List<String> availableServices = addressSpaceType.getServiceNames();
            Map<String, CertSpec> serviceCertProviders = new HashMap<>();

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
                        endpoint.getCertSpec().ifPresent(certProvider -> serviceCertProviders.put(endpoint.getService(), certProvider));
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
                if (!serviceCertProviders.containsKey(service)) {
                    serviceCertProviders.put(service, new CertSpec(defaultCertProvider));
                }
            }

            // Step 3: Ensure all endpoints have their certProviders set
            endpoints = endpoints.stream()
                    .map(endpoint -> {
                        if (!endpoint.getCertSpec().isPresent()) {
                            return new Endpoint.Builder(endpoint)
                                    .setCertSpec(serviceCertProviders.get(endpoint.getService()))
                                    .build();
                        } else {
                            return endpoint;
                        }
                    }).collect(Collectors.toList());

            for (Map.Entry<String, CertSpec> entry : serviceCertProviders.entrySet()) {
                if (entry.getValue().getSecretName() == null) {
                    entry.getValue().setSecretName("external-certs-" + entry.getKey());
                }
            }

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
            resourceList.addAll(kubernetes.processTemplate(resourceDefinition.getTemplateName().get(), parameterValues.toArray(new ParameterValue[0])).getItems());

            for (Endpoint endpoint : endpoints) {
                Service service = null;
                for (HasMetadata resource : resourceList) {
                    if (resource.getKind().equals("Service") && resource.getMetadata().getName().equals(endpoint.getService())) {
                        service = (Service) resource;
                        break;
                    }
                }
                HasMetadata item = kubernetes.createEndpoint(endpoint, service, addressSpace.getName(), addressSpace.getNamespace());
                if (item != null) {
                    resourceList.add(item);
                }
            }
        }
        return resourceList;
    }

}
