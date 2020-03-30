/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { MemoryRouter } from "react-router";

import {
  DeviceListAlert,
  DeviceList,
  IDevice,
  EmptyDeviceList,
  DeviceListToolbar
} from "modules/device";
import { text, select, boolean } from "@storybook/addon-knobs";

import { getTableCells } from "modules/device/utils";
import { action } from "@storybook/addon-actions";
import { IRowData } from "@patternfly/react-table";
import { DeviceDetailNavigation } from "modules/device-detail";
import { DropdownItem } from "@patternfly/react-core";

export default {
  title: "Device"
};

const rows: IDevice[] = [
  {
    id: "littlesensor1",
    type: "Using gateways",
    status: true,
    selected: true,
    lastSeen: "2020-01-20T11:44:28.607Z",
    lastUpdated: "2020-01-20T11:44:28.607Z",
    creationTimeStamp: "2020-01-20T11:44:28.607Z"
  },
  {
    id: "jboss20",
    type: "Using gateways",
    status: false,
    selected: false,
    lastSeen: "2020-04-20T11:44:28.607Z",
    lastUpdated: "2020-04-29T11:44:28.607Z",
    creationTimeStamp: "2020-04-30T11:44:28.607Z"
  }
];

const kebabItems: React.ReactNode[] = [
  <DropdownItem onClick={action("kebab enable devices")}>Enable</DropdownItem>,
  <DropdownItem onClick={action("kebab disable devices")}>
    Disable
  </DropdownItem>,
  <DropdownItem onClick={action("kebab delete devices")}>Delete</DropdownItem>
];

const bulkSelectItems: React.ReactNode[] = [
  <DropdownItem key="item-1" onClick={action("Deselect all")}>
    Select none (0 items)
  </DropdownItem>,
  <DropdownItem key="item-2" onClick={action("Select all items in the page")}>
    Select page (10 items)
  </DropdownItem>,
  <DropdownItem key="item-3" onClick={action("Select all items")}>
    Select all (100 items)
  </DropdownItem>
];

const actionResolver = (rowData: IRowData) => [
  {
    title: "Edit Device",
    onClick: () => {}
  }
];

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

export const deviceTable = () => (
  <MemoryRouter>
    <DeviceList
      rows={rows.map(getTableCells)}
      onSelect={async () => {}}
      actionResolver={actionResolver}
    />
  </MemoryRouter>
);

export const deviceDetailHeaderNavigation = () => {
  const options = ["deviceInfo", "liveDataStream", "connectionInfo"];
  return (
    <MemoryRouter>
      <DeviceDetailNavigation
        activeItem={select("Active Nav Item", options, "deviceInfo")}
      />
    </MemoryRouter>
  );
};

export const emptyDevice = () => (
  <EmptyDeviceList
    handleInputDeviceInfo={action("input device info handler clicked")}
    handleJSONUpload={action("json upload handler clicked")}
  />
);

export const deviceToolbar = () => {
  return (
    <MemoryRouter>
      <DeviceListToolbar
        itemCount={100}
        perPage={10}
        page={1}
        kebabItems={kebabItems}
        onSetPage={action("Pagination change page number")}
        onPerPageSelect={action("Pagination change items per page")}
        handleInputDeviceInfo={action("input device info handler clicked")}
        handleJSONUpload={action("json upload handler clicked")}
        isOpen={boolean("is Open", true)}
        handleOnSelect={action("On select handler for bulk select component")}
        handleOnToggle={action("On toggle handler for bulk select component")}
        isChecked={boolean("isChecked", false)}
        items={bulkSelectItems}
        handleOnChange={action("checkbox dropdown changed")}
      />
    </MemoryRouter>
  );
};
