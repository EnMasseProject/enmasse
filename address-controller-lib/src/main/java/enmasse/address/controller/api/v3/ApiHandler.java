package enmasse.address.controller.api.v3;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * This is a handler for doing operations on the addressing manager that works independent of AMQP and HTTP.
 */
public class ApiHandler {
    private final AddressManager addressManager;

    public ApiHandler(AddressManager addressManager) {
        this.addressManager = addressManager;
    }

    public AddressList getAddresses() {
        return AddressList.fromGroups(addressManager.listDestinationGroups());
    }

    public AddressList putAddresses(AddressList addressList) {
        addressManager.destinationsUpdated(addressList.getDestinationGroups());
        return addressList;
    }

    public AddressList appendAddress(Address address) {
        Set<DestinationGroup> destinationGroups = new HashSet<>(addressManager.listDestinationGroups());
        Destination newDest = address.getDestination();
        DestinationGroup group = null;
        for (DestinationGroup groupIt : destinationGroups) {
            if (groupIt.getGroupId().equals(newDest.group())) {
                group = groupIt;
                break;
            }
        }

        if (group == null) {
            destinationGroups.add(new DestinationGroup(newDest.group(), Collections.singleton(newDest)));
        } else {
            Set<Destination> destinations = new HashSet<>(group.getDestinations());
            destinations.add(newDest);
            destinationGroups.remove(group);
            destinationGroups.add(new DestinationGroup(newDest.group(), destinations));
        }
        addressManager.destinationsUpdated(destinationGroups);
        return AddressList.fromGroups(destinationGroups);
    }


    public Optional<Address> getAddress(String address) {
        return addressManager.listDestinationGroups().stream()
                .flatMap(g -> g.getDestinations().stream())
                .filter(d -> d.address().equals(address))
                .findAny()
                .map(Address::new);
    }

    public Address putAddress(Address address) {
        appendAddress(address);
        return address;
    }


    public AddressList deleteAddress(String address) {
        Set<DestinationGroup> destinationGroups = addressManager.listDestinationGroups();
        Set<DestinationGroup> newGroups = new HashSet<>();
        for (DestinationGroup group : destinationGroups) {
            Set<Destination> newDestinations = new HashSet<>();
            for (Destination destination : group.getDestinations()) {
                if (!destination.address().equals(address)) {
                    newDestinations.add(destination);
                }
            }
            if (!newDestinations.isEmpty()) {
                newGroups.add(group);
            }
        }
        addressManager.destinationsUpdated(newGroups);
        return AddressList.fromGroups(newGroups);
    }

    public AddressList appendAddresses(AddressList list) {
        Set<DestinationGroup> destinationGroups = new HashSet<>(addressManager.listDestinationGroups());
        destinationGroups.addAll(list.getDestinationGroups());
        addressManager.destinationsUpdated(destinationGroups);
        return AddressList.fromGroups(destinationGroups);
    }

}
