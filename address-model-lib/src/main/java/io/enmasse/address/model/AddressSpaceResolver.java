/*
 * Copyright 2018 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
