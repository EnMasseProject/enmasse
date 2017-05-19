package enmasse.controller.instance;

import enmasse.controller.model.Instance;

/**
 * Factory for instances
 */
public interface InstanceFactory {
    void create(Instance instance) throws Exception;
    void delete(Instance instance) throws Exception;
}
