/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Table,
  TableHeader,
  TableVariant,
  TableBody,
  IRowData
} from "@patternfly/react-table";
import { StyleForTable } from "modules/address-space";
import { css } from "@patternfly/react-styles";
import { IEndPoint } from "modules/endpoints/EndpointPage";

interface IEndPointListProps {
  endpoints: IEndPoint[];
}
const EndPointList: React.FunctionComponent<IEndPointListProps> = ({
  endpoints
}) => {
  const tableColumns = ["Name", "Type", "Host", "Ports", ""];
  const toTableCells = (row: IEndPoint) => {
    const tableRow: IRowData = {
      cells: [
        row.name,
        row.type,
        row.host,
        {
          title: row.ports.map(port => (
            <>
              {port.protocol}
              <br />
            </>
          ))
        },

        {
          title: row.ports.map(port => (
            <>
              {port.portNumber}
              <br />
            </>
          ))
        }
      ]
    };
    return tableRow;
  };
  const tableRows = endpoints.map(toTableCells);
  return (
    <div className={css(StyleForTable.scroll_overflow)}>
      <Table cells={tableColumns} rows={tableRows} aria-label="Endpoint List">
        <TableHeader id="endpoint-list-table-bodheader" />
        <TableBody />
      </Table>
    </div>
  );
};

export { EndPointList };
