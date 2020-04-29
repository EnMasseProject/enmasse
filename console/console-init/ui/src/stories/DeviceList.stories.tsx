/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";

import { DeviceListAlert } from "modules/device";
import { text } from "@storybook/addon-knobs";

export default {
  title: "Device"
};

export const deviceAlert = () => (
  <MemoryRouter>
    <DeviceListAlert
      visible={true}
      variant={"info"}
      title={text("Alert title", "Run filter to view your devices")}
      description={text(
        "Alert description",
        "You have a total of 36,300 devices"
      )}
    />
  </MemoryRouter>
);
