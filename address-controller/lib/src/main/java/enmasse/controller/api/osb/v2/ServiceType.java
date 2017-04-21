package enmasse.controller.api.osb.v2;

import java.util.Optional;
import java.util.UUID;

public enum ServiceType {
    ANYCAST("ac6348d6-eeea-43e5-9b97-5ed18da5dcaf", false, false, null, "914e9acc-242e-42e3-8995-4ec90d928c2b"),
    MULTICAST("7739ea7d-8de4-4fe8-8297-90f703904587", false, true, null, "6373d6b9-b701-4636-a5ff-dc5b835c9223"),
    QUEUE("7739ea7d-8de4-4fe8-8297-90f703904589", true, false, "queue"),
    TOPIC("7739ea7d-8de4-4fe8-8297-90f703904590", true, true, "topic");

    private UUID uuid;
    private boolean storeAndForward;
    private boolean multicast;
    private String flavorType;
    private UUID defaultPlanUuid;

    ServiceType(String uuid, boolean storeAndForward, boolean multicast, String flavorType) {
        this(uuid, storeAndForward, multicast, flavorType, null);
    }

    ServiceType(String uuid, boolean storeAndForward, boolean multicast, String flavorType, String defaultPlanUuid) {
        this.uuid = UUID.fromString(uuid);
        this.storeAndForward = storeAndForward;
        this.multicast = multicast;
        this.flavorType = flavorType;
        if (defaultPlanUuid != null) {
            this.defaultPlanUuid = UUID.fromString(defaultPlanUuid);
        }
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

    public boolean storeAndForward() {
        return storeAndForward;
    }

    public boolean multicast() {
        return multicast;
    }

    public Optional<String> flavorType() {
        return Optional.ofNullable(flavorType);
    }

    public UUID defaultPlanUuid() {
        return defaultPlanUuid;
    }

    public boolean supportsOnlyDefaultPlan() {
        return defaultPlanUuid() != null;
    }
}
