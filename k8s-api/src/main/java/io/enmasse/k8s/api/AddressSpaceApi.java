/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.k8s.api.cache.CacheWatcher;

/**
 * API for managing address spaces.
 */
public interface AddressSpaceApi {
    Optional<AddressSpace> getAddressSpaceWithName(String namespace, String id);
    void createAddressSpace(AddressSpace addressSpace) throws Exception;

    /**
     * Replace an existing address space instance with the newly provided one.
     * <br>
     * <strong>Note:</strong> Implementations of this interface must ensure
     * that further changes to the address space instance don't have an impact
     * on the internal state.
     *
     * @param addressSpace The address space to update.
     * @return {@code true} is the address space as found and updated. {@code false} if the address space could not be found.
     * @throws Exception In case of any error other than a non-existing instance.
     */
    boolean replaceAddressSpace(AddressSpace addressSpace) throws Exception;
    boolean deleteAddressSpace(AddressSpace addressSpace);
    Set<AddressSpace> listAddressSpaces(String namespace);
    Set<AddressSpace> listAddressSpacesWithLabels(String namespace, Map<String, String> labels);

    Set<AddressSpace> listAllAddressSpaces();
    Set<AddressSpace> listAllAddressSpacesWithLabels(Map<String, String> labels);

    void deleteAddressSpaces(String namespace);

    Watch watchAddressSpaces(CacheWatcher<AddressSpace> watcher, Duration resyncInterval) throws Exception;

    AddressApi withAddressSpace(AddressSpace addressSpace);
}
