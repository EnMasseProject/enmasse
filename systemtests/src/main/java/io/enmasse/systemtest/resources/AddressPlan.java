/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.resources;

import io.enmasse.systemtest.AddressType;

import java.util.List;

public class AddressPlan {

    List<AddressResource> addressResources;
    private String name;
    private AddressType type;

    public AddressPlan(String name, AddressType type, List<AddressResource> addressResources) {
        this.name = name;
        this.type = type;
        this.addressResources = addressResources;
    }

    public String getName() {
        return name;
    }

    public AddressType getType() {
        return type;
    }

    public List<AddressResource> getAddressResources() {
        return addressResources;
    }

    public double getRequiredCreditFromResource(String addressResource) throws java.lang.IllegalStateException {
        for (AddressResource res : this.getAddressResources()) {
            if (addressResource.equals(res.getName())) {
                return res.getCredit();
            }
        }
        throw new java.lang.IllegalStateException(String.format("address resource '%s' didn't found", addressResource));
    }
}
