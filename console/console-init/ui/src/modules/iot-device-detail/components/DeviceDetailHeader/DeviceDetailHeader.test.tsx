/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { DeviceDetailHeader } from "./DeviceDetailHeader";

describe("<DeviceDetailHeader />", () => {
  it("should render the iot project header", () => {
    const props = {
      deviceName: "device-name",
      addedDate: "2019-11-25T05:24:05.755Z",
      lastTimeSeen: "2019-11-25T05:24:05.755Z",
      isEnabled: true,
      changeEnable: jest.fn(),
      onEdit: jest.fn(),
      onDelete: jest.fn(),
      onClone: jest.fn()
    };
    const { getByText } = render(
      <MemoryRouter>
        <DeviceDetailHeader {...props} />
      </MemoryRouter>
    );
    getByText("Added Date :");
    getByText("Last time seen :");
    getByText(props.deviceName);
  });
});
