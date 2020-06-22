/*
 * Copyright 2019-2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.iot.model.v1;

import com.fasterxml.jackson.annotation.JsonValue;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum ConfigConditionType {

    READY("Ready"),
    DEGRADED("Degraded"),

    RECONCILED("Reconciled"),

    COMMAND_MESH_READY("CommandMeshReady"),
    AUTH_SERVICE_READY("AuthServiceReady"),
    TENANT_SERVICE_READY("TenantServiceReady"),
    DEVICE_CONNECTION_SERVICE_READY("DeviceConnectionServiceReady"),
    DEVICE_REGISTRY_ADAPTER_SERVICE_READY("DeviceRegistryAdapterServiceReady"),
    DEVICE_REGISTRY_MANAGEMENT_SERVICE_READY("DeviceRegistryManagementServiceReady"),

    HTTP_ADAPTER_READY("HttpAdapterReady"),
    LORAWAN_ADAPTER_READY("LorawanAdapterReady"),
    MQTT_ADAPTER_READY("MqttAdapterReady"),
    SIGFOX_ADAPTER_READY("SigfoxAdapterReady"),
    ;

    @JsonValue
    final private String value;

    private ConfigConditionType(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
