/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.enmasse.common.model.DefaultCustomResource;
import io.enmasse.config.AnnotationKeys;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.*;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractHasMetadataWithAdditionalProperties.class)},
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressSpacePlan extends AbstractHasMetadataWithAdditionalProperties<AddressSpacePlan> implements io.enmasse.admin.model.AddressSpacePlan {

    public static final String KIND = "AddressSpacePlan";

    private AddressSpacePlanSpec spec;

    private String shortDescription;
    private String addressSpaceType;
    private List<ResourceAllowance> resources = new LinkedList<>();
    private List<String> addressPlans = new LinkedList<>();

    public AddressSpacePlan() {
        super(KIND, AdminCrd.API_VERSION_V1BETA2);
    }

    public void setSpec(AddressSpacePlanSpec spec) {
        this.spec = spec;
    }

    public AddressSpacePlanSpec getSpec() {
        return spec;
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

    @JsonIgnore
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

    @JsonIgnore
    @Override
    public List<String> getAddressPlans() {
        if (spec != null) {
            return spec.getAddressPlans();
        } else {
            return addressPlans;
        }
    }

    @JsonIgnore
    @Override
    public String getShortDescription() {
        if (spec != null) {
            return spec.getShortDescription();
        } else {
            return shortDescription;
        }
    }

    @JsonIgnore
    @Override
    public String getDisplayName() {
        String displayName = getMetadata().getName();
        if (spec != null && spec.getDisplayName() != null) {
            displayName = spec.getDisplayName();
        }
        return displayName;
    }

    @JsonIgnore
    @Override
    public int getDisplayOrder() {
        int order = 0;
        if (spec != null && spec.getDisplayOrder() != null) {
            order = spec.getDisplayOrder();
        }
        return order;
    }

    @JsonIgnore
    @Override
    public String getAddressSpaceType() {
        if (spec != null) {
            return spec.getAddressSpaceType();
        } else {
            return addressSpaceType;
        }
    }

    @JsonIgnore
    @Override
    public String getInfraConfigRef() {
        if (spec != null) {
            return spec.getInfraConfigRef();
        } else {
            return getAnnotation(AnnotationKeys.DEFINED_BY);
        }
    }

    @JsonIgnore
    public List<ResourceAllowance> getResources() {
        return resources;
    }

    @JsonProperty("resources")
    public void setResources(List<ResourceAllowance> resources) {
        this.resources = resources;
    }

    @JsonProperty("addressPlans")
    public void setAddressPlans(List<String> addressPlans) {
        this.addressPlans = addressPlans;
    }

    @JsonProperty("shortDescription")
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    @JsonProperty("addressSpaceType")
    public void setAddressSpaceType(String addressSpaceType) {
        this.addressSpaceType = addressSpaceType;
    }

}
