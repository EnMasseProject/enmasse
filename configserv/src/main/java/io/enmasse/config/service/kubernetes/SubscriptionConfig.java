/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.k8s.api.Resource;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.function.Predicate;

/**
 * Configuration for a specific type of resource observation and encoding of those resources
 */
public interface SubscriptionConfig<T> {
    MessageEncoder<T> getMessageEncoder();
    Resource<T> getResource(ObserverKey observerKey, KubernetesClient client);
    Predicate<T> getResourceFilter();
}
