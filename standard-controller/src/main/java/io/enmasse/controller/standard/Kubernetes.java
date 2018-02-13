/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.address.model.AddressSpacePlan;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.EventLogger;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.openshift.client.ParameterValue;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface Kubernetes {
    List<AddressCluster> listClusters();
    boolean isDestinationClusterReady(String clusterId);
    List<Pod> listRouters();


    static void addObjectLabel(KubernetesList items, String labelKey, String labelValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> labels = item.getMetadata().getLabels();
            if (labels == null) {
                labels = new LinkedHashMap<>();
            }
            labels.put(labelKey, labelValue);
            item.getMetadata().setLabels(labels);
        }
    }

    static void addObjectAnnotation(KubernetesList items, String annotationKey, String annotationValue) {
        for (HasMetadata item : items.getItems()) {
            Map<String, String> annotations = item.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new LinkedHashMap<>();
            }
            annotations.put(annotationKey, annotationValue);
            item.getMetadata().setAnnotations(annotations);
        }
    }

    void create(KubernetesList resources);

    void delete(KubernetesList resources);

    KubernetesList processTemplate(String templateName, ParameterValue... parameterValues);
    EventLogger createEventLogger(Clock clock, String componentName);

    AddressApi createAddressApi();

    List<String> listBrokers(String clusterId);

    void scaleDeployment(String deploymentName, int numReplicas);
    void scaleStatefulSet(String setName, int numReplicas);
}
