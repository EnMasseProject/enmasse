package io.enmasse.controller.api.osb.v2;

import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.types.standard.StandardType;

import java.util.Optional;
import java.util.UUID;

public enum ServiceType {
    // TODO: These are tied to the 'standard' address space
    ANYCAST("ac6348d6-eeea-43e5-9b97-5ed18da5dcaf", "enmasse-anycast", StandardType.ANYCAST),
    MULTICAST("7739ea7d-8de4-4fe8-8297-90f703904587", "enmasse-multicast", StandardType.MULTICAST),
    QUEUE("7739ea7d-8de4-4fe8-8297-90f703904589", "enmasse-queue", StandardType.QUEUE),
    TOPIC("7739ea7d-8de4-4fe8-8297-90f703904590", "enmasse-topic", StandardType.TOPIC);

    private UUID uuid;
    private String serviceName;
    private AddressType addressType;

    ServiceType(String uuid, String serviceName, AddressType addressType) {
        this.uuid = UUID.fromString(uuid);
        this.serviceName = serviceName;
        this.addressType = addressType;
    }

    public static Optional<ServiceType> valueOf(UUID uuid) {
        for (ServiceType serviceType : values()) {
            if (serviceType.uuid().equals(uuid)) {
                return Optional.of(serviceType);
            }
        }
        return Optional.empty();
    }

    public UUID uuid() {
        return uuid;
    }

    public String serviceName() {
        return serviceName;
    }

    public AddressType addressType() {
        return addressType;
    }
}
