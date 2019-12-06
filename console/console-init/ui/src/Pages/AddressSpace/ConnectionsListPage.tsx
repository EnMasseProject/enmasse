import * as React from "react";
import { useQuery } from "@apollo/react-hooks";
import {
  IConnection,
  ConnectionList
} from "src/Components/AddressSpace/ConnectionList";
import { EmptyConnection } from "src/Components/Common/EmptyConnection";
import { PageSection, PageSectionVariants } from "@patternfly/react-core";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { IConnectionListResponse } from "src/Types/ResponseTypes";
import { RETURN_ALL_CONECTION_LIST } from "src/Queries/Queries";

export interface IConnectionListPageProps {
  name?: string;
  namespace?: string;
  addressSpaceType?: string;
  hosts: string[];
  containerIds: string[];
  setTotalConnections: (total: number) => void;
  page: number;
  perPage: number;
}

export const ConnectionsListPage: React.FunctionComponent<IConnectionListPageProps> = ({
  name,
  namespace,
  addressSpaceType,
  hosts,
  containerIds,
  setTotalConnections,
  page,
  perPage
}) => {
  let { loading, error, data } = useQuery<IConnectionListResponse>(
    RETURN_ALL_CONECTION_LIST(
      page,
      perPage,
      hosts,
      containerIds,
      name,
      namespace
    ),
    { pollInterval: 20000 }
  );

  if (error) console.log(error);
  if (loading) console.log("page is loading:", loading);
  // if (loading) return <Loading />;
  const { connections } = data || {
    connections: { Total: 0, Connections: [] }
  };

  console.log(connections);
  setTotalConnections(connections.Total);
  // connections.Total=0;
  // connections.Connections=[];
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
      status: "running"
    })
  );
  return (
    <>
      {connections.Total > 0 ? (
        <ConnectionList rows={connectionList ? connectionList : []} />
      ) : (
        <EmptyConnection />
      )}
    </>
  );
};
