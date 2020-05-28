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
  IRowData
} from "@patternfly/react-table";
import { css, StyleSheet } from "@patternfly/react-styles";

export interface IDeviceListProps
  extends Pick<TableProps, "actionResolver" | "sortBy" | "onSelect"> {
  rows: IRowData[];
  onSort?: (_event: any, index: number, direction: string) => void;
}

export interface IDevice {
  id: string;
  type: string;
  status: boolean;
  selected: boolean;
  lastSeen: string;
  lastUpdated: string;
  creationTimeStamp: string;
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
  onSelect
}) => {
  const tableColumns = [
    { title: "Device ID", transforms: [sortable] },
    { title: "Device type", transforms: [sortable] },
    { title: "Status", transforms: [sortable] },
    { title: "Last seen", transforms: [sortable] },
    { title: "Last updated", transforms: [sortable] },
    { title: "Added date", transforms: [sortable] }
  ];

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
        rows={rows}
        aria-label="device list"
        sortBy={sortBy}
        onSort={onSort}
        actionResolver={actionResolver}
      >
        <TableHeader id="devicelist-table-header" />
        <TableBody />
      </Table>
    </div>
  );
};
