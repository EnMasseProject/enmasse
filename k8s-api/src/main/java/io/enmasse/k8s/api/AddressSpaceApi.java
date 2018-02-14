/*
 * Copyright 2017-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api;

import io.enmasse.address.model.AddressSpace;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.Optional;
import java.util.Set;

/**
 * API for managing address spaces.
 */
public interface AddressSpaceApi {
    Optional<AddressSpace> getAddressSpaceWithName(String id);
    void createAddressSpace(AddressSpace addressSpace) throws Exception;
    void replaceAddressSpace(AddressSpace addressSpace) throws Exception;
    void deleteAddressSpace(AddressSpace addressSpace);
    Set<AddressSpace> listAddressSpaces();

    Watch watchAddressSpaces(Watcher<AddressSpace> watcher) throws Exception;

    AddressApi withAddressSpace(AddressSpace addressSpace);
}
