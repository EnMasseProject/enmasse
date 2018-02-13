/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.controller.api.osb.v2;

import java.util.Optional;
import java.util.UUID;

public enum ServiceType {
    // TODO: These are tied to the 'standard' address space
    ANYCAST("ac6348d6-eeea-43e5-9b97-5ed18da5dcaf", "enmasse-anycast", "anycast"),
    MULTICAST("7739ea7d-8de4-4fe8-8297-90f703904587", "enmasse-multicast", "multicast"),
    QUEUE("7739ea7d-8de4-4fe8-8297-90f703904589", "enmasse-queue", "queue"),
    TOPIC("7739ea7d-8de4-4fe8-8297-90f703904590", "enmasse-topic", "topic");

    private UUID uuid;
    private String serviceName;
    private String addressType;

    ServiceType(String uuid, String serviceName, String addressType) {
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

    public String addressType() {
        return addressType;
    }
}
