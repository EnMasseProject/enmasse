package enmasse.controller;

import enmasse.controller.instance.InstanceController;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.util.*;

public class TestInstanceController implements InstanceController {
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
