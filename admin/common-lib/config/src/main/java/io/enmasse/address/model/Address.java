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

/**
 * Represents an Address in the address model
 */
public interface Address {
    /**
     * Return the address name. This is a human readable identifier of addresses with some restrictions:
     * * < 64 characters long
     * * Only contain '-', a-z, 0-9
     *
     * @return The name
     */
    String getName();

    /**
     * Return the UUID of this address.
     */
    String getUuid();

    /**
     * The address space of this address.
     *
     * @return The address space
     */
    String getAddressSpace();

    /**
     * Return the address string.
     *
     * @return The address
     */
    String getAddress();

    /**
     * The address type of this address
     *
     * @return The type
     */
    AddressType getType();

    /**
     * The plan used for this address.
     *
     * @return The plan
     */
    Plan getPlan();

    /**
     * The status for this address.
     *
     * @return The status
     */
    AddressStatus getStatus();
}
