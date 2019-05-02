/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import io.enmasse.common.model.AbstractHasMetadata;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs = {@BuildableReference(AbstractHasMetadata.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"))
public class AddressSpaceSchemaSpec {
    private String description;
    private List<@Valid AddressTypeInformation> addressTypes = new ArrayList<>();
    private List<@Valid AddressSpacePlanDescription> plans = new ArrayList<>();
    private List<@Valid String> authenticationServices = new ArrayList<>();

    public String getDescription() {
        return this.description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<AddressTypeInformation> getAddressTypes() {
        return this.addressTypes;
    }

    public void setAddressTypes(final List<AddressTypeInformation> addressTypes) {
        this.addressTypes = addressTypes;
    }

    public List<AddressSpacePlanDescription> getPlans() {
        return this.plans;
    }

    public void setPlans(final List<AddressSpacePlanDescription> plans) {
        this.plans = plans;
    }

    public List<String> getAuthenticationServices() {
        return authenticationServices;
    }

    public void setAuthenticationServices(List<String> authenticationServices) {
        this.authenticationServices = authenticationServices;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSchemaSpec that = (AddressSpaceSchemaSpec) o;
        return Objects.equals(description, that.description) &&
                Objects.equals(addressTypes, that.addressTypes) &&
                Objects.equals(plans, that.plans) &&
                Objects.equals(authenticationServices, that.authenticationServices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, addressTypes, plans, authenticationServices);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("description=").append(this.description);
        sb.append(",");
        sb.append("plans=").append(this.plans);
        sb.append(",");
        sb.append("addressTypes=").append(this.addressTypes);
        sb.append("authenticationServices=").append(this.authenticationServices);
        return sb.append("}").toString();
    }
}
