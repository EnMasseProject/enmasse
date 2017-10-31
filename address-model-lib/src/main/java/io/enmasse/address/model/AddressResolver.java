/*
 * Copyright 2017 Red Hat Inc.
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

import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.Plan;

/**
 * Resolves types and plans for addresses
 */
public class AddressResolver {
    private final AddressSpaceType addressSpaceType;

    public AddressResolver(AddressSpaceType addressSpaceType) {
        this.addressSpaceType = addressSpaceType;
    }

    public Plan getPlan(Address address) {
        AddressType type = getAddressType(address);
        if (address.getPlan() != null) {
            return findPlan(type, address.getPlan().getName());
        } else {
            return type.getDefaultPlan();
        }
    }

    public AddressType getAddressType(Address address) {
        for (AddressType atype : addressSpaceType.getAddressTypes()) {
            if (atype.getName().equals(address.getType().getName())) {
                return atype;
            }
        }
        throw new RuntimeException("Unknown address type " + address.getType().getName() + " for address space type " + addressSpaceType.getName());
    }

    public Address.Builder resolveDefaults(Address address) {
        Address.Builder builder = new Address.Builder(address);
        builder.setType(getAddressType(address));
        builder.setPlan(getPlan(address));
        return builder;
    }

    private static Plan findPlan(AddressType type, String planName) {
        for (Plan plan : type.getPlans()) {
            if (plan.getName().equals(planName)) {
                return plan;
            }
        }
        throw new RuntimeException("Unknown plan " + planName + " for type " + type.getName());
    }
}
