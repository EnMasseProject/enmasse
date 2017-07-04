package enmasse.controller.instance.api;

import enmasse.controller.address.api.AddressApi;
import enmasse.controller.common.Watcher;
import enmasse.controller.common.Watch;
import enmasse.controller.model.AddressSpaceId;
import enmasse.controller.model.Instance;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.Optional;
import java.util.Set;

/**
 * API for managing instances.
 */
public interface InstanceApi {
    /**
     * Operations for instances.
     */
    Optional<Instance> getInstanceWithId(AddressSpaceId addressSpaceId);
    void createInstance(Instance instance);
    void replaceInstance(Instance instance);
    void deleteInstance(Instance instance);
    Set<Instance> listInstances();
    Instance getInstanceFromConfig(ConfigMap resource);

    Watch watchInstances(Watcher<Instance> watcher) throws Exception;

    AddressApi withInstance(AddressSpaceId id);
}
