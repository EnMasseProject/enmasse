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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RouterSet {
    private static final Logger log = LoggerFactory.getLogger(RouterSet.class);
    private final StatefulSet statefulSet;
    private final AddressSpace addressSpace;
    private final String namespace;
    private boolean modified;

    public RouterSet(StatefulSet statefulSet, String namespace, AddressSpace addressSpace) {
        this.statefulSet = statefulSet;
        this.namespace = namespace;
        this.addressSpace = addressSpace;
        this.modified = false;
    }

    public static RouterSet create(String namespace, AddressSpace addressSpace, NamespacedKubernetesClient client) {
        StatefulSet router = client.apps().statefulSets().withName(KubeUtil.getRouterSetName(addressSpace)).get();
        return new RouterSet(router, namespace, addressSpace);
    }

    public StatefulSet getStatefulSet() {
        return statefulSet;
    }

    public void apply(NamespacedKubernetesClient client) {
        if (modified && statefulSet != null) {
            log.debug("Applying changes to router: {}", statefulSet);
            Map<String, String> annotations = statefulSet.getSpec().getTemplate().getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
            }
            annotations.put(AnnotationKeys.APPLIED_CONFIGURATION, addressSpace.getAnnotation(AnnotationKeys.APPLIED_CONFIGURATION));
            statefulSet.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
            client.apps().statefulSets().inNamespace(namespace).withName(statefulSet.getMetadata().getName()).cascading(false).patch(statefulSet);
        }
    }

    public void setModified() {
        this.modified = true;
    }

    public boolean isModified() {
        return this.modified;
    }
}
