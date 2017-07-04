package enmasse.controller.api.v1;

import enmasse.controller.address.api.AddressApi;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Instance;
import enmasse.controller.model.AddressSpaceId;
import io.enmasse.address.model.Address;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
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

    public Set<Address> getAddresses(AddressSpaceId addressSpaceId) throws IOException {
        return instanceApi.withInstance(addressSpaceId).listAddresses();
    }

    public Set<Address> putAddresses(AddressSpaceId addressSpaceId, Set<Address> addressList) throws Exception {
        Instance instance = getOrCreateInstance(addressSpaceId);
        AddressApi addressApi = instanceApi.withInstance(instance.id());

        Set<Address> toRemove = addressApi.listAddresses();
        toRemove.removeAll(addressList);
        toRemove.forEach(addressApi::deleteAddress);
        addressList.forEach(addressApi::createAddress);
        return addressList;
    }

    private Instance getOrCreateInstance(AddressSpaceId addressSpaceId) throws Exception {
        Optional<Instance> instance = instanceApi.getInstanceWithId(addressSpaceId);
        if (!instance.isPresent()) {
            Instance i = new Instance.Builder(addressSpaceId).build();
            instanceApi.createInstance(i);
            return i;
        } else {
            return instance.get();
        }
    }

    public Set<Address> appendAddress(AddressSpaceId addressSpaceId, Address address) throws Exception {
        getOrCreateInstance(addressSpaceId);
        AddressApi addressApi = instanceApi.withInstance(addressSpaceId);
        addressApi.createAddress(address);
        return addressApi.listAddresses();
    }


    public Optional<Address> getAddress(AddressSpaceId addressSpaceId, String address) {
        return instanceApi.withInstance(addressSpaceId).getAddressWithName(address);
    }

    public Address putAddress(AddressSpaceId instance, Address address) throws Exception {
        appendAddress(instance, address);
        return address;
    }

    public Set<Address> deleteAddress(AddressSpaceId addressSpaceId, String address) throws IOException {
        AddressApi addressApi = instanceApi.withInstance(addressSpaceId);
        addressApi.getAddressWithName(address).ifPresent(addressApi::deleteAddress);

        return addressApi.listAddresses();
    }

    public Set<Address> appendAddresses(AddressSpaceId addressSpaceId, Set<Address> addressSet) throws Exception {
        getOrCreateInstance(addressSpaceId);
        AddressApi addressApi = instanceApi.withInstance(addressSpaceId);
        for (Address address : addressSet) {
            addressApi.createAddress(address);
        }
        return addressApi.listAddresses();
    }

}
