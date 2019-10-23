import React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData
} from "@patternfly/react-table";
import { IAddresses, IConnections } from "../Types/ResourceType";
import { Link } from "react-router-dom";

export interface IAddress {
  name: string;
  typePlan: string;
  messagesIn: number;
  messagesOut: number;
  storedMessages: number;
  senders: number;
  receivers: number;
  shards: number;
  status: "creating" | "deleting" | "running";
}

//Types to be defined later
interface IResourceListProps {
  rows: IAddress[];
  onEdit: (rowData: IAddress) => void;
  onDelete: (rowData: IAddress) => void;
}

const columns = [
  "Name",
  "Type/Plan",
  "Messages In",
  "Messages Out",
  "Stored Messages",
  "Senders",
  "Receivers",
  "Shards"
];

const ResourceList: React.FunctionComponent<IResourceListProps> = ({
  rows,
  onEdit,
  onDelete
}) => {
  const actionResolver = (rowData: IRowData) => {
    // TODO: change actions based on the address object state (eg. is
    // being created, so we can't delete or edit it)
    // TODO: figure out a better way to get access to the original data
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
        row.typePlan,
        row.messagesIn,
        row.messagesOut,
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
  return (
    <Table
      variant={TableVariant.compact}
      cells={columns}
      rows={tableRows}
      actionResolver={actionResolver}
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};

export default ResourceList;
