/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React, { useState } from "react";
import { useQuery } from "@apollo/react-hooks";
import { ISortBy } from "@patternfly/react-table";
import { RETURN_ALL_CONECTION_LIST } from "graphql-module/queries";
import {
  IConnection,
  ConnectionList,
  EmptyConnection
} from "modules/connection/components";
import { getFilteredValue } from "utils";
import { IConnectionListResponse } from "schema/ResponseTypes";
import { POLL_INTERVAL, FetchPolicy } from "constant";

export interface IConnectionProps {
  name?: string;
  namespace?: string;
  hostnames: string[];
  containerIds: string[];
  setTotalConnections: (total: number) => void;
  page: number;
  perPage: number;
  sortValue?: ISortBy;
  setSortValue: (value?: ISortBy) => void;
  addressSpaceType?: string;
}

export const ConnectionContainer: React.FunctionComponent<IConnectionProps> = ({
  name,
  namespace,
  hostnames,
  containerIds,
  setTotalConnections,
  page,
  perPage,
  sortValue,
  setSortValue,
  addressSpaceType
}) => {
  const [sortBy, setSortBy] = useState<ISortBy>();
  if (sortValue && sortBy !== sortValue) {
    setSortBy(sortValue);
  }
  let { data } = useQuery<IConnectionListResponse>(
    RETURN_ALL_CONECTION_LIST(
      page,
      perPage,
      hostnames,
      containerIds,
      name,
      namespace,
      sortBy
    ),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  const { connections } = data || {
    connections: { total: 0, connections: [] }
  };
  setTotalConnections(connections.total);
  const connectionList: IConnection[] = connections.connections.map(
    connection => ({
      hostname: connection.spec.hostname,
      containerId: connection.spec.containerId,
      protocol: connection.spec.protocol,
      encrypted: connection.spec.encrypted,
      messageIn: getFilteredValue(connection.metrics, "enmasse_messages_in"),
      messageOut: getFilteredValue(connection.metrics, "enmasse_messages_out"),
      senders: getFilteredValue(connection.metrics, "enmasse_senders"),
      receivers: getFilteredValue(connection.metrics, "enmasse_receivers"),
      status: "running",
      name: connection.metadata.name,
      creationTimestamp: connection.metadata.creationTimestamp
    })
  );

  const onSort = (_event: any, index: any, direction: any) => {
    setSortBy({ index: index, direction: direction });
    setSortValue({ index: index, direction: direction });
  };
  return (
    <>
      <ConnectionList
        rows={connectionList}
        addressSpaceType={addressSpaceType}
        sortBy={sortBy}
        onSort={onSort}
      />
      {connections.total > 0 ? "" : <EmptyConnection />}
    </>
  );
};
