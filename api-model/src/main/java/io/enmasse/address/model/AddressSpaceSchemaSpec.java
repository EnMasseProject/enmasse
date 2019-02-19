/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<@Valid NamedDescription> plans = new ArrayList<>();

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

    public List<NamedDescription> getPlans() {
        return this.plans;
    }

    public void setPlans(final List<NamedDescription> plans) {
        this.plans = plans;
    }

    public static AddressSpaceSchemaSpec fromAddressSpaceType(final AddressSpaceType type ) {
        if ( type == null ) {
            return null;
        }

        return new AddressSpaceSchemaSpecBuilder()
                .withDescription(type.getDescription())
                .withAddressTypes(type.getAddressTypes().stream()
                        .map(addressType -> AddressTypeInformation.fromAddressType(addressType))
                        .collect(Collectors.toList())
                        )
                .withPlans(type.getPlans().stream()
                        .map(plan -> new NamedDescription(plan.getMetadata().getName(), plan.getShortDescription()))
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("description=").append(this.description);
        sb.append(",");
        sb.append("plans=").append(this.plans);
        sb.append(",");
        sb.append("addressTypes=").append(this.addressTypes);
        return sb.append("}").toString();
    }
}
