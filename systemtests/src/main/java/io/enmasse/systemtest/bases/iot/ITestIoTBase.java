/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.iot;

public interface ITestIoTBase {
     String IOT_ADDRESS_EVENT = "event";
     String IOT_ADDRESS_TELEMETRY = "telemetry";
     String IOT_ADDRESS_CONTROL = "control";
     String IOT_ADDRESS_COMMAND = "command";
     String IOT_ADDRESS_COMMAND_RESPONSE = "command_response";
     String DEVICE_REGISTRY_TEST_ADDRESSSPACE = "device-registry-test-addrspace";
     String DEVICE_REGISTRY_TEST_PROJECT = "device-registry-test-project";
     String iotProjectNamespace = "iot-project-ns";

}
