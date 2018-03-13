/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.standard;

public class RouterCluster {
    private final String name;
    private final int replicas;
    private int newReplicas;

    public RouterCluster(String name, int replicas) {
        this.name = name;
        this.replicas = replicas;
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
}
