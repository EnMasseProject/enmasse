/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.admin.model.AddressSpacePlan;
import io.enmasse.admin.model.v1.InfraConfig;
import io.enmasse.model.validation.DefaultValidator;

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

    public boolean validate(AddressSpace addressSpace) {
        if (addressSpace.getSpec().getAuthenticationService() != null && !schema.findAuthenticationService(addressSpace.getSpec().getAuthenticationService().getName()).isPresent()) {
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage("Unknown authentication service '" + addressSpace.getSpec().getAuthenticationService().getName() + "'");
            return false;
        }

        AddressSpaceType addressSpaceType = schema.findAddressSpaceType(addressSpace.getSpec().getType()).orElse(null);
        // This should never happen
        if (addressSpaceType == null) {
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage("Unknown address space type '" + addressSpace.getSpec().getType() + "'");
            return false;
        } else {
            AddressSpacePlan plan = addressSpaceType.findAddressSpacePlan(addressSpace.getSpec().getPlan()).orElse(null);
            if (plan == null) {
                // Don't set status false, as long as applied plan is good
                addressSpace.getStatus().appendMessage("Unknown address space plan '" + addressSpace.getSpec().getPlan() + "'");
                return false;
            }
        }

        try {
            DefaultValidator.validate(addressSpace);
        } catch (Exception e) {
            addressSpace.getStatus().setReady(false);
            addressSpace.getStatus().appendMessage("Error validating address space '" + addressSpace.getMetadata().getName() + "' in namespace '" + addressSpace.getMetadata().getNamespace() + "': " + e.getMessage());
            return false;
        }

        return true;
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
