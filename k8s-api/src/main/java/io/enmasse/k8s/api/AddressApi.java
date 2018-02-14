/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;

import java.util.Optional;
import java.util.Set;

/**
 * API for managing addresses in kubernetes.
 */
public interface AddressApi {
    Optional<Address> getAddressWithName(String name);
    Optional<Address> getAddressWithUuid(String uuid);
    Set<Address> listAddresses();

    void createAddress(Address address);
    void replaceAddress(Address address);
    void deleteAddress(Address address);

    Watch watchAddresses(Watcher<Address> watcher) throws Exception;
    Watch watchAddresses(Watcher<Address> watcher, boolean useEventLoop) throws Exception;
}
