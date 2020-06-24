/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.admin.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private AddressSpacePlanStatus status;

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
                Objects.equals(spec, that.getSpec()) &&
                Objects.equals(status, that.getStatus());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), spec, status);
    }

    @Override
    public String toString() {
        return "AddressSpacePlan{" +
                "metadata='" + getMetadata()+ '\'' +
                ", spec='" + spec + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

    @JsonIgnore
    @Override
    public Map<String, Double> getResourceLimits() {
        return spec.getResourceLimits();
    }

    @JsonIgnore
    @Override
    public List<String> getAddressPlans() {
        return spec.getAddressPlans();
    }

    @JsonIgnore
    @Override
    public String getShortDescription() {
        return spec.getShortDescription();
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
        return spec.getAddressSpaceType();
    }

    @JsonIgnore
    @Override
    public String getInfraConfigRef() {
        return spec.getInfraConfigRef();
    }

    public AddressSpacePlanStatus getStatus() {
        return status;
    }

    public void setStatus(AddressSpacePlanStatus status) {
        this.status = status;
    }
}
