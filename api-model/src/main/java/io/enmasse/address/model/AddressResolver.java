/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.address.model;

import io.enmasse.admin.model.v1.AddressPlan;

import java.util.Optional;

public class AddressResolver {
    private final AddressSpaceType addressSpaceType;

    public AddressResolver(AddressSpaceType addressSpaceType) {
        this.addressSpaceType = addressSpaceType;
    }

    public AddressPlan getPlan(Address address) {
        return findPlan(address).orElseThrow(() -> new UnresolvedAddressException("Unknown address plan " + address.getPlan() + " for address type " + address.getType()));
    }

    public Optional<AddressPlan> findPlan(Address address) {
        return getType(address).findAddressPlan(address.getPlan());
    }

    public AddressPlan getPlan(AddressType addressType, Address address) {
        return addressType.findAddressPlan(address.getPlan()).orElseThrow(() -> new UnresolvedAddressException("Unknown address plan " + address.getPlan() + " for address type " + address.getType()));
    }

    public AddressType getType(Address address) {
        return addressSpaceType.findAddressType(address.getType()).orElseThrow(() -> new UnresolvedAddressException("Unknown address type " + address.getType()));
    }

    public void validate(Address address) {
        getPlan(getType(address), address);
    }
}
