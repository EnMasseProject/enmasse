/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import io.fabric8.kubernetes.api.model.HasMetadata;

public class HasMetadataFieldExtractor<T extends HasMetadata> implements FieldExtractor<T> {

    @Override
    public String getKey(T item) {
        return item.getMetadata().getNamespace() + "/" + item.getMetadata().getName();
    }

    @Override
    public String getResourceVersion(T item) {
        return item.getMetadata().getResourceVersion();
    }

    @Override
    public Long getGeneration(T item) {
        return item.getMetadata().getGeneration();
    }
}
