import React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData,
  sortable,
  RowWrapper
} from "@patternfly/react-table";
import { Link } from "react-router-dom";
import { TypePlan } from "../TypePlan";
import { AddressListFilterWithPagination } from "./AddressListFilterWithPaginationHeader";

export interface IAddress {
  name: string;
  namespace: string;
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

  //TODO: Display error after the phase variable is exposed from backend.
  const toTableCells = (row: IAddress) => {
    const tableRow: IRowData = {
      cells: [
        { title: <Link to={`addresses/${row.name}`}>{row.name}</Link> },
        { title: <TypePlan type={row.type} plan={row.plan} /> },
        {
          title: row.messagesIn
        },
        {
          title: row.messagesOut
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
    "Type/Plan",
    { title: "Messages In", transforms: [sortable] },
    { title: "Messages Out", transforms: [sortable] },
    { title: "Stored Messages", transforms: [sortable] },
    "Senders",
    "Receivers",
    "Shards"
  ];
  const sortBy = {};
  return (
    <Table
      variant={TableVariant.compact}
      cells={tableColumns}
      rows={tableRows}
      actionResolver={actionResolver}
      aria-label="Address List"
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};
