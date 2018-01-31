/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
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
