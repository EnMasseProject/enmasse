/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;
import io.enmasse.k8s.api.cache.CacheWatcher;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TestAddressApi implements AddressApi {
    public boolean throwException = false;

    private final Set<Address> addresses = new LinkedHashSet<>();

    @Override
    public void createAddress(Address destination) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        addresses.add(destination);
    }

    @Override
    public boolean replaceAddress(Address destination) {
        if (addresses.stream().noneMatch(d -> d.getName().equals(destination.getName()))) {
            return false;
        }
        deleteAddress(destination); // necessary, because a simple set.add() doesn't replace the element
        createAddress(destination);
        return true;
    }

    @Override
    public boolean deleteAddress(Address destination) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return addresses.remove(destination);
    }

    @Override
    public Watch watchAddresses(CacheWatcher<Address> watcher, Duration resyncInterval) throws Exception {
        return null;
    }

    @Override
    public Optional<Address> getAddressWithName(String namespace, String address) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return addresses.stream().filter(d -> d.getName().equals(address)).findAny();
    }

    @Override
    public Set<Address> listAddresses(String namespace) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        Set<Address> listed = new LinkedHashSet<>();
        for (Address address : addresses) {
            if (namespace.equals(address.getNamespace())) {
                listed.add(address);
            }
        }
        return listed;
    }

    @Override
    public Set<Address> listAddressesWithLabels(String namespace, Map<String, String> labels) {
        return listAddresses(namespace);
    }

    @Override
    public void deleteAddresses(String namespace) {
        addresses.removeIf(address -> namespace.equals(address.getNamespace()));
    }

    public void setAllAddressesReady(boolean ready) {
        addresses.stream().forEach(d -> replaceAddress(new Address.Builder(d).setStatus(new Status(ready)).build()));
    }
}
