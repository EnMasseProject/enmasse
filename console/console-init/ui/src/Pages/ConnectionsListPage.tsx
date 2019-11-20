import * as React from "react";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { Loading } from "use-patternfly";
import {
  IConnection,
  ConnectionList
} from "src/Components/AddressSpace/ConnectionList";
import { EmptyConnection } from "src/Components/Common/EmptyConnection";
import { useParams } from "react-router";
import {
  Pagination,
  PageSection,
  PageSectionVariants
} from "@patternfly/react-core";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { IConnectionListResponse } from "src/Types/ResponseTypes";

const RETURN_ALL_CONECTION_LIST = (name?: string, namespace?: string) => {
  let filter = "";
  if (name) {
    filter += "`$.Spec.AddressSpace.ObjectMeta.Name` = '" + name + "'";
  }
  if (namespace) {
    filter +=
      " AND `$.Spec.AddressSpace.ObjectMeta.Namespace` = '" + namespace + "'";
  }
  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
    connections(
      filter: "${filter}"
    ) {
      Total
      Connections {
        ObjectMeta {
          Name
        }
        Spec {
          Hostname
          ContainerId
          Protocol
        }
        Metrics {
          Name
          Type
          Value
          Units
        }
      }
    }
  }`
  );
  return ALL_CONECTION_LIST;
};
export default function ConnectionsListPage() {
  const { name, namespace } = useParams();
  let { loading, error, data } = useQuery<IConnectionListResponse>(
    RETURN_ALL_CONECTION_LIST(name, namespace),
    { pollInterval: 5000 }
  );

  if (error) console.log(error);
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
      messagesIn: getFilteredValue(connection.Metrics, "enmasse_messages_in"),
      messagesOut: getFilteredValue(connection.Metrics, "enmasse_messages_out"),
      senders: getFilteredValue(connection.Metrics, "enmasse_senders"),
      receivers: getFilteredValue(connection.Metrics, "enmasse_receivers"),
      status: "running"
    })
  );
  return (
    <>
      {connections.Total === 0 ? (
        <EmptyConnection />
      ) : (
        <PageSection variant={PageSectionVariants.light}>
          <Pagination
            itemCount={523}
            perPage={10}
            page={1}
            onSetPage={() => {}}
            widgetId="pagination-options-menu-top"
            onPerPageSelect={() => {}}
          />
          <ConnectionList rows={connectionList} />{" "}
          <Pagination
            itemCount={523}
            perPage={10}
            page={1}
            onSetPage={() => {}}
            widgetId="pagination-options-menu-top"
            onPerPageSelect={() => {}}
          />
        </PageSection>
      )}
    </>
  );
}
