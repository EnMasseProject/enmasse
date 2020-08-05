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
  SortByDirection
} from "@patternfly/react-table";
import { StyleSheet, css } from "aphrodite";

export interface IDeviceListProps
  extends Pick<TableProps, "actionResolver" | "sortBy"> {
  deviceRows: IRowData[];
  onSelectDevice: (device: IDevice, isSelected: boolean) => void;
  onSort?: (_event: any, index: number, direction: SortByDirection) => void;
}

export interface IDevice {
  deviceId?: string | null;
  via?: string[];
  viaGroups?: string[];
  enabled?: boolean | null;
  selected?: boolean | null;
  lastSeen?: string | Date;
  updated?: string | Date;
  created?: string | Date;
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
  onSelectDevice
}) => {
  const tableColumns = [
    { title: "Device ID", transforms: [sortable] },
    { title: "Connection type" },
    { title: "Status", transforms: [sortable] },
    { title: "Last seen", transforms: [sortable] },
    { title: "Last updated", transforms: [sortable] },
    { title: "Added date", transforms: [sortable] }
  ];

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
