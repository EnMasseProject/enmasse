import React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData,
  sortable,
  IExtraData
} from "@patternfly/react-table";
import { Link } from "react-router-dom";
import { TypePlan } from "../Common/TypePlan";
import { Messages } from "../Common/Messages";
import { Error } from "../Common/Error";

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
  status?: string;
}

interface IAddressListProps {
  rowsData: IAddress[];
  onEdit: (rowData: IAddress) => void;
  onDelete: (rowData: IAddress) => void;
  // onCheckboxEdit: (rowData: IRowData[]) => void;
  // rows: IRowData[];
}

export const AddressList: React.FunctionComponent<IAddressListProps> = ({
  rowsData,
  onEdit,
  onDelete
  // onCheckboxEdit,
  // rows
}) => {
  //TODO: Add loading icon based on status
  // const [tableRows, setTableRows] = useState([]);

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
          { title: <Link to={`addresses/${row.name}`}>{row.name}</Link> },
          { title: <TypePlan type={row.type} plan={row.plan} /> },
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
  let tableRows = rowsData.map(toTableCells);
  const tableColumns = [
    "Name",
    "Type/Plan",
    {
      title: (
        <span style={{ display: "inline-flex" }}>
          Messages In
          <br />
          {`(over last 5 min)`}
        </span>
      ),
      transforms: [sortable]
    },
    {
      title: (
        <span style={{ display: "inline-flex" }}>
          Messages Out
          <br />
          {`(over last 5 min)`}
        </span>
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
      rows = tableRows.map(oneRow => {
        oneRow.selected = isSelected;
        return oneRow;
      });
    } else {
      rows = [...tableRows];
      rows[rowIndex].selected = isSelected;
    }
    tableRows = rows;
    // onCheckboxEdit(rows);
  };
  // console.log(rows);
  return (
    <Table
      variant={TableVariant.compact}
      onSelect={onSelect}
      cells={tableColumns}
      rows={tableRows}
      actionResolver={actionResolver}
      aria-label="Address List"
      canSelectAll={true}
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};
