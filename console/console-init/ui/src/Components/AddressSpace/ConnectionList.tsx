import * as React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData
} from "@patternfly/react-table";
import { Link, useParams } from "react-router-dom";
import { ConnectionProtocolFormat } from "../Common/ConnectionListFormatter";

interface IConnectionListProps {
  rows: IConnection[];
}

export interface IConnection {
  hostname: string;
  containerId: string;
  protocol: string;
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
        { title: <Link to={`connection/${row.hostname}`}>{row.hostname}</Link> },
        row.containerId,
        { title: <ConnectionProtocolFormat protocol={row.protocol} /> },
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
    {title:"Hostname",dataLabel:"host"},
    "Container ID",
    "Protocol",
    "Messages In",
    "Messages Out",
    "Senders",
    "Receivers"
  ];

  return (
    <Table

      variant={TableVariant.compact}
      cells={tableColumns}
      rows={tableRows}
      aria-label="connection list"
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};
