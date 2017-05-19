package enmasse.controller.api;

import enmasse.controller.model.Instance;
import enmasse.controller.model.InstanceId;

import java.util.HashMap;
import java.util.Map;

public class TestAddressManager implements AddressManager {
    public boolean throwException = false;
    private final Map<InstanceId, AddressSpace> managerMap = new HashMap<>();

    public TestAddressManager addManager(InstanceId instance, AddressSpace addressSpace) {
        managerMap.put(instance, addressSpace);
        return this;
    }

    @Override
    public AddressSpace getAddressSpace(Instance instance) {
        if (throwException) {
            throw new RuntimeException("buhu");
        }
        return managerMap.get(instance.id());
    }
}
