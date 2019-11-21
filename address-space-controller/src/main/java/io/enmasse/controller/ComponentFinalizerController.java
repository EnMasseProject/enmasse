/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.config.AnnotationKeys;
import io.enmasse.controller.common.Kubernetes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentFinalizerController extends AbstractFinalizerController {
    private static final Logger log = LoggerFactory.getLogger(AddressFinalizerController.class);
    private final Kubernetes kubernetes;
    public static final String FINALIZER_COMPONENTS = "enmasse.io/components";

    public ComponentFinalizerController(Kubernetes kubernetes) {
        super(FINALIZER_COMPONENTS);
        this.kubernetes = kubernetes;
    }

    @Override
    public String toString() {
        return "ComponentFinalizerController";
    }

    @Override
    protected Result processFinalizer(AddressSpace addressSpace) {
        log.info("Processing component finalizer for {}/{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName());

        final String infraUuid = addressSpace.getAnnotation(AnnotationKeys.INFRA_UUID);
        if (infraUuid != null) {
            try {
                kubernetes.deleteResources(infraUuid);
            } catch (Exception e) {
                log.warn("Error finalizing {}/{}", addressSpace.getMetadata().getNamespace(), addressSpace.getMetadata().getName(), e);
                return Result.waiting(addressSpace);
            }
        }
        return Result.completed(addressSpace);
    }
}
