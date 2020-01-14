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
import { TypePlan } from "../../Common/TypePlan";
import { Messages } from "../../Common/Messages";
import { Error } from "../../Common/Error";
import useWindowDimensions from "src/Components/Common/WindowDimension";

export interface IAddress {
  name: string;
  namespace: string;
  type: string;
  planLabel: string;
  planValue: string;
  messagesIn: number;
  messagesOut: number;
  storedMessages: number;
  senders: number;
  receivers: number;
  shards: number;
  isReady: boolean;
  errorMessages?: string[];
  status?: string;
}

interface IAddressListProps {
  rowsData: IAddress[];
  onEdit: (rowData: IAddress) => void;
  onDelete: (rowData: IAddress) => void;
  sortBy?: ISortBy;
  onSort?: (_event: any, index: number, direction: string) => void;
}

export const AddressList: React.FunctionComponent<IAddressListProps> = ({
  rowsData,
  onEdit,
  onDelete,
  sortBy,
  onSort
}) => {
  const [tableRows, setTableRows] = React.useState<IRowData[]>([]);
  const { width } = useWindowDimensions();
  const actionResolver = (rowData: IRowData) => {
    const originalData = rowData.originalData as IAddress;
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
  };
  //TODO: Display error after the phase variable is exposed from backend.
  const toTableCells = (row: IAddress) => {
    if (row.isReady) {
      const tableRow: IRowData = {
        cells: [
          { title: <Link to={`addresses/${row.name}`}>{row.name}</Link> },
          { title: <TypePlan type={row.type} plan={row.planLabel} /> },
          {
            title: (
              <Messages
                count={row.messagesIn}
                column="MessagesIn"
                isReady={row.isReady}
              />
            )
          },
          {
            title: (
              <Messages
                count={row.messagesOut}
                column="MessagesOut"
                isReady={row.isReady}
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
    } else {
      const tableRow: IRowData = {
        cells: [
          { title: <Link to={`addresses/${row.name}`}>{row.name}</Link> },
          { title: <TypePlan type={row.type} plan={row.planLabel} /> },
          {
            title: row.errorMessages ? (
              <Error message={row.errorMessages[0]} type={row.status} />
            ) : (
              ""
            ),
            props: { colSpan: 6 }
          }
        ],
        originalData: row
      };
      return tableRow;
    }
  };
  useEffect(() => setTableRows(rowsData.map(toTableCells)), [rowsData]);
  const tableColumns = [
    { title: "Name", transforms: [sortable] },
    "Type/Plan",
    {
      title:
        width > 769 ? (
          <span style={{ display: "inline-flex" }}>
            Messages In
            <br />
            {`(over last 5 min)`}
          </span>
        ) : (
          "Message In"
        ),
      transforms: [sortable]
    },
    {
      title:
        width > 769 ? (
          <span style={{ display: "inline-flex" }}>
            Messages Out
            <br />
            {`(over last 5 min)`}
          </span>
        ) : (
          "Messages Out"
        ),
      transforms: [sortable]
    },
    { title: "Stored Messages", transforms: [sortable] },
    "Senders",
    "Receivers",
    "Shards"
  ];

  const onSelect = (
    event: React.MouseEvent,
    isSelected: boolean,
    rowIndex: number,
    rowData: IRowData,
    extraData: IExtraData
  ) => {
    let rows;
    if (rowIndex === -1) {
      rows = tableRows.map((oneRow: IRowData) => {
        oneRow.selected = isSelected;
        return oneRow;
      });
    } else {
      rows = [...tableRows];
      rows[rowIndex].selected = isSelected;
    }
    // setTableRows(rows);
  };

  return (
    <Table
      variant={TableVariant.compact}
      onSelect={onSelect}
      cells={tableColumns}
      rows={tableRows}
      actionResolver={actionResolver}
      aria-label="Address List"
      canSelectAll={true}
      sortBy={sortBy}
      onSort={onSort}
    >
      <TableHeader id="address-list-table-bodheader" />
      <TableBody />
    </Table>
  );
};
