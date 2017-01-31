package enmasse.address.controller.admin;

import enmasse.address.controller.model.DestinationGroup;

import java.util.Set;

/**
 * Manages the address space state
 */
public interface AddressManager {
    void destinationsUpdated(Set<DestinationGroup> destinationGroups);
    Set<DestinationGroup> listDestinationGroups();
}
