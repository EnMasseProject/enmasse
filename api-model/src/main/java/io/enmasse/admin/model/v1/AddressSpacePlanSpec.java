/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"displayName", "displayOrder", "shortDescription", "infraConfigRef", "addressSpaceType", "addressPlans", "resourceLimits"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpacePlanSpec extends AbstractWithAdditionalProperties {
    private Integer displayOrder;
    private String displayName;
    private String shortDescription;
    private String addressSpaceType;
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private Map<String, Double> resourceLimits = new HashMap<>();
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<String> addressPlans = new ArrayList<>();
    private String infraConfigRef;

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getAddressSpaceType() {
        return addressSpaceType;
    }

    public void setAddressSpaceType(String addressSpaceType) {
        this.addressSpaceType = addressSpaceType;
    }

    public Map<String, Double> getResourceLimits() {
        return Collections.unmodifiableMap(resourceLimits);
    }

    public void setResourceLimits(Map<String, Double> resources) {
        this.resourceLimits = new HashMap<>(resources);
    }

    public void setAddressPlans(List<String> addressPlans) {
        this.addressPlans = new ArrayList<>(addressPlans);
    }

    public List<String> getAddressPlans() {
        return Collections.unmodifiableList(addressPlans);
    }

    public String getInfraConfigRef() {
        return infraConfigRef;
    }

    public void setInfraConfigRef(String infraConfigRef) {
        this.infraConfigRef = infraConfigRef;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpacePlanSpec that = (AddressSpacePlanSpec) o;
        return Objects.equals(shortDescription, that.shortDescription) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(displayOrder, that.displayOrder) &&
                Objects.equals(addressSpaceType, that.addressSpaceType) &&
                Objects.equals(addressPlans, that.addressPlans) &&
                Objects.equals(resourceLimits, that.resourceLimits) &&
                Objects.equals(infraConfigRef, that.infraConfigRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortDescription, displayName, displayOrder, addressSpaceType, resourceLimits, addressPlans, infraConfigRef);
    }

    @Override
    public String toString() {
        return "AddressPlanSpec{" +
                "shortDescription='" + shortDescription + '\'' +
                ", displayName='" + displayName + '\'' +
                ", displayOrder='" + displayOrder + '\'' +
                ", addressSpaceType='" + addressSpaceType + '\'' +
                ", resourceLimits=" + resourceLimits +
                ", addressPlans=" + addressPlans +
                ", infraConfigRef=" + infraConfigRef +
                '}';
    }
}
