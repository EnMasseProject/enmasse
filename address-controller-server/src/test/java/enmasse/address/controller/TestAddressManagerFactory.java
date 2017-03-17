package enmasse.address.controller;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.admin.AddressManagerFactory;
import enmasse.address.controller.model.InstanceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestAddressManagerFactory implements AddressManagerFactory {
    private final Map<InstanceId, AddressManager> managerMap = new HashMap<>();

    public TestAddressManagerFactory addManager(InstanceId instance, AddressManager addressManager) {
        managerMap.put(instance, addressManager);
        return this;
    }

    @Override
    public Optional<AddressManager> getAddressManager(InstanceId instance) {
        return Optional.ofNullable(managerMap.get(instance));
    }

    @Override
    public AddressManager getOrCreateAddressManager(InstanceId instance) {
        if (!managerMap.containsKey(instance)) {
            managerMap.put(instance, new TestAddressManager());
        }
        return managerMap.get(instance);
    }
}
