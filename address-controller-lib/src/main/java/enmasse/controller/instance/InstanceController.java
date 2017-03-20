package enmasse.controller.instance;

import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.util.Optional;
import java.util.Set;

/**
 * Manages instances of EnMasse
 */
public interface InstanceController {
    Optional<Instance> get(InstanceId instanceId);
    void create(Instance instance);
    void delete(Instance instance);
    Set<Instance> list();
}
