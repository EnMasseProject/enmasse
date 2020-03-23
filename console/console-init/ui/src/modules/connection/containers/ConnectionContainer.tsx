/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useQuery } from "@apollo/react-hooks";
import { RETURN_ALL_CONECTION_LIST } from "graphql-module/queries";
import { ISortBy } from "@patternfly/react-table";
import {
  IConnection,
  ConnectionList
} from "modules/connection/components/ConnectionList/ConnectionList";
import { EmptyConnection } from "modules/connection/components/EmptyConnection/EmptyConnection";
import { getFilteredValue } from "components/common/ConnectionListFormatter";
import { IConnectionListResponse } from "types/ResponseTypes";
import { POLL_INTERVAL, FetchPolicy } from "constants/constants";

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
  const [sortBy, setSortBy] = React.useState<ISortBy>();
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
      {connections.total > 0 ? (
        <ConnectionList
          rows={connectionList ? connectionList : []}
          addressSpaceType={addressSpaceType}
          sortBy={sortBy}
          onSort={onSort}
        />
      ) : (
        <EmptyConnection />
      )}
    </>
  );
};
