/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.admin.model.AddressSpacePlan;
import io.enmasse.admin.model.v1.InfraConfig;

import java.util.Optional;

public class AddressSpaceResolver {
    private final Schema schema;
    public AddressSpaceResolver(Schema schema) {
        this.schema = schema;
    }

    public AddressSpacePlan getPlan(AddressSpaceType addressSpaceType, String plan) {
        return addressSpaceType.findAddressSpacePlan(plan).orElseThrow(() -> new UnresolvedAddressSpaceException("Unknown address space plan " + plan));
    }

    public AddressSpaceType getType(String type) {
        return schema.findAddressSpaceType(type).orElseThrow(() -> new UnresolvedAddressSpaceException("Unknown address space type " + type));
    }

    public Optional<AddressSpacePlan> getPlan(String type, String plan) {
        AddressSpaceType addressSpaceType = getType(type);
        return addressSpaceType.findAddressSpacePlan(plan);
    }

    public void validate(AddressSpace addressSpace) {
        getPlan(getType(addressSpace.getSpec().getType()), addressSpace.getSpec().getPlan());
    }

    public InfraConfig getInfraConfig(String typeName, String planName) {
        AddressSpaceType type = getType(typeName);
        AddressSpacePlan plan = getPlan(type, planName);
        String infraConfigName = plan.getInfraConfigRef();

        return type.getInfraConfigs().stream()
                .filter(c -> c.getMetadata().getName().equals(infraConfigName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown infra config " + infraConfigName + " for type " + type.getName()));
    }
}
