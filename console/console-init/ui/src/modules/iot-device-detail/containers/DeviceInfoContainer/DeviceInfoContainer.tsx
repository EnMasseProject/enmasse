/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { DeviceInfo } from "modules/iot-device-detail/components";
import { mock_iot_device } from "mock-data";

export const DeviceInfoContainer = () => {
  const metadataList = [
    {
      headers: ["Message info parameter", "Type", "Value"],
      data: mock_iot_device.default
    },
    {
      headers: ["Basic info parameter", "Type", "Value"],
      data: mock_iot_device.ext
    }
  ];

  return (
    <DeviceInfo
      id="device-info"
      deviceList={mock_iot_device.via}
      metadataList={metadataList}
      credentials={mock_iot_device.credentials}
    />
  );
};
