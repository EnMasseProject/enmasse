package io.enmasse.config.service.podsense;

import io.enmasse.config.service.model.ResourceFactory;
import io.enmasse.config.service.kubernetes.MessageEncoder;
import io.enmasse.config.service.kubernetes.ObserverOptions;
import io.enmasse.config.service.kubernetes.SubscriptionConfig;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Operation;

import java.util.Collections;
import java.util.function.Predicate;

/**
 * PodSense supports subscribing to a set of pods matching a label set. The response contains a list of running Pods with their IPs and ports.
 */
public class PodSenseSubscriptionConfig implements SubscriptionConfig<PodResource> {

    @Override
    public MessageEncoder<PodResource> getMessageEncoder() {
        return new PodSenseMessageEncoder();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ObserverOptions getObserverOptions(KubernetesClient client) {
        Operation<Pod , ?, ?, ?>[] ops = new Operation[1];
        ops[0] = client.pods();
        return new ObserverOptions(Collections.emptyMap(), ops);
    }

    @Override
    public ResourceFactory<PodResource> getResourceFactory() {
        return in -> new PodResource((Pod) in);
    }

    @Override
    public Predicate<PodResource> getResourceFilter() {
        return podResource -> podResource.getHost() != null && !podResource.getHost().isEmpty();
    }
}
