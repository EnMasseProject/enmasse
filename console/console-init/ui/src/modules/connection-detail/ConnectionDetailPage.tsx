/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useParams } from "react-router";
import { Link } from "react-router-dom";
import { useQuery } from "@apollo/react-hooks";
import {
  PageSection,
  Breadcrumb,
  BreadcrumbItem
} from "@patternfly/react-core";
import {
  Loading,
  useA11yRouteChange,
  useBreadcrumb,
  useDocumentTitle
} from "use-patternfly";
import {
  ConnectionDetailHeader,
  IConnectionHeaderDetailProps
} from "./components/ConnectionDetailHeader";
import { getFilteredValue } from "utils";
import { IConnectionDetailResponse } from "schema/ResponseTypes";
import { RETURN_CONNECTION_DETAIL } from "graphql-module/queries";
import { ConnectionLinksWithToolbar } from "modules/connection-detail/components";
import { POLL_INTERVAL, FetchPolicy } from "constant";
import { NoDataFound } from "components";
import { getProductFilteredValue, getSplitValue } from "./utils";

export default function ConnectionDetailPage() {
  const { name, namespace, type, connectionname } = useParams();
  useDocumentTitle("Connection Details");
  useA11yRouteChange();

  const breadcrumb = React.useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link id="connection-detail-page-home-link" to={"/"}>
            Home
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem>
          <Link
            id="connection-detail-connections-link"
            to={`/messaging-projects/${namespace}/${name}/${type}/connections`}
          >
            {name}
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem isActive={true}>Connection</BreadcrumbItem>
      </Breadcrumb>
    ),
    [name, namespace, type]
  );

  useBreadcrumb(breadcrumb);

  const { loading, data } = useQuery<IConnectionDetailResponse>(
    RETURN_CONNECTION_DETAIL(name || "", namespace || "", connectionname || ""),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  if (loading) return <Loading />;

  const { connections } = data || {
    connections: { total: 0, connections: [] }
  };

  const renderNoRecordFound = () => {
    return (
      <NoDataFound
        type={"Connection"}
        name={connectionname || ""}
        routeLink={`/messaging-projects/${namespace}/${name}/${type}/connections`}
      />
    );
  };
  if (connections.total <= 0 || connections.connections.length <= 0) {
    return renderNoRecordFound();
  }

  const connection = connections.connections[0];

  //Change this logic
  const jvmObject =
    connection &&
    connection.spec &&
    connection.spec.properties &&
    connection.spec.properties.length > 0
      ? getSplitValue(
          getProductFilteredValue(connection.spec.properties, "platform")
        )
      : { jvm: "-", os: "-" };

  const getConnectionDetail = () => {
    const connectionDetail: IConnectionHeaderDetailProps = {
      hostname: connection && connection.spec.hostname,
      containerId: connection && connection.spec.containerId,
      version:
        connection &&
        getProductFilteredValue(connection.spec.properties, "version"),
      protocol: connection && connection.spec.protocol.toUpperCase(),
      encrypted: (connection && connection.spec.encrypted) || false,
      creationTimestamp: connection && connection.metadata.creationTimestamp,
      messageIn:
        connection &&
        getFilteredValue(connection.metrics, "enmasse_messages_in"),
      messageOut:
        connection &&
        getFilteredValue(connection.metrics, "enmasse_messages_out"),
      //Change this logic
      platform: jvmObject && jvmObject.jvm,
      os: jvmObject && jvmObject.os,
      product:
        connection &&
        connection.spec.properties &&
        connection.spec.properties.length > 0
          ? getProductFilteredValue(connection.spec.properties, "product")
          : "-"
    };
    return connectionDetail;
  };

  return (
    <>
      <ConnectionDetailHeader
        {...getConnectionDetail()}
        addressSpaceType={type}
      />
      <PageSection>
        <ConnectionLinksWithToolbar
          name={name}
          namespace={namespace}
          connectionName={connectionname}
        />
      </PageSection>
    </>
  );
}
