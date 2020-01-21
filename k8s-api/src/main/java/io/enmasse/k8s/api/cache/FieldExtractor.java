/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

public interface FieldExtractor<T> {
    String getKey(T obj);
    String getResourceVersion(T obj);
    Long getGeneration(T obj);
}
