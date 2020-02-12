/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import {
  IConnection,
  ConnectionList
} from "components/AddressSpace/Connection/ConnectionList";
import { EmptyConnection } from "components/AddressSpace/Connection/EmptyConnection";
import { getFilteredValue } from "components/common/ConnectionListFormatter";
import { IConnectionListResponse } from "types/ResponseTypes";
import { RETURN_ALL_CONECTION_LIST } from "queries";
import { ISortBy } from "@patternfly/react-table";
import { POLL_INTERVAL } from "constants/constants";

export interface IConnectionListPageProps {
  name?: string;
  namespace?: string;
  hostnames: string[];
  containerIds: string[];
  setTotalConnections: (total: number) => void;
  page: number;
  perPage: number;
  sortValue?: ISortBy;
  setSortValue: (value?: ISortBy) => void;
}

export const ConnectionsListPage: React.FunctionComponent<IConnectionListPageProps> = ({
  name,
  namespace,
  hostnames,
  containerIds,
  setTotalConnections,
  page,
  perPage,
  sortValue,
  setSortValue
}) => {
  const [sortBy, setSortBy] = React.useState<ISortBy>();
  if (sortValue && sortBy != sortValue) {
    setSortBy(sortValue);
  }
  let { loading, error, data } = useQuery<IConnectionListResponse>(
    RETURN_ALL_CONECTION_LIST(
      page,
      perPage,
      hostnames,
      containerIds,
      name,
      namespace,
      sortBy
    ),
    { pollInterval: POLL_INTERVAL }
  );

  if (error) {
    console.log(error);
  }
  // if (loading) return <Loading />;
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
        rows={connectionList ? connectionList : []}
        sortBy={sortBy}
        onSort={onSort}
      />
      {connections.total > 0 ? " " : <EmptyConnection />}
    </>
  );
};
