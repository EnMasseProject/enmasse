package enmasse.controller.instance;

import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.util.Optional;
import java.util.Set;

/**
 * Manages instances of EnMasse
 */
public interface InstanceManager {
    Optional<Instance> get(InstanceId instanceId);
    Optional<Instance> get(String uuid);
    void create(Instance instance) throws Exception;
    void delete(Instance instance);
    Set<Instance> list();
    boolean isReady(Instance instance) throws Exception;
}
