import React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData
} from "@patternfly/react-table";
import { Link } from "react-router-dom";
import {
  AddressSpaceType,
  AddressSpaceStatus,
  AddressSpaceIcon
} from "../Common/AddressSpaceListFormatter";

export interface IAddressSpace {
  name: string;
  nameSpace: string;
  creationTimestamp: string;
  type: string;
  displayName: string;
  isReady: boolean;
  status?: "creating" | "deleting" | "running";
}

interface IAddressListProps {
  rows: IAddressSpace[];
  onEdit: (rowData: IAddressSpace) => void;
  onDelete: (rowData: IAddressSpace) => void;
}

export const AddressSpaceLsit: React.FunctionComponent<IAddressListProps> = ({
  rows,
  onEdit,
  onDelete
}) => {
  //TODO: Add loading icon based on status
  const actionResolver = (rowData: IRowData) => {
    const originalData = rowData.originalData as IAddressSpace;
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

  const toTableCells = (row: IAddressSpace) => {
    const tableRow: IRowData = {
      cells: [
        {
          header: "name",
          title: (
            <>
              <Link to={`address-space/${row.name}/addresses`}>{row.name}</Link>
              <br />
              {row.nameSpace}
            </>
          )
        },
        // row.type,?
        { header: "type", title: 
        <><AddressSpaceIcon /><AddressSpaceType type={row.type} /></> },
        { title: <AddressSpaceStatus isReady={row.isReady} /> },
        row.creationTimestamp
      ],
      originalData: row
    };
    return tableRow;
  };
  const tableRows = rows.map(toTableCells);
  const tableColumns = ["Name/Name Space", "Type", "Status", "Time created"];

  return (
    <Table
      variant={TableVariant.compact}
      cells={tableColumns}
      rows={tableRows}
      actionResolver={actionResolver}
      aria-label="address space list">
      <TableHeader />
      <TableBody />
    </Table>
  );
};
