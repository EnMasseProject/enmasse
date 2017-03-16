package enmasse.address.controller;

import enmasse.address.controller.admin.InstanceManager;
import enmasse.address.controller.model.Instance;
import enmasse.address.controller.model.InstanceId;

import java.util.*;

public class TestInstanceManager implements InstanceManager {
    Map<InstanceId, Instance> instances = new HashMap<>();

    @Override
    public Optional<Instance> get(InstanceId instanceId) {
        return Optional.ofNullable(instances.get(instanceId));
    }

    @Override
    public Optional<Instance> get(String instanceId) {
        return get(InstanceId.withId(instanceId));
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
