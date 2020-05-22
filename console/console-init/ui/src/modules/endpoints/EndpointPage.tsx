/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

import React from "react";
import { useDocumentTitle, useA11yRouteChange, Loading } from "use-patternfly";
import { PageSectionVariants, PageSection } from "@patternfly/react-core";
import { EndPointList } from "./components";
import { IEndpointResponse, IEndpointListResponse } from "schema/ResponseTypes";
import { useLocation, useParams } from "react-router";
import { RETURN_ALL_ENDPOINTS_FOR_ADDRESS_SPACE } from "graphql-module";
import { POLL_INTERVAL, FetchPolicy } from "constant";
import { IEndpointProtocol } from "schema/ResponseTypes";
import { useQuery } from "@apollo/react-hooks";
import { TablePagination } from "components";

export interface IEndPoint {
  name?: string;
  type?: string;
  host?: string;
  ports?: IEndpointProtocol[];
}

export default function AddressPage() {
  useDocumentTitle("Endpoint List");
  useA11yRouteChange();
  const location = useLocation();
  const { name, namespace } = useParams();
  const searchParams = new URLSearchParams(location.search);
  const page = parseInt(searchParams.get("page") || "", 10) || 1;
  const perPage = parseInt(searchParams.get("perPage") || "", 10) || 10;
  const { loading, data } = useQuery<IEndpointListResponse>(
    RETURN_ALL_ENDPOINTS_FOR_ADDRESS_SPACE(name, namespace, page, perPage),
    { pollInterval: POLL_INTERVAL, fetchPolicy: FetchPolicy.NETWORK_ONLY }
  );

  if (loading) {
    return <Loading />;
  }

  let messagingEndpoints: IEndpointResponse[] = [];
  if (data) {
    messagingEndpoints = data.messagingEndpoints.messagingEndpoints;
  }
  const endpointList: IEndPoint[] = messagingEndpoints.map(
    (endpoint: IEndpointResponse) => {
      const { metadata, status } = endpoint;
      return {
        name: metadata?.name,
        type: status?.type,
        host: status?.host,
        ports: status?.ports
      };
    }
  );

  const renderPagination = () => {
    if (data && data.total > 10) {
      return (
        <TablePagination
          itemCount={(data && data.total) || 10}
          variant={"top"}
          page={page}
          perPage={perPage}
        />
      );
    }
  };
  return (
    <PageSection variant={PageSectionVariants.light}>
      {renderPagination()}
      <EndPointList endpoints={endpointList} />
      {renderPagination()}
    </PageSection>
  );
}
