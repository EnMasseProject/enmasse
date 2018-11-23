/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.admin.model.v1.AddressSpacePlan;
import io.enmasse.admin.model.v1.InfraConfig;

import java.util.*;

/**
 * Represents the Standard address space type.
 */
public class AddressSpaceType {
    private final String name;
    private final String description;
    private final InfraConfigDeserializer infraConfigDeserializer;
    private final List<AddressSpacePlan> plans;
    private final List<AddressType> addressTypes;
    private final List<EndpointSpec> availableEndpoints;
    private final List<InfraConfig> infraConfigs;

    public AddressSpaceType(String name, String description, InfraConfigDeserializer infraConfigDeserializer, List<AddressSpacePlan> plans, List<AddressType> addressTypes, List<EndpointSpec> availableEndpoints, List<InfraConfig> infraConfigs) {
        this.name = name;
        this.description = description;
        this.infraConfigDeserializer = infraConfigDeserializer;
        this.plans = plans;
        this.addressTypes = addressTypes;
        this.availableEndpoints = availableEndpoints;
        this.infraConfigs = infraConfigs;
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
            if (plan.getMetadata().getName().equals(name)) {
                return Optional.of(plan);
            }
        }
        return Optional.empty();
    }

    public Optional<InfraConfig> findInfraConfig(String name) {
        for (InfraConfig infraConfig : infraConfigs) {
            if (name.equals(infraConfig.getMetadata().getName())) {
                return Optional.of(infraConfig);
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

    public List<InfraConfig> getInfraConfigs() {
        return infraConfigs;
    }

    public InfraConfigDeserializer getInfraConfigDeserializer() {
        return infraConfigDeserializer;
    }

    public static class Builder {
        private String name;
        private String description;
        private InfraConfigDeserializer infraConfigDeserializer;
        private List<AddressType> addressTypes;
        private List<AddressSpacePlan> addressSpacePlans;
        private List<EndpointSpec> availableEndpoints;
        private List<InfraConfig> infraConfigs;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setInfraConfigDeserializer(InfraConfigDeserializer infraConfigDeserializer) {
            this.infraConfigDeserializer = infraConfigDeserializer;
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

        public Builder setInfraConfigs(List<InfraConfig> infraConfigs) {
            this.infraConfigs = infraConfigs;
            return this;
        }

        public AddressSpaceType build() {
            Objects.requireNonNull(name);
            Objects.requireNonNull(description);
            Objects.requireNonNull(infraConfigDeserializer);
            Objects.requireNonNull(addressSpacePlans);
            Objects.requireNonNull(addressTypes);
            Objects.requireNonNull(availableEndpoints);
            Objects.requireNonNull(infraConfigs);

            return new AddressSpaceType(name, description, infraConfigDeserializer, addressSpacePlans, addressTypes, availableEndpoints, infraConfigs);
        }
    }
}
