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

import java.util.*;

public class Schema {
    private final List<AddressSpaceType> addressSpaceTypes;
    private final List<ResourceDefinition> resourceDefinitions;

    public Schema(List<AddressSpaceType> addressSpaceTypes, List<ResourceDefinition> resourceDefinitions) {
        this.addressSpaceTypes = addressSpaceTypes;
        this.resourceDefinitions = resourceDefinitions;
    }

    public List<AddressSpaceType> getAddressSpaceTypes() {
        return Collections.unmodifiableList(addressSpaceTypes);
    }

    public Optional<AddressSpaceType> findAddressSpaceType(String name) {
        for (AddressSpaceType type : addressSpaceTypes) {
            if (type.getName().equals(name)) {
                return Optional.ofNullable(type);
            }
        }
        return Optional.empty();
    }

    public Optional<ResourceDefinition> findResourceDefinition(String name) {
        for (ResourceDefinition resourceDefinition : resourceDefinitions) {
            if (name.equals(resourceDefinition.getName())) {
                return Optional.of(resourceDefinition);
            }
        }
        return Optional.empty();
    }

    public List<ResourceDefinition> getResourceDefinitions() {
        return Collections.unmodifiableList(resourceDefinitions);
    }

    public static class Builder {
        private List<AddressSpaceType> addressSpaceTypes = new ArrayList<>();
        private List<ResourceDefinition> resourceDefinitions = new ArrayList<>();

        public Builder setAddressSpaceTypes(List<AddressSpaceType> addressSpaceTypes) {
            this.addressSpaceTypes = new ArrayList<>(addressSpaceTypes);
            return this;
        }

        public Builder setResourceDefinitions(List<ResourceDefinition> resourceDefinitions) {
            this.resourceDefinitions = new ArrayList<>(resourceDefinitions);
            return this;
        }

        public Schema build() {
            Objects.requireNonNull(addressSpaceTypes, "addressSpaceTypes not set");
            Objects.requireNonNull(resourceDefinitions, "resourceDefinitions not set");
            return new Schema(addressSpaceTypes, resourceDefinitions);
        }
    }
}
