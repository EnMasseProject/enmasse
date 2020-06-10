/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Table,
  TableVariant,
  TableHeader,
  TableBody,
  IRowData,
  sortable,
  ISortBy
} from "@patternfly/react-table";
import { ExternalLinkAltIcon } from "@patternfly/react-icons";
import { css } from "@patternfly/react-styles";
import { Link } from "react-router-dom";
import { Tooltip, TooltipPosition } from "@patternfly/react-core";
import { StyleForTable } from "modules/project";

interface IAddressLinksProps {
  rows: IAddressLink[];
  sortBy?: ISortBy;
  onSort?: (_event: any, index: number, direction: string) => void;
}

export interface IAddressLink {
  role: string;
  containerId: string;
  name: string;
  deliveryRate?: number | string;
  backlog?: number | string;
  connectionName?: string;
  addressSpaceName?: string;
  addressSpaceNamespace?: string;
  addressSpaceType?: string;
}

export const AddressLinks: React.FunctionComponent<IAddressLinksProps> = ({
  rows,
  onSort,
  sortBy
}) => {
  const toTableCells = (row: IAddressLink) => {
    const tableRow: IRowData = {
      cells: [
        row.role,
        row.containerId,
        {
          title: (
            <>
              {row.name}{" "}
              <Link
                to={`/messaging-projects/${row.addressSpaceNamespace}/${row.addressSpaceName}/${row.addressSpaceType}/connections/${row.connectionName}`}
              >
                <Tooltip
                  position={TooltipPosition.top}
                  content={<div>Go to the link</div>}
                >
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
    { title: "Name", transforms: [sortable] },
    { title: "Delivery Rate", transforms: [sortable] },
    { title: "Backlog", transforms: [sortable] }
  ];

  return (
    <div className={css(StyleForTable.scroll_overflow)}>
      <Table
        variant={TableVariant.compact}
        cells={tableColumns}
        rows={tableRows}
        aria-label="client list"
        onSort={onSort}
        sortBy={sortBy}
      >
        <TableHeader />
        <TableBody />
      </Table>
    </div>
  );
};
