/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;

import java.util.List;

public class DeleteController implements Controller {
    private final Kubernetes kubernetes;

    public DeleteController(Kubernetes kubernetes) {
        this.kubernetes = kubernetes;
    }

    @Override
    public String toString() {
        return "DeleteController";
    }

    @Override
    public void reconcileAll(List<AddressSpace> desiredAddressSpaces) {
        String [] uuids = desiredAddressSpaces.stream()
                .map(a -> a.getAnnotation(AnnotationKeys.INFRA_UUID))
                .toArray(String[]::new);
        kubernetes.deleteResourcesNotIn(uuids);
    }
}
