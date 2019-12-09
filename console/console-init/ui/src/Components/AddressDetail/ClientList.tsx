import * as React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData,
  sortable
} from "@patternfly/react-table";
import { ExternalLinkAltIcon } from "@patternfly/react-icons";
import { Link } from "react-router-dom";
import { Tooltip, TooltipPosition } from "@patternfly/react-core";

interface IClientListProps {
  rows: IClient[];
}

export interface IClient {
  role: string;
  containerId: string;
  name: string;
  deliveryRate?: number;
  backlog?: number;
  connectionName?: string;
  addressSpaceName?: string;
  addressSpaceNamespace?: string;
  addressSpaceType?: string;
}

export const ClientList: React.FunctionComponent<IClientListProps> = ({
  rows
}) => {
  const toTableCells = (row: IClient) => {
    const tableRow: IRowData = {
      cells: [
        row.role,
        row.containerId,
        {
          title: (
            <>
              {row.name}{" "}
              <Link
                to={`/address-spaces/${row.addressSpaceNamespace}/${row.addressSpaceName}/${row.addressSpaceType}/connections/${row.connectionName}`}
              >
                <Tooltip position={TooltipPosition.top} content={<div>Go to the link</div>}>
                <ExternalLinkAltIcon />
                </Tooltip>
              </Link>
            </>
          )
        },
        row.deliveryRate ? row.deliveryRate : "-",
        row.backlog ? row.backlog : "-"
      ],
      originalData: row
    };
    return tableRow;
  };
  const tableRows = rows.map(toTableCells);
  const tableColumns = [
    "Role",
    "Container ID",
    "Name",
    { title: "Delivery Rate", transforms: [sortable] },
    { title: "Backlog", transforms: [sortable] }
  ];

  return (
    <Table
      variant={TableVariant.compact}
      cells={tableColumns}
      rows={tableRows}
      aria-label="client list"
    >
      <TableHeader />
      <TableBody />
    </Table>
  );
};
