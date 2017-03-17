package enmasse.address.controller.admin;

import enmasse.address.controller.model.Instance;
import enmasse.address.controller.model.InstanceId;

import java.util.Optional;
import java.util.Set;

/**
 * Manages instances of EnMasse
 */
public interface InstanceManager {
    Optional<Instance> get(InstanceId instanceId);
    void create(Instance instance);
    void delete(Instance instance);
    Set<Instance> list();
}
