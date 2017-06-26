package enmasse.controller.api.v3;

import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.address.v3.Address;
import enmasse.controller.address.v3.AddressList;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * This is a handler for doing operations on the addressing manager that works independent of AMQP and HTTP.
 */
public class AddressApiHelper {
    private final InstanceApi instanceApi;

    public AddressApiHelper(InstanceApi instanceApi) {
        this.instanceApi = instanceApi;
    }

    public AddressList getAddresses(InstanceId instanceId) throws IOException {
        return AddressList.fromSet(
                instanceApi.withInstance(instanceId)
                .listDestinations());
    }

    public AddressList putAddresses(InstanceId instanceId, AddressList addressList) throws Exception {
        Instance instance = getOrCreateInstance(instanceId);
        DestinationApi destinationApi = instanceApi.withInstance(instance.id());

        Set<Destination> toRemove = destinationApi.listDestinations();
        toRemove.removeAll(addressList.getDestinations());

        toRemove.forEach(destinationApi::deleteDestination);
        addressList.getDestinations().forEach(destinationApi::createDestination);
        return addressList;
    }

    private Instance getOrCreateInstance(InstanceId instanceId) throws Exception {
        Optional<Instance> instance = instanceApi.getInstanceWithId(instanceId);
        if (!instance.isPresent()) {
            Instance i = new Instance.Builder(instanceId).build();
            instanceApi.createInstance(i);
            return i;
        } else {
            return instance.get();
        }
    }

    public AddressList appendAddress(InstanceId instanceId, Address address) throws Exception {
        getOrCreateInstance(instanceId);
        DestinationApi destinationApi = instanceApi.withInstance(instanceId);
        destinationApi.createDestination(address.getDestination());
        return AddressList.fromSet(destinationApi.listDestinations());
    }


    public Optional<Address> getAddress(InstanceId instanceId, String address) {
        return instanceApi.withInstance(instanceId).getDestinationWithAddress(address).map(Address::new);
    }

    public Address putAddress(InstanceId instance, Address address) throws Exception {
        appendAddress(instance, address);
        return address;
    }

    public AddressList deleteAddress(InstanceId instanceId, String address) throws IOException {
        DestinationApi destinationApi = instanceApi.withInstance(instanceId);
        destinationApi.getDestinationWithAddress(address).ifPresent(destinationApi::deleteDestination);

        return AddressList.fromSet(destinationApi.listDestinations());
    }

    public AddressList appendAddresses(InstanceId instanceId, AddressList list) throws Exception {
        getOrCreateInstance(instanceId);
        DestinationApi destinationApi = instanceApi.withInstance(instanceId);
        for (Destination destination : list.getDestinations()) {
            destinationApi.createDestination(destination);
        }
        return AddressList.fromSet(destinationApi.listDestinations());
    }

}
