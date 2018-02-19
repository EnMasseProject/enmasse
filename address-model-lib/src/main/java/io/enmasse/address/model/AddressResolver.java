/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.ArrayList;
import java.util.List;

public class AddressResolver {
    private final Schema schema;
    private final AddressSpaceType addressSpaceType;

    public AddressResolver(Schema schema, AddressSpaceType addressSpaceType) {
        this.schema = schema;
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
            schema.findResourceDefinition(resourceName)
                    .ifPresent(resourceDefinitions::add);
        }
        return resourceDefinitions;
    }

    public ResourceDefinition getResourceDefinition(AddressPlan addressPlan, String resourceName) {
        if (isShardedTopic(addressPlan)) {
            resourceName = resourceName + "-topic";
        }
        String finalResourceName = resourceName;
        return schema.findResourceDefinition(resourceName).orElseThrow(() -> new UnresolvedAddressException("Unknown resource definition " + finalResourceName));
    }

    private boolean isShardedTopic(AddressPlan addressPlan) {
        if (addressPlan.getAddressType().equals("topic")) {
            boolean isSharded = true;
            for (ResourceRequest resourceRequest : addressPlan.getRequiredResources()) {
                if (resourceRequest.getResourceName().equals("broker") && resourceRequest.getAmount() < 1) {
                    isSharded = false;
                    break;
                }
            }
            return isSharded;
        }
        return false;
    }

    public void validate(Address address) {
        getResourceDefinitions(getPlan(getType(address), address));
    }
}
