package io.enmasse.controller.api.v1;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a handler for doing operations on the addressing manager that works independent of AMQP and HTTP.
 */
public class AddressApiHelper {
    private static final Logger log = LoggerFactory.getLogger(AddressApiHelper.class.getName());
    private final AddressSpaceApi addressSpaceApi;

    public AddressApiHelper(AddressSpaceApi addressSpaceApi) {
        this.addressSpaceApi = addressSpaceApi;
    }

    public AddressList getAddresses(String addressSpaceId) throws IOException {
        Optional<AddressSpace> addressSpace = addressSpaceApi.getAddressSpaceWithName(addressSpaceId);
        if (!addressSpace.isPresent()) {
            throw new NotFoundException("Address space with id " + addressSpaceId + " not found");
        }
        return new AddressList(addressSpaceApi.withAddressSpace(addressSpace.get()).listAddresses());
    }

    public AddressList putAddresses(String addressSpaceId, AddressList addressList) throws Exception {
        AddressSpace addressSpace = getAddressSpace(addressSpaceId);
        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);

        Set<Address> toRemove = new HashSet<>(addressApi.listAddresses());
        toRemove.removeAll(addressList);
        toRemove.forEach(addressApi::deleteAddress);
        addressList.forEach(addressApi::createAddress);
        return new AddressList(addressApi.listAddresses());
    }

    private AddressSpace getAddressSpace(String addressSpaceId) throws Exception {
        // TODO: Make our own exception for this API
        return addressSpaceApi.getAddressSpaceWithName(addressSpaceId)
                .orElseThrow(() -> new NotFoundException("Address space " + addressSpaceId + " not found"));
    }

    public AddressList appendAddress(String addressSpaceId, Address address) throws Exception {
        AddressSpace addressSpace = getAddressSpace(addressSpaceId);
        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);
        addressApi.createAddress(address);
        return new AddressList(addressApi.listAddresses());
    }


    public Optional<Address> getAddress(String addressSpaceId, String address) {
        return addressSpaceApi.getAddressSpaceWithName(addressSpaceId).flatMap(s -> addressSpaceApi.withAddressSpace(s).getAddressWithName(address));
    }

    public Address putAddress(String instance, Address address) throws Exception {
        appendAddress(instance, address);
        return address;
    }

    public AddressList deleteAddress(String addressSpaceId, String name) throws IOException {
        return addressSpaceApi.getAddressSpaceWithName(addressSpaceId)
                .map(addressSpace -> {
                    AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);
                    addressApi.getAddressWithName(name).ifPresent(addressApi::deleteAddress);
                    return new AddressList(addressApi.listAddresses());
                }).orElse(new AddressList());
    }

    public AddressList appendAddresses(String addressSpaceId, AddressList addressList) throws Exception {
        AddressSpace addressSpace = getAddressSpace(addressSpaceId);
        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);
        for (Address address : addressList) {
            addressApi.createAddress(address);
        }
        return new AddressList(addressApi.listAddresses());
    }

}
