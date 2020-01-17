/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.fabric8.kubernetes.api.model.Doneable;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs = {@BuildableReference(AbstractWithAdditionalProperties.class)},
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"minAvailable", "maxUnavailable", "minReplicas", "resources", "linkCapacity", "handshakeTimeout", "idleTimeout", "workerThreads", "policy", "podTemplate"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfigSpecRouter extends AbstractWithAdditionalProperties {
    private StandardInfraConfigSpecRouterResources resources;
    private Integer minReplicas;
    private Integer minAvailable;
    private Integer maxUnavailable;
    private Integer linkCapacity;
    private Integer handshakeTimeout;
    private Integer idleTimeout;
    private Integer workerThreads;
    private RouterPolicySpec policy;
    private PodTemplateSpec podTemplate;

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

    public Integer getMinAvailable() {
        return minAvailable;
    }

    public void setMinAvailable(Integer minAvailable) {
        this.minAvailable = minAvailable;
    }

    public Integer getMaxUnavailable() {
        return maxUnavailable;
    }

    public void setMaxUnavailable(Integer maxUnavailable) {
        this.maxUnavailable = maxUnavailable;
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

    public PodTemplateSpec getPodTemplate() {
        return podTemplate;
    }

    public void setPodTemplate(PodTemplateSpec podTemplate) {
        this.podTemplate = podTemplate;
    }

    public Integer getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Integer idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public Integer getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(Integer workerThreads) {
        this.workerThreads = workerThreads;
    }

    public RouterPolicySpec getPolicy() {
        return policy;
    }

    public void setPolicy(RouterPolicySpec policy) {
        this.policy = policy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfigSpecRouter that = (StandardInfraConfigSpecRouter) o;
        return Objects.equals(resources, that.resources) &&
                Objects.equals(minReplicas, that.minReplicas) &&
                Objects.equals(minAvailable, that.minAvailable) &&
                Objects.equals(maxUnavailable, that.maxUnavailable) &&
                Objects.equals(handshakeTimeout, that.handshakeTimeout) &&
                Objects.equals(idleTimeout, that.idleTimeout) &&
                Objects.equals(linkCapacity, that.linkCapacity) &&
                Objects.equals(workerThreads, that.workerThreads) &&
                Objects.equals(policy, that.policy) &&
                Objects.equals(podTemplate, that.podTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resources, linkCapacity, minAvailable, maxUnavailable, minReplicas, handshakeTimeout, idleTimeout, workerThreads, policy, podTemplate);
    }

    @Override
    public String toString() {
        return "StandardInfraConfigSpecRouter{" +
                "resources=" + resources +
                ", minReplicas=" + minReplicas +
                ", minAvailable=" + minAvailable +
                ", maxUnavailable=" + maxUnavailable +
                ", linkCapacity=" + linkCapacity +
                ", handshakeTimeout=" + handshakeTimeout +
                ", idleTimeout=" + idleTimeout +
                ", workerThreads=" + workerThreads +
                ", policy=" + policy +
                ", podTemplate=" + podTemplate +
                '}';
    }
}
