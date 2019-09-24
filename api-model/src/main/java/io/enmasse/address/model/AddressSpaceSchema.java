/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import io.enmasse.admin.model.AddressSpacePlan;
import io.enmasse.admin.model.v1.AuthenticationService;
import io.enmasse.common.model.AbstractHasMetadata;
import io.enmasse.common.model.DefaultCustomResource;
import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;
import io.sundr.builder.annotations.Inline;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        refs= {@BuildableReference(AbstractHasMetadata.class)},
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"
                )
        )
@DefaultCustomResource
@SuppressWarnings("serial")
public class AddressSpaceSchema extends AbstractHasMetadata<AddressSpaceSchema> {

    public static final String KIND = "AddressSpaceSchema";

    @NotNull @Valid
    private AddressSpaceSchemaSpec spec;

    public AddressSpaceSchema() {
        super(KIND, CoreCrd.API_VERSION);
    }

    public void setSpec(final AddressSpaceSchemaSpec spec) {
        this.spec = spec;
    }

    public AddressSpaceSchemaSpec getSpec() {
        return this.spec;
    }

    public static AddressSpaceSchema fromAddressSpaceType(final AddressSpaceType addressSpaceType, final List<AuthenticationService> authenticationServiceList) {
        if (addressSpaceType == null) {
            return null;
        }

        return new AddressSpaceSchemaBuilder()
                .withNewMetadata()
                .withName(addressSpaceType.getName())
                .endMetadata()

                .editOrNewSpec()
                .withDescription(addressSpaceType.getDescription())
                .withAddressTypes(addressSpaceType.getAddressTypes().stream()
                        .map(AddressTypeInformation::fromAddressType)
                        .collect(Collectors.toList()))
                .withPlans(addressSpaceType.getPlans().stream()
                        .sorted(Comparator.comparingInt(AddressSpacePlan::getDisplayOrder))
                        .map(plan -> new AddressSpacePlanDescription(plan.getMetadata().getName(), plan.getDisplayName(), plan.getShortDescription(), plan.getResourceLimits()))
                        .collect(Collectors.toList()))
                .withAuthenticationServices(authenticationServiceList.stream()
                        .map(authenticationService -> authenticationService.getMetadata().getName())
                        .collect(Collectors.toList()))
                .endSpec()
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressSpaceSchema that = (AddressSpaceSchema) o;
        return Objects.equals(spec, that.spec) &&
                Objects.equals(getMetadata(), that.getMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(spec, getMetadata());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("metadata=").append(getMetadata());
        sb.append(",");
        sb.append("spec=").append(this.spec);
        return sb.append("}").toString();
    }
}
