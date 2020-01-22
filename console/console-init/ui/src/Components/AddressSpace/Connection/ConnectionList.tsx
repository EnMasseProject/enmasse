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
import { Link } from "react-router-dom";
import { ConnectionProtocolFormat } from "../../Common/ConnectionListFormatter";
import useWindowDimensions from "src/Components/Common/WindowDimension";
import { FormatDistance } from "use-patternfly";

interface IConnectionListProps {
  rows: IConnection[];
  sortBy?: ISortBy;
  onSort?: (_event: any, index: number, direction: string) => void;
}

export interface IConnection {
  hostname: string;
  containerId: string;
  protocol: string;
  encrypted: boolean;
  messageIn: number;
  messageOut: number;
  senders: number;
  receivers: number;
  status: "creating" | "deleting" | "running";
  name: string;
  creationTimestamp: string;
}

export const ConnectionList: React.FunctionComponent<IConnectionListProps> = ({
  rows,
  sortBy,
  onSort
}) => {
  const { width } = useWindowDimensions();
  const toTableCells = (row: IConnection) => {
    const tableRow: IRowData = {
      cells: [
        {
          title: <Link to={`connections/${row.name}`}>{row.hostname}</Link>
        },
        row.containerId,
        {
          title: (
            <ConnectionProtocolFormat
              protocol={row.protocol}
              encrypted={row.encrypted}
            />
          )
        },
        {
          title: (
            <>
              <FormatDistance date={row.creationTimestamp} /> ago
            </>
          )
        },
        row.messageIn,
        row.messageOut,
        row.senders,
        row.receivers
      ],
      originalData: row
    };
    return tableRow;
  };
  const tableRows = rows.map(toTableCells);
  const tableColumns = [
    { title: "Hostname", dataLabel: "host", transforms: [sortable] },
    { title: "Container ID", transforms: [sortable] },
    { title: "Protocol", transforms: [sortable] }, 
    { title: "Time created", transforms: [sortable] }, 
    {
      title:
        width > 769 ? (
          <span style={{ display: "inline-flex" }}>
            Message In
            <br />
            {`(over last 5 min)`}
          </span>
        ) : (
          "Message In"
        ),
      transforms: [sortable]
    },
    {
      title:
        width > 769 ? (
          <span style={{ display: "inline-flex" }}>
            Message Out
            <br />
            {`(over last 5 min)`}
          </span>
        ) : (
          "Message Out"
        ),
      transforms: [sortable]
    },
    {
      title: "Senders",
      transforms: [sortable]
    },
    {
      title: "Receivers",
      transforms: [sortable]
    }
  ];

  return (
    <Table
      variant={TableVariant.compact}
      cells={tableColumns}
      rows={tableRows}
      aria-label="connection list"
      sortBy={sortBy}
      onSort={onSort}
    >
      <TableHeader id="connectionlist-table-header" />
      <TableBody />
    </Table>
  );
};
