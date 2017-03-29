package enmasse.controller.address;

import enmasse.controller.model.DestinationGroup;

import java.util.Set;

/**
 * The {@link AddressSpace} maintains the number of destinations to be consistent with the number of destinations in config.
 */
public interface AddressSpace {
    void setDestinations(Set<DestinationGroup> newGroups);

    Set<DestinationGroup> getDestinations();
}
