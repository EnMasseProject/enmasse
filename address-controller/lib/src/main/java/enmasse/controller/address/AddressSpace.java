package enmasse.controller.address;

import enmasse.controller.model.Destination;

import java.util.Set;

/**
 * The {@link AddressSpace} maintains the number of destinations to be consistent with the number of destinations in config.
 */
public interface AddressSpace {
    Set<Destination> addDestination(Destination destination);
    Set<Destination> deleteDestination(String address);

    Set<Destination> setDestinations(Set<Destination> destinations);
    Set<Destination> addDestinations(Set<Destination> destination);
    Set<Destination> getDestinations();
}
