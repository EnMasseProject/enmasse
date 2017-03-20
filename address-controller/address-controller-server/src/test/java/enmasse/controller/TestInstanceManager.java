package enmasse.controller;

import enmasse.controller.instance.InstanceManager;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.util.*;

public class TestInstanceManager implements InstanceManager {
    Map<InstanceId, Instance> instances = new HashMap<>();

    @Override
    public Optional<Instance> get(InstanceId instanceId) {
        return Optional.ofNullable(instances.get(instanceId));
    }

    @Override
    public void create(Instance instance) {
        instances.put(instance.id(), instance);
    }

    @Override
    public void delete(Instance instance) {
        instances.remove(instance.id());
    }

    @Override
    public Set<Instance> list() {
        return new LinkedHashSet<>(instances.values());
    }
}
