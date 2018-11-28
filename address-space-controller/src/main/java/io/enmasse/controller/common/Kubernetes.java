/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.common;

import io.enmasse.address.model.AddressSpace;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.client.ParameterValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Interface for Kubernetes operations done by the address space controller
 */
public interface Kubernetes {

    String getNamespace();

    void create(KubernetesList resources);
    void apply(KubernetesList resourceList);
    KubernetesList processTemplate(String templateName, ParameterValue ... parameterValues);

    void deleteResourcesNotIn(String[] addressSpaces);

    Set<Deployment> getReadyDeployments();

    Optional<Secret> getSecret(String secretName);

    void ensureServiceAccountExists(AddressSpace addressSpace);

    boolean existsAddressSpace(AddressSpace addressSpace);

    static void addObjectAnnotation(HasMetadata item, String annotationKey, String annotationValue) {
        Map<String, String> annotations = item.getMetadata().getAnnotations();
        if (annotations == null) {
            annotations = new LinkedHashMap<>();
        }
        annotations.put(annotationKey, annotationValue);
        item.getMetadata().setAnnotations(annotations);
    }
}
