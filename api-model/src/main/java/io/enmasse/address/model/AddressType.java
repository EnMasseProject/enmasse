/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.admin.model.AddressPlan;
import io.sundr.builder.annotations.Buildable;

import java.util.*;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Model type of address type.
 */
@Buildable(
        editableEnabled = false,
        generateBuilderPackage = false,
        builderPackage = "io.fabric8.kubernetes.api.builder"
        )
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressType {
    private String name;
    private String description;
    private List<@Valid AddressPlan> plans = new ArrayList<>();

    public AddressType() {
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setPlans(List<AddressPlan> addressPlans) {
        this.plans = addressPlans;
    }

    public List<AddressPlan> getPlans() {
        return Collections.unmodifiableList(plans);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AddressType addressType = (AddressType) o;

        return name.equals(addressType.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public Optional<AddressPlan> findAddressPlan(String planName) {
        for (AddressPlan plan : plans) {
            if (plan.getMetadata().getName().equals(planName)) {
                return Optional.of(plan);
            }
        }

        return Optional.empty();
    }
}
