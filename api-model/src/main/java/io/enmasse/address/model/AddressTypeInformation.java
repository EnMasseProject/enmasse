/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.kubernetes.api.model.Doneable;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.Inline;

/**
 * A reduced, info only, view on the {@link AddressType} object.
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder",
        inline = @Inline(
                type = Doneable.class,
                prefix = "Doneable",
                value = "done"))
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressTypeInformation {

    @NotNull
    private String name;
    private String description;

    private List<@Valid NamedDescription> plans = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPlans(final List<NamedDescription> plans) {
        this.plans = plans;
    }

    public List<NamedDescription> getPlans() {
        return plans;
    }

    public static AddressTypeInformation fromAddressType(final AddressType addressType) {
        if (addressType == null) {
            return null;
        }

        return new AddressTypeInformationBuilder()
                .withName(addressType.getName())
                .withDescription(addressType.getDescription())
                .withPlans(addressType.getPlans().stream()
                        .map(plan -> new NamedDescription(plan.getMetadata().getName(), plan.getShortDescription()))
                        .collect(Collectors.toList()))
                .build();
    }

}
