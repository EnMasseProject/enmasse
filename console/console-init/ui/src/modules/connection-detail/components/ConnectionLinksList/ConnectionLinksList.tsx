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
  IRowData,
  sortable,
  ISortBy
} from "@patternfly/react-table";
import { StyleSheet, css } from "aphrodite";

const StyleForTable = StyleSheet.create({
  scroll_overflow: {
    overflowY: "auto",
    paddingBottom: 100
  }
});

interface IConnectionLinksListProps {
  rows: ILink[];
  sortBy?: ISortBy;
  onSort?: (_event: any, index: number, direction: string) => void;
}

export interface ILink {
  role: string;
  name: string;
  address: string;
  deliveries: number | string;
  rejected: number | string;
  released: number | string;
  modified: number | string;
  presettled: number | string;
  undelivered: number | string;
  accepted: number | string;
  status?: "creating" | "deleting" | "running";
}

export const ConnectionLinksList: React.FunctionComponent<IConnectionLinksListProps> = ({
  rows,
  sortBy,
  onSort
}) => {
  const toTableCells = (row: ILink) => {
    const tableRow: IRowData = {
      cells: [
        row.role,
        row.name,
        row.address,
        row.deliveries,
        row.accepted,
        row.rejected,
        row.released,
        row.modified,
        row.presettled,
        row.undelivered
      ],
      originalData: row
    };
    return tableRow;
  };
  const tableRows = rows.map(toTableCells);
  const tableColumns = [
    "Role",
    { title: "Name", transforms: [sortable] },
    { title: "Address", transforms: [sortable] },
    { title: "Deliveries", transforms: [sortable] },
    { title: "Accepted", transforms: [sortable] },
    { title: "Rejected", transforms: [sortable] },
    { title: "Released", transforms: [sortable] },
    { title: "Modified", transforms: [sortable] },
    { title: "Presettled", transforms: [sortable] },
    { title: "Undelivered", transforms: [sortable] }
  ];

  return (
    <div className={css(StyleForTable.scroll_overflow)}>
      <Table
        variant={TableVariant.compact}
        cells={tableColumns}
        rows={tableRows}
        aria-label="links list"
        onSort={onSort}
        sortBy={sortBy}
      >
        <TableHeader id="connection-link-list-tableheader" />
        <TableBody />
      </Table>
    </div>
  );
};
