package enmasse.controller.api.v3;

import enmasse.controller.address.AddressManager;
import enmasse.controller.address.AddressSpace;
import enmasse.controller.instance.InstanceManager;
import enmasse.controller.model.Destination;
import enmasse.controller.model.DestinationGroup;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * This is a handler for doing operations on the addressing manager that works independent of AMQP and HTTP.
 */
public class ApiHandler {
    private final InstanceManager instanceManager;
    private final AddressManager addressManager;

    public ApiHandler(InstanceManager instanceManager, AddressManager addressManager) {
        this.instanceManager = instanceManager;
        this.addressManager = addressManager;
    }

    public AddressList getAddresses(InstanceId instanceId) {
        return instanceManager.get(instanceId)
                .map(addressManager::getAddressSpace)
                .map(AddressSpace::getDestinations)
                .map(AddressList::fromGroups)
                .orElse(AddressList.fromSet(Collections.emptySet()));
    }

    public AddressList putAddresses(InstanceId instanceId, AddressList addressList) {
        Instance instance = getOrCreateInstance(instanceId);
        addressManager.getAddressSpace(instance).setDestinations(addressList.getDestinationGroups());
        return addressList;
    }

    private Instance getOrCreateInstance(InstanceId instanceId) {
        return instanceManager.get(instanceId).orElseGet(() -> {
            Instance i = new Instance.Builder(instanceId).build();
            instanceManager.create(i);
            return i;
        });
    }

    public AddressList appendAddress(InstanceId instanceId, Address address) {
        Instance instance = getOrCreateInstance(instanceId);
        Set<DestinationGroup> destinationGroups = new HashSet<>(addressManager.getAddressSpace(instance).getDestinations());
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
        addressManager.getAddressSpace(instance).setDestinations(destinationGroups);
        return AddressList.fromGroups(destinationGroups);
    }


    public Optional<Address> getAddress(InstanceId instanceId, String address) {
        return instanceManager.get(instanceId).map(addressManager::getAddressSpace).map(AddressSpace::getDestinations).orElse(Collections.emptySet()).stream()
                .flatMap(g -> g.getDestinations().stream())
                .filter(d -> d.address().equals(address))
                .findAny()
                .map(Address::new);
    }

    public Address putAddress(InstanceId instance, Address address) {
        appendAddress(instance, address);
        return address;
    }


    public AddressList deleteAddress(InstanceId instanceId, String address) {
        Optional<Instance> instance = instanceManager.get(instanceId);
        Set<DestinationGroup> destinationGroups = instance.map(addressManager::getAddressSpace).map(AddressSpace::getDestinations).orElse(Collections.emptySet());
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

        instance.ifPresent(i -> addressManager.getAddressSpace(i).setDestinations(newGroups));

        return AddressList.fromGroups(newGroups);
    }

    public AddressList appendAddresses(InstanceId instanceId, AddressList list) {
        Instance instance = getOrCreateInstance(instanceId);
        AddressSpace addressSpace = addressManager.getAddressSpace(instance);
        Set<DestinationGroup> destinationGroups = new HashSet<>(addressSpace.getDestinations());
        destinationGroups.addAll(list.getDestinationGroups());
        addressSpace.setDestinations(destinationGroups);
        return AddressList.fromGroups(destinationGroups);
    }

}
