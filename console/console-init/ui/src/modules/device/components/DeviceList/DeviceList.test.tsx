/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { IDeviceListProps, DeviceList } from "modules/device/components";
import { getTableCells } from "modules/device/utils";
import { IRowData } from "@patternfly/react-table";

describe("<DeviceList />", () => {
  const rows: IRowData[] = [
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
  ].map(getTableCells);

  const actionResolver = (rowData: IRowData) => [
    {
      title: "Edit Device",
      onClick: jest.fn()
    },
    {
      title: "Delete Device",
      onClick: jest.fn()
    }
  ];

  const onSelect = async () => {};

  const props: IDeviceListProps = {
    rows,
    actionResolver,
    onSelect
  };

  const { getByText } = render(
    <MemoryRouter>
      <DeviceList {...props} />
    </MemoryRouter>
  );

  it("should render table for DeviceList", () => {
    const deviceIDColHead = getByText("Device ID");
    const statusColHead = getByText("Status");

    expect(deviceIDColHead).toBeInTheDocument();
    expect(statusColHead).toBeInTheDocument();
  });
});
