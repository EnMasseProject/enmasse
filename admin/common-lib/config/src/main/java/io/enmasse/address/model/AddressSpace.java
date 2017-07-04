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

import java.util.List;

/**
 * Represents an instance of an {@link AddressSpaceType}.
 */
public interface AddressSpace {
    /**
     * The name of the address space.
     *
     * @return The name
     */
    String getName();

    /**
     * The {@link AddressSpaceType} of this address space.
     *
     * @return The type.
     */
    AddressSpaceType getType();

    /**
     * Retrieve a list of addresses configured in this address space.
     *
     * @return A list of addresses.
     */
    List<Address> getAddresses();

    /**
     * Return the plan used for this address space.
     *
     * @return The plan
     */
    Plan getPlan();
}
