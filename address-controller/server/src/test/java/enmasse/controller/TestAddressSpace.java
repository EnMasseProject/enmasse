package enmasse.controller;

import enmasse.controller.address.AddressSpace;
import enmasse.controller.model.DestinationGroup;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * TODO: Description
 */
public class TestAddressSpace implements AddressSpace {
    public boolean throwException = false;

    private final Set<DestinationGroup> groups = new LinkedHashSet<>();

    @Override
    public void setDestinations(Set<DestinationGroup> newGroups) {
        if (throwException) {
            throw new RuntimeException("buhu");
        }
        groups.clear();
        groups.addAll(newGroups);
    }

    @Override
    public Set<DestinationGroup> getDestinations() {
        if (throwException) {
            throw new RuntimeException("buhu");
        }
        return Collections.unmodifiableSet(groups);
    }
}
