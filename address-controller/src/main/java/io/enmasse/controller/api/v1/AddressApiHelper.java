package io.enmasse.controller.api.v1;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.SecurityContext;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressList;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.controller.api.RbacSecurityContext;
import io.enmasse.controller.api.ResourceVerb;
import io.enmasse.controller.api.osb.v2.OSBExceptions;
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

    private void verifyAuthorized(SecurityContext securityContext, AddressSpace addressSpace, ResourceVerb verb) {
        if (!securityContext.isUserInRole(RbacSecurityContext.rbacToRole(addressSpace.getNamespace(), verb, addressSpace.getCreatedBy()))) {
            throw OSBExceptions.notAuthorizedException();
        }
    }

    public AddressList getAddresses(SecurityContext securityContext, String addressSpaceId) throws IOException {
        Optional<AddressSpace> addressSpace = addressSpaceApi.getAddressSpaceWithName(addressSpaceId);
        if (!addressSpace.isPresent()) {
            throw new NotFoundException("Address space with id " + addressSpaceId + " not found");
        }
        verifyAuthorized(securityContext, addressSpace.get(), ResourceVerb.list);
        return new AddressList(addressSpaceApi.withAddressSpace(addressSpace.get()).listAddresses());
    }

    public AddressList putAddresses(SecurityContext securityContext, String addressSpaceId, AddressList addressList) throws Exception {
        AddressSpace addressSpace = getAddressSpace(addressSpaceId);
        verifyAuthorized(securityContext, addressSpace, ResourceVerb.create);
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

    public Optional<Address> getAddress(SecurityContext securityContext, String addressSpaceId, String address) throws Exception {
        AddressSpace addressSpace = getAddressSpace(addressSpaceId);
        verifyAuthorized(securityContext, addressSpace, ResourceVerb.get);
        return addressSpaceApi.withAddressSpace(addressSpace).getAddressWithName(address);
    }

    public AddressList deleteAddress(SecurityContext securityContext, String addressSpaceId, String name) throws Exception {
        AddressSpace addressSpace = getAddressSpace(addressSpaceId);
        verifyAuthorized(securityContext, addressSpace, ResourceVerb.delete);
        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);
        addressApi.getAddressWithName(name).ifPresent(addressApi::deleteAddress);
        return new AddressList(addressApi.listAddresses());
    }

    public AddressList appendAddresses(SecurityContext securityContext, String addressSpaceId, AddressList addressList) throws Exception {
        AddressSpace addressSpace = getAddressSpace(addressSpaceId);
        verifyAuthorized(securityContext, addressSpace, ResourceVerb.create);
        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);
        for (Address address : addressList) {
            addressApi.createAddress(address);
        }
        return new AddressList(addressApi.listAddresses());
    }

}
