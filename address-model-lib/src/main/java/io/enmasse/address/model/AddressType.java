/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.*;

/**
 * Model type of address type.
 */
public class AddressType {
    private final String name;
    private final String description;
    private final List<AddressPlan> addressPlans;

    private AddressType(String name, String description, List<AddressPlan> addressPlans) {
        this.name = name;
        this.description = description;
        this.addressPlans = addressPlans;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<AddressPlan> getAddressPlans() {
        return Collections.unmodifiableList(addressPlans);
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
        for (AddressPlan plan : addressPlans) {
            if (plan.getName().equals(planName)) {
                return Optional.of(plan);
            }
        }
        return Optional.empty();
    }

    public static class Builder {
        private String name;
        private String description;
        private List<AddressPlan> addressPlans;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setAddressPlans(List<AddressPlan> addressPlans) {
            this.addressPlans = new ArrayList<>(addressPlans);
            return this;
        }

        public AddressType build() {
            Objects.requireNonNull(name);
            Objects.requireNonNull(description);
            Objects.requireNonNull(addressPlans);

            return new AddressType(name, description, addressPlans);
        }
    }
}
