/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.*;

/**
 * Represents the Standard address space type.
 */
public class AddressSpaceType {
    private final String name;
    private final String description;
    private final List<AddressSpacePlan> plans;
    private final List<AddressType> addressTypes;
    private final List<EndpointSpec> availableEndpoints;

    public AddressSpaceType(String name, String description, List<AddressSpacePlan> plans, List<AddressType> addressTypes, List<EndpointSpec> availableEndpoints) {
        this.name = name;
        this.description = description;
        this.plans = plans;
        this.addressTypes = addressTypes;
        this.availableEndpoints = availableEndpoints;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<AddressType> getAddressTypes() {
        return Collections.unmodifiableList(addressTypes);
    }

    public List<AddressSpacePlan> getPlans() {
        return Collections.unmodifiableList(plans);
    }

    public Optional<AddressSpacePlan> findAddressSpacePlan(String name) {
        for (AddressSpacePlan plan : plans) {
            if (plan.getName().equals(name)) {
                return Optional.ofNullable(plan);
            }
        }
        return Optional.empty();
    }

    public List<EndpointSpec> getAvailableEndpoints() {
        return Collections.unmodifiableList(availableEndpoints);
    }

    public Optional<AddressType> findAddressType(String type) {
        for (AddressType addressType : addressTypes) {
            if (addressType.getName().equals(type)) {
                return Optional.of(addressType);
            }
        }
        return Optional.empty();
    }

    public static class Builder {
        private String name;
        private String description;
        private List<AddressType> addressTypes;
        private List<AddressSpacePlan> addressSpacePlans;
        private List<EndpointSpec> availableEndpoints;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setAddressTypes(List<AddressType> addressTypes) {
            this.addressTypes = new ArrayList<>(addressTypes);
            return this;
        }

        public Builder setAddressSpacePlans(List<AddressSpacePlan> addressSpacePlans) {
            this.addressSpacePlans = new ArrayList<>(addressSpacePlans);
            return this;
        }

        public Builder setAvailableEndpoints(List<EndpointSpec> availableEndpoints) {
            this.availableEndpoints = new ArrayList<>(availableEndpoints);
            return this;
        }

        public AddressSpaceType build() {
            Objects.requireNonNull(name);
            Objects.requireNonNull(description);
            Objects.requireNonNull(addressSpacePlans);
            Objects.requireNonNull(addressTypes);
            Objects.requireNonNull(availableEndpoints);

            return new AddressSpaceType(name, description, addressSpacePlans, addressTypes, availableEndpoints);
        }
    }
}
