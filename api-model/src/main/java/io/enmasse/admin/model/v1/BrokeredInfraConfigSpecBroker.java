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
        refs= {
                @BuildableReference(AbstractWithAdditionalProperties.class)
        },
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"resources", "addressFullPolicy", "globalMaxSize", "storageClassName", "updatePersistentVolumeClaim", "podTemplate", "javaOpts"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrokeredInfraConfigSpecBroker extends AbstractWithAdditionalProperties {
    private BrokeredInfraConfigSpecBrokerResources resources;
    private String addressFullPolicy;
    private String globalMaxSize;
    private String storageClassName;
    private Boolean updatePersistentVolumeClaim;
    private PodTemplateSpec podTemplate;
    private String javaOpts;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BrokeredInfraConfigSpecBroker that = (BrokeredInfraConfigSpecBroker) o;
        return Objects.equals(resources, that.resources) &&
                Objects.equals(addressFullPolicy, that.addressFullPolicy) &&
                Objects.equals(globalMaxSize, that.globalMaxSize) &&
                Objects.equals(storageClassName, that.storageClassName) &&
                Objects.equals(updatePersistentVolumeClaim, that.updatePersistentVolumeClaim) &&
                Objects.equals(podTemplate, that.podTemplate) &&
                Objects.equals(javaOpts, that.javaOpts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resources, addressFullPolicy, globalMaxSize, storageClassName, updatePersistentVolumeClaim, podTemplate, javaOpts);
    }

    @Override
    public String toString() {
        return "BrokeredInfraConfigSpecBroker{" +
                "resources=" + resources +
                ", addressFullPolicy='" + addressFullPolicy + '\'' +
                ", globalMaxSize='" + globalMaxSize + '\'' +
                ", storageClassName='" + storageClassName + '\'' +
                ", updatePersistentVolumeClaim='" + updatePersistentVolumeClaim + '\'' +
                ", podTemplate='" + podTemplate + '\'' +
                ", javaOpts='" + javaOpts + '\'' +
                '}';
    }

    public void setResources(BrokeredInfraConfigSpecBrokerResources resources) {
        this.resources = resources;
    }

    public BrokeredInfraConfigSpecBrokerResources getResources() {
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

    public void setJavaOpts(String javaOpts) {
        this.javaOpts = javaOpts;
    }

    public String getJavaOpts() {
        return javaOpts;
    }
}
