/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.*;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"resources", "addressFullPolicy", "storageClassName", "updatePersistentVolumeClaim"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfigSpecBroker {
    private final StandardInfraConfigSpecBrokerResources resources;
    private final String addressFullPolicy;
    private final String storageClassName;
    private final Boolean updatePersistentVolumeClaim;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    @JsonCreator
    public StandardInfraConfigSpecBroker(@JsonProperty("resources") StandardInfraConfigSpecBrokerResources resources,
                                         @JsonProperty("addressFullPolicy") String addressFullPolicy,
                                         @JsonProperty("storageClassName") String storageClassName,
                                         @JsonProperty("updatePersistentVolumeClaim") Boolean updatePersistentVolumeClaim) {
        this.resources = resources;
        this.addressFullPolicy = addressFullPolicy;
        this.storageClassName = storageClassName;
        this.updatePersistentVolumeClaim = updatePersistentVolumeClaim;
    }

    public StandardInfraConfigSpecBrokerResources getResources() {
        return resources;
    }

    public String getAddressFullPolicy() {
        return addressFullPolicy;
    }

    public String getStorageClassName() {
        return storageClassName;
    }

    public Boolean getUpdatePersistentVolumeClaim() {
        return updatePersistentVolumeClaim;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfigSpecBroker that = (StandardInfraConfigSpecBroker) o;
        return Objects.equals(resources, that.resources) &&
                Objects.equals(addressFullPolicy, that.addressFullPolicy) &&
                Objects.equals(storageClassName, that.storageClassName) &&
                Objects.equals(updatePersistentVolumeClaim, that.updatePersistentVolumeClaim);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resources, addressFullPolicy, storageClassName, updatePersistentVolumeClaim);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
