/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceResolver;
import io.enmasse.address.model.AddressSpaceType;
import io.enmasse.address.model.CertSpec;
import io.enmasse.address.model.EndpointSpec;
import io.enmasse.address.model.Schema;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.SchemaProvider;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.enmasse.controller.common.ControllerReason.AddressSpaceCreated;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;

public class CreateController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(CreateController.class.getName());

    private final Kubernetes kubernetes;
    private final SchemaProvider schemaProvider;
    private final InfraResourceFactory infraResourceFactory;
    private final String namespace;
    private final EventLogger eventLogger;
    private final String defaultCertProvider;

    public CreateController(Kubernetes kubernetes, SchemaProvider schemaProvider, InfraResourceFactory infraResourceFactory, String namespace, EventLogger eventLogger, String defaultCertProvider) {
        this.kubernetes = kubernetes;
        this.schemaProvider = schemaProvider;
        this.infraResourceFactory = infraResourceFactory;
        this.namespace = namespace;
        this.eventLogger = eventLogger;
        this.defaultCertProvider = defaultCertProvider;
    }

    private static List<EndpointSpec> validateEndpoints(AddressSpaceResolver addressSpaceResolver, AddressSpace addressSpace) {
        // Set default endpoints from type
        AddressSpaceType addressSpaceType = addressSpaceResolver.getType(addressSpace);
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
        Kubernetes instanceClient = kubernetes.withNamespace(addressSpace.getAnnotation(AnnotationKeys.NAMESPACE));

        Schema schema = schemaProvider.getSchema();
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schema);
        addressSpaceResolver.validate(addressSpace);

        List<EndpointSpec> endpoints;
        if (namespace.equals(addressSpace.getAnnotation(AnnotationKeys.NAMESPACE))) {
            if (instanceClient.hasService("messaging")) {
                return addressSpace;
            }
            endpoints = validateEndpoints(addressSpaceResolver, addressSpace);
        } else {
            if (kubernetes.existsNamespace(addressSpace.getAnnotation(AnnotationKeys.NAMESPACE))) {
                return addressSpace;
            }
            endpoints = validateEndpoints(addressSpaceResolver, addressSpace);
            kubernetes.createNamespace(addressSpace);
            kubernetes.addAddressSpaceAdminRoleBinding(addressSpace);
            kubernetes.addSystemImagePullerPolicy(namespace, addressSpace);
            kubernetes.addAddressSpaceRoleBindings(addressSpace);
            kubernetes.createServiceAccount(addressSpace.getAnnotation(AnnotationKeys.NAMESPACE), kubernetes.getAddressSpaceAdminSa());
            schemaProvider.copyIntoNamespace(addressSpaceResolver.getPlan(addressSpaceResolver.getType(addressSpace), addressSpace), addressSpace.getAnnotation(AnnotationKeys.NAMESPACE));
        }
        log.info("Creating address space {}", addressSpace);

        // Ensure the required certs are set
        List<EndpointSpec> newEndpoints = new ArrayList<>();
        for (EndpointSpec endpoint : endpoints) {
            CertSpec certSpec = endpoint.getCertSpec().orElse(new CertSpec());

            EndpointSpec.Builder endpointBuilder = new EndpointSpec.Builder(endpoint);

            if (certSpec.getProvider() == null) {
                certSpec.setProvider(defaultCertProvider);
            }

            if (certSpec.getSecretName() == null) {
                certSpec.setSecretName("external-certs-" + endpoint.getService());
            }

            endpointBuilder.setCertSpec(certSpec);
            newEndpoints.add(endpointBuilder.build());
        }
        addressSpace = new AddressSpace.Builder(addressSpace)
                .setEndpointList(newEndpoints)
                .build();

        KubernetesList resourceList = new KubernetesListBuilder()
                .addAllToItems(infraResourceFactory.createResourceList(addressSpace))
                .build();

        if (log.isDebugEnabled()) {
            for (HasMetadata item : resourceList.getItems()) {
                log.debug("Creating {} of kind {}", item.getMetadata().getName(), item.getKind());
            }
        }

        kubernetes.create(resourceList, addressSpace.getAnnotation(AnnotationKeys.NAMESPACE));
        eventLogger.log(AddressSpaceCreated, "Created address space", Normal, ControllerKind.AddressSpace, addressSpace.getName());
        return addressSpace;
    }

    @Override
    public String toString() {
        return "CreateController";
    }
}
