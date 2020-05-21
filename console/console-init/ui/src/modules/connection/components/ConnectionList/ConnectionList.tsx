/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { Link } from "react-router-dom";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData,
  sortable,
  ISortBy
} from "@patternfly/react-table";
import { FormatDistance } from "use-patternfly";
import { css } from "@patternfly/react-styles";
import { ConnectionProtocolFormat } from "utils";
import { useWindowDimensions } from "components";
import { StyleForTable } from "modules/msg-and-iot";

interface IConnectionListProps {
  rows: IConnection[];
  addressSpaceType?: string;
  sortBy?: ISortBy;
  onSort?: (_event: any, index: number, direction: string) => void;
}
export interface IConnection {
  hostname: string;
  containerId: string;
  protocol: string;
  encrypted: boolean;
  messageIn: number | string;
  messageOut: number | string;
  senders: number | string;
  receivers: number | string;
  status: "creating" | "deleting" | "running";
  name: string;
  creationTimestamp: string;
}

export const ConnectionList: React.FunctionComponent<IConnectionListProps> = ({
  rows,
  addressSpaceType,
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
        !addressSpaceType || addressSpaceType === "brokered"
          ? ""
          : row.messageOut,
        row.senders,
        row.receivers
      ],
      originalData: row
    };
    return tableRow;
  };

  const tableRows = rows && rows.map(toTableCells);

  const tableColumns = [
    { title: "Hostname", transforms: [sortable] },
    { title: "Container ID", transforms: [sortable] },
    { title: "Protocol", transforms: [sortable] },
    { title: "Time created", transforms: [sortable] },
    {
      title:
        width > 769 ? (
          <span style={{ display: "inline-flex" }}>
            Message In/sec
            <br />
            {`(over last 5 min)`}
          </span>
        ) : (
          "Message In/sec"
        ),
      transforms: [sortable]
    },
    !addressSpaceType || addressSpaceType === "brokered"
      ? ""
      : {
          title:
            width > 769 ? (
              <span style={{ display: "inline-flex" }}>
                Message Out/sec
                <br />
                {`(over last 5 min)`}
              </span>
            ) : (
              "Message Out/sec"
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
    <div className={css(StyleForTable.scroll_overflow)}>
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
    </div>
  );
};
