package enmasse.address.controller.api;

import enmasse.address.controller.admin.AddressManager;
import enmasse.address.controller.admin.AddressManagerFactory;
import enmasse.address.controller.model.TenantId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestAddressManagerFactory implements AddressManagerFactory {
    private final Map<TenantId, AddressManager> managerMap = new HashMap<>();

    public TestAddressManagerFactory addManager(TenantId tenant, AddressManager addressManager) {
        managerMap.put(tenant, addressManager);
        return this;
    }

    @Override
    public Optional<AddressManager> getAddressManager(TenantId tenant) {
        return Optional.ofNullable(managerMap.get(tenant));
    }

    @Override
    public AddressManager getOrCreateAddressManager(TenantId tenant) {
        if (!managerMap.containsKey(tenant)) {
            managerMap.put(tenant, new TestAddressManager());
        }
        return managerMap.get(tenant);
    }
}
