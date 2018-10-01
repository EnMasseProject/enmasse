/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import java.util.*;

public class Schema {
    private final List<AddressSpaceType> addressSpaceTypes;

    public Schema(List<AddressSpaceType> addressSpaceTypes) {
        this.addressSpaceTypes = addressSpaceTypes;
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

    public static class Builder {
        private List<AddressSpaceType> addressSpaceTypes = new ArrayList<>();

        public Builder setAddressSpaceTypes(List<AddressSpaceType> addressSpaceTypes) {
            this.addressSpaceTypes = new ArrayList<>(addressSpaceTypes);
            return this;
        }


        public Schema build() {
            Objects.requireNonNull(addressSpaceTypes, "addressSpaceTypes not set");
            return new Schema(addressSpaceTypes);
        }
    }
}
