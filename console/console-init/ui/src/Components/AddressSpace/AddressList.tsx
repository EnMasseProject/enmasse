import React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData,
  sortable
} from "@patternfly/react-table";
import { Link } from "react-router-dom";
import { TypePlan } from "../Common/TypePlan";
import {Messages} from "../Common/Messages";

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
  isReady: boolean;
  errorMessages?: string[];
  status?: "creating" | "deleting" | "running";
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
          { title: <Link to={`address/${row.name}`}>{row.name}</Link> },
          { title: <TypePlan type={row.type} plan={row.plan} /> },
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
          { title: <Link to={`address/${row.name}`}>{row.name}</Link> },
          { title: <TypePlan type={row.type} plan={row.plan} /> },
          {
            title: row.errorMessages ? row.errorMessages[0] : "",
            props: { colSpan: 6 }
          }
        ]
      };
      return tableRow;
    }
  };
  const tableRows = rows.map(toTableCells);
  const tableColumns = [
    "Name",
    "Type/Plan",
    { title: "Messages In", transforms:[sortable] },
    { title: "Messages Out", transforms:[sortable]  },
    { title: "Stored Messages", transforms:[sortable]  },
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
      aria-label="Address List">
      <TableHeader />
      <TableBody />
    </Table>
  );
};
