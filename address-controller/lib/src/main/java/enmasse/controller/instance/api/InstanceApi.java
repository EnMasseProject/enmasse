package enmasse.controller.instance.api;

import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
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
    Optional<Instance> getInstanceWithId(InstanceId instanceId);
    Optional<Instance> getInstanceWithUuid(String uuid);
    void createInstance(Instance instance);
    void replaceInstance(Instance instance);
    void deleteInstance(Instance instance);
    Set<Instance> listInstances();
    Instance getInstanceFromConfig(ConfigMap resource);

    DestinationApi withInstance(InstanceId id);
}
