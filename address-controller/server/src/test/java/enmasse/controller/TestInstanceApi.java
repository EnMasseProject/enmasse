package enmasse.controller;

import enmasse.controller.address.api.DestinationApi;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.*;

public class TestInstanceApi implements InstanceApi {
    Map<InstanceId, Instance> instances = new HashMap<>();
    Map<InstanceId, DestinationApi> destinationApiMap = new LinkedHashMap<>();
    public boolean throwException = false;

    @Override
    public Optional<Instance> getInstanceWithId(InstanceId instanceId) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return Optional.ofNullable(instances.get(instanceId));
    }

    @Override
    public Optional<Instance> getInstanceWithUuid(String uuid) {
        for (Instance i : instances.values()) {
            if (i.uuid().filter(u -> uuid.equals(u)).isPresent()) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    @Override
    public void createInstance(Instance instance) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        instances.put(instance.id(), instance);
    }

    @Override
    public void deleteInstance(Instance instance) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        instances.remove(instance.id());
    }

    @Override
    public Set<Instance> listInstances() {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return new LinkedHashSet<>(instances.values());
    }

    @Override
    public Instance getInstanceFromConfig(ConfigMap resource) {
        return enmasse.controller.instance.v3.Instance.fromJson(resource.getData().get("config.json"));
    }

    @Override
    public DestinationApi withInstance(InstanceId id) {
        if (!destinationApiMap.containsKey(id)) {
            destinationApiMap.put(id, new TestDestinationApi());
        }
        return destinationApiMap.get(id);
    }
}
