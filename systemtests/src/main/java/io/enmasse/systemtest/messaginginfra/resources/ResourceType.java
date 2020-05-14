/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.messaginginfra.resources;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceType<T extends HasMetadata> {
    String getKind();
    T get(String namespace, String name);
    void create(T resource);
    void delete(T resource) throws Exception;
    boolean isReady(T resource);
}
