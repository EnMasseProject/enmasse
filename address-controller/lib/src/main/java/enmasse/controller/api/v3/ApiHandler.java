package enmasse.controller.api.v3;

import enmasse.controller.address.AddressManager;
import enmasse.controller.address.AddressSpace;
import enmasse.controller.instance.InstanceManager;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.util.Collections;
import java.util.Optional;

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
                .map(AddressList::fromSet)
                .orElse(AddressList.fromSet(Collections.emptySet()));
    }

    public AddressList putAddresses(InstanceId instanceId, AddressList addressList) throws Exception {
        Instance instance = getOrCreateInstance(instanceId);
        addressManager.getAddressSpace(instance).setDestinations(addressList.getDestinations());
        return addressList;
    }

    private Instance getOrCreateInstance(InstanceId instanceId) throws Exception {
        Optional<Instance> instance = instanceManager.get(instanceId);
        if (!instance.isPresent()) {
            Instance i = new Instance.Builder(instanceId).build();
            instanceManager.create(i);
            return i;
        } else {
            return instance.get();
        }
    }

    public AddressList appendAddress(InstanceId instanceId, Address address) throws Exception {
        Instance instance = getOrCreateInstance(instanceId);
        return AddressList.fromSet(addressManager.getAddressSpace(instance).addDestination(address.getDestination()));
    }


    public Optional<Address> getAddress(InstanceId instanceId, String address) {
        return instanceManager.get(instanceId).map(addressManager::getAddressSpace).map(AddressSpace::getDestinations).orElse(Collections.emptySet()).stream()
                .filter(d -> d.address().equals(address))
                .findAny()
                .map(Address::new);
    }

    public Address putAddress(InstanceId instance, Address address) throws Exception {
        appendAddress(instance, address);
        return address;
    }

    public AddressList deleteAddress(InstanceId instanceId, String address) {
        Optional<Instance> instance = instanceManager.get(instanceId);
        return AddressList.fromSet(instance.map(i -> addressManager.getAddressSpace(i).deleteDestination(address)).orElse(Collections.emptySet()));
    }

    public AddressList deleteAddressByUuid(InstanceId instanceId, String uuid) {
        Optional<Instance> instance = instanceManager.get(instanceId);
        return AddressList.fromSet(instance.map(i -> addressManager.getAddressSpace(i).deleteDestinationWithUuid(uuid)).orElse(Collections.emptySet()));
    }

    public AddressList appendAddresses(InstanceId instanceId, AddressList list) throws Exception {
        Instance instance = getOrCreateInstance(instanceId);
        AddressSpace addressSpace = addressManager.getAddressSpace(instance);
        return AddressList.fromSet(addressSpace.addDestinations(list.getDestinations()));
    }

}
