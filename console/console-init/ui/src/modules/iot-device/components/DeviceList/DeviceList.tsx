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
  IRowData,
  SortByDirection,
  ICell,
  truncate,
  IExtraColumnData
} from "@patternfly/react-table";
import { StyleSheet, css } from "aphrodite";
import { getTableCells } from "modules/iot-device/utils";
export interface IDeviceListProps
  extends Pick<TableProps, "actionResolver" | "sortBy"> {
  rows: IDevice[];
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
  rows,
  sortBy,
  onSort,
  actionResolver,
  onSelectDevice,
  selectedColumns
}) => {
  const tableColumns: (string | ICell)[] = [];
  selectedColumns.forEach(column => {
    switch (column?.toLowerCase()) {
      case "deviceId":
        tableColumns.push({ title: "Device ID", transforms: [sortable] });
        break;
      case "connectionType":
        tableColumns.push({ title: "Connection type" });
        break;
      case "status":
        tableColumns.push({ title: "Status", transforms: [sortable] });
        break;
      case "lastUpdated":
        tableColumns.push({ title: "Last updated", transforms: [sortable] });
        break;
      case "lastSeen":
        tableColumns.push({ title: "Last seen", transforms: [sortable] });
        break;
      case "addedDate":
        tableColumns.push({ title: "Added date", transforms: [sortable] });
        break;
      case "memberOf":
        tableColumns.push({ title: "MemberOf", cellTransforms: [truncate] });
        break;
      case "viaGateways":
        tableColumns.push({
          title: "Via Gateways",
          cellTransforms: [truncate]
        });
        break;
      default:
        break;
    }
  });

  const deviceRows = rows.map(row => getTableCells(row, selectedColumns));

  const onSelect = (
    _: React.FormEvent<HTMLInputElement>,
    isSelected: boolean,
    rowIndex: number
  ) => {
    const rows = [...deviceRows];
    rows[rowIndex].selected = isSelected;
    onSelectDevice(rows[rowIndex].originalData, isSelected);
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
        rows={deviceRows}
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
