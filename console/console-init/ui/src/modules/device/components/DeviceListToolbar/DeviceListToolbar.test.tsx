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

describe("<DeviceListToolbar />", () => {
  const kebabItems: React.ReactNode[] = [
    <DropdownItem onClick={jest.fn()}>Enable</DropdownItem>,
    <DropdownItem onClick={jest.fn()}>Disable</DropdownItem>,
    <DropdownItem onClick={jest.fn()}>Delete</DropdownItem>
  ];

  const bulkSelectItems: React.ReactNode[] = [
    <DropdownItem key="item-1" onClick={jest.fn()}>
      Select none (0 items)
    </DropdownItem>,
    <DropdownItem key="item-2" onClick={jest.fn()}>
      Select page (10 items)
    </DropdownItem>,
    <DropdownItem key="item-3" onClick={jest.fn()}>
      Select all (100 items)
    </DropdownItem>
  ];

  const props: IDeviceListToolbarProps = {
    itemCount: 100,
    perPage: 10,
    page: 1,
    kebabItems: kebabItems,
    onSetPage: jest.fn(),
    onPerPageSelect: jest.fn(),
    handleInputDeviceInfo: jest.fn(),
    handleJSONUpload: jest.fn(),
    isOpen: true,
    handleOnSelect: jest.fn(),
    handleOnToggle: jest.fn(),
    isChecked: false,
    items: bulkSelectItems,
    handleOnChange: jest.fn()
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

  it("should render appropriate options for BulkSelect", () => {
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
