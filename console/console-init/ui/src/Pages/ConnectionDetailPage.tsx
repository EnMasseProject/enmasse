import * as React from "react";
import {
  ConnectionDetailHeader,
  IConnectionHeaderDetailProps
} from "src/Components/ConnectionDetail/ConnectionDetailHeader";
import { PageSection } from "@patternfly/react-core";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { useParams } from "react-router";
import { Loading } from "use-patternfly";
import { ILink, LinkList } from "src/Components/LinkList";
import { getFilteredValue } from "src/Components/Common/ConnectionListFormatter";
import { IConnectionDetailResponse } from "src/Types/ResponseTypes";

const RETURN_CONNECTION_DETAIL = (
  addressSpaceName?: string,
  addressSpaceNameSpcae?: string,
  connectionName?: string
) => {
  let filter = "";
  if (addressSpaceName) {
    filter +=
      "`$.Spec.AddressSpace.ObjectMeta.Name` = '" + addressSpaceName + "' AND ";
  }
  if (addressSpaceNameSpcae) {
    filter +=
      "`$.Spec.AddressSpace.ObjectMeta.Namespace` = '" +
      addressSpaceNameSpcae +
      "' AND ";
  }
  if (connectionName) {
    filter += "`$.ObjectMeta.Name` = '" + connectionName + "'";
  }

  const CONNECTION_DETAIL = gql`
  query single_connections {
    connections(
      filter: "${filter}"
    ) {
      Total
      Connections {
        ObjectMeta {
          Name
          Namespace
          CreationTimestamp
          ResourceVersion
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
        Links {
          Total
          Links {
            ObjectMeta {
              Name
              Namespace
            }
            Spec {
              Role
            }
            Metrics {
              Name
              Type
              Value
              Units
            }
          }
        }
      }
    }
  }
  `;
  return CONNECTION_DETAIL;
};

export default function ConnectionDetailPage() {
  const { name, namespace, connectionname } = useParams();
  const { loading, error, data } = useQuery<IConnectionDetailResponse>(
    RETURN_CONNECTION_DETAIL(name || "", namespace || "", connectionname || ""),
    { pollInterval: 5000 }
  );
  if (loading) return <Loading />;
  if (error) {
    console.log(error);
    return <Loading />;
  }
  const { connections } = data || {
    connections: { Total: 0, Connections: [] }
  };
  const connection = connections.Connections[0];
  console.log(connection);
  const connectionDetail: IConnectionHeaderDetailProps = {
    hostname: connection.ObjectMeta.Name,
    containerId: connection.ObjectMeta.Namespace,
    version: connection.ObjectMeta.ResourceVersion,
    protocol: connection.Spec.Protocol.toUpperCase(),
    messagesIn: getFilteredValue(connection.Metrics, "enmasse_messages_in"),
    messagesOut: getFilteredValue(connection.Metrics, "enmasse_messages_out"),
    //Change
    os: "Mac OS X 10.12.6,x86_64",
    platform: connection.Spec.ContainerId,
    //Change
    product: "QpidJMS"
  };

  const linkRows: ILink[] = connection.Links.Links.map(link => ({
    name: link.ObjectMeta.Name,
    role: link.Spec.Role,
    address: link.ObjectMeta.Namespace,
    deliveries: getFilteredValue(link.Metrics, "enmasse_deliveries"),
    rejected: getFilteredValue(link.Metrics, "enmasse_rejected"),
    released: getFilteredValue(link.Metrics, "enmasse_released"),
    modified: getFilteredValue(link.Metrics, "enmasse_modified"),
    presettled: getFilteredValue(link.Metrics, "enmasse_presettled"),
    undelivered: getFilteredValue(link.Metrics, "enmasse_undelivered")
  }));
  return (
    <>
      <ConnectionDetailHeader {...connectionDetail} />
      <PageSection>
        <LinkList rows={linkRows} />
      </PageSection>
    </>
  );
}
