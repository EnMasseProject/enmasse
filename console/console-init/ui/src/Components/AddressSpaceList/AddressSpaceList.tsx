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
import { FormatDistance } from "use-patternfly";

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

export const AddressSpaceList: React.FunctionComponent<IAddressListProps> = ({
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
              <Link
                to={`address-spaces/${row.nameSpace}/${row.name}/${row.type}/addresses`}
              >
                {row.name}
              </Link>
              <br />
              {row.nameSpace}
            </>
          )
        },
        {
          header: "type",
          title: (
            <>
              <AddressSpaceIcon />
              <AddressSpaceType type={row.type} />
            </>
          )
        },
        { title: <AddressSpaceStatus isReady={row.isReady} /> },
        {
          title: (
            <>
              <FormatDistance date={row.creationTimestamp} /> ago
            </>
          )
        }
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
      onSelect={() => {}}
      cells={tableColumns}
      rows={tableRows}
      actionResolver={actionResolver}
      aria-label="address space list"
    >
      <TableHeader id="aslist-table-header" />
      <TableBody />
    </Table>
  );
};
