import React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData
} from "@patternfly/react-table";
import { Link } from "react-router-dom";
import { Messages } from "./Messages";

export interface IAddress {
  name: string;
  type: string;
  plan: string;
  messagesIn: number;
  messagesOut: number;
  storedMessages: number;
  senders: number;
  receivers: number;
  shards: number;
  status: "creating" | "deleting" | "running";
}

interface IAddressListProps {
  rows: IAddress[];
  onEdit: (rowData: IAddress) => void;
  onDelete: (rowData: IAddress) => void;
}

export const AddressList: React.FunctionComponent<IAddressListProps> = ({
  rows,
  onEdit,
  onDelete
}) => {
  //TODO: Add loading icon based on status
  const actionResolver = (rowData: IRowData) => {
    const originalData = rowData.originalData as IAddress;
    const status = originalData.status;
    switch (status) {
      case "creating":
      case "deleting":
        return [];
      default:
        return [
          {
            title: "Edit",
            onClick: () => onEdit(originalData)
          },
          {
            title: "Delete",
            onClick: () => onDelete(originalData)
          }
        ];
    }
  };

  const toTableCells = (row: IAddress) => {
    const tableRow: IRowData = {
      cells: [
        { title: <Link to="#">{row.name}</Link> },
        row.type,
        row.plan,
        {
          title: (
            <Messages
              count={row.messagesIn}
              column="MessagesIn"
              status={row.status}
            />
          )
        },
        {
          title: (
            <Messages
              count={row.messagesOut}
              column="MessagesOut"
              status={row.status}
            />
          )
        },
        row.storedMessages,
        row.senders,
        row.receivers,
        row.shards
      ],
      originalData: row
    };
    return tableRow;
  };
  const tableRows = rows.map(toTableCells);
  const tableColumns = [
    "Name",
    "Type",
    "Plan",
    "Messages In",
    "Messages Out",
    "Stored Messages",
    "Senders",
    "Receivers",
    "Shards"
  ];

  return (
    <Table
      variant={TableVariant.compact}
      cells={tableColumns}
      rows={tableRows}
      actionResolver={actionResolver}
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};
