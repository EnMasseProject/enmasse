/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.bases.iot;

import io.enmasse.systemtest.model.addressspace.AddressSpaceType;

public interface ITestIoTBase {
     String IOT_ADDRESS_EVENT = "event";
     String IOT_ADDRESS_TELEMETRY = "telemetry";
     String IOT_ADDRESS_CONTROL = "control";
     String IOT_ADDRESS_COMMAND = "command";
     String IOT_ADDRESS_COMMAND_RESPONSE = "command_response";
     String[] IOT_ADDRESSES = new String[] {
             IOT_ADDRESS_TELEMETRY,
             IOT_ADDRESS_EVENT,
             IOT_ADDRESS_CONTROL,
             IOT_ADDRESS_COMMAND,
             IOT_ADDRESS_COMMAND_RESPONSE,
     };
     String iotProjectNamespace = "iot-project-ns";
}
