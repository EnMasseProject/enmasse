/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.address.model.*;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.api.common.SchemaProvider;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.enmasse.controller.common.ControllerReason.AddressSpaceCreated;
import static io.enmasse.controller.common.ControllerReason.AddressSpaceUpgraded;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;

public class CreateController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(CreateController.class.getName());

    private static final ObjectMapper mapper = new ObjectMapper();
    private final Kubernetes kubernetes;
    private final SchemaProvider schemaProvider;
    private final InfraResourceFactory infraResourceFactory;
    private final EventLogger eventLogger;
    private final String defaultCertProvider;
    private final String version;

    public CreateController(Kubernetes kubernetes, SchemaProvider schemaProvider, InfraResourceFactory infraResourceFactory, EventLogger eventLogger, String defaultCertProvider, String version) {
        this.kubernetes = kubernetes;
        this.schemaProvider = schemaProvider;
        this.infraResourceFactory = infraResourceFactory;
        this.eventLogger = eventLogger;
        this.defaultCertProvider = defaultCertProvider;
        this.version = version;
    }

    private static List<EndpointSpec> validateEndpoints(AddressSpaceResolver addressSpaceResolver, AddressSpace addressSpace) {
        // Set default endpoints from type
        AddressSpaceType addressSpaceType = addressSpaceResolver.getType(addressSpace.getType());
        if (addressSpace.getEndpoints().isEmpty()) {
            return addressSpaceType.getAvailableEndpoints();
        } else {
            // Validate endpoints;
            List<EndpointSpec> endpoints = addressSpace.getEndpoints();
            Set<String> services = addressSpaceType.getAvailableEndpoints().stream()
                    .map(EndpointSpec::getService)
                    .collect(Collectors.toSet());
            Set<String> actualServices = endpoints.stream()
                    .map(EndpointSpec::getService)
                    .collect(Collectors.toSet());

            services.removeAll(actualServices);
            if (!services.isEmpty()) {
                log.warn("Endpoint list is missing reference to services: {}", services);
                throw new IllegalArgumentException("Endpoint list is missing reference to services: " + services);
            }
            return endpoints;
        }
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) throws Exception {
        Schema schema = schemaProvider.getSchema();
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schema);
        addressSpaceResolver.validate(addressSpace);

        List<EndpointSpec> endpoints = validateEndpoints(addressSpaceResolver, addressSpace);

        // Ensure the required certs are set
        List<EndpointSpec> newEndpoints = new ArrayList<>();
        for (EndpointSpec endpoint : endpoints) {
            EndpointSpec.Builder endpointBuilder = new EndpointSpec.Builder(endpoint);

            CertSpec.Builder certSpec = endpoint.getCertSpec().map(CertSpec.Builder::new).orElse(new CertSpec.Builder());
            if (certSpec.getProvider() == null) {
                certSpec.setProvider(defaultCertProvider);
            }

            if (certSpec.getSecretName() == null) {
                certSpec.setSecretName(KubeUtil.getExternalCertSecretName(endpoint.getService(), addressSpace));
            }

            endpointBuilder.setCertSpec(certSpec.build());
            newEndpoints.add(endpointBuilder.build());
        }
        addressSpace = new AddressSpace.Builder(addressSpace)
                .setEndpointList(newEndpoints)
                .build();

        InfraConfig desiredInfraConfig = getInfraConfig(addressSpace);
        InfraConfig currentInfraConfig = parseCurrentInfraConfig(addressSpace);
        if (currentInfraConfig == null && !kubernetes.existsAddressSpace(addressSpace)) {
            kubernetes.ensureServiceAccountExists(addressSpace);
            KubernetesList resourceList = new KubernetesListBuilder()
                    .addAllToItems(infraResourceFactory.createInfraResources(addressSpace, desiredInfraConfig))
                    .build();

            log.info("Creating address space {}", addressSpace);

            kubernetes.create(resourceList);
            eventLogger.log(AddressSpaceCreated, "Created address space", Normal, ControllerKind.AddressSpace, addressSpace.getName());
            addressSpace.putAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG, mapper.writeValueAsString(desiredInfraConfig));
        } else if (currentInfraConfig == null || !currentInfraConfig.equals(desiredInfraConfig)) {

            if (version.equals(desiredInfraConfig.getVersion())) {

                kubernetes.ensureServiceAccountExists(addressSpace);
                KubernetesList resourceList = new KubernetesListBuilder()
                        .addAllToItems(infraResourceFactory.createInfraResources(addressSpace, desiredInfraConfig))
                        .build();

                log.info("Upgrading address space {}", addressSpace);

                kubernetes.apply(resourceList);
                eventLogger.log(AddressSpaceUpgraded, "Upgraded address space", Normal, ControllerKind.AddressSpace, addressSpace.getName());
                addressSpace.putAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG, mapper.writeValueAsString(desiredInfraConfig));
            } else {
                log.info("Version of desired config ({}) does not match controller version ({}), skipping upgrade", desiredInfraConfig.getVersion(), version);
            }
        }
        return addressSpace;
    }

    private InfraConfig getInfraConfig(AddressSpace addressSpace) {
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
        return addressSpaceResolver.getInfraConfig(addressSpace.getType(), addressSpace.getPlan());
    }

    private InfraConfig parseCurrentInfraConfig(AddressSpace addressSpace) throws IOException {
        if (addressSpace.getAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG) == null) {
            return null;
        }
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schemaProvider.getSchema());
        AddressSpaceType type = addressSpaceResolver.getType(addressSpace.getType());
        return type.getInfraConfigDeserializer().fromJson(addressSpace.getAnnotation(AnnotationKeys.APPLIED_INFRA_CONFIG));
    }

    @Override
    public String toString() {
        return "CreateController";
    }
}
