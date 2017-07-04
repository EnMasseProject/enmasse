package enmasse.controller.api;

import enmasse.controller.address.api.AddressApi;
import enmasse.controller.common.Watch;
import enmasse.controller.common.Watcher;
import enmasse.controller.instance.api.InstanceApi;
import enmasse.controller.model.AddressSpaceId;
import enmasse.controller.model.Instance;
import io.enmasse.address.model.Address;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.*;
import java.util.stream.Collectors;

public class TestInstanceApi implements InstanceApi {
    Map<AddressSpaceId, Instance> instances = new HashMap<>();
    Map<AddressSpaceId, TestAddressApi> destinationApiMap = new LinkedHashMap<>();
    public boolean throwException = false;

    @Override
    public Optional<Instance> getInstanceWithId(AddressSpaceId addressSpaceId) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        return Optional.ofNullable(instances.get(addressSpaceId));
    }

    @Override
    public void createInstance(Instance instance) {
        if (throwException) {
            throw new RuntimeException("foo");
        }
        instances.put(instance.id(), instance);
    }

    @Override
    public void replaceInstance(Instance instance) {
        createInstance(instance);
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
    public Watch watchInstances(Watcher<Instance> watcher) throws Exception {
        return null;
    }

    @Override
    public AddressApi withInstance(AddressSpaceId id) {
        if (!destinationApiMap.containsKey(id)) {
            destinationApiMap.put(id, new TestAddressApi());
        }
        return getAddressApi(id);
    }

    public TestAddressApi getAddressApi(AddressSpaceId id) {
        return destinationApiMap.get(id);
    }

    public Set<Address> getAddresses() {
        return getAddressApis().stream()
                .flatMap(d -> d.listAddresses().stream())
                .collect(Collectors.toSet());
    }

    public Set<String> getAddressUuids() {
        return getAddresses().stream().map(d -> d.getUuid()).collect(Collectors.toSet());
    }

    public Collection<TestAddressApi> getAddressApis() {
        return destinationApiMap.values();
    }

    public void setAllInstancesReady(boolean ready) {
        instances.entrySet().stream().forEach(entry -> instances.put(
                entry.getKey(),
                new Instance.Builder(entry.getValue()).status(new Instance.Status(ready)).build()));
    }
}
