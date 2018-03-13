/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesList;
import io.fabric8.kubernetes.api.model.extensions.Deployment;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Represents a cluster of resources for a given destination.
 */
public class BrokerCluster {
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
    private final KubernetesList resources;
    private final int replicas;
    private int newReplicas;

    public BrokerCluster(String clusterId, KubernetesList resources) {
        this.clusterId = clusterId;
        this.resources = resources;
        this.replicas = findReplicas(resources.getItems());
        this.newReplicas = replicas;
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
}
