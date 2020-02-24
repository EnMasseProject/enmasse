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
import { ConnectionProtocolFormat } from "components/common/ConnectionListFormatter";
import useWindowDimensions from "components/common/WindowDimension";
import { FormatDistance } from "use-patternfly";
import { StyleForTable } from "components/AddressSpaceList/AddressSpaceList";
import { css } from "@patternfly/react-styles";

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
