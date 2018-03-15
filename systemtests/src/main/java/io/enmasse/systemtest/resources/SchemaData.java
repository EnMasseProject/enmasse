/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import java.util.List;
import java.util.stream.Collectors;

public class SchemaData {
    private List<AddressSpaceTypeData> addressSpaceTypes;

    public SchemaData(List<AddressSpaceTypeData> addressSpaceTypes) {
        this.addressSpaceTypes = addressSpaceTypes;
    }

    public List<AddressSpaceTypeData> getAddressSpaceTypes() {
        return addressSpaceTypes;
    }

    public AddressSpaceTypeData getAddressSpaceType(String name) {
        return this.addressSpaceTypes.stream().filter(s -> s.getName().equals(name)).collect(Collectors.toList()).get(0);
    }
}
