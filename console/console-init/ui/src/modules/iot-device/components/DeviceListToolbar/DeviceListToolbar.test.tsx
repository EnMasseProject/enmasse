/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import {
  DeviceListToolbar,
  IDeviceListToolbarProps
} from "./DeviceListToolbar";
import { DropdownItem } from "@patternfly/react-core";
import { ICreateDeviceButtonProps } from "modules/iot-device";

describe("<DeviceListToolbar />", () => {
  const kebabItems: React.ReactNode[] = [
    <DropdownItem key="enable" onClick={jest.fn()}>
      Enable
    </DropdownItem>,
    <DropdownItem key="disable" onClick={jest.fn()}>
      Disable
    </DropdownItem>,
    <DropdownItem key="delete" onClick={jest.fn()}>
      Delete
    </DropdownItem>
  ];

  const addDeviceBtnProps: ICreateDeviceButtonProps = {
    handleInputDeviceInfo: jest.fn(),
    handleJSONUpload: jest.fn()
  };

  const toolbarProps: IDeviceListToolbarProps = {
    kebabItems: kebabItems,
    isOpen: true,
    onSelectAllDevices: jest.fn(),
    onToggle: jest.fn(),
    isChecked: false,
    onChange: jest.fn()
  };

  const props = {
    ...addDeviceBtnProps,
    ...toolbarProps
  };

  it("should render the toolbar for device list", () => {
    const { getByText } = render(
      <MemoryRouter>
        <DeviceListToolbar {...props} />
      </MemoryRouter>
    );

    getByText("Add device");
  });

  it("should show appropriate options in CreateDeviceButton", () => {
    const { getByText } = render(
      <MemoryRouter>
        <DeviceListToolbar {...props} />
      </MemoryRouter>
    );

    const createDeviceBtn = getByText("Add device");

    fireEvent.click(createDeviceBtn);

    getByText("Add with wizard");
    getByText("Add in JSON");
  });

  // ToDo: Modify after bulk select is finalized
  xit("should render appropriate options for BulkSelect", () => {
    const { getByText } = render(
      <MemoryRouter>
        <DeviceListToolbar {...props} />
      </MemoryRouter>
    );

    getByText("Select none (0 items)");
    getByText("Select page (10 items)");
    getByText("Select all (100 items)");
  });
});
