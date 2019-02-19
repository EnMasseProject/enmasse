/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.enmasse.admin.model.v1.StandardInfraConfig;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Represents a cluster of resources for a given destination.
 */
public class BrokerCluster {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BrokerCluster that = (BrokerCluster) o;

        if (!clusterId.equals(that.clusterId)) return false;
        return resources.equals(that.resources);
    }

    @Override
    public int hashCode() {
        int result = clusterId.hashCode();
        result = 31 * result + resources.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return clusterId;
    }

    private final String clusterId;
    private final int replicas;
    private KubernetesList resources;
    private int newReplicas;
    private final int readyReplicas;

    public BrokerCluster(String clusterId, KubernetesList resources) {
        this.clusterId = clusterId;
        this.resources = resources;
        this.replicas = findReplicas(resources.getItems());
        this.readyReplicas = findReadyReplicas(resources.getItems());
        this.newReplicas = replicas;
    }

    private int findReadyReplicas(List<HasMetadata> items) {
        for (HasMetadata item : items) {
            if (item instanceof StatefulSet) {
                return Optional.ofNullable(((StatefulSet)item).getStatus()).map(StatefulSetStatus::getReadyReplicas).orElse(0);
            } else if (item instanceof Deployment) {
                return Optional.ofNullable(((Deployment)item).getStatus()).map(DeploymentStatus::getReadyReplicas).orElse(0);
            }
        }
        return 0;
    }

    private int findReplicas(List<HasMetadata> items) {
        for (HasMetadata item : items) {
            if (item instanceof StatefulSet) {
                return ((StatefulSet)item).getSpec().getReplicas();
            } else if (item instanceof Deployment) {
                return ((Deployment)item).getSpec().getReplicas();
            }
        }
        return 0;
    }

    private StandardInfraConfig findStandardInfraConfig(List<HasMetadata> items) throws IOException {
        StandardInfraConfig config = null;
        for (HasMetadata item : items) {
            if (item instanceof StatefulSet) {
                if (item.getMetadata().getAnnotations() != null && item.getMetadata().getAnnotations().get(AnnotationKeys.APPLIED_INFRA_CONFIG) != null) {
                    config = mapper.readValue(item.getMetadata().getAnnotations().get(AnnotationKeys.APPLIED_INFRA_CONFIG), StandardInfraConfig.class);
                    break;
                }
            }
        }
        return config;
    }


    public void setNewReplicas(int replicas) {
        this.newReplicas = replicas;
    }

    public int getReplicas() {
        return replicas;
    }

    public int getNewReplicas() {
        return newReplicas;
    }

    public boolean hasChanged() {
        return replicas != newReplicas;
    }

    public KubernetesList getResources() {
        return resources;
    }

    public String getClusterId() {
        return clusterId;
    }

    public StandardInfraConfig getInfraConfig() throws IOException {
        return findStandardInfraConfig(resources.getItems());
    }

    public void updateResources(BrokerCluster upgradedCluster, StandardInfraConfig infraConfig) throws Exception {
        if (upgradedCluster != null) {
            this.resources = upgradedCluster.getResources();
            for (HasMetadata item : resources.getItems()) {
                if (item instanceof StatefulSet) {
                    Kubernetes.addObjectAnnotation(item, AnnotationKeys.APPLIED_INFRA_CONFIG, mapper.writeValueAsString(infraConfig));
                }
            }
        }
    }

    public int getReadyReplicas() {
        return readyReplicas;
    }
}
