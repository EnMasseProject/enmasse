/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";

import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  sortable,
  TableProps,
  SortByDirection,
  ICell,
  truncate,
  IExtraColumnData
} from "@patternfly/react-table";
import { StyleSheet, css } from "aphrodite";
import { getTableCells } from "modules/iot-device/utils";
export interface IDeviceListProps
  extends Pick<TableProps, "actionResolver" | "sortBy"> {
  deviceRows: IDevice[];
  onSelectDevice: (device: IDevice, isSelected: boolean) => void;
  onSort?: (
    event: any,
    index: number,
    direction: SortByDirection,
    extraData: IExtraColumnData
  ) => void;
  selectedColumns: string[];
}

export interface IDevice {
  deviceId?: string | null;
  via?: string[];
  viaGroups?: string[];
  memberOf?: string[];
  enabled?: boolean | null;
  selected?: boolean | null;
  lastSeen?: string | Date;
  updated?: string | Date;
  created?: string | Date;
  credentials?: string;
}

export const StyleForFooteredTable = StyleSheet.create({
  scroll_overflow: {
    overflowY: "auto"
  }
});

export const DeviceList: React.FunctionComponent<IDeviceListProps> = ({
  deviceRows,
  sortBy,
  onSort,
  actionResolver,
  onSelectDevice,
  selectedColumns
}) => {
  const tableColumns: (string | ICell)[] = [];
  selectedColumns.forEach(column => {
    switch (column?.toLowerCase()) {
      case "deviceid":
        tableColumns.push({ title: "Device ID", transforms: [sortable] });
        break;
      case "connectiontype":
        tableColumns.push({ title: "Connection type" });
        break;
      case "status":
        tableColumns.push({ title: "Status", transforms: [sortable] });
        break;
      case "lastupdated":
        tableColumns.push({ title: "Last updated", transforms: [sortable] });
        break;
      case "lastseen":
        tableColumns.push({ title: "Last seen", transforms: [sortable] });
        break;
      case "addeddate":
        tableColumns.push({ title: "Added date", transforms: [sortable] });
        break;
      case "memberof":
        tableColumns.push({ title: "MemberOf", cellTransforms: [truncate] });
        break;
      case "viagateways":
        tableColumns.push({
          title: "Via Gateways",
          cellTransforms: [truncate]
        });
        break;
      default:
        break;
    }
  });

  const mappedDeviceRows = deviceRows.map(row =>
    getTableCells(row, selectedColumns)
  );

  const onSelect = (
    _: React.FormEvent<HTMLInputElement>,
    isSelected: boolean,
    rowIndex: number
  ) => {
    const deviceRows = [...mappedDeviceRows];
    deviceRows[rowIndex].selected = isSelected;
    onSelectDevice(deviceRows[rowIndex].originalData, isSelected);
  };

  return (
    <div
      className={css(StyleForFooteredTable.scroll_overflow)}
      id="device-list-table"
    >
      <Table
        variant={TableVariant.compact}
        canSelectAll={false}
        onSelect={onSelect}
        cells={tableColumns}
        rows={mappedDeviceRows}
        aria-label="device list"
        sortBy={sortBy}
        onSort={onSort}
        actionResolver={actionResolver}
      >
        <TableHeader id="device-list-table-header" />
        <TableBody />
      </Table>
    </div>
  );
};
