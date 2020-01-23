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
  onEdit: (rowData: IAddressSpace) => void;
  onDelete: (rowData: IAddressSpace) => void;
  sortBy?: ISortBy;
  onSort?: (_event: any, index: number, direction: string) => void;
  setSelectedAddressSpaces: (values: IRowData[]) => void;
}

export interface IAddressSpaceNameAndNameSpace {
  name: string;
  namespace: string;
}

export const AddressSpaceList: React.FunctionComponent<IAddressListProps> = ({
  rows,
  onEdit,
  onDelete,
  sortBy,
  onSort,
  setSelectedAddressSpaces
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
    const dataPresentInTableRow = tableRows.filter(
      data => data.originalData.name === row.name
    );
    let isSelected;
    if (dataPresentInTableRow && dataPresentInTableRow.length > 0) {
      if (dataPresentInTableRow[0].selected) {
        isSelected = dataPresentInTableRow[0].selected;
      }
    }
    return getRowData(row, isSelected);
  };
  const getRowData = (row: IAddressSpace, isSelected?: boolean) => {
    const tableRow: IRowData = {
      selected: isSelected,
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

  const matchAndUpdateRow = (rowData: IAddressSpace) => {
    const dataFromOldTableRows = tableRows.filter(
      row => row.originalData.name === rowData.name
    );
    if (dataFromOldTableRows && dataFromOldTableRows.length > 0) {
      return getRowData(rowData, dataFromOldTableRows[0].selected);
    }
    return getRowData(rowData, undefined);
  };

  const onSelect = async (
    event: React.MouseEvent,
    isSelected: boolean,
    rowIndex: number,
    rowData: IRowData,
    extraData: IExtraData
  ) => {
    let rows;
    if (rowIndex === -1) {
      rows = tableRows.map(oneRow => {
        oneRow.selected = isSelected;
        return oneRow;
      });
    } else {
      rows = [...tableRows];
      rows[rowIndex].selected = isSelected;
    }
    setTableRows(rows)
    setSelectedAddressSpaces(rows);
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
