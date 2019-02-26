/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import io.enmasse.common.model.AbstractHasMetadata;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractHasMetadata.class)},
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressSpacePlan extends AbstractHasMetadata<AddressSpacePlan> implements io.enmasse.admin.model.AddressSpacePlan {

    public static final String KIND = "AddressSpacePlan";

    private AddressSpacePlanSpec spec;

    private String shortDescription;
    private String addressSpaceType;
    private List<ResourceAllowance> resources = new LinkedList<>();
    private List<String> addressPlans = new LinkedList<>();

    private Map<String, Object> additionalProperties = new HashMap<>(0);

    public AddressSpacePlan() {
        super(KIND, AdminCrd.API_VERSION_V1BETA1);
    }

    public void setResources(List<ResourceAllowance> resources) {
        this.resources = resources;
    }
    
    public List<ResourceAllowance> getResources() {
        return resources;
    }

    public void setAddressPlans(List<String> addressPlans) {
        this.addressPlans = addressPlans;
    }

    @Override
    public Map<String, Double> getResourceLimits() {
        if (spec != null) {
            return spec.getResourceLimits();
        } else {
            Map<String, Double> resourceLimits = new HashMap<>();
            for (ResourceAllowance allowance : resources) {
                resourceLimits.put(allowance.getName(), allowance.getMax());
            }
            return resourceLimits;
        }
    }

    public List<String> getAddressPlans() {
        if (spec != null) {
            return spec.getAddressPlans();
        } else {
            return addressPlans;
        }
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }
    
    public String getShortDescription() {
        if (spec != null) {
            return spec.getShortDescription();
        } else {
            return shortDescription;
        }
    }

    public void setAddressSpaceType(String addressSpaceType) {
        this.addressSpaceType = addressSpaceType;
    }
    
    public String getAddressSpaceType() {
        if (spec != null) {
            return spec.getAddressSpaceType();
        } else {
            return addressSpaceType;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpacePlan that = (AddressSpacePlan) o;
        return Objects.equals(getMetadata(), that.getMetadata()) &&
                Objects.equals(spec, that.getSpec());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), spec);
    }

    @Override
    public String toString() {
        return "AddressSpacePlan{" +
                "metadata='" + getMetadata()+ '\'' +
                ", spec='" + spec + '\'' +
                '}';
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    public void setSpec(AddressSpacePlanSpec spec) {
        this.spec = spec;
    }

    public AddressSpacePlanSpec getSpec() {
        return spec;
    }
}
