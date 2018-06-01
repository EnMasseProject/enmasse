/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.*;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.AuthenticationServiceResolverFactory;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.TemplateParameter;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.client.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
            parameters.put(TemplateParameter.ADDRESS_SPACE_NAMESPACE, addressSpace.getNamespace());
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authResolver.getHost(authService));
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authResolver.getPort(authService)));

            authResolver.getCaSecretName(authService).ifPresent(secretName -> kubernetes.getSecret(secretName).ifPresent(secret -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_CERT, secret.getData().get("tls.crt"))));
            authResolver.getClientSecretName(authService).ifPresent(secret -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, secret));
            authResolver.getSaslInitHost(addressSpace, authService).ifPresent(saslInitHost -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, saslInitHost));
            authResolver.getOAuthURL(authService).ifPresent(url -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_OAUTH_URL, url));

            Map<String, String> serviceCertMapping = new HashMap<>();
            for (EndpointSpec endpoint : addressSpace.getEndpoints()) {
                endpoint.getCertSpec().ifPresent(cert -> {
                    if (cert.getProvider() == null) {
                        cert.setProvider(defaultCertProvider);
                    }
                    serviceCertMapping.put(endpoint.getService(), cert.getSecretName());
                });
            }

            if (serviceCertMapping.containsKey("messaging")) {
                parameters.put(TemplateParameter.MESSAGING_SECRET, serviceCertMapping.get("messaging"));
            }
            if (serviceCertMapping.containsKey("console")) {
                parameters.put(TemplateParameter.CONSOLE_SECRET, serviceCertMapping.get("console"));
            }
            if (serviceCertMapping.containsKey("mqtt")) {
                parameters.put(TemplateParameter.MQTT_SECRET, serviceCertMapping.get("mqtt"));
            }

            parameters.putAll(resourceDefinition.getTemplateParameters());

            List<ParameterValue> parameterValues = new ArrayList<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                parameterValues.add(new ParameterValue(entry.getKey(), entry.getValue()));
            }

            // Step 5: Create infrastructure
            resourceList.addAll(kubernetes.processTemplate(resourceDefinition.getTemplateName().get(), parameterValues.toArray(new ParameterValue[0])).getItems());

            for (EndpointSpec endpoint : addressSpace.getEndpoints()) {
                Service service = null;
                for (HasMetadata resource : resourceList) {
                    if (resource.getKind().equals("Service") && resource.getMetadata().getName().equals(endpoint.getService())) {
                        service = (Service) resource;
                        break;
                    }
                }
                HasMetadata item = kubernetes.createEndpoint(endpoint, service, addressSpace.getName(), addressSpace.getAnnotation(AnnotationKeys.NAMESPACE));
                if (item != null) {
                    resourceList.add(item);
                }
            }
        }
        return resourceList;
    }

}
