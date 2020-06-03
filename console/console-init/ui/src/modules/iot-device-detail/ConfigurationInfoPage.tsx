/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { ConfigurationInfo } from "modules/iot-device-detail/components";
import { mock_iot_device, mock_adapters } from "mock-data";

export default function ConfigurationInfoPage() {
  return (
    <ConfigurationInfo
      id="configuation-info-page"
      adapters={mock_adapters}
      credentials={mock_iot_device.credentials}
    />
  );
}
