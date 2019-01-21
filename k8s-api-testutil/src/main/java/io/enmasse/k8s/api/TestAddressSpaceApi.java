/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceBuilder;
import io.enmasse.k8s.api.cache.CacheWatcher;

import java.time.Duration;
import java.util.*;

public class TestAddressSpaceApi implements AddressSpaceApi {
    Map<String, AddressSpace> addressSpaces = new HashMap<>();
    Map<String, TestAddressApi> addressApiMap = new LinkedHashMap<>();
    public boolean throwException = false;

    @Override
    public Optional<AddressSpace> getAddressSpaceWithName(String namespace, String addressSpaceId) {
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
        addressSpaces.put(addressSpace.getMetadata().getName(), addressSpace);
    }

    @Override
    public boolean replaceAddressSpace(AddressSpace addressSpace) {
        if (!addressSpaces.containsKey(addressSpace.getMetadata().getName())) {
            return false;
        }
        createAddressSpace(addressSpace);
        return true;
    }

    @Override
    public boolean deleteAddressSpace(AddressSpace addressSpace) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return addressSpaces.remove(addressSpace.getMetadata().getName()) != null;
    }

    @Override
    public Set<AddressSpace> listAddressSpaces(String namespace) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return new LinkedHashSet<>(addressSpaces.values());
    }

    @Override
    public Set<AddressSpace> listAddressSpacesWithLabels(String namespace, Map<String, String> labels) {
        return null;
    }

    @Override
    public Set<AddressSpace> listAllAddressSpaces() {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return new LinkedHashSet<>(addressSpaces.values());
    }

    @Override
    public Set<AddressSpace> listAllAddressSpacesWithLabels(Map<String, String> labels) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return new LinkedHashSet<>(addressSpaces.values());
    }

    @Override
    public void deleteAddressSpaces(String namespace) {
        for (AddressSpace addressSpace : new HashSet<>(addressSpaces.values())) {
            if (namespace.equals(addressSpace.getMetadata().getNamespace())) {
                addressSpaces.remove(addressSpace.getMetadata().getName());
                addressApiMap.remove(addressSpace.getMetadata().getName());
            }
        }
    }

    @Override
    public Watch watchAddressSpaces(CacheWatcher<AddressSpace> watcher, Duration resyncInterval) throws Exception {
        return null;
    }

    @Override
    public AddressApi withAddressSpace(AddressSpace addressSpace) {
        final String addressSpaceName = addressSpace.getMetadata().getName();
        if (!addressApiMap.containsKey(addressSpaceName)) {
            addressSpaces.put(addressSpaceName, addressSpace);
            addressApiMap.put(addressSpaceName, new TestAddressApi());
        }
        return getAddressApi(addressSpaceName);
    }

    public TestAddressApi getAddressApi(String id) {
        return addressApiMap.get(id);
    }

    public Collection<TestAddressApi> getAddressApis() {
        return addressApiMap.values();
    }

    public void setAllInstancesReady(boolean ready) {
        addressSpaces.entrySet().stream().forEach(entry -> addressSpaces.put(
                entry.getKey(),
                new AddressSpaceBuilder(entry.getValue()).withNewStatus(ready).build()));
    }
}
