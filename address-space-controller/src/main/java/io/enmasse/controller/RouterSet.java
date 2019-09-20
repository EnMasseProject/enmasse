/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.KubeUtil;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.NamespacedKubernetesClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RouterSet {
    private final StatefulSet statefulSet;
    private final String namespace;
    private boolean modified;

    public RouterSet(StatefulSet statefulSet, String namespace) {
        this.statefulSet = statefulSet;
        this.namespace = namespace;
        this.modified = false;
    }

    public static RouterSet create(String namespace, AddressSpace addressSpace, NamespacedKubernetesClient client) {
        StatefulSet router = client.apps().statefulSets().withName(KubeUtil.getRouterSetName(addressSpace)).get();
        return new RouterSet(router, namespace);
    }

    public StatefulSet getStatefulSet() {
        return statefulSet;
    }

    public void apply(NamespacedKubernetesClient client) {
        if (modified && statefulSet != null) {
            Map<String, String> annotations = statefulSet.getSpec().getTemplate().getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
            }
            long generation = Optional.ofNullable(annotations.get(AnnotationKeys.GENERATION)).map(Long::parseLong).orElse(0L);
            annotations.put(AnnotationKeys.GENERATION, String.valueOf(generation + 1));
            statefulSet.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
            client.apps().statefulSets().inNamespace(namespace).withName(statefulSet.getMetadata().getName()).cascading(false).patch(statefulSet);
        }
    }

    public void setModified() {
        this.modified = true;
    }
}
