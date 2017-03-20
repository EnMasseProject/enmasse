package enmasse.controller.api;

import enmasse.controller.address.AddressManager;
import enmasse.controller.address.AddressManagerFactory;
import enmasse.controller.model.InstanceId;

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
