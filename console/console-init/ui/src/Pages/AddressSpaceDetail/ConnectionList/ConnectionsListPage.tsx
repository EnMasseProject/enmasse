/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import {
  IConnection,
  ConnectionList
} from "src/Components/AddressSpace/Connection/ConnectionList";
import { EmptyConnection } from "src/Components/AddressSpace/Connection/EmptyConnection";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { IConnectionListResponse } from "src/Types/ResponseTypes";
import { RETURN_ALL_CONECTION_LIST } from "src/Queries/Queries";
import { ISortBy } from "@patternfly/react-table";

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
    { pollInterval: 20000 }
  );

  if (error) {
    console.log(error);
  }
  // if (loading) return <Loading />;
  const { connections } = data || {
    connections: { Total: 0, Connections: [] }
  };
  
  setTotalConnections(connections.Total);
  const connectionList: IConnection[] = connections.Connections.map(
    connection => ({
      hostname: connection.Spec.Hostname,
      containerId: connection.Spec.ContainerId,
      protocol: connection.Spec.Protocol,
      encrypted: connection.Spec.Encrypted,
      messagesIn: getFilteredValue(connection.Metrics, "enmasse_messages_in"),
      messagesOut: getFilteredValue(connection.Metrics, "enmasse_messages_out"),
      senders: getFilteredValue(connection.Metrics, "enmasse_senders"),
      receivers: getFilteredValue(connection.Metrics, "enmasse_receivers"),
      status: "running",
      name: connection.ObjectMeta.Name
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
      {connections.Total > 0 ? " " : <EmptyConnection />}
    </>
  );
};
