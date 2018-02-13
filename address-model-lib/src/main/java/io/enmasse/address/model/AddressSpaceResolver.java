/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.config.AnnotationKeys;

public class AddressSpaceResolver {
    private final Schema schema;
    public AddressSpaceResolver(Schema schema) {
        this.schema = schema;
    }

    public AddressSpacePlan getPlan(AddressSpaceType addressSpaceType, AddressSpace addressSpace) {
        return addressSpaceType.findAddressSpacePlan(addressSpace.getPlan()).orElseThrow(() -> new UnresolvedAddressSpaceException("Unknown address space plan " + addressSpace.getPlan()));
    }

    public AddressSpaceType getType(AddressSpace addressSpace) {
        return schema.findAddressSpaceType(addressSpace.getType()).orElseThrow(() -> new UnresolvedAddressSpaceException("Unknown address space type " + addressSpace.getType()));
    }

    public ResourceDefinition getResourceDefinition(AddressSpacePlan plan) {
        String definedBy = plan.getAnnotations().get(AnnotationKeys.DEFINED_BY);
        if (definedBy == null) {
            return null;
        } else {
            return schema.findResourceDefinition(definedBy).orElseThrow(() -> new UnresolvedAddressSpaceException("Unknown resource definition " + definedBy));
        }
    }

    public void validate(AddressSpace addressSpace) {
        getResourceDefinition(getPlan(getType(addressSpace), addressSpace));
    }
}
