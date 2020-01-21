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
        refs = {@BuildableReference(AbstractHasMetadataWithAdditionalProperties.class)},
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"resources", "addressFullPolicy", "globalMaxSize", "storageClassName", "updatePersistentVolumeClaim", "podTemplate", "connectorIdleTimeout", "connectorWorkerThreads", "minAvailable", "maxUnavailable"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfigSpecBroker extends AbstractWithAdditionalProperties {
    private StandardInfraConfigSpecBrokerResources resources;
    private String addressFullPolicy;
    private String globalMaxSize;
    private String storageClassName;
    private Boolean updatePersistentVolumeClaim;
    private PodTemplateSpec podTemplate;
    private Integer connectorIdleTimeout;
    private Integer connectorWorkerThreads;
    private Integer minAvailable;
    private Integer maxUnavailable;

    public void setResources(StandardInfraConfigSpecBrokerResources resources) {
        this.resources = resources;
    }

    public StandardInfraConfigSpecBrokerResources getResources() {
        return resources;
    }

    public void setAddressFullPolicy(String addressFullPolicy) {
        this.addressFullPolicy = addressFullPolicy;
    }

    public String getAddressFullPolicy() {
        return addressFullPolicy;
    }

    public String getGlobalMaxSize() {
        return globalMaxSize;
    }

    public void setGlobalMaxSize(String globalMaxSize) {
        this.globalMaxSize = globalMaxSize;
    }

    public void setStorageClassName(String storageClassName) {
        this.storageClassName = storageClassName;
    }

    public String getStorageClassName() {
        return storageClassName;
    }

    public void setUpdatePersistentVolumeClaim(Boolean updatePersistentVolumeClaim) {
        this.updatePersistentVolumeClaim = updatePersistentVolumeClaim;
    }

    /*
     * NOTE: This is required due to a bug in the builder generator. For a boolean object
     * type it requires an "is" type of the getter. Luckily we can hide this behind the "default"
     * visibility. Also the "is" variant must appear before the "get" variant.
     */
    Boolean isUpdatePersistentVolumeClaim() {
        return updatePersistentVolumeClaim;
    }

    public Boolean getUpdatePersistentVolumeClaim() {
        return updatePersistentVolumeClaim;
    }

    public PodTemplateSpec getPodTemplate() {
        return podTemplate;
    }

    public void setPodTemplate(PodTemplateSpec podTemplate) {
        this.podTemplate = podTemplate;
    }

    public Integer getConnectorIdleTimeout() {
        return connectorIdleTimeout;
    }

    public void setConnectorIdleTimeout(Integer connectorIdleTimeout) {
        this.connectorIdleTimeout = connectorIdleTimeout;
    }

    public Integer getConnectorWorkerThreads() {
        return connectorWorkerThreads;
    }

    public void setConnectorWorkerThreads(Integer connectorWorkerThreads) {
        this.connectorWorkerThreads = connectorWorkerThreads;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfigSpecBroker that = (StandardInfraConfigSpecBroker) o;
        return Objects.equals(resources, that.resources) &&
                Objects.equals(addressFullPolicy, that.addressFullPolicy) &&
                Objects.equals(globalMaxSize, that.globalMaxSize) &&
                Objects.equals(storageClassName, that.storageClassName) &&
                Objects.equals(updatePersistentVolumeClaim, that.updatePersistentVolumeClaim) &&
                Objects.equals(podTemplate, that.podTemplate) &&
                Objects.equals(connectorIdleTimeout, that.connectorIdleTimeout) &&
                Objects.equals(connectorWorkerThreads, that.connectorWorkerThreads) &&
                Objects.equals(minAvailable, that.minAvailable) &&
                Objects.equals(maxUnavailable, that.maxUnavailable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resources, addressFullPolicy, globalMaxSize, storageClassName, updatePersistentVolumeClaim, podTemplate, connectorIdleTimeout, connectorWorkerThreads, minAvailable, maxUnavailable);
    }

    @Override
    public String toString() {
        return "StandardInfraConfigSpecBroker{" +
                "resources=" + resources +
                ", addressFullPolicy='" + addressFullPolicy + '\'' +
                ", storageClassName='" + storageClassName + '\'' +
                ", globalMaxSize='" + globalMaxSize + '\'' +
                ", updatePersistentVolumeClaim=" + updatePersistentVolumeClaim +
                ", podTemplate=" + podTemplate +
                ", connectorIdleTimeout=" + connectorIdleTimeout +
                ", connectorWorkerThreads=" + connectorWorkerThreads +
                ", minAvailable=" + minAvailable +
                ", maxUnavailable=" + maxUnavailable +
                '}';
    }

}
