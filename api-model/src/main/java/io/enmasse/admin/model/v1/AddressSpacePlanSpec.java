/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

import java.util.*;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(type = Doneable.class, prefix = "Doneable", value = "done")
)
@JsonPropertyOrder({"displayName", "shortDescription", "addressSpaceType", "addressPlans", "resourceLimits"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSpacePlanSpec extends AbstractWithAdditionalProperties {
    private String shortDescription;
    private String addressSpaceType;
    private Map<String, Double> resourceLimits = new HashMap<>();
    private List<String> addressPlans;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpacePlanSpec that = (AddressSpacePlanSpec) o;
        return Objects.equals(shortDescription, that.shortDescription) &&
                Objects.equals(addressSpaceType, that.addressSpaceType) &&
                Objects.equals(addressPlans, that.addressPlans) &&
                Objects.equals(resourceLimits, that.resourceLimits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shortDescription, addressSpaceType, resourceLimits, addressPlans);
    }

    @Override
    public String toString() {
        return "AddressPlanSpec{" +
                "shortDescription='" + shortDescription + '\'' +
                ", addressSpaceType='" + addressSpaceType + '\'' +
                ", resourceLimits=" + resourceLimits +
                ", addressPlans=" + addressPlans +
                '}';
    }

    public void setAddressPlans(List<String> addressPlans) {
        this.addressPlans = new ArrayList<>(addressPlans);
    }

    public List<String> getAddressPlans() {
        return addressPlans;
    }
}
