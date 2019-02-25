/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"minReplicas", "resources", "linkCapacity", "handshakeTimeout"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfigSpecRouter extends AbstractWithAdditionalProperties {
    private StandardInfraConfigSpecRouterResources resources;
    private Integer minReplicas;
    private Integer linkCapacity;
    private Integer handshakeTimeout;

    public StandardInfraConfigSpecRouter() {
    }

    public StandardInfraConfigSpecRouter(final StandardInfraConfigSpecRouterResources resources, final Integer minReplicas, final Integer linkCapacity, final Integer handshakeTimeout) {
        setResources(resources);
        setMinReplicas(minReplicas);
        setLinkCapacity(linkCapacity);
        setHandshakeTimeout(handshakeTimeout);
    }

    public void setResources(StandardInfraConfigSpecRouterResources resources) {
        this.resources = resources;
    }

    public StandardInfraConfigSpecRouterResources getResources() {
        return resources;
    }

    public void setLinkCapacity(Integer linkCapacity) {
        this.linkCapacity = linkCapacity;
    }

    public Integer getLinkCapacity() {
        return linkCapacity;
    }

    public void setMinReplicas(Integer minReplicas) {
        this.minReplicas = minReplicas;
    }

    public Integer getMinReplicas() {
        return minReplicas;
    }

    public Integer getHandshakeTimeout() {
        return handshakeTimeout;
    }

    public void setHandshakeTimeout(Integer handshakeTimeout) {
        this.handshakeTimeout = handshakeTimeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfigSpecRouter that = (StandardInfraConfigSpecRouter) o;
        return Objects.equals(resources, that.resources) &&
                Objects.equals(minReplicas, that.minReplicas) &&
                Objects.equals(handshakeTimeout, that.handshakeTimeout) &&
                Objects.equals(linkCapacity, that.linkCapacity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resources, linkCapacity, minReplicas, handshakeTimeout);
    }

    @Override
    public String toString() {
        return "StandardInfraConfigSpecRouter{" +
                "resources=" + resources +
                ", minReplicas=" + minReplicas +
                ", linkCapacity=" + linkCapacity +
                ", handshakeTimeout=" + handshakeTimeout +
                '}';
    }
}
