package io.enmasse.controller.k8s.api;

import io.enmasse.controller.common.Watcher;
import io.enmasse.controller.common.Watch;
import io.enmasse.address.model.AddressSpace;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.Optional;
import java.util.Set;

/**
 * API for managing address spaces.
 */
public interface AddressSpaceApi {
    Optional<AddressSpace> getAddressSpaceWithName(String id);
    void createAddressSpace(AddressSpace addressSpace);
    void replaceAddressSpace(AddressSpace addressSpace);
    void deleteAddressSpace(AddressSpace addressSpace);
    Set<AddressSpace> listAddressSpaces();
    AddressSpace getAddressSpaceFromConfig(ConfigMap resource);

    Watch watchAddressSpaces(Watcher<AddressSpace> watcher) throws Exception;

    AddressApi withAddressSpace(AddressSpace addressSpace);
}
