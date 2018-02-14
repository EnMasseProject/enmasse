/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;

import java.util.LinkedHashSet;
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
    public void replaceAddress(Address destination) {
        deleteAddress(destination); // necessary, because a simple set.add() doesn't replace the element
        createAddress(destination);
    }

    @Override
    public void deleteAddress(Address destination) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        addresses.remove(destination);
    }

    @Override
    public Watch watchAddresses(Watcher<Address> watcher) throws Exception {
        return null;
    }

    @Override
    public Watch watchAddresses(Watcher<Address> watcher, boolean useEventLoop) throws Exception {
        return null;
    }

    @Override
    public Optional<Address> getAddressWithName(String address) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return addresses.stream().filter(d -> d.getAddress().equals(address)).findAny();
    }

    @Override
    public Optional<Address> getAddressWithUuid(String uuid) {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return addresses.stream().filter(d -> d.getUuid().equals(uuid)).findAny();
    }

    @Override
    public Set<Address> listAddresses() {
        if (throwException) {
            throw new RuntimeException("exception");
        }
        return new LinkedHashSet<>(addresses);
    }

    public void setAllAddressesReady(boolean ready) {
        addresses.stream().forEach(d -> replaceAddress(new Address.Builder(d).setStatus(new Status(ready)).build()));
    }
}
