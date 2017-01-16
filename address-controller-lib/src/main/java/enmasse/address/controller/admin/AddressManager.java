package enmasse.address.controller.admin;

import enmasse.address.controller.model.Destination;

import java.io.IOException;
import java.util.Set;

/**
 * Manages the address space state
 */
public interface AddressManager {
    void destinationsUpdated(Set<Destination> destinations);
    Set<Destination> listDestinations();
}
