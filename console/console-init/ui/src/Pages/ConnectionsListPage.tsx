import * as React from "react";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { Loading } from "use-patternfly";
import {
  IConnection,
  ConnectionList
} from "src/Components/AddressSpace/ConnectionList";
import { EmptyConnection } from "src/Components/Common/EmptyConnection";

interface IConnectionListResponse {
  connections: {
    Total: number;
    Connections: Array<{
      Metadata: {
        Name: string;
      };
      Spec: {
        Hostname: string;
        ContainerId: string;
        Protocol: string;
      };
    }>;
  };
}
export default function ConnectionsListPage() {
  const name="jupiter_as1",namespace="app1_ns";
  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
      connections(
        filter: "\`$.Spec.AddressSpace.Metadata.Name\` = '${name}' AND \`$.Spec.AddressSpace.Metadata.Namespace\` = '${namespace}'"
      ) {
        Total
        Connections {
          Metadata {
            Name
          }
          Spec {
            Hostname
            ContainerId
            Protocol
          }
        }
      }
    }`
  );

  let { loading, error, data } = useQuery<IConnectionListResponse>(
    ALL_CONECTION_LIST,
    { pollInterval: 5000 }
  );

  if(error) console.log(error);
  if (loading) return <Loading />;
  const { connections } = data || {
    connections: { Total: 0, Connections: [] }
  };

  console.log(connections);
  const connectionList: IConnection[] = connections.Connections.map(
    connection => ({
      hostname: connection.Spec.Hostname,
      containerId: connection.Spec.ContainerId,
      protocol: connection.Spec.Protocol,
      messagesIn: 0,
      messagesOut: 0,
      senders: 0,
      receivers: 0,
      status: "running"
    })
  );
  // console.log(connectionList);
  if (connections.Total === 0) {
    return <EmptyConnection />;
  }
  return (
    <ConnectionList rows={connectionList} />
  );
}
