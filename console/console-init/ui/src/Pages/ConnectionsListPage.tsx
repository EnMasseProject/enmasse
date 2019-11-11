import * as React from 'react';
import { Link } from 'react-router-dom';
import { PageSection, PageSectionVariants } from '@patternfly/react-core';
import gql from 'graphql-tag';
import { useQuery } from '@apollo/react-hooks';
import { Loading } from 'use-patternfly';
import { IConnection, ConnectionList } from 'src/Components/AddressSpace/ConnectionList';
import { EmptyConnection } from 'src/Components/Common/EmptyConnection';

interface IConnectionListResponse {
    connections:{
        Total:number;
        Connections :Array<{
            Metadata : {
                Name:string
            };
            Spec :{
                Hostname:string;
                ContainerId:string;
                Protocol:string;
            };
        }>;
    }
}
export default function ConnectionsListPage() {
    const ALL_CONECTION_LIST=gql(
        `query all_connections_for_addressspace_view {
            connections(
              filter: "\`$.Spec.AddressSpace.Metadata.Name\` = 'jupiter_as1' AND \`$.Spec.AddressSpace.Metadata.Namespace\` = 'app1_ns'"
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
    )

    const {loading, data} = useQuery<IConnectionListResponse>(ALL_CONECTION_LIST);

    if(loading) return <Loading />
    
    // console.log(data);
    const { connections } = data || {
        connections: { Total: 0, Connections: [] }
      };
      const connectionList: IConnection[] = connections.Connections.map(connection => ({
        hostname:connection.Spec.Hostname,
        containerId:connection.Spec.ContainerId,
        protocol:connection.Spec.Protocol,
        messagesIn:0,
        messagesOut:0,
        senders:0,
        receivers:0,
        status:"running"
      }));
      if (connections.Total==0) {
          return <EmptyConnection/>
      }
    return(
        <ConnectionList rows={connectionList}/>
        // <PageSection variant={PageSectionVariants.light}>
        // {console.log("vconn")}
        // <h1>Connections List Page</h1>
        // <Link to="/address-space/456/connection/123">Connection Detial Page</Link>
        // </PageSection>
    )
}