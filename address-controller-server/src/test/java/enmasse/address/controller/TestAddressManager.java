package enmasse.address.controller;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.model.DestinationGroup;

import java.util.LinkedHashSet;
import java.util.Set;

public class TestAddressManager implements AddressManager {
    public Set<DestinationGroup> destinationList = new LinkedHashSet<>();
    public boolean throwException = false;

    @Override
    public void destinationsUpdated(Set<DestinationGroup> destinationList) {
        if (throwException) {
            throw new RuntimeException();
        }
        this.destinationList = new LinkedHashSet<>(destinationList);
    }

    @Override
    public Set<DestinationGroup> listDestinationGroups() {
        if (throwException) {
            throw new RuntimeException();
        }
        return this.destinationList;
    }
}
