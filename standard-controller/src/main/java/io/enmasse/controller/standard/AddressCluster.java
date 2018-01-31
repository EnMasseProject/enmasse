/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.controller.standard;

import io.fabric8.kubernetes.api.model.KubernetesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a cluster of resources for a given destination.
 */
public class AddressCluster {
    private static final Logger log = LoggerFactory.getLogger(AddressCluster.class.getName());

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressCluster that = (AddressCluster) o;

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

    public AddressCluster(String clusterId, KubernetesList resources) {
        this.clusterId = clusterId;
        this.resources = resources;
    }

    public KubernetesList getResources() {
        return resources;
    }

    public String getClusterId() {
        return clusterId;
    }
}
