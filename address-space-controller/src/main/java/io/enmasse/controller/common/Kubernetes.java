/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.admin.model.v1.InfraConfig;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;

import java.io.IOException;
import java.util.*;

/**
 * Interface for Kubernetes operations done by the address space controller
 */
public interface Kubernetes {

    String getNamespace();

    void create(KubernetesList resources);
    void apply(KubernetesList resourceList, boolean patchPersistentVolumeClaims);
    KubernetesList processTemplate(String templateName, Map<String, String> parameters);

    void deleteResourcesNotIn(String[] addressSpaces);

    Set<Deployment> getReadyDeployments(AddressSpace addressSpace);
    Set<StatefulSet> getReadyStatefulSets(AddressSpace addressSpace);

    Optional<Secret> getSecret(String secretName);

    boolean existsAddressSpace(AddressSpace addressSpace);

    static void addObjectAnnotation(HasMetadata item, String annotationKey, String annotationValue) {
        Map<String, String> annotations = item.getMetadata().getAnnotations();
        if (annotations == null) {
            annotations = new LinkedHashMap<>();
        }
        annotations.put(annotationKey, annotationValue);
        item.getMetadata().setAnnotations(annotations);
    }

    String getAppliedPlan(AddressSpace addressSpace);
    InfraConfig getAppliedInfraConfig(AddressSpace addressSpace) throws IOException;
}
