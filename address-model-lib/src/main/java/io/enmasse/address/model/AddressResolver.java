/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.address.model.v1.SchemaProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddressResolver {
    private final SchemaProvider schemaProvider;
    private final AddressSpaceType addressSpaceType;

    public AddressResolver(SchemaProvider schemaProvider, AddressSpaceType addressSpaceType) {
        this.schemaProvider = schemaProvider;
        this.addressSpaceType = addressSpaceType;
    }

    public AddressPlan getPlan(AddressType addressType, Address address) {
        return addressType.findAddressPlan(address.getPlan()).orElseThrow(() -> new UnresolvedAddressException("Unknown address plan " + address.getPlan()));
    }

    public AddressType getType(Address address) {
        return addressSpaceType.findAddressType(address.getType()).orElseThrow(() -> new UnresolvedAddressException("Unknown address type " + address.getType()));
    }

    public List<ResourceDefinition> getResourceDefinitions(AddressPlan plan) {
        List<ResourceDefinition> resourceDefinitions = new ArrayList<>();
        for (ResourceRequest request : plan.getRequiredResources()) {
            String resourceName = request.getResourceName();
            if (plan.getAddressType().equals("topic")) {
                resourceName = request.getResourceName() + "-topic";
            }
            schemaProvider.getSchema().findResourceDefinition(resourceName)
                    .ifPresent(resourceDefinitions::add);
        }
        return resourceDefinitions;
    }

    public ResourceDefinition getResourceDefinition(AddressPlan addressPlan, String resourceName) {
        if (addressPlan.getAddressType().equals("topic")) {
            resourceName = resourceName + "-topic";
        }
        String finalResourceName = resourceName;
        return schemaProvider.getSchema().findResourceDefinition(resourceName).orElseThrow(() -> new UnresolvedAddressException("Unknown resource definition " + finalResourceName));
    }

    public void validate(Address address) {
        getResourceDefinitions(getPlan(getType(address), address));
    }
}
