/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

public class AddressSpaceSchema {
    private final AddressSpaceType addressSpaceType;
    private final String creationTimestamp;

    public AddressSpaceSchema(AddressSpaceType addressSpaceType, String creationTimestamp) {
        this.addressSpaceType = addressSpaceType;
        this.creationTimestamp = creationTimestamp;
    }

    public AddressSpaceType getAddressSpaceType() {
        return addressSpaceType;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }
}
