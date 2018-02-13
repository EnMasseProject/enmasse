/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.kubernetes;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.dsl.Operation;

import java.util.Map;

/**
 * Options to configure an a resource observer.
 */
public class ObserverOptions {
    private final Map<String, String> labelFilter;
    private final Operation<? extends HasMetadata, ?, ?, ?>[] operations;

    public ObserverOptions(Map<String, String> labelFilter, Operation<? extends HasMetadata, ?, ?, ?>[] operations) {
        this.labelFilter = labelFilter;
        this.operations = operations;
    }


    public Operation<? extends HasMetadata, ?, ?, ?>[] getOperations() {
        return operations;
    }

    public Map<String, String> getObserverFilter() {
        return labelFilter;
    }
}
