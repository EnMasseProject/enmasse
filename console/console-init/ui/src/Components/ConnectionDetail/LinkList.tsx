/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData,
  sortable,
  ISortBy
} from "@patternfly/react-table";

interface ILinkListProps {
  rows: ILink[];
  sortBy?: ISortBy;
  onSort?: (_event: any, index: number, direction: string) => void;
}

export interface ILink {
  role: string;
  name: string;
  address: string;
  deliveries: number;
  rejected: number;
  released: number;
  modified: number;
  presettled: number;
  undelivered: number;
  status?: "creating" | "deleting" | "running";
}

export const LinkList: React.FunctionComponent<ILinkListProps> = ({
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
    { title: "Rejected", transforms: [sortable] },
    { title: "Released", transforms: [sortable] },
    { title: "Modified", transforms: [sortable] },
    { title: "Presettled", transforms: [sortable] },
    { title: "Undelivered", transforms: [sortable] }
  ];

  return (
    <Table
      variant={TableVariant.compact}
      cells={tableColumns}
      rows={tableRows}
      aria-label="links list"
      onSort={onSort}
      sortBy={sortBy}
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};
