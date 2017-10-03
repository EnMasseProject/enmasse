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
package io.enmasse.controller.brokered;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.types.AddressSpaceType;
import io.enmasse.address.model.types.brokered.BrokeredAddressSpaceType;
import io.enmasse.controller.common.AddressSpaceController;

import java.util.Set;

public class BrokeredController implements AddressSpaceController {
    private final BrokeredAddressSpaceType type = new BrokeredAddressSpaceType();

    @Override
    public AddressSpaceType getAddressSpaceType() {
        return type;
    }

    @Override
    public void resourcesUpdated(Set<AddressSpace> instances) throws Exception {
        // Ignored: brokered address space controller runs within the address space
    }
}
