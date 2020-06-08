/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";
import { text } from "@storybook/addon-knobs";
import { action } from "@storybook/addon-actions";
import { DeviceDetailHeader } from "modules/iot-device-detail";

export default {
  title: "Device"
};
export const deviceDetailHeader = () => (
  <MemoryRouter>
    <DeviceDetailHeader
      deviceName={text("Device Name", "Device foo")}
      addedDate="2019-11-25T05:24:05.755Z"
      lastTimeSeen="2019-11-25T05:24:05.755Z"
      onChange={action("onEnableChange Clicked")}
      onDelete={action("onDelete Clicked")}
      onEditMetadata={action("onEdit Clicked")}
      onEditDeviceInJson={action("onEdit Clicked")}
      onClone={action("onClone Clicked")}
    />
  </MemoryRouter>
);
