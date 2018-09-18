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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.*;

public class TemplateInfraResourceFactory implements InfraResourceFactory {
    private static final Logger log = LoggerFactory.getLogger(TemplateInfraResourceFactory.class.getName());

    private final Kubernetes kubernetes;
    private final SchemaProvider schemaProvider;
    private final AuthenticationServiceResolverFactory authResolverFactory;
    private final boolean openShift;

    public TemplateInfraResourceFactory(Kubernetes kubernetes, SchemaProvider schemaProvider, AuthenticationServiceResolverFactory authResolverFactory, boolean openShift) {
        this.kubernetes = kubernetes;
        this.schemaProvider = schemaProvider;
        this.authResolverFactory = authResolverFactory;
        this.openShift = openShift;
    }

    @Override
    public List<HasMetadata> createResourceList(AddressSpace addressSpace) {
        List<HasMetadata> resourceList = new ArrayList<>();

        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
        AddressSpaceType addressSpaceType = addressSpaceResolver.getType(addressSpace);
        AddressSpacePlan plan = addressSpaceResolver.getPlan(addressSpaceType, addressSpace);
        ResourceDefinition resourceDefinition = addressSpaceResolver.getResourceDefinition(plan);


        if (resourceDefinition != null && resourceDefinition.getTemplateName().isPresent()) {
            AuthenticationService authService = addressSpace.getAuthenticationService();
            AuthenticationServiceResolver authResolver = authResolverFactory.getResolver(authService.getType());

            String kcIdpHint = "";
            if  (addressSpace.getAnnotation(AnnotationKeys.KC_IDP_HINT) != null) {
                kcIdpHint = addressSpace.getAnnotation(AnnotationKeys.KC_IDP_HINT);
                if ("none".equals(kcIdpHint)) {
                    kcIdpHint = "";
                }
            } else if (this.openShift && authService.getType() == AuthenticationServiceType.STANDARD) {
                kcIdpHint = "openshift-v3";
            }

            Map<String, String> parameters = new HashMap<>();

            String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
            parameters.put(TemplateParameter.ADDRESS_SPACE, addressSpace.getName());
            parameters.put(TemplateParameter.INFRA_UUID, infraUuid);
            parameters.put(TemplateParameter.ADDRESS_SPACE_NAMESPACE, addressSpace.getNamespace());
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_HOST, authResolver.getHost(authService));
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_PORT, String.valueOf(authResolver.getPort(authService)));
            parameters.put(TemplateParameter.ADDRESS_SPACE_ADMIN_SA, KubeUtil.getAddressSpaceSaName(addressSpace));
            parameters.put(TemplateParameter.ADDRESS_SPACE_PLAN, addressSpace.getPlan());
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_KC_IDP_HINT, kcIdpHint);

            String encodedCaCert = authResolver.getCaSecretName(authService)
                    .map(secretName ->
                        kubernetes.getSecret(secretName).map(secret ->
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
            authResolver.getClientSecretName(authService).ifPresent(secret -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_CLIENT_SECRET, secret));
            parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_SASL_INIT_HOST, authResolver.getSaslInitHost(addressSpace, authService));
            authResolver.getOAuthURL(authService).ifPresent(url -> parameters.put(TemplateParameter.AUTHENTICATION_SERVICE_OAUTH_URL, url));

            Map<String, CertSpec> serviceCertMapping = new HashMap<>();
            for (EndpointSpec endpoint : addressSpace.getEndpoints()) {
                    endpoint.getCertSpec().ifPresent(cert -> {
                        serviceCertMapping.put(endpoint.getService(), cert);
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
