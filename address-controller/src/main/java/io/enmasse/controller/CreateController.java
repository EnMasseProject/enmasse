/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceResolver;
import io.enmasse.address.model.Schema;
import io.enmasse.controller.common.ControllerKind;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.k8s.api.EventLogger;
import io.enmasse.k8s.api.SchemaApi;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.KubernetesListBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.enmasse.controller.common.ControllerReason.AddressSpaceCreated;
import static io.enmasse.k8s.api.EventLogger.Type.Normal;

public class CreateController implements Controller {
    private static final Logger log = LoggerFactory.getLogger(CreateController.class.getName());

    private final Kubernetes kubernetes;
    private final SchemaProvider schemaProvider;
    private final InfraResourceFactory infraResourceFactory;
    private final String namespace;
    private final EventLogger eventLogger;

    public CreateController(Kubernetes kubernetes, SchemaProvider schemaProvider, InfraResourceFactory infraResourceFactory, String namespace, EventLogger eventLogger) {
        this.kubernetes = kubernetes;
        this.schemaProvider = schemaProvider;
        this.infraResourceFactory = infraResourceFactory;
        this.namespace = namespace;
        this.eventLogger = eventLogger;
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) throws Exception {
        Kubernetes instanceClient = kubernetes.withNamespace(addressSpace.getNamespace());

        Schema schema = schemaProvider.getSchema();
        AddressSpaceResolver addressSpaceResolver = new AddressSpaceResolver(schema);
        addressSpaceResolver.validate(addressSpace);

        if (namespace.equals(addressSpace.getNamespace())) {
            if (instanceClient.hasService("messaging")) {
                return addressSpace;
            }
        } else {
            if (kubernetes.existsNamespace(addressSpace.getNamespace())) {
                return addressSpace;
            }
            kubernetes.createNamespace(addressSpace);
            kubernetes.addAddressSpaceAdminRoleBinding(addressSpace);
            kubernetes.addSystemImagePullerPolicy(namespace, addressSpace);
            kubernetes.addAddressSpaceRoleBindings(addressSpace);
            kubernetes.createServiceAccount(addressSpace.getNamespace(), kubernetes.getAddressSpaceAdminSa());
            schemaProvider.copyIntoNamespace(addressSpaceResolver.getPlan(addressSpaceResolver.getType(addressSpace), addressSpace), addressSpace.getNamespace());
        }
        log.info("Creating address space {}", addressSpace);

        KubernetesList resourceList = new KubernetesListBuilder()
                .addAllToItems(infraResourceFactory.createResourceList(addressSpace))
                .build();

        if (log.isDebugEnabled()) {
            for (HasMetadata item : resourceList.getItems()) {
                log.debug("Creating {} of kind {}", item.getMetadata().getName(), item.getKind());
            }
        }

        kubernetes.create(resourceList, addressSpace.getNamespace());
        eventLogger.log(AddressSpaceCreated, "Created address space", Normal, ControllerKind.AddressSpace, addressSpace.getName());
        return addressSpace;
    }

    @Override
    public String toString() {
        return "CreateController";
    }
}
