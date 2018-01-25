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

    public void validate(Address address) {
        getResourceDefinitions(getPlan(getType(address), address));
    }
}
