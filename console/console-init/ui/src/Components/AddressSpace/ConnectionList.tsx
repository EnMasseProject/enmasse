import * as React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData,
  sortable
} from "@patternfly/react-table";
import { Link } from "react-router-dom";
import { ConnectionProtocolFormat } from "../Common/ConnectionListFormatter";

interface IConnectionListProps {
  rows: IConnection[];
}

export interface IConnection {
  hostname: string;
  containerId: string;
  protocol: string;
  encrypted: boolean;
  messagesIn: number;
  messagesOut: number;
  senders: number;
  receivers: number;
  status: "creating" | "deleting" | "running";
}

export const ConnectionList: React.FunctionComponent<IConnectionListProps> = ({
  rows
}) => {
  const toTableCells = (row: IConnection) => {
    const tableRow: IRowData = {
      cells: [
        {
          title: <Link to={`connections/${row.hostname}`}>{row.hostname}</Link>
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
        row.messagesIn,
        row.messagesOut,
        row.senders,
        row.receivers
      ],
      originalData: row
    };
    return tableRow;
  };
  const tableRows = rows.map(toTableCells);
  const tableColumns = [
    { title: "Hostname", dataLabel: "host" },
    "Container ID",
    "Protocol",
    {
      title: (
        <span style={{ display: "inline-flex" }}>
          Messages In
          <br />
          {`(over last 5 min)`}
        </span>
      )
    },
    {
      title: (
        <span style={{ display: "inline-flex" }}>
          Messages Out
          <br />
          {`(over last 5 min)`}
        </span>
      )
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
      aria-label="connection list">
      <TableHeader />
      <TableBody />
    </Table>
  );
};
