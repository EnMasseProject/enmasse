/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.Address;
import io.enmasse.k8s.api.cache.CacheWatcher;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * API for managing addresses in kubernetes.
 */
public interface AddressApi {
    Optional<Address> getAddressWithName(String namespace, String name);

    ContinuationResult<Address> listAddresses(String namespace, Integer limit, ContinuationResult<Address> continueValue, Map<String,String> labels);

    default Collection<Address> listAddresses(String namespace) {
        return listAddressesWithLabels(namespace, Collections.emptyMap());
    }

    default Collection<Address> listAddressesWithLabels(String namespace, Map<String, String> labels) {
        return listAddresses(namespace, null, null, labels).getItems();
    }

    void deleteAddresses(String namespace);

    void createAddress(Address address);
    boolean replaceAddress(Address address);
    default boolean replaceAddressStatus(Address address) throws Exception {
        return false;
    }
    boolean deleteAddress(Address address);

    Watch watchAddresses(CacheWatcher<Address> watcher, Duration resyncInterval) throws Exception;
}
