package enmasse.address.controller.api.v3;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.admin.AddressManagerFactory;
import enmasse.address.controller.model.Destination;
import enmasse.address.controller.model.DestinationGroup;
import enmasse.address.controller.model.InstanceId;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * This is a handler for doing operations on the addressing manager that works independent of AMQP and HTTP.
 */
public class ApiHandler {
    private final AddressManagerFactory addressManagerFactory;

    public ApiHandler(AddressManagerFactory addressManagerFactory) {
        this.addressManagerFactory = addressManagerFactory;
    }

    public AddressList getAddresses(InstanceId instance) {
        Optional<AddressManager> addressManager = addressManagerFactory.getAddressManager(instance);
        return AddressList.fromGroups(addressManager.map(AddressManager::listDestinationGroups).orElse(Collections.emptySet()));
    }

    public AddressList putAddresses(InstanceId instance, AddressList addressList) {
        AddressManager addressManager = addressManagerFactory.getOrCreateAddressManager(instance);
        addressManager.destinationsUpdated(addressList.getDestinationGroups());
        return addressList;
    }

    public AddressList appendAddress(InstanceId instance, Address address) {
        AddressManager addressManager = addressManagerFactory.getOrCreateAddressManager(instance);
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


    public Optional<Address> getAddress(InstanceId instance, String address) {
        Optional<AddressManager> addressManager = addressManagerFactory.getAddressManager(instance);
        return addressManager.map(AddressManager::listDestinationGroups).orElse(Collections.emptySet()).stream()
                .flatMap(g -> g.getDestinations().stream())
                .filter(d -> d.address().equals(address))
                .findAny()
                .map(Address::new);
    }

    public Address putAddress(InstanceId instance, Address address) {
        appendAddress(instance, address);
        return address;
    }


    public AddressList deleteAddress(InstanceId instance, String address) {
        Optional<AddressManager> addressManager = addressManagerFactory.getAddressManager(instance);
        Set<DestinationGroup> destinationGroups = addressManager.map(AddressManager::listDestinationGroups).orElse(Collections.emptySet());
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

        addressManager.ifPresent(mgr -> mgr.destinationsUpdated(newGroups));

        return AddressList.fromGroups(newGroups);
    }

    public AddressList appendAddresses(InstanceId instance, AddressList list) {
        AddressManager addressManager = addressManagerFactory.getOrCreateAddressManager(instance);
        Set<DestinationGroup> destinationGroups = new HashSet<>(addressManager.listDestinationGroups());
        destinationGroups.addAll(list.getDestinationGroups());
        addressManager.destinationsUpdated(destinationGroups);
        return AddressList.fromGroups(destinationGroups);
    }

}
