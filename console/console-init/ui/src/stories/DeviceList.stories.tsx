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
  getTableCells
} from "modules/device";
import { text } from "@storybook/addon-knobs";
import { IRowData } from "@patternfly/react-table";

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
