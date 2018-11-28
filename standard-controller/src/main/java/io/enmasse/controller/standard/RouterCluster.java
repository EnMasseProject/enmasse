/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

import io.enmasse.admin.model.v1.StandardInfraConfig;

public class RouterCluster {
    private final String name;
    private final int replicas;
    private final StandardInfraConfig infraConfig;
    private int newReplicas;

    public RouterCluster(String name, int replicas, StandardInfraConfig infraConfig) {
        this.name = name;
        this.replicas = replicas;
        this.infraConfig = infraConfig;
        this.newReplicas = replicas;
    }

    public String getName() {
        return name;
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

    public StandardInfraConfig getInfraConfig() {
        return infraConfig;
    }
}
