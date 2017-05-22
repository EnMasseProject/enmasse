package enmasse.controller.instance;

import enmasse.controller.model.Instance;

/**
 * Factory for instances
 */
public interface InstanceManager {
    void create(Instance instance);
    boolean isReady(Instance instance);
    void delete(Instance instance);
}
