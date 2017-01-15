package enmasse.storage.controller.admin;

import enmasse.storage.controller.model.Destination;

import java.util.Set;

/**
 * Manages the address space state
 */
public interface AddressManager {
    void destinationsUpdated(Set<Destination> destinations);
    Set<Destination> listDestinations();
}
