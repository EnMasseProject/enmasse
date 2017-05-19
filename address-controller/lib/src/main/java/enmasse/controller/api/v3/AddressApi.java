package enmasse.controller.api.v3;

import enmasse.controller.address.v3.Address;
import enmasse.controller.address.v3.AddressList;
import enmasse.controller.common.Kubernetes;
import enmasse.controller.model.Destination;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * This is a handler for doing operations on the addressing manager that works independent of AMQP and HTTP.
 */
public class AddressApi {
    private final Kubernetes kubernetes;

    public AddressApi(Kubernetes kubernetes) {
        this.kubernetes = kubernetes;
    }

    public AddressList getAddresses(InstanceId instanceId) throws IOException {
        return AddressList.fromSet(
                kubernetes.withInstance(instanceId)
                .listDestinations());
    }

    public AddressList putAddresses(InstanceId instanceId, AddressList addressList) throws Exception {
        Instance instance = getOrCreateInstance(instanceId);
        Kubernetes instanceKube = kubernetes.withInstance(instance.id());
        Set<Destination> toRemove = instanceKube.listDestinations();
        toRemove.removeAll(addressList.getDestinations());

        toRemove.forEach(instanceKube::deleteDestination);
        addressList.getDestinations().forEach(instanceKube::createDestination);
        return addressList;
    }

    private Instance getOrCreateInstance(InstanceId instanceId) throws Exception {
        Optional<Instance> instance = kubernetes.getInstanceWithId(instanceId);
        if (!instance.isPresent()) {
            Instance i = new Instance.Builder(instanceId).build();
            kubernetes.createInstance(i);
            return i;
        } else {
            return instance.get();
        }
    }

    public AddressList appendAddress(InstanceId instanceId, Address address) throws Exception {
        getOrCreateInstance(instanceId);
        kubernetes.withInstance(instanceId).createDestination(address.getDestination());
        return AddressList.fromSet(kubernetes.withInstance(instanceId).listDestinations());
    }


    public Optional<Address> getAddress(InstanceId instanceId, String address) {
        return kubernetes.withInstance(instanceId).getDestinationWithAddress(address).map(Address::new);
    }

    public Address putAddress(InstanceId instance, Address address) throws Exception {
        appendAddress(instance, address);
        return address;
    }

    public AddressList deleteAddress(InstanceId instanceId, String address) throws IOException {
        Kubernetes instanceKube = kubernetes.withInstance(instanceId);
        instanceKube.getDestinationWithAddress(address).ifPresent(instanceKube::deleteDestination);

        return AddressList.fromSet(instanceKube.listDestinations());
    }

    public AddressList deleteAddressByUuid(InstanceId instanceId, String uuid) {
        Kubernetes instanceKube = kubernetes.withInstance(instanceId);
        instanceKube.getDestinationWithUuid(uuid).ifPresent(instanceKube::deleteDestination);

        return AddressList.fromSet(instanceKube.listDestinations());
    }

    public AddressList appendAddresses(InstanceId instanceId, AddressList list) throws Exception {
        Instance instance = getOrCreateInstance(instanceId);
        Kubernetes instanceKube = kubernetes.withInstance(instance.id());
        for (Destination destination : list.getDestinations()) {
            instanceKube.createDestination(destination);
        }
        return AddressList.fromSet(instanceKube.listDestinations());
    }

}
