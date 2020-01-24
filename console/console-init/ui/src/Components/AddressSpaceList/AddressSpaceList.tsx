/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useEffect } from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData,
  sortable,
  IExtraData,
  ISortBy
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
  onSelectAddressSpace: (data: IAddressSpace, isSelected: boolean) => void;
  onSelectAllAddressSpace: (dataList:IAddressSpace[], isSelected: boolean) => void;
  onEdit: (rowData: IAddressSpace) => void;
  onDelete: (rowData: IAddressSpace) => void;
  sortBy?: ISortBy;
  onSort?: (_event: any, index: number, direction: string) => void;
}

export const AddressSpaceList: React.FunctionComponent<IAddressListProps> = ({
  rows,
  onSelectAddressSpace,
  onSelectAllAddressSpace,
  onEdit,
  onDelete,
  sortBy,
  onSort
}) => {
  //TODO: Add loading icon based on status
  const [tableRows, setTableRows] = React.useState<IRowData[]>([]);

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
    const oldRowData = tableRows.filter(r=>r.originalData.name===row.name);
    let selected;
    if(oldRowData && oldRowData.length>0) {
      selected= oldRowData[0].selected;
    }
    const tableRow: IRowData = {
      selected: selected,
      cells: [
        {
          header: "name",
          title: (
            <>
              <Link
                to={`address-spaces/${row.nameSpace}/${row.name}/${row.type}/addresses`}>
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
  useEffect(() => setTableRows(rows.map(toTableCells)), [rows]);
  const tableColumns = [
    { 
      title: (
        <span style={{ display: "inline-flex"}}>
          <div>
            Name
            <br />
            <small>Namespace</small>
          </div>
        </span>
      ),
      transforms: [sortable]
    },
    "Type",
    "Status",
    "Time created"
  ];

  const onSelect = async (
    event: React.MouseEvent,
    isSelected: boolean,
    rowIndex: number,
    rowData: IRowData,
    extraData: IExtraData
  ) => {

    let rows;
    if (rowIndex === -1) {
      rows = tableRows.map(a=>{
        const data =a;
        data.selected=isSelected
        return data;
      })
      onSelectAllAddressSpace(rows.map(row=>row.originalData),isSelected);
    } else {
      rows = [...tableRows];
      rows[rowIndex].selected = isSelected;
      onSelectAddressSpace(rows[rowIndex].originalData,isSelected);
    }
    setTableRows(rows);
  };

  return (
    <Table
      variant={TableVariant.compact}
      onSelect={onSelect}
      cells={tableColumns}
      rows={tableRows}
      actionResolver={actionResolver}
      aria-label="address space list"
      onSort={onSort}
      sortBy={sortBy}>
      <TableHeader id="aslist-table-header" />
      <TableBody />
    </Table>
  );
};
