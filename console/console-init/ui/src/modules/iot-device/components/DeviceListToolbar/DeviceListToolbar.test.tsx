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
import { DropdownItem, PaginationProps } from "@patternfly/react-core";
import { ICreateDeviceButtonProps } from "modules/iot-device";

describe("<DeviceListToolbar />", () => {
  const kebabItems: React.ReactNode[] = [
    <DropdownItem onClick={jest.fn()}>Enable</DropdownItem>,
    <DropdownItem onClick={jest.fn()}>Disable</DropdownItem>,
    <DropdownItem onClick={jest.fn()}>Delete</DropdownItem>
  ];

  const paginationProps: PaginationProps = {
    itemCount: 100,
    perPage: 10,
    page: 1,
    onSetPage: jest.fn(),
    onPerPageSelect: jest.fn()
  };

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
    ...toolbarProps,
    ...paginationProps
  };

  it("should render the toolbar for device list", () => {
    const { getByText } = render(
      <MemoryRouter>
        <DeviceListToolbar {...props} />
      </MemoryRouter>
    );

    getByText("Add device");
    getByText("of 10");
  });

  it("should show appropriate options in CreateDeviceButton", () => {
    const { getByText } = render(
      <MemoryRouter>
        <DeviceListToolbar {...props} />
      </MemoryRouter>
    );

    const createDeviceBtn = getByText("Add device");

    fireEvent.click(createDeviceBtn);

    getByText("Input device info");
    getByText("Upload a JSON file");
  });

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
