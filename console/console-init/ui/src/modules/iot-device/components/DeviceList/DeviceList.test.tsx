/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { render } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { IDeviceListProps, DeviceList } from "modules/iot-device/components";
import { getTableCells } from "modules/iot-device/utils";
import { IRowData } from "@patternfly/react-table";

describe("<DeviceList />", () => {
  const deviceRows: IRowData[] = [
    {
      id: "littlesensor1",
      viaGateway: true,
      enabled: true,
      selected: true,
      lastSeen: "2020-01-20T11:44:28.607Z",
      lastUpdated: "2020-01-20T11:44:28.607Z",
      creationTimeStamp: "2020-01-20T11:44:28.607Z"
    },
    {
      id: "jboss20",
      viaGateway: true,
      enabled: false,
      selected: false,
      jsonData: '{"ext":{"ocean":"atlantic"}}',
      lastSeen: "2020-04-20T11:44:28.607Z",
      lastUpdated: "2020-04-29T11:44:28.607Z",
      creationTimeStamp: "2020-04-30T11:44:28.607Z"
    },
    {
      id: "jboss20",
      viaGateway: true,
      lastSeen: "2020-04-20T11:44:28.607Z",
      lastUpdated: null,
      creationTimeStamp: "2020-04-30T11:44:28.607Z"
    },
    {
      id: null,
      viaGateway: null,
      enabled: false,
      selected: null,
      lastSeen: undefined,
      lastUpdated: null,
      creationTimeStamp: null
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

  const onSelectDevice = jest.fn();

  const props: IDeviceListProps = {
    deviceRows,
    actionResolver,
    onSelectDevice
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
