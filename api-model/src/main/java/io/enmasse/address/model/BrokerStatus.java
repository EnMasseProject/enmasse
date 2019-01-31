/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.enmasse.common.model.AbstractHasMetadata;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractHasMetadata.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrokerStatus {
    private String clusterId;
    private String containerId;
    private BrokerState state;

    public BrokerStatus() {
    }

    public BrokerStatus(String clusterId, String containerId) {
        this.clusterId = clusterId;
        this.containerId = containerId;
    }

    public BrokerStatus(String clusterId, String containerId, BrokerState brokerState) {
        this.clusterId = clusterId;
        this.containerId = containerId;
        this.state = brokerState;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
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

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getClusterId() {
        return clusterId;
    }
}
