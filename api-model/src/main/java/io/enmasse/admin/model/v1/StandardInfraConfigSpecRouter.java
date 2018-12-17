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
@JsonPropertyOrder({"minReplicas", "resources", "linkCapacity"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfigSpecRouter extends AbstractWithAdditionalProperties {
    private StandardInfraConfigSpecRouterResources resources;
    private int minReplicas;
    private int linkCapacity;

    public StandardInfraConfigSpecRouter() {
    }

    public StandardInfraConfigSpecRouter(final StandardInfraConfigSpecRouterResources resources, final int minReplicas, final int linkCapacity) {
        setResources(resources);
        setMinReplicas(minReplicas);
        setLinkCapacity(linkCapacity);
    }

    public void setResources(StandardInfraConfigSpecRouterResources resources) {
        this.resources = resources;
    }

    public StandardInfraConfigSpecRouterResources getResources() {
        return resources;
    }

    public void setLinkCapacity(int linkCapacity) {
        this.linkCapacity = linkCapacity;
    }

    public int getLinkCapacity() {
        return linkCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfigSpecRouter that = (StandardInfraConfigSpecRouter) o;
        return Objects.equals(resources, that.resources) &&
                Objects.equals(linkCapacity, that.linkCapacity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resources, linkCapacity);
    }

    public void setMinReplicas(int minReplicas) {
        this.minReplicas = minReplicas;
    }

    public int getMinReplicas() {
        return minReplicas;
    }

    @Override
    public String toString() {
        return "StandardInfraConfigSpecRouter{" +
                "resources=" + resources +
                ", minReplicas=" + minReplicas +
                ", linkCapacity=" + linkCapacity +
                '}';
    }
}
