/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.k8s.api.cache.CacheWatcher;
import io.fabric8.kubernetes.client.Watcher;

import static java.util.Optional.ofNullable;

import java.io.Closeable;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class TestAddressSpaceApi implements AddressSpaceApi {
    Map<String,Map<String, AddressSpace>> addressSpaces = new HashMap<>();
    Map<String,Map<String, TestAddressApi>> addressApiMap = new LinkedHashMap<>();
    public boolean throwException = false;

    @Override
    public Optional<AddressSpace> getAddressSpaceWithName(String namespace, String addressSpaceId) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return ofNullable(addressSpaces.get(namespace))
                .map(ns -> ns.get(addressSpaceId));
    }

    @Override
    public void createAddressSpace(AddressSpace addressSpace) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        addressSpaces
            .computeIfAbsent(addressSpace.getMetadata().getNamespace(), ns -> new HashMap<>())
            .put(addressSpace.getMetadata().getName(), addressSpace);
    }

    @Override
    public boolean replaceAddressSpace(AddressSpace addressSpace) {
        Map<String,AddressSpace> addressSpaces = this.addressSpaces.get(addressSpace.getMetadata().getNamespace());
        if ( addressSpaces == null ) {
            return false;
        }
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
        return ofNullable(addressSpaces.get(addressSpace.getMetadata().getNamespace()))
                .map(as -> as.remove(addressSpace.getMetadata().getName()).getMetadata() != null )
                .orElse(false);
    }

    @Override
    public Set<AddressSpace> listAddressSpaces(String namespace) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return ofNullable(addressSpaces.get(namespace))
                .map(as -> new LinkedHashSet<>(as.values()))
                .orElseGet(LinkedHashSet::new);
    }

    @Override
    public AddressSpaceList listAddressSpaces(String namespace, Map<String, String> labels) {
        return new AddressSpaceList(listAddressSpacesWithLabels(namespace, labels));
    }

    @Override
    public Set<AddressSpace> listAddressSpacesWithLabels(String namespace, Map<String, String> labels) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return ofNullable(addressSpaces.get(namespace))
                .map(as -> new LinkedHashSet<>(as.values()))
                .orElseGet(LinkedHashSet::new);
    }

    @Override
    public Set<AddressSpace> listAllAddressSpaces() {
        if (throwException) {
            throw new RuntimeException("foo");
        }

        return addressSpaces.values().stream()
                .flatMap(as -> as.values().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<AddressSpace> listAllAddressSpacesWithLabels(Map<String, String> labels) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return listAllAddressSpaces();
    }

    @Override
    public void deleteAddressSpaces(String namespace) {
        Map<String,AddressSpace> addressSpaces = this.addressSpaces.computeIfAbsent(namespace, x -> new HashMap<>());
        Map<String,TestAddressApi> addressApiMap = this.addressApiMap.computeIfAbsent(namespace,  x -> new HashMap<>());

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
        final String addressSpaceNamespace = addressSpace.getMetadata().getNamespace();
        final String addressSpaceName = addressSpace.getMetadata().getName();

        Map<String,TestAddressApi> addressApiMap = this.addressApiMap.computeIfAbsent(addressSpaceNamespace, x -> new HashMap<>() );
        Map<String,AddressSpace> addressSpaces = this.addressSpaces.computeIfAbsent(addressSpaceNamespace, x -> new HashMap<>() );

        if (!addressApiMap.containsKey(addressSpaceName)) {
            addressSpaces.put(addressSpaceName, addressSpace);
            addressApiMap.put(addressSpaceName, new TestAddressApi());
        }

        return addressApiMap.get(addressSpaceName);
    }

    @Override
    public AddressSpaceList listAllAddressSpaces(Map<String, String> labels) {
        return new AddressSpaceList(listAllAddressSpaces());
    }

    @Override
    public Closeable watch(Watcher<AddressSpace> watcher, String namespace, String resourceVersion, Map<String, String> labels) {
        throw new UnsupportedOperationException();
    }

    public Collection<TestAddressApi> getAddressApis() {
        return addressApiMap.values().stream()
                .flatMap(as -> as.values().stream())
                .collect(Collectors.toList());
    }

    public void setAllInstancesReady(boolean ready) {
        addressSpaces.values().stream()
                .flatMap(as -> as.values().stream())
                .forEach(as -> as.getStatus().setReady(ready));

    }

}
