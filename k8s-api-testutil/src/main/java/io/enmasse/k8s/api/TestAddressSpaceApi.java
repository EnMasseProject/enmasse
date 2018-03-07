/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.Status;
import io.enmasse.k8s.api.cache.Store;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class TestAddressSpaceApi implements AddressSpaceApi {
    Map<String, AddressSpace> addressSpaces = new HashMap<>();
    Map<String, TestAddressApi> addressApiMap = new LinkedHashMap<>();
    public boolean throwException = false;

    @Override
    public Optional<AddressSpace> getAddressSpaceWithName(String addressSpaceId) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return Optional.ofNullable(addressSpaces.get(addressSpaceId));
    }

    @Override
    public void createAddressSpace(AddressSpace addressSpace) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        addressSpaces.put(addressSpace.getName(), addressSpace);
    }

    @Override
    public void replaceAddressSpace(AddressSpace addressSpace) {
        createAddressSpace(addressSpace);
    }

    @Override
    public void deleteAddressSpace(AddressSpace addressSpace) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        addressSpaces.remove(addressSpace.getName());
    }

    @Override
    public Set<AddressSpace> listAddressSpaces() {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return new LinkedHashSet<>(addressSpaces.values());
    }

    @Override
    public Watch watchAddressSpaces(Watcher<AddressSpace> watcher, Duration resyncInterval) throws Exception {
        return null;
    }

    @Override
    public AddressApi withAddressSpace(AddressSpace addressSpace) {
        if (!addressApiMap.containsKey(addressSpace.getName())) {
            addressSpaces.put(addressSpace.getName(), addressSpace);
            addressApiMap.put(addressSpace.getName(), new TestAddressApi());
        }
        return getAddressApi(addressSpace.getName());
    }

    public TestAddressApi getAddressApi(String id) {
        return addressApiMap.get(id);
    }

    public Set<Address> getAddresses() {
        return getAddressApis().stream()
                .flatMap(d -> d.listAddresses().stream())
                .collect(Collectors.toSet());
    }

    public Set<String> getAddressUuids() {
        return getAddresses().stream().map(d -> d.getUuid()).collect(Collectors.toSet());
    }

    public Collection<TestAddressApi> getAddressApis() {
        return addressApiMap.values();
    }

    public void setAllInstancesReady(boolean ready) {
        addressSpaces.entrySet().stream().forEach(entry -> addressSpaces.put(
                entry.getKey(),
                new AddressSpace.Builder(entry.getValue()).setStatus(new Status(ready)).build()));
    }
}
