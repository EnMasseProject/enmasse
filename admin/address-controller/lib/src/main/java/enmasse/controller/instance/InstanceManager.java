package enmasse.controller.instance;

import enmasse.controller.model.Instance;
import enmasse.controller.model.AddressSpaceId;

import java.util.Set;

/**
 * Factory for instances
 */
public interface InstanceManager {
    void create(Instance instance);
    boolean isReady(Instance instance);
    void retainInstances(Set<AddressSpaceId> desiredInstances);
}
