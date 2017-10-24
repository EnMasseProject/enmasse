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
