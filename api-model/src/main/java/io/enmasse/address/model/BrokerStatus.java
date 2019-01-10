/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Objects;

public class BrokerStatus {
    private final String clusterId;
    private final String containerId;
    private BrokerState state;

    public BrokerStatus(String clusterId, String containerId) {
        this.clusterId = clusterId;
        this.containerId = containerId;
    }

    public BrokerStatus(String clusterId, String containerId, BrokerState brokerState) {
        this.clusterId = clusterId;
        this.containerId = containerId;
        this.state = brokerState;
    }

    public String getContainerId() {
        return containerId;
    }

    public BrokerState getState() {
        return state;
    }

    public BrokerStatus setState(BrokerState state) {
        this.state = state;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokerStatus that = (BrokerStatus) o;
        return Objects.equals(clusterId, that.clusterId) &&
                Objects.equals(containerId, that.containerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId, containerId);
    }

    @Override
    public String toString() {
        return "{clusterId=" + clusterId + ",containerId=" + containerId + ",state=" + (state == null ? "null" : state.name()) + "}";
    }


    public String getClusterId() {
        return clusterId;
    }
}
