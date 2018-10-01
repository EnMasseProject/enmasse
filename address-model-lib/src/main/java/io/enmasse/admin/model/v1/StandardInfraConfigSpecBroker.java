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
@JsonPropertyOrder({"resources", "addressFullPolicy"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StandardInfraConfigSpecBroker {
    private StandardInfraConfigSpecBrokerResources resources;
    private String addressFullPolicy;
    private Map<String, Object> additionalProperties = new HashMap<>(0);

    public StandardInfraConfigSpecBroker() { }

    public StandardInfraConfigSpecBroker(StandardInfraConfigSpecBrokerResources resources, String addressFullPolicy) {
        this.resources = resources;
        this.addressFullPolicy = addressFullPolicy;
    }

    public StandardInfraConfigSpecBrokerResources getResources() {
        return resources;
    }

    public String getAddressFullPolicy() {
        return addressFullPolicy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardInfraConfigSpecBroker that = (StandardInfraConfigSpecBroker) o;
        return Objects.equals(resources, that.resources) &&
                Objects.equals(addressFullPolicy, that.addressFullPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resources, addressFullPolicy);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void setResources(StandardInfraConfigSpecBrokerResources resources) {
        this.resources = resources;
    }

    public void setAddressFullPolicy(String addressFullPolicy) {
        this.addressFullPolicy = addressFullPolicy;
    }
}
