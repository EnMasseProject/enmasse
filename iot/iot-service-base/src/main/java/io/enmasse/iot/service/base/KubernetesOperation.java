/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.service.base;

import io.fabric8.kubernetes.client.KubernetesClient;

@FunctionalInterface
public interface KubernetesOperation<T> {
    T run(KubernetesClient client) throws Exception;
}