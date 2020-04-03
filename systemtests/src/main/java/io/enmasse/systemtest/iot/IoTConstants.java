/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.systemtest.iot;

import io.enmasse.systemtest.model.addressspace.AddressSpacePlans;

public interface IoTConstants {
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

     String IOT_PROJECT_NAMESPACE = "iot-project-ns";
     String IOT_DEFAULT_ADDRESS_SPACE_PLAN = AddressSpacePlans.STANDARD_SMALL;
}
