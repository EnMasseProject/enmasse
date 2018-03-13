/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.openshift.client.ParameterValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface Kubernetes {
    List<BrokerCluster> listClusters();
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

    List<String> listBrokers(String clusterId);
    RouterCluster getRouterCluster();

    void scaleDeployment(String name, int numReplicas);
    void scaleStatefulSet(String name, int numReplicas);
}
