/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import java.util.Set;

/**
 * Interface for components supporting listing and watching a resource.
 */
public interface Resource<T> {
    io.fabric8.kubernetes.client.Watch watchResources(io.fabric8.kubernetes.client.Watcher watcher);
    Set<T> listResources();
}
