package enmasse.controller;

import enmasse.controller.address.AddressManager;
import enmasse.controller.model.DestinationGroup;

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
