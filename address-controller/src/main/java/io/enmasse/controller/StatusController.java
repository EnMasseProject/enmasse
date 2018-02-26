/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.controller.common.Kubernetes;
import io.enmasse.controller.common.KubernetesHelper;

import java.util.Set;
import java.util.stream.Collectors;

public class StatusController implements Controller {
    private final Kubernetes kubernetes;
    private final InfraResourceFactory infraResourceFactory;

    public StatusController(Kubernetes kubernetes, InfraResourceFactory infraResourceFactory) {
        this.kubernetes = kubernetes;
        this.infraResourceFactory = infraResourceFactory;
    }

    @Override
    public AddressSpace handle(AddressSpace addressSpace) throws Exception {
        boolean isReady = isReady(addressSpace);
        if (addressSpace.getStatus().isReady() != isReady) {
            addressSpace.getStatus().setReady(isReady);
        }
        return addressSpace;
    }

    private boolean isReady(AddressSpace addressSpace) {
        Set<String> readyDeployments = kubernetes.withNamespace(addressSpace.getNamespace()).getReadyDeployments().stream()
                .map(deployment -> deployment.getMetadata().getName())
                .collect(Collectors.toSet());

        Set<String> requiredDeployments = infraResourceFactory.createResourceList(addressSpace).stream()
                .filter(KubernetesHelper::isDeployment)
                .map(item -> item.getMetadata().getName())
                .collect(Collectors.toSet());

        return readyDeployments.containsAll(requiredDeployments);
    }

    @Override
    public String toString() {
        return "EndpointController";
    }
}
