package io.enmasse.systemtest.resources;

import io.enmasse.systemtest.AddressType;

import java.util.List;

public class AddressPlan {

    private String name;
    private AddressType type;
    List<AddressResource> addressResources;

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
}
