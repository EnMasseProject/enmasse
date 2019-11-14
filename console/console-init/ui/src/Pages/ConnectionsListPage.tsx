import * as React from "react";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { Loading } from "use-patternfly";
import {
  IConnection,
  ConnectionList
} from "src/Components/AddressSpace/ConnectionList";
import { EmptyConnection } from "src/Components/Common/EmptyConnection";
import { IMetrics } from "./AddressesListPage";
import { useParams } from "react-router";
import { Pagination, PageSection, PageSectionVariants } from "@patternfly/react-core";
import { Header } from "@patternfly/react-table/dist/js/components/Table/base";
import { StyleSheet } from "@patternfly/react-styles";

const styles = StyleSheet.create({
  header_bottom_border : { 
    borderBottom: "0.01em",
    borderRightColor: "lightgrey"
  }
})
interface IConnectionListResponse {
  connections: {
    Total: number;
    Connections: Array<{
      ObjectMeta: {
        Name: string;
      };
      Spec: {
        Hostname: string;
        ContainerId: string;
        Protocol: string;
      };
      Metrics: Array<{
        Name: string;
        Type: string;
        Value: number;
        Units: string;
      }>;
    }>;
  };
}

const return_ALL_CONECTION_LIST = (name?: string, namespace?: string) => {
  const ALL_CONECTION_LIST = gql(
    `query all_connections_for_addressspace_view {
    connections(
      filter: "\`$.Spec.AddressSpace.ObjectMeta.Name\` = '${name}' AND \`$.Spec.AddressSpace.ObjectMeta.Namespace\` = '${namespace}'"
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
    return_ALL_CONECTION_LIST(name, namespace),
    { pollInterval: 5000 }
  );

  if (error) console.log(error);
  if (loading) return <Loading />;
  const { connections } = data || {
    connections: { Total: 0, Connections: [] }
  };

  const getFilteredValue = (object: IMetrics[], value: string) => {
    const filtered = object.filter(obj => obj.Name === value);
    if (filtered.length > 0) {
      return filtered[0].Value;
    }
    return 0;
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
          <div className={styles.header_bottom_border}>
          <Pagination
            itemCount={523}
            perPage={10}
            page={1}
            onSetPage={() => {}}
            widgetId="pagination-options-menu-top"
            onPerPageSelect={() => {}}
          />
          </div>
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
