/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import {
  Table,
  TableHeader,
  TableBody,
  IRowData,
  ISortBy
} from "@patternfly/react-table";
import { IEndPoint } from "modules/endpoints/EndpointPage";
import { EmptyEndpoints } from "../EmptyEndpoints";

interface IEndPointListProps {
  endpoints: IEndPoint[];
  sortBy?: ISortBy;
}

const EndPointList: React.FunctionComponent<IEndPointListProps> = ({
  endpoints,
  sortBy
}) => {
  const tableColumns = [
    { title: "Name", id: "th-name", key: "th-name" },
    { title: "Type", id: "th-type", key: "th-type" },
    { title: "Host", id: "th-host", key: "th-host" },
    { title: "Ports", id: "th-ports", key: "th-ports" },
    { title: "", id: "th-ports-number", key: "th-ports-number" }
  ];
  const toTableCells = (row: IEndPoint) => {
    const { name, type, host, ports } = row;
    const tableRow: IRowData = {
      cells: [
        {
          title: name,
          id: `row-name-${name && name}`,
          key: `row-name-${name && name}`
        },
        {
          title: type,
          id: `row-type-${type && type}`,
          key: `row-type-${type && type}`
        },
        {
          title: host,
          id: `row-host-${host && host}`,
          key: `row-host-${host && host}`
        },
        {
          title:
            ports &&
            ports.map((port, index) => (
              <div key={`${port.name}-${index}`}>
                {port.name?.toString()?.toUpperCase()}
                <br />
              </div>
            )),
          id: `row-ports-name-${ports && ports[0]?.name}`,
          key: `row-ports-name-${ports && ports[0]?.name}`
        },

        {
          title:
            ports &&
            ports.map((port, index) => (
              <div key={`${port?.port}-${index}`}>
                {port.port}
                <br />
              </div>
            )),
          id: `row-ports-port-${ports && ports[0]?.port}`,
          key: `row-ports-port-${ports && ports[0]?.port}`
        }
      ],
      key: `table-row-${name}`
    };
    return tableRow;
  };
  const tableRows = endpoints.map(toTableCells);
  return (
    <>
      {endpoints && endpoints.length > 0 ? (
        <Table
          cells={tableColumns}
          rows={tableRows}
          aria-label="Endpoint List"
          sortBy={sortBy}
        >
          <TableHeader id="endpoint-list-table-bodheader" />
          <TableBody />
        </Table>
      ) : (
        <EmptyEndpoints />
      )}
    </>
  );
};

export { EndPointList };
