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
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Represents a cluster of resources for a given destination.
 */
public class BrokerCluster {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(BrokerCluster.class.getName());

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
    private KubernetesList resources;

    public BrokerCluster(String clusterId, KubernetesList resources) {
        this.clusterId = clusterId;
        this.resources = resources;
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
}
