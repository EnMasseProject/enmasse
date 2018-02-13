/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.config.service.podsense;

import io.enmasse.config.service.kubernetes.MessageEncoder;
import io.enmasse.config.service.kubernetes.SubscriptionConfig;
import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.k8s.api.Resource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * PodSense supports subscribing to a set of pods matching a label set. The response contains a list of running Pods with their IPs and ports.
 */
public class PodSenseSubscriptionConfig implements SubscriptionConfig<Pod> {

    @Override
    public MessageEncoder<Pod> getMessageEncoder() {
        return new PodSenseMessageEncoder();
    }

    @Override
    public Resource<Pod> getResource(ObserverKey observerKey, KubernetesClient client) {
        return new Resource<Pod>() {
            @Override
            public Watch watchResources(Watcher watcher) {
                return client.pods().inNamespace(client.getNamespace()).withLabels(observerKey.getLabelFilter()).watch(watcher);
            }

            @Override
            public Set<Pod> listResources() {
                return client.pods().inNamespace(client.getNamespace()).withLabels(observerKey.getLabelFilter()).list().getItems().stream()
                        .filter(pod -> filterPod(observerKey, pod))
                        .map(Pod::new)
                        .collect(Collectors.toSet());
            }
        };
    }

    private boolean filterPod(ObserverKey observerKey, io.fabric8.kubernetes.api.model.Pod pod) {
        Map<String, String> annotationFilter = observerKey.getAnnotationFilter();
        Map<String, String> annotations = pod.getMetadata().getAnnotations();
        if (annotationFilter.isEmpty()) {
            return true;
        }

        if (annotations == null) {
            return false;
        }

        for (Map.Entry<String, String> filterEntry : annotationFilter.entrySet()) {
            String annotationValue = annotations.get(filterEntry.getKey());
            if (annotationValue == null || !annotationValue.equals(filterEntry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Predicate<Pod> getResourceFilter() {
        return podResource -> podResource.getHost() != null && !podResource.getHost().isEmpty();
    }
}
