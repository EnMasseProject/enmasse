/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.api.v1;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import io.enmasse.address.model.*;
import io.enmasse.k8s.api.SchemaProvider;
import io.enmasse.k8s.api.AddressApi;
import io.enmasse.k8s.api.AddressSpaceApi;

/**
 * This is a handler for doing operations on the addressing manager that works independent of AMQP and HTTP.
 */
public class AddressApiHelper {
    private final AddressSpaceApi addressSpaceApi;
    private final SchemaProvider schemaProvider;

    public AddressApiHelper(AddressSpaceApi addressSpaceApi, SchemaProvider schemaProvider) {
        this.addressSpaceApi = addressSpaceApi;
        this.schemaProvider = schemaProvider;
    }

    protected AddressList queryAddresses(Collection<AddressSpace> addressSpaces, BiFunction<String, AddressApi, Collection<Address>> lister) throws Exception {
        final AddressList list = new AddressList();

        for (final AddressSpace addressSpace : addressSpaces) {
            list.addAll(lister.apply(addressSpace.getNamespace(), addressSpaceApi.withAddressSpace(addressSpace)));
        }

        return list;
    }

    protected Collection<AddressSpace> getAddressSpaces(String namespace, String addressSpaceId) throws Exception {
        if (addressSpaceId == null) {
            if (namespace == null || namespace.isEmpty()) {
                return addressSpaceApi.listAllAddressSpaces();
            } else {
                return addressSpaceApi.listAddressSpaces(namespace);
            }
        } else {
            return Collections.singleton(getAddressSpace(namespace, addressSpaceId));
        }
    }

    public AddressList getAddresses(String namespace, String addressSpaceId) throws Exception {
        return queryAddresses(getAddressSpaces(namespace, addressSpaceId), (ns, api) -> api.listAddresses(ns));
    }

    public AddressList getAddressesWithLabels(String namespace, String addressSpaceId, Map<String, String> labels) throws Exception {
        return queryAddresses(getAddressSpaces(namespace, addressSpaceId), (ns, api) -> api.listAddressesWithLabels(ns, labels));
    }

    public AddressList getAllAddresses() throws Exception {
        return queryAddresses(addressSpaceApi.listAllAddressSpaces(), (ns, api) -> api.listAddresses(ns));
    }

    public AddressList getAllAddressesWithLabels(final Map<String, String> labels) throws Exception {
        return queryAddresses(addressSpaceApi.listAllAddressSpaces(), (ns, api) -> api.listAddressesWithLabels(ns, labels));
    }

    private void validateAddress(AddressSpace addressSpace, Address address) {
        AddressResolver addressResolver = getAddressResolver(addressSpace);
        /*
          Brokered address space has no operator that manipulates address phase and readiness, so we need to perform validation at API server.
          For the standard address space, the validation is done in AddressController#onUpdate in order to avoid slowing down the request.
         */
        if (addressSpace.getType().equals("brokered")) {
            Set<Address> existingAddresses = addressSpaceApi.withAddressSpace(addressSpace).listAddresses(address.getNamespace());
            addressResolver.validate(address);
            for (Address existing : existingAddresses) {
                if (address.getAddress().equals(existing.getAddress()) && !address.getName().equals(existing.getName())) {
                    throw new BadRequestException("Address '" + address.getAddress() + "' already exists with resource name '" + existing.getName() + "'");
                }
            }
        }
    }

    private AddressResolver getAddressResolver(AddressSpace addressSpace) {
        Schema schema = schemaProvider.getSchema();
        AddressSpaceType type = schema.findAddressSpaceType(addressSpace.getType()).orElseThrow(() -> new UnresolvedAddressSpaceException("Unable to resolve address space type " + addressSpace.getType()));

        return new AddressResolver(type);
    }

    private AddressSpace getAddressSpace(String namespace, String addressSpaceId) throws Exception {
        return addressSpaceApi.getAddressSpaceWithName(namespace, addressSpaceId)
                .orElseThrow(() -> new NotFoundException("Address space " + addressSpaceId + " not found"));
    }

    public Optional<Address> getAddress(String namespace, String addressSpaceId, String address) throws Exception {
        AddressSpace addressSpace = getAddressSpace(namespace, addressSpaceId);
        return addressSpaceApi.withAddressSpace(addressSpace).getAddressWithName(namespace, address);
    }

    public Address deleteAddress(String namespace, String addressSpaceId, String name) throws Exception {
        AddressSpace addressSpace = getAddressSpace(namespace, addressSpaceId);
        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);

        Optional<Address> addressOptional = addressApi.getAddressWithName(namespace, name);
        return addressOptional.filter(addressApi::deleteAddress).orElse(null);
    }

    public Address createAddress(String addressSpaceId, Address address) throws Exception {
        AddressSpace addressSpace = getAddressSpace(address.getNamespace(), addressSpaceId);
        validateAddress(addressSpace, address);
        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);
        addressApi.createAddress(address);
        return address;
    }

    public void createAddresses(String addressSpaceId, Set<Address> address) throws Exception {

        List<String> allResourceNames = address.stream().map(Address::getName).collect(Collectors.toList());
        Set<String> resourceNameSet = new HashSet<>(allResourceNames);
        if (resourceNameSet.size() != allResourceNames.size()) {
            List<String> duplicates = new ArrayList<>(allResourceNames);
            resourceNameSet.forEach(duplicates::remove);  // removes first
            throw new BadRequestException("Address resource names must be unique. Duplicate resource names: " + duplicates);
        }

        List<Address> sorted = address.stream()
                .sorted(Comparator.comparing(Address::getNamespace))
                .collect(Collectors.toList());

        Map<Address, AddressApi> apiMap = new HashMap<>();
        AddressSpace addressSpace = null;
        AddressApi addressApi = null;
        AddressResolver addressResolver = null;
        Set<Address> existingAddresses = null;
        for(Address a : sorted) {
            if (addressSpace == null || !Objects.equals(addressSpace.getNamespace(), a.getNamespace())) {
                addressSpace = getAddressSpace(a.getNamespace(), addressSpaceId);
                addressApi = addressSpaceApi.withAddressSpace(addressSpace);
                addressResolver = getAddressResolver(addressSpace);
                existingAddresses = addressApi.listAddresses(a.getNamespace());
            }

            addressResolver.validate(a);
            for (Address existing : existingAddresses) {
                if (a.getAddress().equals(existing.getAddress()) && !a.getName().equals(existing.getName())) {
                    throw new BadRequestException("Address '" + a.getAddress() + "' already exists with resource name '" + existing.getName() + "'");
                }
            }
            apiMap.put(a, addressApi);
        }

        apiMap.forEach((addr, api) -> api.createAddress(addr));
    }

    public Address replaceAddress(String addressSpaceId, Address address) throws Exception {
        AddressSpace addressSpace = getAddressSpace(address.getNamespace(), addressSpaceId);
        validateAddress(addressSpace, address);
        AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);
        if (!addressApi.replaceAddress(address)) {
            throw new NotFoundException("Address " + address.getName() + " not found");
        }
        return address;
    }

    public static Map<String,String> parseLabelSelector(String labelSelector) {
        Map<String, String> labels = new HashMap<>();
        String [] pairs = labelSelector.split(",");
        for (String pair : pairs) {
            String elements[] = pair.split("=");
            if (elements.length > 1) {
                labels.put(elements[0], elements[1]);
            }
        }
        return labels;
    }

    public void deleteAddresses(String namespace) {
        for (AddressSpace addressSpace : addressSpaceApi.listAddressSpaces(namespace)) {
            AddressApi addressApi = addressSpaceApi.withAddressSpace(addressSpace);
            addressApi.deleteAddresses(namespace);
        }
    }
}
