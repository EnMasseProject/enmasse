package enmasse.address.controller.api;

import enmasse.address.controller.admin.InstanceManager;
import enmasse.address.controller.model.Instance;
import enmasse.address.controller.model.InstanceId;

import java.util.*;

public class TestInstanceManager implements InstanceManager {
    Map<InstanceId, Instance> instances = new HashMap<>();
    public boolean throwException = false;

    @Override
    public Optional<Instance> get(InstanceId instanceId) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return Optional.ofNullable(instances.get(instanceId));
    }

    @Override
    public Optional<Instance> get(String instanceId) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return get(InstanceId.withId(instanceId));
    }

    @Override
    public void create(Instance instance) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        instances.put(instance.id(), instance);
    }

    @Override
    public void delete(Instance instance) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        instances.remove(instance.id());
    }

    @Override
    public Set<Instance> list() {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return new LinkedHashSet<>(instances.values());
    }
}
