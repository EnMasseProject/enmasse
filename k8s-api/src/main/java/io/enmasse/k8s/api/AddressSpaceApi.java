/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpace;
import io.enmasse.address.model.AddressSpaceList;
import io.enmasse.k8s.api.cache.CacheWatcher;
import io.fabric8.kubernetes.client.Watcher;

import java.io.Closeable;
import java.util.Map;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * API for managing address spaces.
 */
public interface AddressSpaceApi {
    Optional<AddressSpace> getAddressSpaceWithName(String namespace, String id);
    void createAddressSpace(AddressSpace addressSpace) throws Exception;

    boolean replaceAddressSpace(AddressSpace addressSpace) throws Exception;
    boolean deleteAddressSpace(AddressSpace addressSpace);
    AddressSpaceList listAddressSpaces(String namespace, Map<String, String> labels);
    Set<AddressSpace> listAddressSpaces(String namespace);
    Set<AddressSpace> listAddressSpacesWithLabels(String namespace, Map<String, String> labels);

    AddressSpaceList listAllAddressSpaces(Map<String, String> labels);
    Set<AddressSpace> listAllAddressSpaces();
    Set<AddressSpace> listAllAddressSpacesWithLabels(Map<String, String> labels);

    void deleteAddressSpaces(String namespace);

    Watch watchAddressSpaces(CacheWatcher<AddressSpace> watcher, Duration resyncInterval) throws Exception;

    Closeable watch(Watcher<AddressSpace> watcher, String namespace, String resourceVersion, Map<String, String> labels);

    AddressApi withAddressSpace(AddressSpace addressSpace);
}
