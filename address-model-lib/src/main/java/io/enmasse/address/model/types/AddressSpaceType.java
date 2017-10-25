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
package io.enmasse.address.model.types;

import java.util.List;

/**
 * Represents an AddressSpaceType. A type has a name, a description, a list of supported address types,
 * and a list of available plans.
 */
public interface AddressSpaceType {
    /**
     * Get the type name.
     *
     * @return The type name
     */
    String getName();

    /**
     * Get a longer description about the address space type.
     *
     * @return The description
     */
    String getDescription();

    /**
     * Get list of address types supported by this address space type.
     *
     * @return A list of address types
     */
    List<AddressType> getAddressTypes();

    /**
     * Get the list of available plans for this type.
     *
     * @return A list of plans
     */
    List<Plan> getPlans();

    /**
     * Get default plan for this address space type.
     */
    Plan getDefaultPlan();

    /**
     * Get list of supported services in this address space type.
     */
    List<String> getServiceNames();
}
