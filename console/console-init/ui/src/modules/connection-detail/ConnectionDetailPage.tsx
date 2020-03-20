/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import * as React from "react";
import {
  ConnectionDetailHeader,
  IConnectionHeaderDetailProps
} from "modules/connection-detail/components/ConnectionDetailHeader/ConnectionDetailHeader";
import {
  PageSection,
  Breadcrumb,
  BreadcrumbItem
} from "@patternfly/react-core";
import { useQuery } from "@apollo/react-hooks";
import { useParams } from "react-router";
import {
  Loading,
  useA11yRouteChange,
  useBreadcrumb,
  useDocumentTitle
} from "use-patternfly";
import { getFilteredValue } from "components/common/ConnectionListFormatter";
import { IConnectionDetailResponse } from "types/ResponseTypes";
import { Link } from "react-router-dom";
import { RETURN_CONNECTION_DETAIL } from "graphql-module/queries";
import { ConnectionDetailToolbar } from "./components";
import { POLL_INTERVAL, FetchPolicy } from "constants/constants";
import { NoDataFound } from "components/common/NoDataFound";

const getProductFilteredValue = (object: any[], value: string) => {
  if (object && object != null) {
    const filtered = object.filter(obj => obj.Key === value);
    if (filtered.length > 0) {
      return filtered[0].Value;
    }
  }
  return "-";
};

const getSplitValue = (value: string) => {
  let string1 = value.split(", OS: ");
  let string2 = string1[0].split("JVM:");
  let os, jvm;
  if (string1.length > 1) {
    os = string1[1];
  }
  if (string2.length > 0) {
    jvm = string2[1];
  }
  return { jvm: jvm, os: os };
};

export default function ConnectionDetailPage() {
  const { name, namespace, type, connectionname } = useParams();
  useDocumentTitle("Connection Details");
  const breadcrumb = React.useMemo(
    () => (
      <Breadcrumb>
        <BreadcrumbItem>
          <Link id="cdetail-link-home" to={"/"}>
            Home
          </Link>
        </BreadcrumbItem>
        <BreadcrumbItem>
          <Link
            id="cdetail-link-connections"
            to={`/address-spaces/${namespace}/${name}/${type}/connections`}
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
  useA11yRouteChange();
  const { loading, data } = useQuery<IConnectionDetailResponse>(
    RETURN_CONNECTION_DETAIL(name || "", namespace || "", connectionname || ""),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );
  if (loading) return <Loading />;

  const { connections } = data || {
    connections: { total: 0, connections: [] }
  };
  if (connections.connections.length <= 0) {
    return (
      <NoDataFound
        type={"Connection"}
        name={connectionname || ""}
        routeLink={`/address-spaces/${namespace}/${name}/${type}/connections`}
      />
    );
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
      connection && getFilteredValue(connection.metrics, "enmasse_messages_in"),
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

  return (
    <>
      <ConnectionDetailHeader {...connectionDetail} addressSpaceType={type} />
      <PageSection>
        <ConnectionDetailToolbar
          name={name}
          namespace={namespace}
          connectionName={connectionname}
        />
      </PageSection>
    </>
  );
}
