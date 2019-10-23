import * as React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData
} from "@patternfly/react-table";
import { Link } from "react-router-dom";

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

const ConnectionList: React.FunctionComponent<IConnectionListProps> = ({
  rows
}) => {
  const toTableCells = (row: IConnection) => {
    const tableRow: IRowData = {
      cells: [
        { title: <Link to="#">{row.hostname}</Link> },
        row.containerId,
        row.protocol,
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
    "Hostname",
    "Container ID",
    "Protocol",
    "Messages In",
    "Messages Out",
    "Senders",
    "Receivers"
  ];

  return (
    <Table variant={TableVariant.compact} cells={tableColumns} rows={tableRows}>
      <TableHeader />
      <TableBody />
    </Table>
  );
};

export default ConnectionList;
