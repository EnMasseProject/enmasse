import * as React from "react";
import { ConnectionDetailHeader, IConnectionHeaderDetailProps } from "src/Components/ConnectionDetail/ConnectionDetailHeader";
import { PageSection } from "@patternfly/react-core";
import {
  ConnectionList,
  IConnection
} from "src/Components/AddressSpace/ConnectionList";
import gql from "graphql-tag";
import { useQuery } from "@apollo/react-hooks";
import { useParams } from "react-router";
import { Loading } from "use-patternfly";
import { IMetrics } from "./AddressesListPage";
import { ILink, LinkList } from "src/Components/LinkList";

interface IConnectionDetailResponse {
  connections: {
    Total: number;
    Connections: Array<{
      ObjectMeta: {
        Name: string;
        Namespace: string;
        CreationTimeStamp: string;
        ResourceVersion:string;
      };
      Spec: {
        Hostname:string,
        ContainerId:string,
        Protocol:string,
      };
      Metrics:Array<{
        Name:string,
        Type:string,
        Value:number,
        Units:string,
      }>;
      Links:{
        Total:number;
        Links:Array<{
          ObjectMeta:{
            Name:string;
            Namespace:string;
          };
          Spec: {
            Role:string;
          };
          Metrics:Array<{
            Name:string;
            Type:string;
            Value:number;
            Units:string;
          }>;
        }>;
      }
    }>;
  };
}
const return_CONNECTION_DETAIL = (
  addressSpaceName?: string,
  addressSpaceNameSpcae?: string,
  connectionName?: string
) => {
  const CONNECTION_DETAIL = gql`
  query single_connections {
    connections(
      filter: "\`$.Spec.AddressSpace.ObjectMeta.Name\` = '${addressSpaceName}' AND \`$.Spec.AddressSpace.ObjectMeta.Namespace\` = '${addressSpaceNameSpcae}' AND \`$.ObjectMeta.Name\` = '${connectionName}'"
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
        Metrics{
          Name
          Type
          Value
          Units
        }
        Links{
          Total
          Links{
            ObjectMeta{
              Name
              Namespace
            }
            Spec {
              Role
            }
            Metrics{
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

const getFilteredValue = (object: IMetrics[], value: string) => {
  const filtered = object.filter(obj => obj.Name === value);
  if (filtered.length > 0) {
    return filtered[0].Value;
  }
  return 0;
};

export default function ConnectionDetailPage() {
  const {name,namespace,connectionname,connectionnamespace} = useParams();
  const{loading,data} = useQuery<IConnectionDetailResponse>(
    return_CONNECTION_DETAIL(name||"",namespace||"",connectionname||""),
    {pollInterval:5000}
  )
  if(loading) return <Loading/>
  const { connections } = data || {
    connections: { Total: 0, Connections: [] }
  };
  const connection = connections.Connections[0];
  console.log(connection)
  const connectionDetail:IConnectionHeaderDetailProps ={
    hostname:connection.ObjectMeta.Name,
    containerId:connection.ObjectMeta.Namespace,
    version:connection.ObjectMeta.ResourceVersion,
    protocol:connection.Spec.Protocol.toUpperCase(),
    messagesIn:getFilteredValue(connection.Metrics, "enmasse_messages_in"),
    messagesOut:getFilteredValue(connection.Metrics, "enmasse_messages_out"),
    //Change
    os:"Mac OS X 10.12.6,x86_64",
    platform:connection.Spec.ContainerId,
    //Change
    product:"QpidJMS"
  }

  const linkRows:ILink[] = connection.Links.Links.map(link=>({
    name:link.ObjectMeta.Name,
    role:link.Spec.Role,
    address:link.ObjectMeta.Namespace,
    deliveries:getFilteredValue(link.Metrics,"enmasse_deliveries"),
    rejected:getFilteredValue(link.Metrics,"enmasse_rejected"),
    released:getFilteredValue(link.Metrics,"enmasse_released"),
    modified:getFilteredValue(link.Metrics,"enmasse_modified"),
    presettled:getFilteredValue(link.Metrics,"enmasse_presettled"),
    undelivered:getFilteredValue(link.Metrics,"enmasse_undelivered"),
  }));
  return (
    <>
      <ConnectionDetailHeader {...connectionDetail}/>
      <PageSection>
        <LinkList rows={linkRows} />
      </PageSection>
    </>
  );
}
