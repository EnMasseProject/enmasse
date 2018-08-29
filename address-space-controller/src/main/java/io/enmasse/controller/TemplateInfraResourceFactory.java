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
import io.fabric8.openshift.client.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TemplateInfraResourceFactory implements InfraResourceFactory {
    private static final Logger log = LoggerFactory.getLogger(TemplateInfraResourceFactory.class.getName());

    private final Kubernetes kubernetes;
    private final SchemaProvider schemaProvider;
    private final AuthenticationServiceResolverFactory authResolverFactory;

    public TemplateInfraResourceFactory(Kubernetes kubernetes, SchemaProvider schemaProvider, AuthenticationServiceResolverFactory authResolverFactory) {
        this.kubernetes = kubernetes;
        this.schemaProvider = schemaProvider;
        this.authResolverFactory = authResolverFactory;
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

            String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
            parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getName());
            parameters.put(TemplateParameter.INFRA_UUID, infraUuid);
            parameters.put(TemplateParameter.ADDRESS_SPACE_NAMESPACE, addressSpace.getNamespace());
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authResolver.getHost(authService));
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authResolver.getPort(authService)));
            parameters.put(TemplateParameter.ADDRESS_SPACE_ADMIN_SA, "sa-" + infraUuid);
            parameters.put(TemplateParameter.ADDRESS_SPACE_PLAN, addressSpace.getPlan());

            authResolver.getCaSecretName(authService).ifPresent(secretName -> kubernetes.getSecret(secretName).ifPresent(secret -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CA_CERT, secret.getData().get("tls.crt"))));
            authResolver.getClientSecretName(authService).ifPresent(secret -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, secret));
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, authResolver.getSaslInitHost(addressSpace, authService));
            authResolver.getOAuthURL(authService).ifPresent(url -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_OAUTH_URL, url));

            Map<String, CertSpec> serviceCertMapping = new HashMap<>();
            for (EndpointSpec endpoint : addressSpace.getEndpoints()) {
                    endpoint.getCertSpec().ifPresent(cert -> {
                        serviceCertMapping.put(endpoint.getService().split("-")[0], cert);
                });
            }
            if (serviceCertMapping.containsKey("messaging")) {
                parameters.put(TemplateParameter.MESSAGING_SECRET, serviceCertMapping.get("messaging").getSecretName());
            }
            if (serviceCertMapping.containsKey("console")) {
                parameters.put(TemplateParameter.CONSOLE_SECRET, serviceCertMapping.get("console").getSecretName());
            }
            if (serviceCertMapping.containsKey("mqtt")) {
                parameters.put(TemplateParameter.MQTT_SECRET, serviceCertMapping.get("mqtt").getSecretName());
            }

            parameters.putAll(resourceDefinition.getTemplateParameters());

            List<ParameterValue> parameterValues = new ArrayList<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                parameterValues.add(new ParameterValue(entry.getKey(), entry.getValue()));
            }

            // Step 5: Create infrastructure
            resourceList.addAll(kubernetes.processTemplate(resourceDefinition.getTemplateName().get(), parameterValues.toArray(new ParameterValue[0])).getItems());
        }
        return resourceList;
    }

}
